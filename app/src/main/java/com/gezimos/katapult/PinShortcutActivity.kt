package com.gezimos.katapult

import android.app.Activity
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.widget.Toast
import com.gezimos.katapult.util.PrefsManager
import com.gezimos.katapult.util.ShortcutHelper

class PinShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handlePinRequest(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePinRequest(intent)
        finish()
    }

    private fun handlePinRequest(intent: Intent?) {
        if (intent?.action != LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT) return
        try {
            val launcherApps = getSystemService(LauncherApps::class.java)
            val request = launcherApps.getPinItemRequest(intent) ?: return
            if (request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) return
            val info = request.shortcutInfo ?: return

            val label = ShortcutHelper.label(info)
            val prefs = PrefsManager(applicationContext)
            val added = prefs.addSavedShortcut(info.`package`, info.id, label)
            val accepted = try {
                request.accept()
            } catch (_: Exception) {
                false
            }
            if (!accepted) {
                if (added) prefs.removeSavedShortcut(info.`package`, info.id)
                return
            }
            ShortcutHelper.syncPins(applicationContext, prefs, info.`package`)
            Toast.makeText(
                applicationContext,
                getString(
                    if (added) R.string.shortcut_pinned else R.string.shortcut_already_added,
                    label,
                ),
                Toast.LENGTH_SHORT,
            ).show()
        } catch (_: Exception) {}
    }
}
