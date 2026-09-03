package com.gezimos.katapult.util

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings

object UsageHelper {

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

    fun ignoredPackages(context: Context): Set<String> {
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
        return ignored
    }

    fun stats(context: Context): Map<String, UsageStats> {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            usm.queryAndAggregateUsageStats(0L, System.currentTimeMillis()) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
