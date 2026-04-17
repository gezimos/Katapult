package com.gezimos.katapult.util

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("StaticFieldLeak")
class AudioWidgetHelper private constructor(private val context: Context) {

    data class MediaInfo(
        val packageName: String,
        val isPlaying: Boolean,
        val title: String?,
        val artist: String?,
        val controller: MediaController,
    )

    private val _state = MutableStateFlow<MediaInfo?>(null)
    val state: StateFlow<MediaInfo?> = _state

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeSessionsListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private var currentController: MediaController? = null
    private var currentCallback: MediaController.Callback? = null
    private var userDismissed = false
    private var dismissedPackageName: String? = null

    companion object {
        @Volatile
        private var INSTANCE: AudioWidgetHelper? = null

        fun getInstance(context: Context): AudioWidgetHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioWidgetHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun initialize(componentName: ComponentName) {
        mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        val manager = mediaSessionManager ?: return

        val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            handleSessionsChanged(controllers)
        }
        activeSessionsListener = listener

        try {
            manager.addOnActiveSessionsChangedListener(listener, componentName)
            handleSessionsChanged(manager.getActiveSessions(componentName))
        } catch (_: Exception) {}
    }

    fun cleanup() {
        activeSessionsListener?.let { listener ->
            mediaSessionManager?.removeOnActiveSessionsChangedListener(listener)
        }
        activeSessionsListener = null
        unregisterCallback()
        mediaSessionManager = null
    }

    private fun handleSessionsChanged(controllers: List<MediaController>?) {
        val active = controllers?.firstOrNull { c ->
            val s = c.playbackState?.state
            s == PlaybackState.STATE_PLAYING || s == PlaybackState.STATE_PAUSED
        }

        if (active != null) {
            if (userDismissed &&
                active.packageName == dismissedPackageName &&
                active.playbackState?.state != PlaybackState.STATE_PLAYING
            ) return

            if (active.playbackState?.state == PlaybackState.STATE_PLAYING) {
                userDismissed = false
                dismissedPackageName = null
            }

            registerCallback(active)
            updateState(active)
        } else {
            unregisterCallback()
            _state.value = null
        }
    }

    private fun registerCallback(controller: MediaController) {
        if (currentController?.sessionToken == controller.sessionToken) {
            // Same session — just update state, callback already registered
            return
        }
        unregisterCallback()

        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) = updateState(controller)
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                if (state?.state == PlaybackState.STATE_PLAYING) {
                    userDismissed = false
                    dismissedPackageName = null
                }
                if (state?.state == PlaybackState.STATE_STOPPED) _state.value = null
                else updateState(controller)
            }
            override fun onSessionDestroyed() { _state.value = null }
        }

        try {
            controller.registerCallback(callback)
            currentController = controller
            currentCallback = callback
        } catch (_: Exception) {}
    }

    private fun unregisterCallback() {
        currentCallback?.let { cb ->
            try { currentController?.unregisterCallback(cb) } catch (_: Exception) {}
        }
        currentCallback = null
        currentController = null
    }

    private fun updateState(controller: MediaController) {
        if (userDismissed && controller.playbackState?.state != PlaybackState.STATE_PLAYING) return
        val metadata = controller.metadata
        val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
        _state.value = MediaInfo(
            packageName = controller.packageName,
            isPlaying = isPlaying,
            title = metadata?.description?.title?.toString(),
            artist = metadata?.description?.subtitle?.toString(),
            controller = controller,
        )
    }

    fun playPause(): Boolean {
        val controller = _state.value?.controller ?: return false
        return try {
            if (controller.playbackState?.state == PlaybackState.STATE_PLAYING)
                controller.transportControls.pause()
            else
                controller.transportControls.play()
            true
        } catch (_: Exception) { false }
    }

    fun skipNext(): Boolean {
        val controller = _state.value?.controller ?: return false
        return try { controller.transportControls.skipToNext(); true } catch (_: Exception) { false }
    }

    fun skipPrevious(): Boolean {
        val controller = _state.value?.controller ?: return false
        return try { controller.transportControls.skipToPrevious(); true } catch (_: Exception) { false }
    }

    fun stop(): Boolean {
        val controller = _state.value?.controller ?: return false
        return try {
            unregisterCallback()
            controller.transportControls.stop()
            dismiss()
            true
        } catch (_: Exception) { dismiss(); false }
    }

    fun openApp(): Boolean {
        val pkg = _state.value?.packageName ?: return false
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
                true
            } ?: false
        } catch (_: Exception) { false }
    }

    fun dismiss() {
        userDismissed = true
        dismissedPackageName = currentController?.packageName ?: _state.value?.packageName
        _state.value = null
    }

    fun resetDismissalState() {
        val manager = mediaSessionManager ?: return
        try {
            val componentName = ComponentName(context, com.gezimos.katapult.service.NotificationListener::class.java)
            val controllers = manager.getActiveSessions(componentName)
            handleSessionsChanged(controllers)
        } catch (_: Exception) {}
    }
}
