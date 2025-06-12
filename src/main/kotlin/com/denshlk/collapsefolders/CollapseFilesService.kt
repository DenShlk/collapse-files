package com.denshlk.collapsefolders

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.impl.nodes.BasePsiNode
import com.intellij.ide.projectView.impl.AbstractProjectViewPane
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ide.FrameStateListener
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.WindowManager
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.util.Comparator
import javax.swing.JComponent

@Service(Service.Level.PROJECT)
class CollapseFilesService(private val project: Project) {
    val openFilesTracker = OpenFilesTracker(project)

    // TODO: Periodic cleanup
    private val expandedNodes = mutableSetOf<String>()

    companion object {
        private val LOG = Logger.getInstance(CollapseFilesService::class.java)

        fun getInstance(project: Project): CollapseFilesService {
            return project.getService(CollapseFilesService::class.java)
        }
    }

    init {
        LOG.info("Initializing CollapseFoldersService for project: ${project.name}")

        // Load currently open files on startup
        loadCurrentlyOpenFiles()

        // Register file editor listener
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            openFilesTracker
        )
        LOG.info("Registered file editor listener")

        // Listen to project structure changes
        project.messageBus.connect().subscribe(
            ModuleRootListener.TOPIC,
            object : ModuleRootListener {
                override fun rootsChanged(event: ModuleRootEvent) {
                    LOG.info("Project roots changed, refreshing project view")
                    refreshProjectView()
                }
            }
        )
        LOG.info("Registered module root listener")

        // Set up project view focus tracking
        setupProjectViewFocusTracking()

        LOG.info("CollapseFoldersService initialization completed")
    }

    private fun loadCurrentlyOpenFiles() {
        LOG.info("Loading currently open files on startup")
        val fileEditorManager = FileEditorManager.getInstance(project)
        val openFiles = fileEditorManager.openFiles

        LOG.info("Found ${openFiles.size} currently open files")
        openFiles.forEach { file ->
            LOG.info("Processing already open file: ${file.path}")
            // Simulate file opening to update tracker
            openFilesTracker.fileOpened(fileEditorManager, file)
        }

        if (openFiles.isNotEmpty()) {
            refreshProjectView()
        }
    }

    private fun setupProjectViewFocusTracking() {
        ApplicationManager.getApplication().invokeLater {
            try {
                val projectView = ProjectView.getInstance(project)
                val projectViewPane = projectView.currentProjectViewPane

                if (projectViewPane != null) {
                    val tree = projectViewPane.tree
                    tree.addFocusListener(object : FocusListener {
                        override fun focusGained(e: FocusEvent?) {}

                        override fun focusLost(e: FocusEvent?) {
                            LOG.info("Project view lost focus, rebuilding project view")
                            expandedNodes.clear()
                            refreshProjectView()
                        }
                    })
                    LOG.info("Successfully added focus listener to project view tree")
                } else {
                    LOG.info("Project view pane not available yet, will retry later")
                }
            } catch (e: Exception) {
                LOG.warn("Could not set up project view focus tracking: ${e.message}")
            }
        }
    }

    private fun refreshProjectView() {
        LOG.info("Refreshing project view from service")
        ApplicationManager.getApplication().invokeLater {
            val projectView = ProjectView.getInstance(project)
            projectView.currentProjectViewPane?.updateFromRoot(true)
            LOG.info("Service project view refresh completed")
        }
    }

    /**
     * Gets the current comparator used by the project view pane for sorting nodes.
     * This ensures we use the same sorting logic as IntelliJ, respecting user settings.
     */
    fun getCurrentComparator(): Comparator<NodeDescriptor<*>>? {
        return try {
            val projectView = ProjectView.getInstance(project)
            val projectViewPane = projectView.currentProjectViewPane

            if (projectViewPane is AbstractProjectViewPane) {
                LOG.info("Successfully retrieved comparator from project view pane")
                // Hacky hack, method is protected
                val method = AbstractProjectViewPane::class.java.getDeclaredMethod("createComparator")
                method.isAccessible = true
                method.invoke(projectViewPane) as Comparator<NodeDescriptor<*>>
            } else {
                LOG.info("Project view pane is not available or not an AbstractProjectViewPane")
                null
            }
        } catch (e: Exception) {
            LOG.warn("Could not get comparator from project view pane: ${e.message}")
            null
        }
    }

    fun isNodeExpanded(nodeSignature: String): Boolean {
        return expandedNodes.contains(nodeSignature)
    }

    fun setNodeExpanded(nodeKey: String, expanded: Boolean) {
        if (expanded) {
            expandedNodes.add(nodeKey)
            LOG.info("Node expanded: $nodeKey")
        } else {
            expandedNodes.remove(nodeKey)
            LOG.info("Node collapsed: $nodeKey")
        }
    }

    fun generateCollapsedNodeKey(collapsedItems: List<BasePsiNode<*>>, itemType: ItemType): String {
        // Create a signature based on the collapsed items and their type
        var items = ""
        if (collapsedItems.isNotEmpty()) {
            val from = collapsedItems.first().virtualFile?.path ?: ""
            val to = collapsedItems.last().virtualFile?.path ?: ""
            items = "${from}...${to}"
        }
        return "${itemType.displayName}_$items"
    }
}

