package com.gezimos.katapult

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.gezimos.katapult.ui.App
import com.gezimos.katapult.util.AudioWidgetHelper
import com.gezimos.katapult.util.DeviceHelper
import com.gezimos.katapult.util.EinkHelper
import com.gezimos.katapult.util.EinkRefreshHelper

class MainActivity : ComponentActivity() {

    val viewModel: MainViewModel by viewModels()

    private var einkHelper: EinkHelper? = null

    val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.setWallpaper(this, it) }
    }

    val iconPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.saveImportedIcon(this, uri)
        else viewModel.cancelIconImport()
    }

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            viewModel.goBack()
            // On home with nothing to close: do nothing. This is a launcher.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setWindowAnimations(0)
        overridePendingTransition(0, 0)

        onBackPressedDispatcher.addCallback(this, backCallback)

        if (DeviceHelper.isMuditaKompakt()) {
            einkHelper = EinkHelper(packageName).also { helper ->
                lifecycle.addObserver(helper)
                val saved = viewModel.prefs.einkHelperMode
                if (saved != EinkHelper.MEINK_MODE_DISABLED) helper.setMeinkMode(saved)
            }
        }

        setContent {
            App(viewModel = viewModel, imagePicker = imagePicker, iconPicker = iconPicker)
        }
    }

    fun setMeinkMode(mode: Int) {
        einkHelper?.setMeinkMode(mode)
    }

    override fun onResume() {
        super.onResume()
        overridePendingTransition(0, 0)
        viewModel.applyStatusBar(this)
        viewModel.startClock()
        viewModel.loadApps()
        viewModel.refreshNotifications()
        viewModel.startNotificationListener()
        AudioWidgetHelper.getInstance(this).resetDismissalState()

        val savedMode = viewModel.prefs.einkHelperMode
        if (savedMode != EinkHelper.MEINK_MODE_DISABLED) {
            einkHelper?.setMeinkMode(savedMode)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopClock()
        viewModel.stopNotificationListener()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        overridePendingTransition(0, 0)
        viewModel.navigateTo(Screen.HOME)

        val savedMode = viewModel.prefs.einkHelperMode
        if (savedMode != EinkHelper.MEINK_MODE_DISABLED) {
            einkHelper?.setMeinkMode(savedMode)
        }

        if (viewModel.prefs.einkRefreshOnHome) {
            EinkRefreshHelper.refresh(this)
        }
    }
}
