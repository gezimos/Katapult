package com.gezimos.katapult.util

import android.content.Context
import android.content.Intent
import android.os.Build

object DeviceHelper {

    fun isMuditaKompakt(): Boolean {
        return Build.BRAND.equals("Mudita", ignoreCase = true) ||
               Build.MODEL.equals("Kompakt", ignoreCase = true) ||
               Build.DEVICE.equals("Kompakt", ignoreCase = true) ||
               Build.PRODUCT.equals("Kompakt", ignoreCase = true)
    }

    fun isDefaultLauncher(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }
}
