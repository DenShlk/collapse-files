package com.denshlk.collapsefiles

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/*
Tracks which files are open in editor.

TODO: https://plugins.jetbrains.com/docs/intellij/virtual-file.html#are-there-any-utilities-for-analyzing-and-manipulating-virtual-files
For storing a large set of Virtual Files, use the dedicated VfsUtilCore.createCompactVirtualFileSet() method.
 */
class OpenFilesTracker(private val project: Project) : FileEditorManagerListener {
    private val openFileCounts = mutableMapOf<VirtualFile, Int>()

    companion object {
        private val LOG = Logger.getInstance(OpenFilesTracker::class.java)
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        LOG.debug("File opened: ${file.path}")
        ascendFilePath(file) { pathFile ->
            openFileCounts[pathFile] = openFileCounts.getOrDefault(pathFile, 0) + 1
        }
        refreshProjectView()
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        LOG.debug("File closed: ${file.path}")
        ascendFilePath(file) { pathFile ->
            val currentCount = openFileCounts.getOrDefault(pathFile, 0)
            if (currentCount <= 1) {
                openFileCounts.remove(pathFile)
            } else {
                openFileCounts[pathFile] = currentCount - 1
            }
        }
        refreshProjectView()
    }

    private fun ascendFilePath(file: VirtualFile, iter: (VirtualFile) -> Unit) {
        var parent: VirtualFile? = file
        while (parent != null) {
            iter(parent)
            parent = parent.parent
        }
    }
    
    fun isFileOpen(file: VirtualFile): Boolean {
        val isOpen = openFileCounts.containsKey(file)
        LOG.debug("Checking if file is open: ${file.path} -> $isOpen")
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