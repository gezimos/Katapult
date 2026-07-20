package com.gezimos.katapult.lockscreen

import android.service.dreams.DreamService
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Optional charging screensaver: Android auto-starts the selected system screen saver while
 * the device sleeps on a charger. Shows [ScreensaverView] (the same UI as the on-demand
 * screensaver). Purely a bonus — the main, zero-setup path is [ScreensaverActivity], which
 * needs no screen-saver selection. To use this one, pick Katapult under the system Screen
 * saver settings.
 *
 * Part of the self-contained experimental .lockscreen package; to remove, also delete the
 * <service> entry in AndroidManifest.xml.
 */
class KatapultDreamService :
    DreamService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    companion object {
        /** True while the dream is showing, so the lockscreen widget hides and isn't doubled. */
        @Volatile
        var isDreaming = false
            private set
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Interactive so a stray tap doesn't dismiss it; exit with the power button.
        isInteractive = true
        isFullscreen = true
        isScreenBright = true
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@KatapultDreamService)
            setViewTreeViewModelStoreOwner(this@KatapultDreamService)
            setViewTreeSavedStateRegistryOwner(this@KatapultDreamService)
            setContent { ScreensaverView() }
        }
        setContentView(composeView)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        isDreaming = true
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onDreamingStopped() {
        isDreaming = false
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onDetachedFromWindow()
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        super.onDestroy()
    }
}
