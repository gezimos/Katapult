package com.gezimos.katapult.model

data class AppModel(
    val packageName: String,
    val label: String,
    val activityName: String,
    val shortcutId: String = "",
) {
    val key: String = if (shortcutId.isEmpty()) packageName else "$packageName|$shortcutId"

    val isFolder: Boolean get() = packageName.startsWith(FOLDER_PREFIX)

    val folderId: String get() = packageName.removePrefix(FOLDER_PREFIX)

    companion object {
        const val FOLDER_PREFIX = "folder:"
        const val FOLDER_LIMIT = 9
    }
}
