package com.gezimos.katapult

import android.app.AlarmManager
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.BatteryManager
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.text.format.DateFormat
import android.view.WindowInsetsController
import java.text.SimpleDateFormat
import java.util.Locale
import com.gezimos.katapult.util.AudioWidgetHelper
import com.gezimos.katapult.util.WeatherHelper
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
import com.gezimos.katapult.util.ShortcutHelper
import com.gezimos.katapult.util.UsageHelper
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
    var iconOverrideTarget by mutableStateOf<String?>(null)
    var iconImportError by mutableStateOf(false)
    var sheetDismissSignal by mutableIntStateOf(0)
        private set
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
    var weather by mutableStateOf<WeatherHelper.Weather?>(null)
        private set
    private val weatherRefreshMs = 10 * 60 * 1000L
    private var weatherCheckedAt = -weatherRefreshMs
    var roundedIcons by mutableStateOf(prefs.roundedIcons)
    var iconSize by mutableStateOf(prefs.iconSize)
    var darkMode by mutableStateOf(prefs.darkMode)

    var showLockscreenReEnable by mutableStateOf(false)
    private var lockscreenReEnableChecked = false

    fun checkLockscreenService(context: Context) {
        if (lockscreenReEnableChecked || screen != Screen.HOME) return
        if (com.gezimos.katapult.lockscreen.LockscreenWidgetService.isEnabled(context)) return
        val needsService = prefs.lockscreenWidget ||
            (prefs.hideStatusBarClock && DeviceHelper.isMuditaKompakt()) ||
            prefs.doubleTapAction == PrefsManager.DOUBLE_TAP_LOCK ||
            (prefs.screensaverEnabled && prefs.screensaverOnPower)
        if (needsService) {
            lockscreenReEnableChecked = true
            showLockscreenReEnable = true
        }
    }

    fun resolveLockscreenReEnable() {
        showLockscreenReEnable = false
    }

    var updateProgress by mutableIntStateOf(-1)
        private set
    var updateReadyTag by mutableStateOf<String?>(null)
        private set
    var updateFailed by mutableStateOf(false)
        private set

    fun downloadUpdate(info: com.gezimos.katapult.util.UpdateChecker.ReleaseInfo) {
        if (updateProgress >= 0) return
        updateProgress = 0
        updateFailed = false
        updateReadyTag = null
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val ok = try {
                val file = com.gezimos.katapult.util.UpdateChecker.apkFile(ctx)
                com.gezimos.katapult.util.UpdateChecker.downloadApk(info.apkUrl, file) { p ->
                    if (p >= updateProgress + 5 || p == 100) updateProgress = p
                }
            } catch (_: Exception) {
                false
            }
            if (ok) updateReadyTag = info.tag else updateFailed = true
            updateProgress = -1
        }
    }

    fun clearDownloadedUpdate() {
        updateReadyTag = null
        updateFailed = false
        try {
            com.gezimos.katapult.util.UpdateChecker.apkFile(ctx).delete()
        } catch (_: Exception) {}
    }

    fun startScreensaver(context: Context) {
        try {
            context.startActivity(
                Intent(context, com.gezimos.katapult.lockscreen.ScreensaverActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
        } catch (_: Exception) {}
    }

    val gridColumns = 3
    var appsPerPage by mutableIntStateOf(12)
        private set
    val totalPages: Int
        get() = if (orderedApps.isEmpty()) 1 else ((orderedApps.size + appsPerPage - 1) / appsPerPage)

    fun updateAppsPerPage(perPage: Int) {
        val clamped = perPage.coerceAtLeast(gridColumns)
        if (clamped == appsPerPage) return
        appsPerPage = clamped
        val maxPage = (totalPages - 1).coerceAtLeast(0)
        if (currentPage > maxPage) currentPage = maxPage
    }

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
            val sizePx = (iconSize * ctx.resources.displayMetrics.density).toInt()
            for (slot in PrefsManager.HOME_SLOTS) {
                val pkg = getShortcutPackage(slot) ?: continue
                val shortcutId = prefs.loadSlotShortcutId(slot)
                if (shortcutId != null) {
                    IconUtility.loadShortcutIcon(ctx, pkg, shortcutId, sizePx)
                } else {
                    IconUtility.loadIcon(ctx, pkg, getShortcutActivity(slot), sizePx)
                }
            }
            IconUtility.preloadIcons(ctx, orderedApps, sizePx)
        }
    }

    fun updateClock() {
        val now = Date()
        val clockPattern = prefs.clockFormat
        val fullTime = if (clockPattern == "system") DateFormat.getTimeFormat(ctx).format(now)
            else SimpleDateFormat(clockPattern, Locale.getDefault()).format(now)
        clockTime = fullTime.replace(Regex("\\s*[AaPp][Mm]\\s*"), "").trim()
        val upper = fullTime.uppercase()
        clockAmPm = when {
            upper.contains("AM") -> "AM"
            upper.contains("PM") -> "PM"
            else -> null
        }
        val datePattern = prefs.dateFormat
        clockDate = if (datePattern == "system") DateFormat.getLongDateFormat(ctx).format(now)
            else SimpleDateFormat(datePattern, Locale.getDefault()).format(now)

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

        if (!prefs.showWeather) {
            weather = null
        } else if (SystemClock.elapsedRealtime() - weatherCheckedAt >= weatherRefreshMs) {
            refreshWeather()
        }
    }

    private var weatherObserverOn = false

    private val weatherObserver = object : ContentObserver(clockHandler) {
        override fun onChange(selfChange: Boolean) {
            weatherCheckedAt = SystemClock.elapsedRealtime()
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val result = WeatherHelper.read(ctx)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    weather = result
                }
            }
        }
    }

    private fun syncWeatherObserver() {
        val want = prefs.showWeather
        if (want == weatherObserverOn) return
        try {
            if (want) {
                ctx.contentResolver.registerContentObserver(
                    WeatherHelper.WidgetUri, false, weatherObserver,
                )
            } else {
                ctx.contentResolver.unregisterContentObserver(weatherObserver)
            }
            weatherObserverOn = want
        } catch (_: Exception) {
        }
    }

    fun refreshWeather() {
        syncWeatherObserver()
        weatherCheckedAt = SystemClock.elapsedRealtime()
        if (!prefs.showWeather) {
            weather = null
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = WeatherHelper.read(ctx)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                weather = result
            }
        }
    }

    fun startClock() {
        updateClock()
        clockHandler.postDelayed(clockRunnable, 15_000)
    }

    fun stopClock() {
        clockHandler.removeCallbacks(clockRunnable)
    }

    fun loadApps() {
        val currentApps = AppLoader.loadApps(ctx, showSelf = prefs.showKatapultIcon) +
            ShortcutHelper.installedShortcuts(ctx, prefs)
        val hidden = prefs.getHiddenApps()
        val visible = currentApps.filter { it.key !in hidden }
        val renamed = visible.map { app ->
            val customName = prefs.getAppRename(app.key)
            if (customName != null) app.copy(label = customName) else app
        }
        val sortMode = prefs.appSortMode
        if (sortMode != PrefsManager.APP_SORT_MANUAL && UsageHelper.hasPermission(ctx)) {
            val stats = UsageHelper.stats(ctx)
            val ignored = UsageHelper.ignoredPackages(ctx)
            orderedApps = renamed.sortedWith(
                compareByDescending<AppModel> { app ->
                    val s = if (app.packageName in ignored) null else stats[app.packageName]
                    if (s == null) 0L
                    else if (sortMode == PrefsManager.APP_SORT_RECENT) s.lastTimeUsed
                    else s.totalTimeInForeground
                }.thenBy { it.label.lowercase() }
            )
            return
        }
        val savedOrder = prefs.loadAppOrder()
        orderedApps = if (savedOrder != null) {
            prefs.reconcileOrder(savedOrder, renamed)
        } else {
            renamed.sortedBy { it.label.lowercase() }
        }
    }

    fun getAllApps(): List<AppModel> {
        val all = AppLoader.loadApps(ctx) + ShortcutHelper.installedShortcuts(ctx, prefs)
        return all.map { app ->
            val customName = prefs.getAppRename(app.key)
            if (customName != null) app.copy(label = customName) else app
        }.sortedBy { it.label.lowercase() }
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

    fun dismissAllSheets() {
        sheetDismissSignal++
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

    fun enterReorderMode(app: AppModel) {
        val index = orderedApps.indexOfFirst { it.key == app.key }
        if (index >= 0) {
            reorderHighlightIndex = index
            reorderMode = true
        }
        contextMenuApp = null
    }

    fun reorderTap(targetIndex: Int) {
        if (reorderHighlightIndex < 0) {
            reorderHighlightIndex = targetIndex
        } else if (targetIndex == reorderHighlightIndex) {
            reorderHighlightIndex = -1
        } else {
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
        prefs.saveAppOrder(orderedApps.map { it.key })
    }

    fun resetOrder() {
        orderedApps = orderedApps.sortedBy { it.label.lowercase() }
        reorderHighlightIndex = -1
        prefs.saveAppOrder(orderedApps.map { it.key })
    }

    fun hideApp(key: String) {
        prefs.hideApp(key)
        contextMenuApp = null
        loadApps()
    }

    fun renameApp(key: String, newName: String, currentLabel: String) {
        val name = newName.ifBlank { null }
        contextMenuApp = null
        showRenameDialog = false
        if (name == currentLabel) return
        if (name != prefs.getAppRename(key)) prefs.removeFromAppOrder(key)
        prefs.setAppRename(key, name)
        loadApps()
    }

    fun removeShortcut(app: AppModel) {
        forgetShortcut(app.packageName, app.shortcutId)
        contextMenuApp = null
        loadApps()
    }

    fun setShortcutEnabled(packageName: String, shortcutId: String, label: String, enabled: Boolean) {
        if (enabled) {
            prefs.addSavedShortcut(packageName, shortcutId, label)
            prefs.unhideApp("$packageName|$shortcutId")
        } else {
            prefs.removeSavedShortcut(packageName, shortcutId)
            prefs.clearSlotsForShortcut(packageName, shortcutId)
        }
        ShortcutHelper.syncPins(ctx, prefs, packageName)
        loadApps()
    }

    private fun forgetShortcut(packageName: String, shortcutId: String) {
        val key = "$packageName|$shortcutId"
        prefs.removeSavedShortcut(packageName, shortcutId)
        prefs.clearSlotsForShortcut(packageName, shortcutId)
        prefs.setAppRename(key, null)
        prefs.removeFromAppOrder(key)
        prefs.unhideApp(key)
        deleteOverrideFile(key)
        prefs.clearIconOverride(key)
        IconUtility.clearCacheFor(key)
        ShortcutHelper.syncPins(ctx, prefs, packageName)
    }

    fun applyStatusBar(context: Context) {
        val activity = context as? Activity ?: return
        activity.window.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(
                if (prefs.darkMode) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            )
        )
        val controller = activity.window.insetsController ?: return
        val lightBarsMask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        controller.setSystemBarsAppearance(if (prefs.darkMode) 0 else lightBarsMask, lightBarsMask)
        if (prefs.hideStatusBar) {
            controller.hide(android.view.WindowInsets.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(android.view.WindowInsets.Type.statusBars())
        }
    }

    fun launchIntent(context: Context, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun launchApp(context: Context, app: AppModel) {
        if (app.shortcutId.isEmpty() &&
            app.packageName == ctx.packageName &&
            DeviceHelper.isDefaultLauncher(ctx)
        ) {
            navigateTo(Screen.SETTINGS)
            return
        }
        if (app.shortcutId.isNotEmpty()) {
            if (ShortcutHelper.launch(ctx, app.packageName, app.shortcutId)) {
                (context as? Activity)?.overridePendingTransition(0, 0)
            } else {
                launchPackage(context, app.packageName, "")
            }
            return
        }
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
            val shortcutId = prefs.loadSlotShortcutId(slot)
            if (shortcutId != null) {
                if (ShortcutHelper.launch(ctx, saved.first, shortcutId)) {
                    (context as? Activity)?.overridePendingTransition(0, 0)
                } else {
                    launchPackage(context, saved.first, "")
                }
            } else {
                launchPackage(context, saved.first, saved.second)
            }
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
        val shortcutId = prefs.loadSlotShortcutId(slot)
        if (shortcutId != null) {
            prefs.getAppRename("$pkg|$shortcutId")?.let { return it }
            return prefs.getSavedShortcuts()
                .find { it.packageName == pkg && it.shortcutId == shortcutId }
                ?.label ?: defaultLabel
        }
        val customName = prefs.getAppRename(pkg)
        if (customName != null) return customName
        return try {
            val appInfo = ctx.packageManager.getApplicationInfo(pkg, 0)
            ctx.packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            defaultLabel
        }
    }

    fun saveShortcut(slot: String, packageName: String, activityName: String, shortcutId: String = "") {
        prefs.saveShortcut(slot, packageName, activityName, shortcutId)
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
            "center" -> {
                val intent = Intent(Intent.ACTION_VIEW, android.provider.ContactsContract.Contacts.CONTENT_URI)
                val ri = ctx.packageManager.resolveActivity(intent, 0)
                ri?.activityInfo?.packageName
            }
            else -> null
        }
    }

    private fun String.takeIfInstalled(): String? {
        return try {
            ctx.packageManager.getApplicationInfo(this, 0)
            this
        } catch (_: Exception) { null }
    }

    fun refreshNotifications() {
        val counts = NotificationListener.getAllCounts()
        notificationCounts = if (directBadgeHelper != null) {
            val merged = counts.toMutableMap()
            directBadgeHelper.getCounts().forEach { (pkg, n) -> merged[pkg] = (merged[pkg] ?: 0) + n }
            merged
        } else {
            counts
        }
    }

    fun clearNotificationsFor(pkg: String) {
        NotificationListener.cancelFor(pkg)
    }

    fun hasMissedCalls(): Boolean = directBadgeHelper?.hasMissedCalls() == true

    fun canWriteCallLog(): Boolean = directBadgeHelper?.hasWriteCallLogPermission() == true

    fun clearMissedCalls() {
        directBadgeHelper?.clearMissedCalls()
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

    fun setScreensaverWallpaper(context: Context, uri: Uri) {
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            if (bitmap == null) return
            val file = File(ctx.filesDir, "screensaver_wallpaper.png")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            prefs.screensaverWallpaperPath = file.absolutePath
        } catch (_: Exception) {}
    }

    fun clearScreensaverWallpaper() {
        prefs.screensaverWallpaperPath?.let { File(it).delete() }
        prefs.screensaverWallpaperPath = null
    }

    private fun iconFile(key: String): File {
        val dir = File(ctx.filesDir, "icons").apply { mkdirs() }
        val safe = key.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val name = if (safe == key) safe
        else "${safe.take(60)}_${key.hashCode().toUInt().toString(16)}"
        return File(dir, "$name.png")
    }

    fun saveMaterialIcon(key: String, bitmap: Bitmap) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val file = iconFile(key)
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                prefs.setIconOverride(key, "file:${file.absolutePath}")
                IconUtility.clearCacheFor(key)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    shortcutRefresh++
                    loadApps()
                }
            } catch (_: Exception) {}
        }
    }

    fun beginIconImport(key: String) {
        iconOverrideTarget = key
        prefs.pendingIconTarget = key
    }

    fun saveImportedIcon(context: Context, uri: Uri) {
        val key = iconOverrideTarget ?: prefs.pendingIconTarget
        iconOverrideTarget = null
        prefs.pendingIconTarget = null
        if (key == null) {
            iconImportError = true
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val mime = context.contentResolver.getType(uri).orEmpty()
                val isSvg = mime == "image/svg+xml" ||
                    uri.toString().endsWith(".svg", ignoreCase = true)

                val targetPx = 256
                val bitmap: Bitmap? = if (isSvg) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        com.gezimos.katapult.util.SvgRasterizer.rasterize(input, targetPx)
                    }
                } else {
                    decodeSampled(context, uri, targetPx * 2)?.let { fitToIcon(it, targetPx) }
                }
                if (bitmap == null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        iconImportError = true
                    }
                    return@launch
                }

                val file = iconFile(key)
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                prefs.setIconOverride(key, "file:${file.absolutePath}")
                IconUtility.clearCacheFor(key)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    shortcutRefresh++
                    loadApps()
                }
            } catch (_: Throwable) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    iconImportError = true
                }
            }
        }
    }

    private fun decodeSampled(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) return null

            var sample = 1
            while (srcW / sample > maxSide || srcH / sample > maxSide) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun cancelIconImport() {
        iconOverrideTarget = null
        prefs.pendingIconTarget = null
    }

    fun setBundledIcon(key: String, resName: String) {
        deleteOverrideFile(key)
        prefs.setIconOverride(key, "res:$resName")
        IconUtility.clearCacheFor(key)
        shortcutRefresh++
    }

    fun clearIconOverride(key: String) {
        deleteOverrideFile(key)
        prefs.clearIconOverride(key)
        IconUtility.clearCacheFor(key)
        shortcutRefresh++
    }

    private fun deleteOverrideFile(key: String) {
        val existing = prefs.getIconOverride(key) ?: return
        if (existing.startsWith("file:")) {
            File(existing.removePrefix("file:")).delete()
        }
    }

    private fun fitToIcon(source: Bitmap, targetPx: Int): Bitmap {
        val target = createBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(target)
        val maxSide = kotlin.math.max(source.width, source.height).toFloat().coerceAtLeast(1f)
        val scale = (targetPx * 0.6f) / maxSide
        val drawnW = source.width * scale
        val drawnH = source.height * scale
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate((targetPx - drawnW) / 2f, (targetPx - drawnH) / 2f)
        }
        canvas.drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        return target
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
        try { ctx.contentResolver.unregisterContentObserver(weatherObserver) } catch (_: Exception) {}
    }
}
