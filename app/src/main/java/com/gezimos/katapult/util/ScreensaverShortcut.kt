package com.gezimos.katapult.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import com.gezimos.katapult.lockscreen.KatapultDreamService

object ScreensaverShortcut {

    const val ALIAS = "com.gezimos.katapult.ScreensaverAlias"

    private fun component(context: Context) = ComponentName(context.packageName, ALIAS)

    fun isIconEnabled(context: Context): Boolean = try {
        context.packageManager.getComponentEnabledSetting(component(context)) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } catch (_: Exception) {
        false
    }

    fun applyPlatform(context: Context) {
        if (!DeviceHelper.isLineageOs()) return
        val pm = context.packageManager
        val dream = ComponentName(context, KatapultDreamService::class.java)
        try {
            if (pm.getComponentEnabledSetting(dream) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                pm.setComponentEnabledSetting(
                    dream,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        } catch (_: Exception) {}
        if (isIconEnabled(context)) setIconEnabled(context, false)
        try {
            context.getSystemService(ShortcutManager::class.java)?.disableShortcuts(listOf("screensaver"))
        } catch (_: Exception) {}
    }

    fun setIconEnabled(context: Context, enabled: Boolean) {
        val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        try {
            context.packageManager.setComponentEnabledSetting(
                component(context),
                state,
                PackageManager.DONT_KILL_APP,
            )
        } catch (_: Exception) {}
    }
}
