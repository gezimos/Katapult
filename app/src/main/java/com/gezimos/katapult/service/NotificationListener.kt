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
        private val counts = ConcurrentHashMap<String, Int>()
        var onCountsChanged: (() -> Unit)? = null
        private val skipPackages = if (DeviceHelper.isMuditaKompakt()) DirectBadgeHelper.DIRECT_PACKAGES else emptySet()

        fun getCount(packageName: String): Int = counts[packageName] ?: 0

        fun getAllCounts(): Map<String, Int> = counts.toMap()
    }

    private fun isGroupSummary(sbn: StatusBarNotification): Boolean =
        sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0

    override fun onListenerConnected() {
        counts.clear()
        for (sbn in activeNotifications) {
            if (isGroupSummary(sbn)) continue
            if (sbn.packageName in skipPackages) continue
            counts[sbn.packageName] = (counts[sbn.packageName] ?: 0) + 1
        }
        onCountsChanged?.invoke()

        val componentName = ComponentName(this, NotificationListener::class.java)
        AudioWidgetHelper.getInstance(this).initialize(componentName)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (isGroupSummary(sbn)) return
        if (sbn.packageName in skipPackages) return
        counts[sbn.packageName] = (counts[sbn.packageName] ?: 0) + 1
        onCountsChanged?.invoke()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (isGroupSummary(sbn)) return
        if (sbn.packageName in skipPackages) return
        val current = counts[sbn.packageName] ?: 0
        if (current <= 1) {
            counts.remove(sbn.packageName)
        } else {
            counts[sbn.packageName] = current - 1
        }
        onCountsChanged?.invoke()
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioWidgetHelper.getInstance(this).cleanup()
    }
}
