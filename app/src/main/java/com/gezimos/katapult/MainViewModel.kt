package com.gezimos.katapult

import android.app.AlarmManager
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.text.format.DateFormat
import android.view.WindowInsetsController
import com.gezimos.katapult.util.AudioWidgetHelper
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.gezimos.katapult.model.AppModel
import com.gezimos.katapult.service.DirectBadgeHelper
import com.gezimos.katapult.service.NotificationListener
import com.gezimos.katapult.ui.IconSize
import com.gezimos.katapult.util.AppLoader
import com.gezimos.katapult.util.DeviceHelper
import com.gezimos.katapult.util.IconUtility
import com.gezimos.katapult.util.PrefsManager
import java.io.File
import java.util.Date

enum class Screen { ONBOARDING, HOME, ALL_APPS, SETTINGS }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = PrefsManager(application)

    var screen by mutableStateOf(if (prefs.onboardingComplete) Screen.HOME else Screen.ONBOARDING)
    var shortcutRefresh by mutableIntStateOf(0)
    var contextMenuApp by mutableStateOf<AppModel?>(null)
    var showRenameDialog by mutableStateOf(false)
    var reorderMode by mutableStateOf(false)
    var reorderHighlightIndex by mutableIntStateOf(-1)
    var orderedApps by mutableStateOf(listOf<AppModel>())
        private set
    var currentPage by mutableIntStateOf(0)
        private set
    var clockTime by mutableStateOf("")
        private set
    var clockAmPm by mutableStateOf<String?>(null)
        private set
    var clockDate by mutableStateOf("")
        private set
    var alarmTime by mutableStateOf<String?>(null)
        private set
    var notificationCounts by mutableStateOf(mapOf<String, Int>())
        private set
    var wallpaperBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var batteryPercent by mutableIntStateOf(-1)
        private set
    var isCharging by mutableStateOf(false)
        private set
    var mediaInfo by mutableStateOf<AudioWidgetHelper.MediaInfo?>(null)
        private set
    var roundedIcons by mutableStateOf(prefs.roundedIcons)

    val appsPerPage = 12
    val totalPages: Int
        get() = if (orderedApps.isEmpty()) 1 else ((orderedApps.size + appsPerPage - 1) / appsPerPage)

    private val ctx get() = getApplication<Application>()
    private val isMudita = DeviceHelper.isMuditaKompakt()
    private val directBadgeHelper = if (isMudita) DirectBadgeHelper(application) else null

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            clockHandler.postDelayed(this, 15_000)
        }
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateClock()
        }
    }

    init {
        loadApps()
        updateClock()
        loadWallpaper()
        ctx.registerReceiver(powerReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        })
        viewModelScope.launch {
            AudioWidgetHelper.getInstance(ctx).state.collect { mediaInfo = it }
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val sizePx = (IconSize.value * ctx.resources.displayMetrics.density).toInt()
            // Preload home shortcut icons first (visible immediately)
            val slots = listOf("phone", "sms", "extra_left", "extra_center", "extra_right")
            for (slot in slots) {
                val pkg = getShortcutPackage(slot) ?: continue
                val activity = getShortcutActivity(slot)
                IconUtility.loadIcon(ctx, pkg, activity, sizePx)
            }
            // Then preload all app list icons
            IconUtility.preloadIcons(ctx, orderedApps, sizePx)
        }
    }

    // --- Clock ---

    fun updateClock() {
        val now = Date()
        val fullTime = DateFormat.getTimeFormat(ctx).format(now)
        clockTime = fullTime.replace(Regex("\\s*[AaPp][Mm]\\s*"), "").trim()
        val upper = fullTime.uppercase()
        clockAmPm = when {
            upper.contains("AM") -> "AM"
            upper.contains("PM") -> "PM"
            else -> null
        }
        clockDate = DateFormat.getLongDateFormat(ctx).format(now)

        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val info = alarmManager.nextAlarmClock
        alarmTime = if (info != null) {
            DateFormat.getTimeFormat(ctx).format(Date(info.triggerTime))
        } else null

        val batteryIntent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryIntent != null) {
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            batteryPercent = (level * 100) / scale
            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }
    }

    fun startClock() {
        updateClock()
        clockHandler.postDelayed(clockRunnable, 15_000)
    }

    fun stopClock() {
        clockHandler.removeCallbacks(clockRunnable)
    }

    // --- Apps ---

    fun loadApps() {
        val currentApps = AppLoader.loadApps(ctx, showSelf = prefs.showKatapultIcon)
        val hidden = prefs.getHiddenApps()
        val visible = currentApps.filter { it.packageName !in hidden }
        val renamed = visible.map { app ->
            val customName = prefs.getAppRename(app.packageName)
            if (customName != null) app.copy(label = customName) else app
        }
        val savedOrder = prefs.loadAppOrder()
        orderedApps = if (savedOrder != null) {
            prefs.reconcileOrder(savedOrder, renamed)
        } else {
            renamed.sortedBy { it.label.lowercase() }
        }
    }


    fun getPageApps(page: Int): List<AppModel> {
        val start = page * appsPerPage
        val end = minOf(start + appsPerPage, orderedApps.size)
        return if (start < orderedApps.size) orderedApps.subList(start, end) else emptyList()
    }

    fun getAllApps(): List<AppModel> {
        return AppLoader.loadApps(ctx).map { app ->
            val customName = prefs.getAppRename(app.packageName)
            if (customName != null) app.copy(label = customName) else app
        }
    }

    fun showPage(page: Int) {
        if (page < 0 || page >= totalPages) return
        currentPage = page
    }

    fun navigateTo(target: Screen) {
        if (reorderMode) finishReorder()
        if (target == Screen.ALL_APPS) currentPage = 0
        screen = target
    }


    fun goBack(): Boolean {
        if (reorderMode) {
            reorderMode = false
            reorderHighlightIndex = -1
            loadApps()
            return true
        }
        if (contextMenuApp != null) { contextMenuApp = null; showRenameDialog = false; return true }
        return when (screen) {
            Screen.ONBOARDING -> false
            Screen.SETTINGS -> { screen = Screen.HOME; true }
            Screen.ALL_APPS -> { screen = Screen.HOME; true }
            Screen.HOME -> false
        }
    }

    // --- Reorder ---

    fun enterReorderMode(app: AppModel) {
        val index = orderedApps.indexOfFirst { it.packageName == app.packageName }
        if (index >= 0) {
            reorderHighlightIndex = index
            reorderMode = true
        }
        contextMenuApp = null
    }

    fun reorderTap(targetIndex: Int) {
        if (reorderHighlightIndex < 0) {
            // Nothing selected — select this app
            reorderHighlightIndex = targetIndex
        } else if (targetIndex == reorderHighlightIndex) {
            // Tapped the same app — deselect
            reorderHighlightIndex = -1
        } else {
            // Swap and clear selection
            val list = orderedApps.toMutableList()
            val temp = list[reorderHighlightIndex]
            list[reorderHighlightIndex] = list[targetIndex]
            list[targetIndex] = temp
            orderedApps = list
            reorderHighlightIndex = -1
        }
    }

    fun finishReorder() {
        reorderMode = false
        reorderHighlightIndex = -1
        prefs.saveAppOrder(orderedApps.map { it.packageName })
    }

    fun resetOrder() {
        orderedApps = orderedApps.sortedBy { it.label.lowercase() }
        reorderHighlightIndex = -1
        prefs.saveAppOrder(orderedApps.map { it.packageName })
    }

    // --- App actions ---

    fun hideApp(packageName: String) {
        prefs.hideApp(packageName)
        contextMenuApp = null
        loadApps()
    }

    fun renameApp(packageName: String, newName: String) {
        prefs.setAppRename(packageName, newName.ifBlank { null })
        contextMenuApp = null
        showRenameDialog = false
        loadApps()
    }

    fun getAppDisplayName(packageName: String): String {
        return prefs.getAppRename(packageName)
            ?: AppLoader.loadApps(ctx).find { it.packageName == packageName }?.label
            ?: packageName
    }

    // --- Status bar ---

    fun applyStatusBar(context: Context) {
        val activity = context as? Activity ?: return
        val controller = activity.window.insetsController ?: return
        if (prefs.hideStatusBar) {
            controller.hide(android.view.WindowInsets.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(android.view.WindowInsets.Type.statusBars())
        }
    }

    // --- Launching ---

    fun launchIntent(context: Context, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun launchApp(context: Context, app: AppModel) {
        launchPackage(context, app.packageName, app.activityName)
    }

    fun launchPackage(context: Context, packageName: String, activityName: String) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(packageName, activityName)
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = ctx.packageManager.getLaunchIntentForPackage(packageName)
            if (fallback != null) context.startActivity(fallback)
        }
        (context as? Activity)?.overridePendingTransition(0, 0)
    }

    fun launchShortcut(context: Context, slot: String) {
        val saved = prefs.loadShortcut(slot)
        if (saved != null) {
            launchPackage(context, saved.first, saved.second)
            return
        }
        val defaultPkg = getDefaultPackageForSlot(slot)
        if (defaultPkg != null) {
            val launchIntent = ctx.packageManager.getLaunchIntentForPackage(defaultPkg)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                (context as? Activity)?.overridePendingTransition(0, 0)
            }
        }
    }

    fun getShortcutPackage(slot: String): String? {
        return prefs.loadShortcut(slot)?.first ?: getDefaultPackageForSlot(slot)
    }

    fun getShortcutActivity(slot: String): String {
        return prefs.loadShortcut(slot)?.second ?: ""
    }

    fun getShortcutLabel(slot: String, defaultLabel: String): String {
        val pkg = getShortcutPackage(slot) ?: return defaultLabel
        val customName = prefs.getAppRename(pkg)
        if (customName != null) return customName
        return try {
            val appInfo = ctx.packageManager.getApplicationInfo(pkg, 0)
            ctx.packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            defaultLabel
        }
    }

    fun saveShortcut(slot: String, packageName: String, activityName: String) {
        prefs.saveShortcut(slot, packageName, activityName)
    }

    private fun getDefaultPackageForSlot(slot: String): String? {
        return when (slot) {
            "phone" -> {
                val intent = Intent(Intent.ACTION_DIAL)
                val ri = ctx.packageManager.resolveActivity(intent, 0)
                ri?.activityInfo?.packageName
            }
            "sms" -> Telephony.Sms.getDefaultSmsPackage(ctx)
            "extra_left" -> "com.mudita.audio.player".takeIfInstalled()
            "extra_center" -> "com.mudita.calendar".takeIfInstalled()
            "extra_right" -> "com.mudita.camera".takeIfInstalled()
            else -> null
        }
    }

    private fun String.takeIfInstalled(): String? {
        return try {
            ctx.packageManager.getApplicationInfo(this, 0)
            this
        } catch (_: Exception) { null }
    }

    // --- Notifications ---

    fun refreshNotifications() {
        val counts = NotificationListener.getAllCounts()
        notificationCounts = if (directBadgeHelper != null) counts + directBadgeHelper.getCounts() else counts
    }

    private val audioHelper get() = AudioWidgetHelper.getInstance(ctx)

    fun mediaPlayPause() { audioHelper.playPause() }
    fun mediaNext() { audioHelper.skipNext() }
    fun mediaPrevious() { audioHelper.skipPrevious() }
    fun mediaStop() { audioHelper.stop() }
    fun mediaOpenApp(context: Context) { audioHelper.openApp() }

    fun startNotificationListener() {
        NotificationListener.onCountsChanged = {
            Handler(Looper.getMainLooper()).post { refreshNotifications() }
        }
        directBadgeHelper?.let {
            it.onCountsChanged = {
                Handler(Looper.getMainLooper()).post { refreshNotifications() }
            }
            it.start()
        }
    }

    fun stopNotificationListener() {
        NotificationListener.onCountsChanged = null
        directBadgeHelper?.let {
            it.onCountsChanged = null
            it.stop()
        }
    }

    // --- Wallpaper ---

    fun setWallpaper(context: Context, uri: Uri) {
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            if (bitmap == null) return

            val file = File(ctx.filesDir, "wallpaper.png")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            prefs.wallpaperPath = file.absolutePath
            wallpaperBitmap = bitmap
        } catch (_: Exception) {}
    }

    fun clearWallpaper() {
        prefs.wallpaperPath?.let { File(it).delete() }
        prefs.wallpaperPath = null
        wallpaperBitmap = null
    }

    private fun loadWallpaper() {
        val path = prefs.wallpaperPath ?: return
        val file = File(path)
        wallpaperBitmap = if (file.exists()) {
            try { BitmapFactory.decodeFile(path) } catch (_: Exception) { null }
        } else null
    }

    override fun onCleared() {
        super.onCleared()
        stopClock()
        stopNotificationListener()
        try { ctx.unregisterReceiver(powerReceiver) } catch (_: Exception) {}
    }
}
