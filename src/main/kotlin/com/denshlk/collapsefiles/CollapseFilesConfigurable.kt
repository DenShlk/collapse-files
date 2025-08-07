package com.denshlk.collapsefiles

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.JRadioButton
import com.intellij.ui.components.JBLabel

class CollapseFilesConfigurable : SearchableConfigurable, com.intellij.openapi.options.Configurable.NoScroll {
    private var panel: JPanel? = null

    private val enableFolderCollapse = JBCheckBox("Enable folder collapsing")
    private val folderThreshold = JSpinner(SpinnerNumberModel(10, 2, 100, 1))

    private val enableFileCollapse = JBCheckBox("Enable file collapsing")
    private val fileThreshold = JSpinner(SpinnerNumberModel(10, 2, 100, 1))

    // Label style
    private val labelStyleCompact =
        JRadioButton("Compact labels like 'file01.txt ... file04.txt (4 files)'. Warning: typing to navigate won't search in ommited names.")
    private val labelStyleFull = JRadioButton("Full labels like 'file01.txt|file02.txt|file03.txt|file04.txt'")

    override fun getId(): String = "com.denshlk.collapsefiles.settings"

    override fun getDisplayName(): String = "Collapse Files"

    override fun createComponent(): JComponent {
        if (panel == null) {
            panel = FormBuilder.createFormBuilder()
                // Folder collapsing
                .addComponent(enableFolderCollapse)
                .addLabeledComponent("Minimum consecutive folders to collapse:", folderThreshold, 1, false)
                .addVerticalGap(8)
                // File collapsing
                .addComponent(enableFileCollapse)
                .addLabeledComponent("Minimum consecutive files to collapse:", fileThreshold, 1, false)
                .addVerticalGap(12)
                // Label style
                .addComponent(JBLabel("Collapsed group display:"))
                .addComponent(labelStyleCompact)
                .addComponent(labelStyleFull)

                .addComponentFillVertically(JPanel(), 0)
                .panel

            val updateEnabledState = {
                folderThreshold.isEnabled = enableFolderCollapse.isSelected
                fileThreshold.isEnabled = enableFileCollapse.isSelected
            }
            enableFolderCollapse.addChangeListener { updateEnabledState() }
            enableFileCollapse.addChangeListener { updateEnabledState() }
        }
        reset()
        return panel as JPanel
    }

    override fun isModified(): Boolean {
        val state = CollapseFilesSettings.getInstance().state
        val compactSelected = labelStyleCompact.isSelected
        return state.folderCollapseEnabled != enableFolderCollapse.isSelected ||
                state.fileCollapseEnabled != enableFileCollapse.isSelected ||
                state.folderCollapseThreshold != (folderThreshold.value as Int) ||
                state.fileCollapseThreshold != (fileThreshold.value as Int) ||
                state.compactCollapsedLabels != compactSelected
    }

    override fun apply() {
        val service = CollapseFilesSettings.getInstance()
        val state = service.state.copy(
            folderCollapseEnabled = enableFolderCollapse.isSelected,
            fileCollapseEnabled = enableFileCollapse.isSelected,
            folderCollapseThreshold = (folderThreshold.value as Int),
            fileCollapseThreshold = (fileThreshold.value as Int),
            compactCollapsedLabels = labelStyleCompact.isSelected
        )
        service.loadState(state)

        // Refresh all open projects to reflect new settings immediately
        ProjectManager.getInstance().openProjects.forEach { project ->
            try {
                val projectView = com.intellij.ide.projectView.ProjectView.getInstance(project)
                projectView.currentProjectViewPane?.updateFromRoot(true)
            } catch (_: Throwable) {
                // ignore
            }
        }
    }

    override fun reset() {
        val state = CollapseFilesSettings.getInstance().state
        enableFolderCollapse.isSelected = state.folderCollapseEnabled
        enableFileCollapse.isSelected = state.fileCollapseEnabled
        folderThreshold.value = state.folderCollapseThreshold
        fileThreshold.value = state.fileCollapseThreshold

        if (state.compactCollapsedLabels) labelStyleCompact.isSelected = true else labelStyleFull.isSelected = true

        folderThreshold.isEnabled = enableFolderCollapse.isSelected
        fileThreshold.isEnabled = enableFileCollapse.isSelected
    }

    override fun disposeUIResources() {
        panel = null
    }
}
