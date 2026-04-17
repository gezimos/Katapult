package com.gezimos.katapult.util

import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup

object EinkRefreshHelper {

    fun refresh(activity: Activity, delayMs: Long = 80) {
        Handler(Looper.getMainLooper()).post {
            val overlay = View(activity)
            overlay.setBackgroundColor(Color.BLACK)
            overlay.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val parent = activity.window.decorView as ViewGroup
            parent.addView(overlay)
            overlay.bringToFront()

            Handler(Looper.getMainLooper()).postDelayed({
                parent.removeView(overlay)
            }, delayMs)
        }
    }
}
