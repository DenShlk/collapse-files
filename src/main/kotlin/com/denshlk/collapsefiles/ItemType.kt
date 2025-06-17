package com.denshlk.collapsefiles

import com.intellij.icons.AllIcons
import javax.swing.Icon

enum class ItemType(val displayName: String, val icon: Icon) {
    FOLDER("folders", AllIcons.Nodes.Folder),
    FILE("files", AllIcons.FileTypes.Any_type)
} 