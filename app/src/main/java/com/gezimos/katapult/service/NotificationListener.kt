package com.gezimos.katapult.service

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.gezimos.katapult.util.AudioWidgetHelper
import com.gezimos.katapult.util.DeviceHelper
import java.util.concurrent.ConcurrentHashMap

class NotificationListener : NotificationListenerService() {

    companion object {

        private val keysByPkg = ConcurrentHashMap<String, MutableSet<String>>()

        private val clearedKeys = ConcurrentHashMap<String, MutableSet<String>>()

        var onCountsChanged: (() -> Unit)? = null
        private val skipPackages = if (DeviceHelper.isMuditaKompakt()) DirectBadgeHelper.DIRECT_PACKAGES else emptySet()

        @Volatile
        private var instance: NotificationListener? = null

        private fun newKeySet(): MutableSet<String> =
            java.util.concurrent.ConcurrentHashMap.newKeySet()

        fun getCount(packageName: String): Int = keysByPkg[packageName]?.size ?: 0

        fun getAllCounts(): Map<String, Int> = keysByPkg.mapValues { it.value.size }

        fun cancelFor(packageName: String) {
            val self = instance ?: return
            if (packageName in skipPackages) return

            val keys: List<String> = try {
                self.activeNotifications
                    .filter { it.packageName == packageName }
                    .map { it.key }
            } catch (_: Exception) {
                emptyList()
            }

            if (keys.isNotEmpty()) {
                clearedKeys.getOrPut(packageName) { newKeySet() }.addAll(keys)
            }

            keysByPkg.remove(packageName)
            onCountsChanged?.invoke()

            if (keys.isEmpty()) return


            try {
                self.cancelNotifications(keys.toTypedArray())
            } catch (_: Exception) {
                for (key in keys) {
                    try { self.cancelNotification(key) } catch (_: Exception) {}
                }
            }
        }
    }

    private fun isGroupSummary(sbn: StatusBarNotification): Boolean =
        sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0

    private fun shouldCount(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName in skipPackages) return false
        if (isGroupSummary(sbn)) return false
        if (clearedKeys[sbn.packageName]?.contains(sbn.key) == true) return false
        return true
    }

    override fun onListenerConnected() {
        instance = this
        keysByPkg.clear()
        for (sbn in activeNotifications) {
            if (!shouldCount(sbn)) continue
            keysByPkg.getOrPut(sbn.packageName) { newKeySet() }.add(sbn.key)
        }
        onCountsChanged?.invoke()

        val componentName = ComponentName(this, NotificationListener::class.java)
        AudioWidgetHelper.getInstance(this).initialize(componentName)
    }

    override fun onListenerDisconnected() {
        instance = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!shouldCount(sbn)) return
        val set = keysByPkg.getOrPut(sbn.packageName) { newKeySet() }
        if (set.add(sbn.key)) {
            onCountsChanged?.invoke()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        clearedKeys[sbn.packageName]?.let { set ->
            set.remove(sbn.key)
            if (set.isEmpty()) clearedKeys.remove(sbn.packageName)
        }
        val set = keysByPkg[sbn.packageName] ?: return
        if (set.remove(sbn.key)) {
            if (set.isEmpty()) keysByPkg.remove(sbn.packageName)
            onCountsChanged?.invoke()
        }
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
        AudioWidgetHelper.getInstance(this).cleanup()
    }
}
