package com.denshlk.collapsefolders

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.NodeSortSettings
import com.intellij.ide.projectView.NodeSortOrder
import com.intellij.ide.projectView.impl.nodes.BasePsiNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile

class CollapsedItemsNode(
    private val key: String,
    private val children: List<BasePsiNode<*>>,
    private val itemType: ItemType,
    parentNode: AbstractTreeNode<*>,
    viewSetting: ViewSettings,
) : ProjectViewNode<Any>(parentNode.project, key,  viewSetting) {

    private val service: CollapseFilesService = CollapseFilesService.getInstance(project)
    private val projectView: ProjectView = ProjectView.getInstance(project)

    companion object {
        private val LOG = Logger.getInstance(CollapsedItemsNode::class.java)
    }
    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        // Collapsed nodes never show children - they get replaced by individual items when expanded
        return emptyList()
    }

    override fun update(presentation: PresentationData) {
        val from = children.first().virtualFile?.name ?: "?"
        val to = children.last().virtualFile?.name ?: "?"
        val text = "$from ... $to (${children.size} ${itemType.displayName})"
        LOG.debug("Updating presentation for CollapsedItemsNode: $text")
        presentation.presentableText = text
        presentation.setIcon(itemType.icon)
        presentation.tooltip = buildTooltip()
    }

    override fun canNavigate(): Boolean = true
    override fun canNavigateToSource(): Boolean = false
    override fun isAlwaysLeaf(): Boolean = true

    // Handle click to expand - this will cause TreeStructureProvider to show individual items instead of this node
    override fun navigate(requestFocus: Boolean) {
        LOG.debug("CollapsedItemsNode clicked: marking as expanded")
        service.setNodeExpanded(key, true)
        projectView.currentProjectViewPane?.updateFromRoot(false)
        LOG.debug("Project view updated - TreeStructureProvider will now show individual items instead of collapsed node")
    }

    override fun contains(file: VirtualFile): Boolean {
        return children.any { child ->
            val childFile = child.virtualFile
            childFile == file || (childFile?.isDirectory == true && file.path.startsWith(childFile.path))
        }
    }

    // Proxy all sorting-related methods to the first child to ensure proper positioning in the tree
    override fun getWeight(): Int {
        return children.firstOrNull()?.weight ?: super.weight
    }

    override fun getManualOrderKey(): Comparable<*>? {
        return children.firstOrNull()?.manualOrderKey ?: super.manualOrderKey
    }

    override fun getSortOrder(settings: NodeSortSettings): NodeSortOrder {
        return children.firstOrNull()?.getSortOrder(settings) ?: super.getSortOrder(settings)
    }

    override fun getTypeSortWeight(sortByType: Boolean): Int {
        return children.firstOrNull()?.getTypeSortWeight(sortByType) ?: super.getTypeSortWeight(sortByType)
    }

    override fun getSortKey(): Comparable<*>? {
        return children.firstOrNull()?.sortKey ?: super.sortKey
    }

    override fun getTypeSortKey(): Comparable<*>? {
        return children.firstOrNull()?.typeSortKey ?: super.typeSortKey
    }

    override fun getTimeSortKey(): Comparable<*>? {
        return children.firstOrNull()?.timeSortKey ?: super.timeSortKey
    }

    override fun getQualifiedNameSortKey(): String? {
        return children.firstOrNull()?.qualifiedNameSortKey ?: super.qualifiedNameSortKey
    }

    private fun buildTooltip(): String {
        val itemNames = children.map { node ->
            node.virtualFile?.name ?: "?"
        }.take(5)

        val tooltip = StringBuilder("Collapsed ${itemType.displayName}:")
        itemNames.forEach { name ->
            tooltip.append("\n• $name")
        }
        if (children.size > 5) {
            tooltip.append("\n... and ${children.size - 5} more")
        }
        return tooltip.toString()
    }
} 