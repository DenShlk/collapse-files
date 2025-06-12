package com.denshlk.collapsefolders

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.BasePsiNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.VirtualFile
import java.util.Comparator

class CollapseFilesTreeStructureProvider : TreeStructureProvider, DumbAware {

    companion object {
        private const val DEFAULT_COLLAPSE_THRESHOLD = 10
        private val LOG = Logger.getInstance(CollapseFilesTreeStructureProvider::class.java)
    }

    override fun modify(
        parent: AbstractTreeNode<*>,
        children: Collection<AbstractTreeNode<*>>,
        settings: ViewSettings
    ): Collection<AbstractTreeNode<*>> {
        val project = parent.project ?: return children

        LOG.info("=== Starting tree structure modification ===")
        LOG.info("Parent: ${getVirtualFile(parent)?.path ?: "unknown"}")
        LOG.info("Children count: ${children.size}")

        if (!isCollapsingEnabled()) {
            LOG.info("Collapsing is disabled, returning original children")
            return children
        }

        val service = try {
            CollapseFilesService.getInstance(project)
        } catch (e: Exception) {
            LOG.info("Service not available, returning original children: ${e.message}")
            return children
        }

        // Try to get the proper comparator from the project view pane
        val comparator = service.getCurrentComparator()
        if (comparator == null) {
            LOG.info("Comparator not available, returning original children")
            return children
        }

        // randomize order to test
        val shuffledChildren = children.shuffled()
        
        // Sort children to ensure consistent order using the same logic as IntelliJ
        // since file name can be used for sorting, we need to init all children
        shuffledChildren.forEach { it.update() }
        val sortedChildren = shuffledChildren.sortedWith(comparator)

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
                LOG.info("  -> Not a file or folder, flushing accumulated items and adding directly")
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
            LOG.info("Processing child: ${childFile?.name ?: "unknown"} (${childFile?.path ?: "no path"})")

            when {
                currentItemType != lastItemType -> {
                    LOG.info("  -> Item type changed from $lastItemType to $currentItemType")
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
                        LOG.info("  -> Can collapse ${currentItemType.displayName}, adding to consecutive list")
                        consecutiveItems.add(child)
                    } else {
                        LOG.info("  -> Cannot collapse ${currentItemType.displayName}, flushing and adding directly")
                        flushConsecutiveItems(consecutiveItems, result, currentItemType, parent, settings)
                        result.add(child)
                    }
                }
            }
        }

        // Flush any remaining items
        LOG.info("Flushing remaining accumulated items")
        if (lastItemType != null) {
            flushConsecutiveItems(consecutiveItems, result, lastItemType, parent, settings)
        }

        LOG.info("Final result count: ${result.size}")
        LOG.info("=== Tree structure modification completed ===")

        return result
    }

    private fun canCollapseItem(node: AbstractTreeNode<*>, openFilesTracker: OpenFilesTracker): Boolean {
        val file = getVirtualFile(node) ?: return false
        val canCollapse = !openFilesTracker.isFileOpen(file)
        LOG.info("Can collapse ${if (file.isDirectory) "folder" else "file"} '${file.name}': $canCollapse")
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
        LOG.info("Flushing ${items.size} consecutive ${itemType.displayName} items (threshold: $threshold)")

        when {
            items.size >= threshold -> {
                val project = parent.project
                if (project != null) {
                    val service = CollapseFilesService.getInstance(project)
                    val nodeKey = service.generateCollapsedNodeKey(items, itemType)

                    if (service.isNodeExpanded(nodeKey)) {
                        LOG.info("  -> Group is expanded, adding ${items.size} items individually")
                        result.addAll(items)
                    } else {
                        LOG.info("  -> Creating collapsed node for ${items.size} items")
                        val node = CollapsedItemsNode(nodeKey, items.toList(), itemType, parent, viewSettings)
                        result.add(node)
                    }
                } else {
                    LOG.info("  -> No project context!")
                }
            }

            items.isNotEmpty() -> {
                LOG.info("  -> Below threshold, adding ${items.size} items individually")
                result.addAll(items)
            }

            else -> {
                LOG.info("  -> No items to flush")
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
        val settings = CollapseFoldersSettings.getInstance()
        val state = settings.getState()
        val enabled = state.folderCollapseEnabled || state.fileCollapseEnabled
        LOG.info("Collapsing enabled: $enabled (folders: ${state.folderCollapseEnabled}, files: ${state.fileCollapseEnabled})")
        return enabled
    }

    private fun getCollapseThreshold(itemType: ItemType): Int {
        val settings = CollapseFoldersSettings.getInstance()
        val state = settings.getState()
        val threshold = when (itemType) {
            ItemType.FOLDER -> if (state.folderCollapseEnabled) state.folderCollapseThreshold else Int.MAX_VALUE
            ItemType.FILE -> if (state.fileCollapseEnabled) state.fileCollapseThreshold else Int.MAX_VALUE
        }
        LOG.info("Collapse threshold for ${itemType.displayName}: $threshold")
        return threshold
    }

} 