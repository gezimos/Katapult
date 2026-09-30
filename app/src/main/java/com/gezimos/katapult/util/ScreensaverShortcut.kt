package com.gezimos.katapult.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object ScreensaverShortcut {

    const val ALIAS = "com.gezimos.katapult.ScreensaverAlias"

    private fun component(context: Context) = ComponentName(context.packageName, ALIAS)

    fun isIconEnabled(context: Context): Boolean = try {
        context.packageManager.getComponentEnabledSetting(component(context)) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } catch (_: Exception) {
        false
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
