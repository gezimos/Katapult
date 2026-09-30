package com.gezimos.katapult.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.gezimos.katapult.R
import com.gezimos.katapult.model.AppModel

object AppLoader {

    private var blacklist: Set<String>? = null

    @Volatile
    private var cached: List<AppModel>? = null

    private var receiverRegistered = false

    private val invalidator = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            cached = null
            UsageHelper.invalidate()
            intent.data?.schemeSpecificPart?.let { IconUtility.clearCacheFor(it) }
        }
    }

    private fun ensureReceiver(context: Context) {
        if (receiverRegistered) return
        receiverRegistered = true
        val app = context.applicationContext
        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        val bulkFilter = IntentFilter().apply {
            addAction(Intent.ACTION_LOCALE_CHANGED)
            addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE)
            addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE)
            addAction(Intent.ACTION_PACKAGES_SUSPENDED)
            addAction(Intent.ACTION_PACKAGES_UNSUSPENDED)
        }
        try {
            app.registerReceiver(invalidator, packageFilter)
            app.registerReceiver(invalidator, bulkFilter)
        } catch (_: Exception) {
            receiverRegistered = false
        }
    }

    private fun getBlacklist(context: Context): Set<String> {
        blacklist?.let { return it }
        val packages = mutableSetOf<String>()
        try {
            val parser = context.resources.getXml(R.xml.blacklist)
            while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "app") {
                    parser.getAttributeValue(null, "packageName")?.let { packages.add(it) }
                }
            }
        } catch (_: Exception) {}
        blacklist = packages
        return packages
    }

    private fun allLauncherApps(context: Context): List<AppModel> {
        cached?.let { return it }
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val pm = context.packageManager
        val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { ri ->
                AppModel(
                    packageName = ri.activityInfo.packageName,
                    label = ri.loadLabel(pm).toString(),
                    activityName = ri.activityInfo.name
                )
            }
            .sortedBy { it.label.lowercase() }
        cached = apps
        return apps
    }

    fun loadApps(context: Context, showSelf: Boolean = false): List<AppModel> {
        ensureReceiver(context)
        val selfPackage = context.packageName
        val blocked = getBlacklist(context)

        return allLauncherApps(context).filter {
            (it.packageName != selfPackage || showSelf) && it.packageName !in blocked &&
                it.activityName != ScreensaverShortcut.ALIAS
        }
    }
}
