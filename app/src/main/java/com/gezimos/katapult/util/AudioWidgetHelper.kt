package com.gezimos.katapult.util

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.view.KeyEvent
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
    private var initializedFor: ComponentName? = null
    private var currentController: MediaController? = null
    private var currentCallback: MediaController.Callback? = null
    private var userDismissed = false
    private var lastControllers: List<MediaController> = emptyList()
    private val watchers = mutableMapOf<MediaSession.Token, Pair<MediaController, MediaController.Callback>>()
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

        if (activeSessionsListener == null || initializedFor != componentName) {
            activeSessionsListener?.let { old ->
                try { manager.removeOnActiveSessionsChangedListener(old) } catch (_: Exception) {}
            }
            val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                handleSessionsChanged(controllers)
            }
            activeSessionsListener = listener
            initializedFor = componentName
            try { manager.addOnActiveSessionsChangedListener(listener, componentName) } catch (_: Exception) {}
        }

        try { handleSessionsChanged(manager.getActiveSessions(componentName)) } catch (_: Exception) {}
    }

    fun cleanup() {
        activeSessionsListener?.let { listener ->
            mediaSessionManager?.removeOnActiveSessionsChangedListener(listener)
        }
        activeSessionsListener = null
        initializedFor = null
        unwatchAll()
        unregisterCallback()
        mediaSessionManager = null
    }

    private fun handleSessionsChanged(controllers: List<MediaController>?) {
        lastControllers = controllers.orEmpty()
        watchAll(lastControllers)
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

    private fun watchAll(controllers: List<MediaController>) {
        val tokens = controllers.map { it.sessionToken }.toSet()
        watchers.keys.filter { it !in tokens }.forEach { token ->
            watchers.remove(token)?.let { (c, cb) ->
                try { c.unregisterCallback(cb) } catch (_: Exception) {}
            }
        }
        controllers.forEach { controller ->
            val token = controller.sessionToken
            if (token in watchers) return@forEach
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    val s = state?.state
                    if ((s == PlaybackState.STATE_PLAYING || s == PlaybackState.STATE_PAUSED) &&
                        currentController?.sessionToken != token
                    ) handleSessionsChanged(lastControllers)
                }
            }
            try {
                controller.registerCallback(callback)
                watchers[token] = controller to callback
            } catch (_: Exception) {}
        }
    }

    private fun unwatchAll() {
        watchers.values.forEach { (c, cb) ->
            try { c.unregisterCallback(cb) } catch (_: Exception) {}
        }
        watchers.clear()
        lastControllers = emptyList()
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

    private fun sendKey(controller: MediaController, keyCode: Int) {
        val sent = controller.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode)) &&
            controller.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        if (!sent) {
            val t = controller.transportControls
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PAUSE -> t.pause()
                KeyEvent.KEYCODE_MEDIA_PLAY -> t.play()
                KeyEvent.KEYCODE_MEDIA_NEXT -> t.skipToNext()
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> t.skipToPrevious()
                KeyEvent.KEYCODE_MEDIA_STOP -> t.stop()
            }
        }
    }

    fun playPause(): Boolean {
        val controller = _state.value?.controller ?: return false
        return try {
            if (controller.playbackState?.state == PlaybackState.STATE_PLAYING)
                sendKey(controller, KeyEvent.KEYCODE_MEDIA_PAUSE)
            else
                sendKey(controller, KeyEvent.KEYCODE_MEDIA_PLAY)
            true
        } catch (_: Exception) { false }
    }

    fun skipNext(): Boolean {
        val controller = _state.value?.controller ?: return false
        return try { sendKey(controller, KeyEvent.KEYCODE_MEDIA_NEXT); true } catch (_: Exception) { false }
    }

    fun skipPrevious(): Boolean {
        val controller = _state.value?.controller ?: return false
        return try { sendKey(controller, KeyEvent.KEYCODE_MEDIA_PREVIOUS); true } catch (_: Exception) { false }
    }

    fun stop(): Boolean {
        val controller = _state.value?.controller ?: return false
        return try {
            unregisterCallback()
            sendKey(controller, KeyEvent.KEYCODE_MEDIA_STOP)
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
