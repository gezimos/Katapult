package com.gezimos.katapult.util

import android.content.Context
import android.provider.Settings

object BrightnessHelper {

    fun toggleBrightness(context: Context, prefs: PrefsManager) {
        if (!Settings.System.canWrite(context)) {
            context.startActivity(
                android.content.Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            return
        }

        val current = Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            128
        )

        if (current > 1) {
            prefs.lastBrightness = current
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 1)
        } else {
            val restore = prefs.lastBrightness.coerceIn(2, 255)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, restore)
        }
    }
}
