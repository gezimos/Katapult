package com.gezimos.katapult.lockscreen

import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gezimos.katapult.util.EinkRefreshHelper
import com.gezimos.katapult.util.PrefsManager

class ScreensaverActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FROM_POWER = "from_power"

        @Volatile
        var isShowing = false
            private set

        @Volatile
        private var lastActive = 0L

        fun recentlyActive(): Boolean = SystemClock.elapsedRealtime() - lastActive < 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isShowing = true
        val fromPower = intent.getBooleanExtra(EXTRA_FROM_POWER, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val prefs = PrefsManager(this)
        window.attributes = window.attributes.apply { screenBrightness = 0f }
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            val light = !prefs.darkMode
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
            if (prefs.hideStatusBar) hide(WindowInsetsCompat.Type.statusBars())
            else show(WindowInsetsCompat.Type.statusBars())
        }

        setContent { ScreensaverView() }

        if (prefs.screensaverEinkRefresh && !fromPower) {
            EinkRefreshHelper.refresh(this, darkMode = prefs.darkMode)
        }

        if (!fromPower) {
            window.decorView.doOnPreDraw {
                window.decorView.post { LockscreenWidgetService.lockScreen() }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isShowing = true
        lastActive = SystemClock.elapsedRealtime()
    }

    override fun onStop() {
        super.onStop()
        lastActive = SystemClock.elapsedRealtime()
        isShowing = false
        val power = getSystemService(PowerManager::class.java)
        if (power != null && !power.isInteractive) {
            finish()
        }
    }
}
