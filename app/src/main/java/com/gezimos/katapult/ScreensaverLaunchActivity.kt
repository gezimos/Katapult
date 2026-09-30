package com.gezimos.katapult

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.gezimos.katapult.lockscreen.ScreensaverActivity

class ScreensaverLaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            startActivity(
                Intent(this, ScreensaverActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_TASK_ON_HOME,
                ),
            )
        } catch (_: Exception) {}
        finish()
        overridePendingTransition(0, 0)
    }
}
