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
        var needRefresh = false
        ascendPath(path) { parentPath ->
            val currentCount = openPathCounts.getOrDefault(parentPath, 0)
            if (currentCount <= 0) {
                needRefresh = true
            }
            openPathCounts[parentPath] = currentCount + 1
        }
        if (needRefresh) {
            refreshProjectView()
        }
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        pathOpened(file.toNioPath())
    }

    fun pathClosed(path: Path) {
        LOG.debug("Path closed: $path")
        var needRefresh = false
        ascendPath(path) { parentFile ->
            val currentCount = openPathCounts.getOrDefault(parentFile, 0)
            if (currentCount <= 1) {
                openPathCounts.remove(parentFile)
                needRefresh = true
            } else {
                openPathCounts[parentFile] = currentCount - 1
            }
        }
        if (needRefresh) {
            refreshProjectView()
        }
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
        LOG.debug("Refreshing project view")
        ApplicationManager.getApplication().invokeLater {
            val projectView = ProjectView.getInstance(project)
            projectView.currentProjectViewPane?.updateFromRoot(true)
            LOG.debug("Project view refresh completed")
        }
    }
} 