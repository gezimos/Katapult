package com.gezimos.katapult.util

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.os.SystemClock
import android.provider.Settings

object UsageHelper {

    private const val TTL_MS = 60_000L
    private const val WINDOW_MS = 30L * 24 * 60 * 60 * 1000

    private var statsCache: Map<String, UsageStats>? = null
    private var statsAt = 0L
    private var ignoredCache: Set<String>? = null
    private var ignoredAt = 0L

    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    fun openSettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (_: Exception) {}
    }

    fun invalidate() {
        statsCache = null
        ignoredCache = null
    }

    fun ignoredPackages(context: Context): Set<String> {
        val now = SystemClock.elapsedRealtime()
        ignoredCache?.let { if (now - ignoredAt < TTL_MS) return it }
        val ignored = mutableSetOf<String>()
        try {
            val pm = context.packageManager
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            pm.queryIntentActivities(homeIntent, PackageManager.MATCH_ALL).forEach {
                ignored.add(it.activityInfo.packageName)
            }
            pm.resolveActivity(Intent(Settings.ACTION_SETTINGS), 0)?.let {
                ignored.add(it.activityInfo.packageName)
            }
        } catch (_: Exception) {}
        ignoredCache = ignored
        ignoredAt = now
        return ignored
    }

    fun stats(context: Context): Map<String, UsageStats> {
        val now = SystemClock.elapsedRealtime()
        statsCache?.let { if (now - statsAt < TTL_MS) return it }
        val result = try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            usm.queryAndAggregateUsageStats(end - WINDOW_MS, end) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
        statsCache = result
        statsAt = now
        return result
    }
}
