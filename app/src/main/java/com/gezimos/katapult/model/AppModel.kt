package com.gezimos.katapult.model

data class AppModel(
    val packageName: String,
    val label: String,
    val activityName: String,
    val shortcutId: String = "",
) {
    val key: String get() = if (shortcutId.isEmpty()) packageName else "$packageName|$shortcutId"
}
