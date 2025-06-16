package com.denshlk.collapsefolders

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger

@Service
@State(
    name = "CollapseFoldersSettings",
    storages = [Storage("collapse-folders.xml")]
)
class CollapseFoldersSettings : PersistentStateComponent<CollapseFoldersSettings.State> {
    
    data class State(
        var folderCollapseEnabled: Boolean = true,
        var fileCollapseEnabled: Boolean = true,
        var folderCollapseThreshold: Int = 10,
        var fileCollapseThreshold: Int = 10
    )
    
    private var settings = State()
    
    companion object {
        private val LOG = Logger.getInstance(CollapseFoldersSettings::class.java)
        
        fun getInstance(): CollapseFoldersSettings {
            return ApplicationManager.getApplication()
                .getService(CollapseFoldersSettings::class.java)
        }
    }
    
    override fun getState(): State {
        LOG.debug("Getting settings state: folderEnabled=${settings.folderCollapseEnabled}, fileEnabled=${settings.fileCollapseEnabled}, folderThreshold=${settings.folderCollapseThreshold}, fileThreshold=${settings.fileCollapseThreshold}")
        return settings
    }
    
    override fun loadState(state: State) {
        LOG.debug("Loading settings state: folderEnabled=${state.folderCollapseEnabled}, fileEnabled=${state.fileCollapseEnabled}, folderThreshold=${state.folderCollapseThreshold}, fileThreshold=${state.fileCollapseThreshold}")
        this.settings = state
    }
} 