package com.gezimos.katapult.util

import android.os.Build

object DeviceHelper {

    fun isMuditaKompakt(): Boolean {
        return Build.BRAND.equals("Mudita", ignoreCase = true) ||
               Build.MODEL.equals("Kompakt", ignoreCase = true) ||
               Build.DEVICE.equals("Kompakt", ignoreCase = true) ||
               Build.PRODUCT.equals("Kompakt", ignoreCase = true)
    }
}
