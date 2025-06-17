package com.denshlk.collapsefiles

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.BasePsiNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.VirtualFile

class CollapseFilesTreeStructureProvider : TreeStructureProvider, DumbAware {

    companion object {
        private val LOG = Logger.getInstance(CollapseFilesTreeStructureProvider::class.java)
    }

    override fun modify(
        parent: AbstractTreeNode<*>,
        children: Collection<AbstractTreeNode<*>>,
        settings: ViewSettings
    ): Collection<AbstractTreeNode<*>> {
        val project = parent.project ?: return children

        LOG.debug("=== Starting tree structure modification ===")
        LOG.debug("Parent: ${getVirtualFile(parent)?.path ?: "unknown"}")
        LOG.debug("Children count: ${children.size}")

        if (!isCollapsingEnabled()) {
            LOG.debug("Collapsing is disabled, returning original children")
            return children
        }

        val service = try {
            CollapseFilesService.getInstance(project)
        } catch (e: Exception) {
            LOG.warn("Service not available, returning original children: ${e.message}")
            return children
        }

        // Try to get the proper comparator from the project view pane
        val comparator = service.getCurrentComparator()
        if (comparator == null) {
            LOG.warn("Comparator not available, returning original children")
            return children
        }
        
        // Sort children to ensure consistent order using the same logic as IntelliJ
        // since file name can be used for sorting, we need to init all children
        children.forEach { it.update() }
        val sortedChildren = children.sortedWith(comparator)

        val result = mutableListOf<AbstractTreeNode<*>>()
        val consecutiveItems = mutableListOf<BasePsiNode<*>>()
        var lastItemType: ItemType? = null

        for (child in sortedChildren) {
            val currentItemType = when (child) {
                is PsiDirectoryNode -> ItemType.FOLDER
                is PsiFileNode -> ItemType.FILE
                else -> null
            }
            if (currentItemType == null) {
                LOG.debug("  -> Not a file or folder, flushing accumulated items and adding directly")
                if (lastItemType != null) {
                    flushConsecutiveItems(consecutiveItems, result, lastItemType, parent, settings)
                }
                result.add(child)
                lastItemType = null
                continue
            }
            // cast
            child as BasePsiNode<*>
            val childFile = child.virtualFile
            LOG.debug("Processing child: ${childFile?.name ?: "unknown"} (${childFile?.path ?: "no path"})")

            when {
                currentItemType != lastItemType -> {
                    LOG.debug("  -> Item type changed from $lastItemType to $currentItemType")
                    if (lastItemType != null) {
                        flushConsecutiveItems(consecutiveItems, result, lastItemType, parent, settings)
                    }
                    if (canCollapseItem(child, service.openFilesTracker)) {
                        consecutiveItems.add(child)
                    } else {
                        result.add(child)
                    }
                    lastItemType = currentItemType
                }
                else -> {
                    if (canCollapseItem(child, service.openFilesTracker)) {
                        LOG.debug("  -> Can collapse ${currentItemType.displayName}, adding to consecutive list")
                        consecutiveItems.add(child)
                    } else {
                        LOG.debug("  -> Cannot collapse ${currentItemType.displayName}, flushing and adding directly")
                        flushConsecutiveItems(consecutiveItems, result, currentItemType, parent, settings)
                        result.add(child)
                    }
                }
            }
        }

        // Flush any remaining items
        LOG.debug("Flushing remaining accumulated items")
        if (lastItemType != null) {
            flushConsecutiveItems(consecutiveItems, result, lastItemType, parent, settings)
        }

        LOG.debug("Final result count: ${result.size}")
        LOG.debug("=== Tree structure modification completed ===")

        return result
    }

    private fun canCollapseItem(node: AbstractTreeNode<*>, openFilesTracker: OpenFilesTracker): Boolean {
        val file = getVirtualFile(node) ?: return false
        val canCollapse = !openFilesTracker.isFileOpen(file)
        LOG.debug("Can collapse ${if (file.isDirectory) "folder" else "file"} '${file.name}': $canCollapse")
        return canCollapse
    }

    private fun flushConsecutiveItems(
        items: MutableList<BasePsiNode<*>>,
        result: MutableList<AbstractTreeNode<*>>,
        itemType: ItemType,
        parent: AbstractTreeNode<*>,
        viewSettings: ViewSettings
    ) {
        val threshold = getCollapseThreshold(itemType)
        LOG.debug("Flushing ${items.size} consecutive ${itemType.displayName} items (threshold: $threshold)")

        when {
            items.size >= threshold -> {
                val project = parent.project
                if (project != null) {
                    val service = CollapseFilesService.getInstance(project)
                    val nodeKey = service.generateCollapsedNodeKey(items, itemType)

                    if (service.isNodeExpanded(nodeKey)) {
                        LOG.debug("  -> Group is expanded, adding ${items.size} items individually")
                        result.addAll(items)
                    } else {
                        LOG.debug("  -> Creating collapsed node for ${items.size} items")
                        val node = CollapsedItemsNode(nodeKey, items.toList(), itemType, parent, viewSettings)
                        result.add(node)
                    }
                } else {
                    LOG.warn("  -> No project context!")
                }
            }

            items.isNotEmpty() -> {
                LOG.debug("  -> Below threshold, adding ${items.size} items individually")
                result.addAll(items)
            }
        }
        items.clear()
    }

    private fun getVirtualFile(node: AbstractTreeNode<*>): VirtualFile? {
        return when (node) {
            is PsiFileNode -> node.virtualFile
            is PsiDirectoryNode -> node.virtualFile
            else -> null
        }
    }

    private fun isCollapsingEnabled(): Boolean {
        val settings = CollapseFilesSettings.getInstance()
        val state = settings.getState()
        val enabled = state.folderCollapseEnabled || state.fileCollapseEnabled
        LOG.debug("Collapsing enabled: $enabled (folders: ${state.folderCollapseEnabled}, files: ${state.fileCollapseEnabled})")
        return enabled
    }

    private fun getCollapseThreshold(itemType: ItemType): Int {
        val settings = CollapseFilesSettings.getInstance()
        val state = settings.getState()
        val threshold = when (itemType) {
            ItemType.FOLDER -> if (state.folderCollapseEnabled) state.folderCollapseThreshold else Int.MAX_VALUE
            ItemType.FILE -> if (state.fileCollapseEnabled) state.fileCollapseThreshold else Int.MAX_VALUE
        }
        LOG.debug("Collapse threshold for ${itemType.displayName}: $threshold")
        return threshold
    }

} 