package com.gezimos.katapult.util

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Process
import com.gezimos.katapult.model.AppModel

object ShortcutHelper {

    private fun launcherApps(context: Context): LauncherApps =
        context.getSystemService(LauncherApps::class.java)

    fun hasHostPermission(context: Context): Boolean = try {
        launcherApps(context).hasShortcutHostPermission()
    } catch (_: Exception) {
        false
    }

    fun queryShortcuts(context: Context, packageName: String): List<ShortcutInfo> = try {
        val query = LauncherApps.ShortcutQuery().apply {
            setPackage(packageName)
            setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        }
        launcherApps(context).getShortcuts(query, Process.myUserHandle())
            .orEmpty()
            .filter { it.isEnabled }
            .distinctBy { it.id }
    } catch (_: Exception) {
        emptyList()
    }

    fun label(info: ShortcutInfo): String =
        info.shortLabel?.toString() ?: info.longLabel?.toString() ?: info.id

    fun syncPins(context: Context, prefs: PrefsManager, packageName: String) {
        try {
            val ids = prefs.getSavedShortcuts()
                .filter { it.packageName == packageName }
                .map { it.shortcutId }
            launcherApps(context).pinShortcuts(packageName, ids, Process.myUserHandle())
        } catch (_: Exception) {}
    }

    fun launch(context: Context, packageName: String, shortcutId: String): Boolean = try {
        launcherApps(context).startShortcut(
            packageName, shortcutId, null, null, Process.myUserHandle(),
        )
        true
    } catch (_: Exception) {
        false
    }

    fun installedShortcuts(context: Context, prefs: PrefsManager): List<AppModel> {
        val saved = prefs.getSavedShortcuts()
        if (saved.isEmpty()) return emptyList()
        val pm = context.packageManager
        return saved.filter {
            try {
                pm.getApplicationInfo(it.packageName, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun syncAllPins(context: Context, prefs: PrefsManager) {
        if (!hasHostPermission(context)) return
        for (pkg in prefs.getSavedShortcuts().map { it.packageName }.distinct()) {
            syncPins(context, prefs, pkg)
        }
    }
}
