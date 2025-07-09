package com.denshlk.collapsefiles

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

/*
Tracks which files are open in editor.

TODO: https://plugins.jetbrains.com/docs/intellij/virtual-file.html#are-there-any-utilities-for-analyzing-and-manipulating-virtual-files
For storing a large set of Virtual Files, use the dedicated VfsUtilCore.createCompactVirtualFileSet() method.
 */
class OpenFilesTracker(private val project: Project) : FileEditorManagerListener {
    private val openPathCounts = mutableMapOf<Path, Int>()

    companion object {
        private val LOG = Logger.getInstance(OpenFilesTracker::class.java)
    }

    fun pathOpened(path: Path) {
        LOG.debug("Path opened: $path")
        ascendPath(path) { parentPath ->
            openPathCounts[parentPath] = openPathCounts.getOrDefault(parentPath, 0) + 1
        }
        refreshProjectView()
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        pathOpened(file.toNioPath())
    }

    fun pathClosed(path: Path) {
        LOG.debug("Path closed: $path")
        ascendPath(path) { pathFile ->
            val currentCount = openPathCounts.getOrDefault(pathFile, 0)
            if (currentCount <= 1) {
                openPathCounts.remove(pathFile)
            } else {
                openPathCounts[pathFile] = currentCount - 1
            }
        }
        refreshProjectView()
    }

    fun fileClosed(file: VirtualFile) {
        pathClosed(file.toNioPath())
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        fileClosed(file)
    }

    private fun ascendPath(path: Path, iter: (Path) -> Unit) {
        var parent: Path? = path
        while (parent != null) {
            iter(parent)
            parent = parent.parent
        }
    }
    
    fun isPathOpen(path: Path): Boolean {
        val isOpen = openPathCounts.containsKey(path)
        LOG.debug("Checking if file is open: $path -> $isOpen")
        return isOpen
    }
    
    private fun refreshProjectView() {
        // TODO: need to rethink if we need to do full refresh for each file change (may be make it incremental?)
        LOG.debug("Refreshing project view")
        ApplicationManager.getApplication().invokeLater {
            val projectView = ProjectView.getInstance(project)
            projectView.currentProjectViewPane?.updateFromRoot(true)
            LOG.debug("Project view refresh completed")
        }
    }
} 