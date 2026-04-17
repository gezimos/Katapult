package com.gezimos.katapult.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.gezimos.katapult.R
import com.gezimos.katapult.model.AppModel

object AppLoader {

    private var blacklist: Set<String>? = null

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

    fun loadApps(context: Context, showSelf: Boolean = false): List<AppModel> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val selfPackage = context.packageName
        val blocked = getBlacklist(context)

        val apps = resolveInfos
            .filter {
                val pkg = it.activityInfo.packageName
                (pkg != selfPackage || showSelf) && pkg !in blocked
            }
            .map { ri ->
                AppModel(
                    packageName = ri.activityInfo.packageName,
                    label = ri.loadLabel(context.packageManager).toString(),
                    activityName = ri.activityInfo.name
                )
            }

        return apps.sortedBy { it.label.lowercase() }
    }
}
