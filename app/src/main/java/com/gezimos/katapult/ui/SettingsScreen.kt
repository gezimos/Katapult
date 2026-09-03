package com.gezimos.katapult.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.gezimos.katapult.lockscreen.LockscreenWidgetService
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gezimos.katapult.MainViewModel
import com.gezimos.katapult.R
import com.gezimos.katapult.util.AppLoader
import com.gezimos.katapult.MainActivity
import com.gezimos.katapult.util.DeviceHelper
import com.gezimos.katapult.util.EinkHelper
import com.gezimos.katapult.util.PrefsManager
import com.gezimos.katapult.util.IconUtility
import com.gezimos.katapult.util.ShortcutHelper
import com.gezimos.katapult.util.UpdateChecker
import com.gezimos.katapult.util.UsageHelper
import kotlinx.coroutines.launch
import kotlin.math.abs

private val ClockFormats = listOf("system", "HH:mm", "hh:mm a", "h:mm a", "h:mm")
private val DateFormats = listOf("system", "EEE, MMM d", "EEEE, MMMM d", "MMMM d, yyyy", "M/d/yyyy", "yyyy-MM-dd")

private fun formatLabel(pattern: String): String =
    if (pattern == "system") "System"
    else java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(java.util.Date())

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val prefs = viewModel.prefs
    val isMudita = remember { DeviceHelper.isMuditaKompakt() }
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) { "" }
    }

    var notificationIndicators by remember { mutableStateOf(prefs.notificationIndicators) }
    var clockFormat by remember { mutableStateOf(prefs.clockFormat) }
    var dateFormat by remember { mutableStateOf(prefs.dateFormat) }
    var showBattery by remember { mutableStateOf(prefs.showBattery) }
    var showAlarm by remember { mutableStateOf(prefs.showAlarm) }
    var showWeather by remember { mutableStateOf(prefs.showWeather) }
    var roundedIcons by remember { mutableStateOf(prefs.roundedIcons) }
    var iconSize by remember { mutableIntStateOf(prefs.iconSize) }
    var darkMode by remember { mutableStateOf(prefs.darkMode) }
    var hideStatusBar by remember { mutableStateOf(prefs.hideStatusBar) }
    var hideStatusBarClock by remember { mutableStateOf(prefs.hideStatusBarClock) }
    var showClockCoverSheet by remember { mutableStateOf(false) }
    var einkRefreshOnHome by remember { mutableStateOf(prefs.einkRefreshOnHome) }
    var einkHelperMode by remember { mutableIntStateOf(prefs.einkHelperMode) }
    var doubleTapAction by remember { mutableIntStateOf(prefs.doubleTapAction) }
    var homeExtraRow by remember { mutableStateOf(prefs.homeExtraRow) }
    var disableMusicWidget by remember { mutableStateOf(prefs.disableMusicWidget) }
    var infiniteScroll by remember { mutableStateOf(prefs.infiniteScroll) }
    var showKatapultIcon by remember { mutableStateOf(prefs.showKatapultIcon) }
    var hideAppNames by remember { mutableStateOf(prefs.hideAppNames) }
    var hideArrowButtons by remember { mutableStateOf(prefs.hideArrowButtons) }
    var homeLongPress by remember { mutableIntStateOf(prefs.homeLongPressAction) }
    var showLongPressSheet by remember { mutableStateOf(false) }
    var appSortMode by remember { mutableIntStateOf(prefs.appSortMode) }
    var showSortSheet by remember { mutableStateOf(false) }
    var pendingSortMode by remember { mutableStateOf<Int?>(null) }
    var showClockFormatSheet by remember { mutableStateOf(false) }
    var showDateFormatSheet by remember { mutableStateOf(false) }
    var showDoubleTapSheet by remember { mutableStateOf(false) }
    var showEinkModeSheet by remember { mutableStateOf(false) }
    var showScreensaverModeSheet by remember { mutableStateOf(false) }
    var showScreensaverCustomSheet by remember { mutableStateOf(false) }
    var hideAllAppsButton by remember { mutableStateOf(prefs.hideAllAppsButton) }
    var swipeUpAllApps by remember { mutableStateOf(prefs.swipeUpAllApps) }
    var homeIslands by remember { mutableStateOf(prefs.homeIslands) }
    var verticalAppGestures by remember { mutableStateOf(prefs.verticalAppGestures) }
    var lockscreenWidget by remember { mutableStateOf(prefs.lockscreenWidget) }
    var lockscreenMusicWidget by remember { mutableStateOf(prefs.lockscreenMusicWidget) }
    var actionServiceEnabled by remember { mutableStateOf(LockscreenWidgetService.isEnabled(context)) }
    val pendingServiceToggles = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var showLockscreenApps by remember { mutableStateOf(false) }
    var showLockscreenReadMe by remember { mutableStateOf(false) }
    var showNotificationsReadMe by remember { mutableStateOf(false) }
    var showScreensaverReadMe by remember { mutableStateOf(false) }
    var showEinkReadMe by remember { mutableStateOf(false) }
    var showGesturesReadMe by remember { mutableStateOf(false) }
    var showUpdateSheet by remember { mutableStateOf(false) }
    var showPermissionsSheet by remember { mutableStateOf(false) }
    var showConfigSheet by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val configExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val ok = try {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    out.write(prefs.exportToJson(context).toByteArray())
                }
                true
            } catch (_: Exception) {
                false
            }
            Toast.makeText(
                context,
                if (ok) R.string.config_exported else R.string.config_export_failed,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
    val configImporter = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val text = try {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
            } catch (_: Exception) {
                null
            }
            if (text != null && prefs.importFromJson(text)) {
                ShortcutHelper.syncAllPins(context, prefs)
                restartApp(context)
            } else {
                Toast.makeText(context, R.string.config_import_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    var lockscreenLatestFirst by remember { mutableStateOf(prefs.lockscreenWidgetLatestFirst) }
    var screensaverIslands by remember { mutableStateOf(prefs.screensaverIslands) }
    var screensaverWallpaper by remember { mutableStateOf(prefs.screensaverWallpaper) }
    var screensaverWallpaperPath by remember { mutableStateOf(prefs.screensaverWallpaperPath) }
    val screensaverWallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.setScreensaverWallpaper(context, it)
            screensaverWallpaperPath = prefs.screensaverWallpaperPath
        }
    }
    var screensaverEnabled by remember { mutableStateOf(prefs.screensaverEnabled) }
    var screensaverShowClock by remember { mutableStateOf(prefs.screensaverShowClock) }
    var screensaverShowNotifications by remember { mutableStateOf(prefs.screensaverShowNotifications) }
    var screensaverDoubleTapBrightness by remember { mutableStateOf(prefs.screensaverDoubleTapBrightness) }
    var screensaverUpdateMode by remember { mutableIntStateOf(prefs.screensaverUpdateMode) }
    var screensaverUpdateMinutes by remember { mutableIntStateOf(prefs.screensaverUpdateMinutes) }
    var screensaverEinkRefresh by remember { mutableStateOf(prefs.screensaverEinkRefresh) }
    var screensaverOnPower by remember { mutableStateOf(prefs.screensaverOnPower) }

    // Poll notification listener permission to sync toggle state
    LaunchedEffect(context) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val hasPermission = NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
            if (notificationIndicators != hasPermission) {
                notificationIndicators = hasPermission
                prefs.notificationIndicators = hasPermission
            }
            val serviceOn = LockscreenWidgetService.isEnabled(context)
            if (actionServiceEnabled != serviceOn) actionServiceEnabled = serviceOn
            if (serviceOn && pendingServiceToggles.isNotEmpty()) {
                for (pending in pendingServiceToggles.toList()) {
                    when (pending) {
                        "clockCover" -> {
                            hideStatusBarClock = true
                            prefs.hideStatusBarClock = true
                            LockscreenWidgetService.onLauncherForeground(true)
                        }
                        "widget" -> {
                            lockscreenWidget = true
                            prefs.lockscreenWidget = true
                        }
                        "music" -> {
                            lockscreenMusicWidget = true
                            prefs.lockscreenMusicWidget = true
                        }
                        "power" -> {
                            screensaverOnPower = true
                            prefs.screensaverOnPower = true
                        }
                    }
                }
                pendingServiceToggles.clear()
            }
            val sortPending = pendingSortMode
            if (sortPending != null && UsageHelper.hasPermission(context)) {
                appSortMode = sortPending
                prefs.appSortMode = sortPending
                pendingSortMode = null
                viewModel.loadApps()
            }
        }
    }

    var currentPage by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(1) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var selectedCategory by rememberSaveable { mutableStateOf<Int?>(null) }

    BackHandler(enabled = selectedCategory != null) { selectedCategory = null }

    LaunchedEffect(pageCount) {
        if (currentPage >= pageCount) currentPage = (pageCount - 1).coerceAtLeast(0)
    }
    LaunchedEffect(selectedCategory) { currentPage = 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalSurface.current)
            .statusBarsPadding()
            .navigationBarsPadding()
            .then(
                if (selectedCategory == null) Modifier.padding(6.dp)
                else Modifier.padding(16.dp)
            ),
    ) {
        if (selectedCategory != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectedCategory != null) {
                Text(
                    text = "‹",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LatoFamily,
                    color = LocalInk.current,
                    modifier = Modifier
                        .clickable { selectedCategory = null }
                        .padding(end = 12.dp),
                )
            }
            Text(
                text = selectedCategory?.let { stringResource(it) }
                    ?: stringResource(R.string.settings),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.weight(1f),
            )
            if (selectedCategory != null && pageCount > 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (i in 0 until pageCount) {
                        Box(
                            Modifier
                                .padding(horizontal = 2.dp)
                                .size(6.dp)
                                .then(
                                    if (i == currentPage) Modifier.background(LocalInk.current, CircleShape)
                                    else Modifier.border(1.5.dp, LocalInk.current, CircleShape)
                                )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        }

        val headerIndices = mutableSetOf<Int>()
        val sectionTitles = mutableListOf<Int>()
        val sectionStarts = mutableListOf<Int>()
        val rows = buildList<@Composable () -> Unit> {
            fun addHeader(titleRes: Int) {
                sectionTitles += titleRes
                sectionStarts += size
                headerIndices += size
                add { SettingsSectionHeader(stringResource(titleRes)) }
            }
            // --- Appearance ---
            addHeader(R.string.section_appearance)
            add {
                SettingsCycleRow(
                    title = stringResource(R.string.icon_shape),
                    description = if (roundedIcons) stringResource(R.string.icon_shape_rounded) else stringResource(R.string.icon_shape_circle),
                    onClick = {
                        roundedIcons = !roundedIcons
                        prefs.roundedIcons = roundedIcons
                        viewModel.roundedIcons = roundedIcons
                    },
                )
            }
            add {
                SettingsCycleRow(
                    title = stringResource(R.string.icon_size),
                    description = stringResource(R.string.icon_size_value, iconSize, iconSize),
                    onClick = {
                        iconSize = if (iconSize == PrefsManager.ICON_SIZE_SMALL)
                            PrefsManager.ICON_SIZE_LARGE else PrefsManager.ICON_SIZE_SMALL
                        prefs.iconSize = iconSize
                        viewModel.iconSize = iconSize
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.dark_mode),
                    description = stringResource(R.string.dark_mode_desc),
                    checked = darkMode,
                    onCheckedChange = {
                        darkMode = it
                        prefs.darkMode = it
                        viewModel.darkMode = it
                        viewModel.applyStatusBar(context)
                    },
                )
            }
            if (isMudita) {
                add {
                    SettingsToggleRow(
                        title = stringResource(R.string.hide_status_bar_clock),
                        description = stringResource(R.string.hide_status_bar_clock_desc),
                        checked = hideStatusBarClock && actionServiceEnabled,
                        onCheckedChange = { requested ->
                            val serviceOn = LockscreenWidgetService.isEnabled(context)
                            actionServiceEnabled = serviceOn
                            val value = requested && serviceOn
                            hideStatusBarClock = value
                            prefs.hideStatusBarClock = value
                            if (requested && !serviceOn) {
                                if ("clockCover" !in pendingServiceToggles) pendingServiceToggles.add("clockCover")
                                showClockCoverSheet = true
                            } else {
                                pendingServiceToggles.remove("clockCover")
                            }
                            LockscreenWidgetService.onLauncherForeground(true)
                        },
                    )
                }
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.hide_status_bar),
                    description = stringResource(R.string.hide_status_bar_desc),
                    checked = hideStatusBar,
                    onCheckedChange = {
                        hideStatusBar = it
                        prefs.hideStatusBar = it
                        viewModel.applyStatusBar(context)
                        LockscreenWidgetService.onLauncherForeground(true)
                    },
                )
            }

            add {
                SettingsToggleRow(
                    title = stringResource(R.string.hide_app_names),
                    description = stringResource(R.string.hide_app_names_desc),
                    checked = hideAppNames,
                    onCheckedChange = {
                        hideAppNames = it
                        prefs.hideAppNames = it
                    },
                )
            }

            // --- Home Screen ---
            addHeader(R.string.section_home)
            add {
                SettingsCycleRow(
                    title = stringResource(R.string.clock_format),
                    description = formatLabel(clockFormat),
                    onClick = { showClockFormatSheet = true },
                )
            }
            add {
                SettingsCycleRow(
                    title = stringResource(R.string.date_format),
                    description = formatLabel(dateFormat),
                    onClick = { showDateFormatSheet = true },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.show_alarm),
                    description = stringResource(R.string.show_alarm_desc),
                    checked = showAlarm,
                    onCheckedChange = {
                        showAlarm = it
                        prefs.showAlarm = it
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.show_battery),
                    description = stringResource(R.string.show_battery_desc),
                    checked = showBattery,
                    onCheckedChange = {
                        showBattery = it
                        prefs.showBattery = it
                    },
                )
            }
            if (isMudita) {
                add {
                    SettingsToggleRow(
                        title = stringResource(R.string.show_weather),
                        description = stringResource(R.string.show_weather_desc),
                        checked = showWeather,
                        onCheckedChange = {
                            showWeather = it
                            prefs.showWeather = it
                            viewModel.refreshWeather()
                        },
                    )
                }
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.extra_dock_row),
                    description = stringResource(R.string.extra_dock_row_desc),
                    checked = homeExtraRow,
                    onCheckedChange = {
                        homeExtraRow = it
                        prefs.homeExtraRow = it
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.home_islands),
                    description = stringResource(R.string.home_islands_desc),
                    checked = homeIslands,
                    onCheckedChange = {
                        homeIslands = it
                        prefs.homeIslands = it
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.disable_music_widget),
                    description = stringResource(R.string.disable_music_widget_desc),
                    checked = disableMusicWidget,
                    onCheckedChange = {
                        disableMusicWidget = it
                        prefs.disableMusicWidget = it
                    },
                )
            }
            add {
                val longPressLabel = when (homeLongPress) {
                    PrefsManager.HOME_LONG_PRESS_NONE ->
                        stringResource(R.string.home_long_press_none)
                    PrefsManager.HOME_LONG_PRESS_APP_INFO ->
                        stringResource(R.string.home_long_press_app_info)
                    PrefsManager.HOME_LONG_PRESS_MENU ->
                        stringResource(R.string.home_long_press_menu)
                    PrefsManager.HOME_LONG_PRESS_CLEAR ->
                        stringResource(R.string.home_long_press_clear)
                    else -> stringResource(R.string.home_long_press_edit)
                }
                SettingsCycleRow(
                    title = stringResource(R.string.home_long_press),
                    description = stringResource(R.string.home_long_press_desc),
                    value = longPressLabel,
                    onClick = { showLongPressSheet = true },
                )
            }

            // --- All Apps ---
            addHeader(R.string.section_all_apps)
            add {
                val sortLabel = when (appSortMode) {
                    PrefsManager.APP_SORT_RECENT -> stringResource(R.string.app_sort_recent)
                    PrefsManager.APP_SORT_USED -> stringResource(R.string.app_sort_used)
                    else -> stringResource(R.string.app_sort_alphabetical)
                }
                SettingsCycleRow(
                    title = stringResource(R.string.app_sort_order),
                    description = stringResource(R.string.app_sort_order_desc),
                    value = sortLabel,
                    onClick = { showSortSheet = true },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.show_katapult_icon),
                    description = stringResource(R.string.show_katapult_icon_desc),
                    checked = showKatapultIcon,
                    onCheckedChange = {
                        showKatapultIcon = it
                        prefs.showKatapultIcon = it
                        viewModel.loadApps()
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.hide_all_apps_button),
                    description = stringResource(R.string.hide_all_apps_button_desc),
                    checked = hideAllAppsButton,
                    onCheckedChange = {
                        hideAllAppsButton = it
                        prefs.hideAllAppsButton = it
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.infinite_scroll),
                    description = stringResource(R.string.infinite_scroll_desc),
                    checked = infiniteScroll,
                    onCheckedChange = {
                        infiniteScroll = it
                        prefs.infiniteScroll = it
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.hide_arrow_buttons),
                    description = stringResource(R.string.hide_arrow_buttons_desc),
                    checked = hideArrowButtons,
                    onCheckedChange = {
                        hideArrowButtons = it
                        prefs.hideArrowButtons = it
                    },
                )
            }

            // --- Gestures ---
            addHeader(R.string.section_gestures)
            add {
                SettingsActionRow(
                    title = stringResource(R.string.lockscreen_readme),
                    description = stringResource(R.string.gestures_readme_desc),
                    onClick = { showGesturesReadMe = true },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.swipe_up_all_apps),
                    description = stringResource(R.string.swipe_up_all_apps_desc),
                    checked = swipeUpAllApps,
                    onCheckedChange = {
                        swipeUpAllApps = it
                        prefs.swipeUpAllApps = it
                    },
                )
            }
            add {
                SettingsCycleRow(
                    title = stringResource(R.string.app_gesture_direction),
                    description = if (verticalAppGestures) stringResource(R.string.gesture_vertical)
                        else stringResource(R.string.gesture_horizontal),
                    onClick = {
                        verticalAppGestures = !verticalAppGestures
                        prefs.verticalAppGestures = verticalAppGestures
                    },
                )
            }
            add {
                val actionLabel = when (doubleTapAction) {
                    PrefsManager.DOUBLE_TAP_BRIGHTNESS -> stringResource(R.string.double_tap_brightness)
                    PrefsManager.DOUBLE_TAP_LOCK -> stringResource(R.string.double_tap_lock)
                    else -> stringResource(R.string.double_tap_off)
                }
                SettingsCycleRow(
                    title = stringResource(R.string.double_tap_action),
                    description = actionLabel,
                    onClick = { showDoubleTapSheet = true },
                )
            }

            // --- Notifications ---
            addHeader(R.string.section_notifications)
            add {
                SettingsActionRow(
                    title = stringResource(R.string.lockscreen_readme),
                    description = stringResource(R.string.notifications_readme_desc),
                    onClick = { showNotificationsReadMe = true },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.notification_indicators),
                    description = stringResource(R.string.notification_indicators_desc),
                    checked = notificationIndicators,
                    onCheckedChange = { requested ->
                        val hasPermission = NotificationManagerCompat.getEnabledListenerPackages(context)
                            .contains(context.packageName)
                        if (requested) {
                            if (hasPermission) {
                                notificationIndicators = true
                                prefs.notificationIndicators = true
                            } else {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                } catch (_: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    })
                                }
                            }
                        } else {
                            try {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            } catch (_: Exception) {}
                        }
                    },
                )
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.app_notifications),
                    description = stringResource(R.string.app_notifications_desc),
                    onClick = {
                        try {
                            context.startActivity(Intent().apply {
                                component = android.content.ComponentName(
                                    "com.android.settings",
                                    "com.android.settings.Settings\$NotificationAppListActivity"
                                )
                            })
                        } catch (_: Exception) {}
                    },
                )
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.notification_log),
                    description = stringResource(R.string.notification_log_desc),
                    onClick = {
                        try {
                            context.startActivity(Intent().apply {
                                component = android.content.ComponentName(
                                    "com.android.settings",
                                    "com.android.settings.Settings\$NotificationStationActivity"
                                )
                            })
                        } catch (_: Exception) {}
                    },
                )
            }

            // --- E-Ink (modes are Mudita only) ---
            addHeader(R.string.section_eink)
            if (isMudita) {
                add {
                    SettingsActionRow(
                        title = stringResource(R.string.lockscreen_readme),
                        description = stringResource(R.string.eink_readme_desc),
                        onClick = { showEinkReadMe = true },
                    )
                }
                add {
                    SettingsCycleRow(
                        title = stringResource(R.string.eink_auto_mode),
                        description = EinkHelper.modeName(einkHelperMode),
                        onClick = { showEinkModeSheet = true },
                    )
                }
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.eink_refresh),
                    description = stringResource(R.string.eink_refresh_desc),
                    checked = einkRefreshOnHome,
                    onCheckedChange = {
                        einkRefreshOnHome = it
                        prefs.einkRefreshOnHome = it
                    },
                )
            }

            // --- System ---
            addHeader(R.string.section_system)
            add {
                SettingsActionRow(
                    title = stringResource(R.string.set_default_launcher),
                    description = stringResource(R.string.set_default_launcher_desc),
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                    },
                )
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.search_settings),
                    description = stringResource(R.string.search_settings_desc),
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_APP_SEARCH_SETTINGS))
                        } catch (_: Exception) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            } catch (_: Exception) {}
                        }
                    },
                )
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.permissions),
                    description = stringResource(R.string.permissions_desc),
                    onClick = { showPermissionsSheet = true },
                )
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.config_backup),
                    description = stringResource(R.string.config_backup_desc),
                    onClick = { showConfigSheet = true },
                )
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.check_updates),
                    description = stringResource(R.string.check_updates_desc),
                    onClick = { showUpdateSheet = true },
                )
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.lic_app_title),
                    description = stringResource(R.string.lic_app_desc),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gezimos/Katapult/blob/main/LICENSE"))
                        )
                    },
                )
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.lic_font_title),
                    description = stringResource(R.string.lic_font_desc),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gezimos/Katapult/blob/main/LICENSE-Lato.txt"))
                        )
                    },
                )
            }

            // --- About ---
            addHeader(R.string.section_about)
            add {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LatoFamily,
                            color = LocalInk.current,
                        )
                        Text(
                            text = versionName,
                            fontSize = 14.sp,
                            fontFamily = LatoFamily,
                            color = LocalInk.current,
                        )
                    }
                }
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.donate_label),
                    description = stringResource(R.string.donate_desc),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/gezimos"))
                        )
                    },
                )
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.github_label),
                    description = stringResource(R.string.github_desc),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gezimos"))
                        )
                    },
                )
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.youtube_label),
                    description = stringResource(R.string.youtube_desc),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@gezimos"))
                        )
                    },
                )
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.inkos_label),
                    description = stringResource(R.string.inkos_desc),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gezimos/inkOS"))
                        )
                    },
                )
            }

            // --- Experimental ---
            addHeader(R.string.section_experimental)
            add {
                SettingsActionRow(
                    title = stringResource(R.string.lockscreen_readme),
                    description = stringResource(R.string.lockscreen_readme_desc),
                    onClick = { showLockscreenReadMe = true },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.lockscreen_widget),
                    description = stringResource(R.string.lockscreen_widget_desc),
                    checked = lockscreenWidget && actionServiceEnabled,
                    onCheckedChange = { requested ->
                        val serviceOn = LockscreenWidgetService.isEnabled(context)
                        actionServiceEnabled = serviceOn
                        val value = requested && serviceOn
                        lockscreenWidget = value
                        prefs.lockscreenWidget = value
                        if (requested && !serviceOn) {
                            if ("widget" !in pendingServiceToggles) pendingServiceToggles.add("widget")
                            try {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } catch (_: Exception) {}
                        } else {
                            pendingServiceToggles.remove("widget")
                        }
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.lockscreen_music_widget),
                    description = stringResource(R.string.lockscreen_music_widget_desc),
                    checked = lockscreenMusicWidget && actionServiceEnabled,
                    onCheckedChange = { requested ->
                        val serviceOn = LockscreenWidgetService.isEnabled(context)
                        actionServiceEnabled = serviceOn
                        val value = requested && serviceOn
                        lockscreenMusicWidget = value
                        prefs.lockscreenMusicWidget = value
                        if (requested && !serviceOn) {
                            if ("music" !in pendingServiceToggles) pendingServiceToggles.add("music")
                            try {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } catch (_: Exception) {}
                        } else {
                            pendingServiceToggles.remove("music")
                        }
                    },
                )
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.lockscreen_widget_apps),
                    description = stringResource(R.string.lockscreen_widget_apps_desc),
                    onClick = { showLockscreenApps = true },
                )
            }
            add {
                SettingsCycleRow(
                    title = stringResource(R.string.lockscreen_sort),
                    description = if (lockscreenLatestFirst) stringResource(R.string.sort_latest_top)
                        else stringResource(R.string.sort_oldest_top),
                    onClick = {
                        lockscreenLatestFirst = !lockscreenLatestFirst
                        prefs.lockscreenWidgetLatestFirst = lockscreenLatestFirst
                    },
                )
            }
            addHeader(R.string.section_screensaver)
            add {
                SettingsActionRow(
                    title = stringResource(R.string.lockscreen_readme),
                    description = stringResource(R.string.screensaver_readme_desc),
                    onClick = { showScreensaverReadMe = true },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.screensaver),
                    description = stringResource(R.string.screensaver_enable_desc),
                    checked = screensaverEnabled,
                    onCheckedChange = {
                        screensaverEnabled = it
                        prefs.screensaverEnabled = it
                    },
                )
            }
            if (screensaverEnabled) {
                add {
                    SettingsActionRow(
                        title = stringResource(R.string.screensaver_system),
                        description = stringResource(R.string.screensaver_settings_desc),
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS))
                            } catch (_: Exception) {}
                        },
                    )
                }
                add {
                    val enabled = listOfNotNull(
                        stringResource(R.string.screensaver_show_clock).takeIf { screensaverShowClock },
                        stringResource(R.string.screensaver_show_notifications)
                            .takeIf { screensaverShowNotifications },
                        stringResource(R.string.home_islands).takeIf { screensaverIslands },
                    )
                    SettingsActionRow(
                        title = stringResource(R.string.screensaver_customization),
                        description = if (enabled.isEmpty()) {
                            stringResource(R.string.double_tap_off)
                        } else {
                            enabled.joinToString(", ")
                        },
                        onClick = { showScreensaverCustomSheet = true },
                    )
                }
                add {
                    SettingsToggleRow(
                        title = stringResource(R.string.screensaver_wallpaper),
                        description = stringResource(R.string.screensaver_wallpaper_desc),
                        checked = screensaverWallpaper,
                        onCheckedChange = {
                            screensaverWallpaper = it
                            prefs.screensaverWallpaper = it
                        },
                    )
                }
                if (screensaverWallpaper) {
                    add {
                        val hasImage = screensaverWallpaperPath != null
                        SettingsActionRow(
                            title = if (hasImage) stringResource(R.string.clear_image)
                                else stringResource(R.string.screensaver_image),
                            description = if (hasImage) stringResource(R.string.screensaver_wallpaper_custom)
                                else stringResource(R.string.screensaver_wallpaper_home),
                            onClick = {
                                if (hasImage) {
                                    viewModel.clearScreensaverWallpaper()
                                    screensaverWallpaperPath = null
                                } else {
                                    screensaverWallpaperPicker.launch("image/*")
                                }
                            },
                        )
                    }
                }
                add {
                    SettingsToggleRow(
                        title = stringResource(R.string.screensaver_double_tap_brightness),
                        description = stringResource(R.string.screensaver_double_tap_brightness_desc),
                        checked = screensaverDoubleTapBrightness,
                        onCheckedChange = {
                            screensaverDoubleTapBrightness = it
                            prefs.screensaverDoubleTapBrightness = it
                        },
                    )
                }
                add {
                    val modeLabel = when (screensaverUpdateMode) {
                        PrefsManager.SCREENSAVER_MODE_INTERVAL -> stringResource(R.string.screensaver_mode_interval)
                        PrefsManager.SCREENSAVER_MODE_STATIC -> stringResource(R.string.screensaver_mode_static)
                        else -> stringResource(R.string.screensaver_mode_auto)
                    }
                    SettingsCycleRow(
                        title = stringResource(R.string.screensaver_update_mode),
                        description = modeLabel,
                        onClick = { showScreensaverModeSheet = true },
                    )
                }
                add {
                    SettingsToggleRow(
                        title = stringResource(R.string.screensaver_eink_refresh),
                        description = stringResource(R.string.screensaver_eink_refresh_desc),
                        checked = screensaverEinkRefresh,
                        onCheckedChange = {
                            screensaverEinkRefresh = it
                            prefs.screensaverEinkRefresh = it
                        },
                    )
                }
                add {
                    SettingsToggleRow(
                        title = stringResource(R.string.screensaver_on_power),
                        description = stringResource(R.string.screensaver_on_power_desc),
                        checked = screensaverOnPower && actionServiceEnabled,
                        onCheckedChange = { requested ->
                            val serviceOn = LockscreenWidgetService.isEnabled(context)
                            actionServiceEnabled = serviceOn
                            val value = requested && serviceOn
                            screensaverOnPower = value
                            prefs.screensaverOnPower = value
                            if (requested && !serviceOn) {
                                if ("power" !in pendingServiceToggles) pendingServiceToggles.add("power")
                                try {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                } catch (_: Exception) {}
                            } else {
                                pendingServiceToggles.remove("power")
                            }
                        },
                    )
                }
            }
        }

        if (selectedCategory == null) {
            SettingsCategoryGrid(
                onSelect = { titleRes -> selectedCategory = titleRes },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        } else {
            val sel = sectionTitles.indexOf(selectedCategory).coerceIn(0, (sectionStarts.size - 1).coerceAtLeast(0))
            val start = sectionStarts[sel] + 1
            val end = sectionStarts.getOrElse(sel + 1) { rows.size }
            val activeRows = rows.subList(start, end)
            SubcomposeLayout(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(sel) {
                        detectVerticalDragGestures(
                            onDragStart = { dragAccumulator = 0f },
                            onDragEnd = {
                                if (abs(dragAccumulator) > 80f && pageCount > 1) {
                                    val delta = if (dragAccumulator < 0) 1 else -1
                                    currentPage = (currentPage + delta).coerceIn(0, pageCount - 1)
                                }
                            },
                            onVerticalDrag = { _, amount -> dragAccumulator += amount },
                        )
                    },
            ) { constraints ->
                val childConstraints = Constraints(maxWidth = constraints.maxWidth)
                val rowPlaceables = activeRows.mapIndexed { i, content ->
                    subcompose(i) { content() }.map { it.measure(childConstraints) }
                }
                val rowHeights = rowPlaceables.map { ps -> ps.sumOf { it.height } }

                val groups = mutableListOf<IntRange>()
                var groupStart = 0
                var accH = 0
                for (i in rowHeights.indices) {
                    if (accH + rowHeights[i] > constraints.maxHeight && i > groupStart) {
                        groups.add(groupStart until i)
                        groupStart = i
                        accH = rowHeights[i]
                    } else {
                        accH += rowHeights[i]
                    }
                }
                if (groupStart < rowHeights.size) groups.add(groupStart until rowHeights.size)
                if (groups.isEmpty()) groups.add(0 until 0)

                if (pageCount != groups.size) pageCount = groups.size

                val page = currentPage.coerceIn(0, groups.size - 1)
                val visible = groups[page]

                layout(constraints.maxWidth, constraints.maxHeight) {
                    var y = 0
                    for (i in visible) {
                        for (p in rowPlaceables[i]) {
                            p.place(0, y)
                            y += p.height
                        }
                    }
                }
            }
        }
    }

    if (showLockscreenApps) {
        LockscreenAppsDialog(
            viewModel = viewModel,
            onDismiss = { showLockscreenApps = false },
        )
    }

    if (showLockscreenReadMe) {
        BottomSheet(onDismiss = { showLockscreenReadMe = false }) {
            Text(
                text = stringResource(R.string.lockscreen_readme),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.lockscreen_readme_privacy),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.a11y_shortcut_warning),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.lockscreen_readme_usage),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
    }

    if (showNotificationsReadMe) {
        BottomSheet(onDismiss = { showNotificationsReadMe = false }) {
            Text(
                text = stringResource(R.string.lockscreen_readme),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.notifications_readme_control),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.notifications_readme_debug),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
    }

    if (showScreensaverReadMe) {
        BottomSheet(onDismiss = { showScreensaverReadMe = false }) {
            Text(
                text = stringResource(R.string.lockscreen_readme),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.screensaver_readme_accessibility),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.a11y_shortcut_warning),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.screensaver_readme_about),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.screensaver_readme_use),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.screensaver_readme_modes),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
    }

    if (showEinkReadMe) {
        BottomSheet(onDismiss = { showEinkReadMe = false }) {
            Text(
                text = stringResource(R.string.lockscreen_readme),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.eink_readme_intro),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.eink_readme_modes),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
    }

    if (showGesturesReadMe) {
        BottomSheet(onDismiss = { showGesturesReadMe = false }) {
            Text(
                text = stringResource(R.string.lockscreen_readme),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.gestures_readme_nav),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.gestures_readme_apps),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
    }

    if (showUpdateSheet) {
        UpdateDialog(
            viewModel = viewModel,
            currentVersion = versionName,
            onDismiss = { showUpdateSheet = false },
        )
    }

    if (showPermissionsSheet) {
        var permTick by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                permTick++
            }
        }
        val notifAccess = remember(permTick) {
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        }
        val serviceOn = remember(permTick) { LockscreenWidgetService.isEnabled(context) }
        val callLog = remember(permTick) {
            context.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        val sms = remember(permTick) {
            context.checkSelfPermission(android.Manifest.permission.READ_SMS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        val writeSettings = remember(permTick) { Settings.System.canWrite(context) }
        val installApps = remember(permTick) { context.packageManager.canRequestPackageInstalls() }
        val usageAccess = remember(permTick) { UsageHelper.hasPermission(context) }
        val openAppDetails = {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
                )
            } catch (_: Exception) {}
        }
        BottomSheet(onDismiss = { showPermissionsSheet = false }) {
            Text(
                text = stringResource(R.string.permissions),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            PermissionRow(
                title = stringResource(R.string.perm_notification_access),
                description = stringResource(R.string.perm_notification_access_desc),
                checked = notifAccess,
            ) {
                try {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (_: Exception) {}
            }
            PermissionRow(
                title = stringResource(R.string.perm_action_service),
                description = stringResource(R.string.perm_action_service_desc),
                checked = serviceOn,
            ) {
                try {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (_: Exception) {}
            }
            PermissionRow(
                title = stringResource(R.string.perm_call_log),
                description = stringResource(R.string.perm_call_log_desc),
                checked = callLog,
                onClick = openAppDetails,
            )
            PermissionRow(
                title = stringResource(R.string.perm_sms),
                description = stringResource(R.string.perm_sms_desc),
                checked = sms,
                onClick = openAppDetails,
            )
            PermissionRow(
                title = stringResource(R.string.perm_write_settings),
                description = stringResource(R.string.perm_write_settings_desc),
                checked = writeSettings,
            ) {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + context.packageName))
                    )
                } catch (_: Exception) {}
            }
            PermissionRow(
                title = stringResource(R.string.perm_usage_access),
                description = stringResource(R.string.perm_usage_access_desc),
                checked = usageAccess,
            ) {
                UsageHelper.openSettings(context)
            }
            PermissionRow(
                title = stringResource(R.string.perm_install_apps),
                description = stringResource(R.string.perm_install_apps_desc),
                checked = installApps,
            ) {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
                    )
                } catch (_: Exception) {}
            }
        }
    }

    if (showSortSheet) {
        ChoiceSheet(
            title = stringResource(R.string.app_sort_order),
            options = listOf(
                PrefsManager.APP_SORT_MANUAL to stringResource(R.string.app_sort_alphabetical),
                PrefsManager.APP_SORT_RECENT to stringResource(R.string.app_sort_recent),
                PrefsManager.APP_SORT_USED to stringResource(R.string.app_sort_used),
            ),
            selected = appSortMode,
            onSelect = { mode ->
                if (mode == PrefsManager.APP_SORT_MANUAL || UsageHelper.hasPermission(context)) {
                    pendingSortMode = null
                    appSortMode = mode
                    prefs.appSortMode = mode
                    viewModel.loadApps()
                } else {
                    pendingSortMode = mode
                    UsageHelper.openSettings(context)
                }
            },
            onDismiss = { showSortSheet = false },
        )
    }

    if (showLongPressSheet) {
        ChoiceSheet(
            title = stringResource(R.string.home_long_press),
            options = listOf(
                PrefsManager.HOME_LONG_PRESS_APP_INFO to stringResource(R.string.home_long_press_app_info),
                PrefsManager.HOME_LONG_PRESS_MENU to stringResource(R.string.home_long_press_menu),
                PrefsManager.HOME_LONG_PRESS_CLEAR to stringResource(R.string.home_long_press_clear),
                PrefsManager.HOME_LONG_PRESS_EDIT to stringResource(R.string.home_long_press_edit),
                PrefsManager.HOME_LONG_PRESS_NONE to stringResource(R.string.home_long_press_none),
            ),
            selected = homeLongPress,
            onSelect = {
                homeLongPress = it
                prefs.homeLongPressAction = it
            },
            onDismiss = { showLongPressSheet = false },
        )
    }

    if (showEinkModeSheet) {
        ChoiceSheet(
            title = stringResource(R.string.eink_auto_mode),
            options = listOf(
                EinkHelper.MEINK_MODE_DISABLED,
                EinkHelper.MEINK_MODE_CLEAR,
                EinkHelper.MEINK_MODE_CONTRAST,
                EinkHelper.MEINK_MODE_READING,
            ).map { it to EinkHelper.modeName(it) },
            selected = einkHelperMode,
            onSelect = {
                einkHelperMode = it
                prefs.einkHelperMode = it
                (context as? MainActivity)?.setMeinkMode(it)
            },
            onDismiss = { showEinkModeSheet = false },
        )
    }

    if (showScreensaverModeSheet) {
        var minutesText by remember {
            mutableStateOf(
                androidx.compose.ui.text.input.TextFieldValue(
                    text = screensaverUpdateMinutes.toString(),
                    selection = androidx.compose.ui.text.TextRange(
                        screensaverUpdateMinutes.toString().length,
                    ),
                ),
            )
        }
        BottomSheet(onDismiss = { showScreensaverModeSheet = false }, imePadding = true) {
            Text(
                text = stringResource(R.string.screensaver_update_mode),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            listOf(
                PrefsManager.SCREENSAVER_MODE_AUTO to stringResource(R.string.screensaver_mode_auto),
                PrefsManager.SCREENSAVER_MODE_INTERVAL to stringResource(R.string.screensaver_mode_interval),
                PrefsManager.SCREENSAVER_MODE_STATIC to stringResource(R.string.screensaver_mode_static),
            ).forEach { (mode, label) ->
                BottomSheetOption(
                    text = label,
                    icon = if (screensaverUpdateMode == mode) Icons.Rounded.RadioButtonChecked
                    else Icons.Rounded.RadioButtonUnchecked,
                ) {
                    screensaverUpdateMode = mode
                    prefs.screensaverUpdateMode = mode
                    if (mode != PrefsManager.SCREENSAVER_MODE_INTERVAL) {
                        showScreensaverModeSheet = false
                    }
                }
            }
            if (screensaverUpdateMode == PrefsManager.SCREENSAVER_MODE_INTERVAL) {
                Text(
                    text = stringResource(R.string.screensaver_update_interval),
                    fontSize = 14.sp,
                    fontFamily = LatoFamily,
                    color = LocalInk.current,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
                BasicTextField(
                    value = minutesText,
                    onValueChange = { v ->
                        val digits = v.text.filter { it.isDigit() }.take(2)
                        minutesText = v.copy(text = digits)
                        val parsed = digits.toIntOrNull()
                        if (parsed != null && parsed > 0) {
                            screensaverUpdateMinutes = parsed
                            prefs.screensaverUpdateMinutes = parsed
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontFamily = LatoFamily,
                        color = LocalInk.current,
                    ),
                    cursorBrush = SolidColor(LocalInk.current),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.5.dp, LocalInk.current)
                        .padding(12.dp),
                )
            }
        }
    }

    if (showScreensaverCustomSheet) {
        MultiChoiceSheet(
            title = stringResource(R.string.screensaver_customization),
            options = listOf(
                Triple(
                    stringResource(R.string.screensaver_show_clock),
                    screensaverShowClock,
                    { v: Boolean -> screensaverShowClock = v; prefs.screensaverShowClock = v },
                ),
                Triple(
                    stringResource(R.string.screensaver_show_notifications),
                    screensaverShowNotifications,
                    { v: Boolean ->
                        screensaverShowNotifications = v
                        prefs.screensaverShowNotifications = v
                    },
                ),
                Triple(
                    stringResource(R.string.home_islands),
                    screensaverIslands,
                    { v: Boolean -> screensaverIslands = v; prefs.screensaverIslands = v },
                ),
            ),
            onDismiss = { showScreensaverCustomSheet = false },
        )
    }

    if (showDoubleTapSheet) {
        ChoiceSheet(
            title = stringResource(R.string.double_tap_action),
            options = listOf(
                PrefsManager.DOUBLE_TAP_OFF to stringResource(R.string.double_tap_off),
                PrefsManager.DOUBLE_TAP_BRIGHTNESS to stringResource(R.string.double_tap_brightness),
                PrefsManager.DOUBLE_TAP_LOCK to stringResource(R.string.double_tap_lock),
            ),
            selected = doubleTapAction,
            onSelect = {
                doubleTapAction = it
                prefs.doubleTapAction = it
                if (it == PrefsManager.DOUBLE_TAP_LOCK && !LockscreenWidgetService.isEnabled(context)) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } catch (_: Exception) {}
                }
            },
            onDismiss = { showDoubleTapSheet = false },
        )
    }

    if (showClockFormatSheet) {
        ChoiceSheet(
            title = stringResource(R.string.clock_format),
            options = ClockFormats.map { it to formatLabel(it) },
            selected = clockFormat,
            onSelect = {
                clockFormat = it
                prefs.clockFormat = it
                viewModel.updateClock()
            },
            onDismiss = { showClockFormatSheet = false },
        )
    }

    if (showDateFormatSheet) {
        ChoiceSheet(
            title = stringResource(R.string.date_format),
            options = DateFormats.map { it to formatLabel(it) },
            selected = dateFormat,
            onSelect = {
                dateFormat = it
                prefs.dateFormat = it
                viewModel.updateClock()
            },
            onDismiss = { showDateFormatSheet = false },
        )
    }

    if (showClockCoverSheet) {
        BottomSheet(onDismiss = { showClockCoverSheet = false }) {
            Text(
                text = stringResource(R.string.hide_status_bar_clock),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = stringResource(R.string.hide_status_bar_clock_a11y),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = stringResource(R.string.a11y_shortcut_warning),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            BottomSheetOption(
                text = stringResource(R.string.lockscreen_reenable_enable),
                icon = Icons.Rounded.Tune,
            ) {
                showClockCoverSheet = false
                try {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (_: Exception) {}
            }
        }
    }

    if (showConfigSheet) {
        BottomSheet(onDismiss = { showConfigSheet = false }) {
            Text(
                text = stringResource(R.string.config_backup),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            BottomSheetOption(stringResource(R.string.config_export), icon = Icons.Rounded.Upload) {
                showConfigSheet = false
                val stamp = java.text.SimpleDateFormat("yyyy-MM-dd-HHmm", java.util.Locale.US)
                    .format(java.util.Date())
                configExporter.launch("katapult-config-$stamp.json")
            }
            BottomSheetOption(stringResource(R.string.config_import), icon = Icons.Rounded.Download) {
                showConfigSheet = false
                configImporter.launch(arrayOf("application/json", "text/plain", "*/*"))
            }
            BottomSheetOption(stringResource(R.string.config_clear), icon = Icons.Rounded.DeleteForever) {
                showConfigSheet = false
                showClearConfirm = true
            }
        }
    }

    if (showClearConfirm) {
        BottomSheet(onDismiss = { showClearConfirm = false }) {
            Text(
                text = stringResource(R.string.config_clear_confirm),
                fontSize = 18.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            BottomSheetOption(stringResource(R.string.config_clear), icon = Icons.Rounded.DeleteForever) {
                showClearConfirm = false
                prefs.clearAll(context)
                restartApp(context)
            }
        }
    }
}

private fun restartApp(context: Context) {
    val intent = Intent.makeRestartActivityTask(
        ComponentName(context, MainActivity::class.java)
    )
    context.startActivity(intent)
}

private sealed interface UpdateState {
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateChecker.ReleaseInfo) : UpdateState
    data object Error : UpdateState
}

@Composable
private fun UpdateDialog(
    viewModel: MainViewModel,
    currentVersion: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Checking) }

    suspend fun check() {
        state = UpdateState.Checking
        val latest = UpdateChecker.fetchLatest()
        state = when {
            latest == null -> UpdateState.Error
            UpdateChecker.isNewer(latest.tag, currentVersion) -> {
                val ready = viewModel.updateReadyTag
                if (ready != null && ready != latest.tag) viewModel.clearDownloadedUpdate()
                UpdateState.Available(latest)
            }
            else -> {
                viewModel.clearDownloadedUpdate()
                UpdateState.UpToDate
            }
        }
    }

    LaunchedEffect(Unit) { check() }

    BottomSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.check_updates),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
            color = LocalInk.current,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        if (viewModel.updateProgress >= 0) {
            Text(
                text = stringResource(R.string.update_downloading, viewModel.updateProgress),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
            return@BottomSheet
        }
        val readyTag = viewModel.updateReadyTag
        val apkExists = remember(readyTag) {
            readyTag != null && UpdateChecker.apkFile(context).exists()
        }
        if (readyTag != null && apkExists) {
            Text(
                text = stringResource(R.string.update_ready_install),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = stringResource(R.string.update_install),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LatoFamily,
                    color = LocalInk.current,
                    modifier = Modifier
                        .clickable {
                            val file = UpdateChecker.apkFile(context)
                            if (!file.exists()) {
                                viewModel.clearDownloadedUpdate()
                            } else if (UpdateChecker.installApk(context, file)) {
                                onDismiss()
                            }
                        }
                        .padding(12.dp),
                )
            }
            return@BottomSheet
        }
        when (val s = state) {
            is UpdateState.Checking -> {
                Text(
                    text = stringResource(R.string.update_checking),
                    fontSize = 14.sp,
                    fontFamily = LatoFamily,
                    color = LocalInk.current,
                )
            }
            is UpdateState.UpToDate -> {
                Text(
                    text = stringResource(R.string.update_up_to_date, currentVersion),
                    fontSize = 14.sp,
                    fontFamily = LatoFamily,
                    color = LocalInk.current,
                )
            }
            is UpdateState.Error -> {
                Text(
                    text = stringResource(R.string.update_error),
                    fontSize = 14.sp,
                    fontFamily = LatoFamily,
                    color = LocalInk.current,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = stringResource(R.string.retry),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LatoFamily,
                        color = LocalInk.current,
                        modifier = Modifier
                            .clickable { scope.launch { check() } }
                            .padding(12.dp),
                    )
                }
            }
            is UpdateState.Available -> {
                Text(
                    text = stringResource(R.string.update_available, s.info.versionName),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LatoFamily,
                    color = LocalInk.current,
                )
                Text(
                    text = stringResource(R.string.update_current_version, currentVersion),
                    fontSize = 13.sp,
                    fontFamily = LatoFamily,
                    color = LocalInk.current,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                if (s.info.notes.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.update_notes_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LatoFamily,
                        color = LocalInk.current,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Text(
                        text = s.info.notes,
                        fontSize = 13.sp,
                        fontFamily = LatoFamily,
                        color = LocalInk.current,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp),
                    )
                }
                if (viewModel.updateFailed) {
                    Text(
                        text = stringResource(R.string.update_download_failed),
                        fontSize = 14.sp,
                        fontFamily = LatoFamily,
                        color = LocalInk.current,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = stringResource(R.string.update_download_install),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LatoFamily,
                        color = LocalInk.current,
                        modifier = Modifier
                            .clickable { viewModel.downloadUpdate(s.info) }
                            .padding(12.dp),
                    )
                }
            }
        }
    }
}


/**
 * Allowlist for the experimental lockscreen widget: checked apps appear on it.
 * Same layout as HiddenAppsDialog; stores the UNCHECKED apps so new installs
 * show up by default.
 */
@Composable
private fun LockscreenAppsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    // Launcher apps plus any package currently posting notifications — system packages
    // like Settings post notifications without having a launcher entry, and they must
    // be listed here to be deselectable.
    val allApps = remember {
        val launcherApps = AppLoader.loadApps(context)
        val known = launcherApps.map { it.packageName }.toSet()
        val extras = (com.gezimos.katapult.service.NotificationListener.getAllCounts().keys +
                viewModel.prefs.getLockscreenExcludedApps())
            .filter { it !in known }
            .distinct()
            .mapNotNull { pkg ->
                try {
                    val pm = context.packageManager
                    com.gezimos.katapult.model.AppModel(
                        packageName = pkg,
                        label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(),
                        activityName = "",
                    )
                } catch (_: Exception) {
                    null
                }
            }
        (launcherApps + extras).sortedBy { it.label.lowercase() }
    }
    var excludedSet by remember { mutableStateOf(viewModel.prefs.getLockscreenExcludedApps()) }
    val itemsPerPage = 6
    var page by remember { mutableIntStateOf(0) }
    val totalPages = (allApps.size + itemsPerPage - 1) / itemsPerPage
    val pageApps = remember(page) {
        val start = page * itemsPerPage
        val end = minOf(start + itemsPerPage, allApps.size)
        allApps.subList(start, end)
    }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val iconShape = if (viewModel.roundedIcons) RoundedCornerShape(percent = 26) else CircleShape

    BottomSheet(onDismiss = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.lockscreen_widget_apps),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.weight(1f),
            )
            val allSelected = excludedSet.isEmpty()
            Icon(
                imageVector = if (allSelected) Icons.Rounded.RemoveDone else Icons.Rounded.DoneAll,
                contentDescription = null,
                tint = LocalInk.current,
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        val updated = if (allSelected) allApps.map { it.packageName }.toSet()
                            else emptySet()
                        viewModel.prefs.setLockscreenExcludedApps(updated)
                        excludedSet = updated
                    },
            )
        }
        Column(
            modifier = Modifier
                .pointerInput(page, totalPages) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onDragEnd = {
                            if (abs(dragAccumulator) > 80f) {
                                if (dragAccumulator < 0 && page < totalPages - 1) page++
                                else if (dragAccumulator > 0 && page > 0) page--
                            }
                        },
                        onHorizontalDrag = { _, amount -> dragAccumulator += amount },
                    )
                },
        ) {
            for (i in 0 until itemsPerPage) {
                if (i < pageApps.size) {
                    val app = pageApps[i]
                    val isShown = app.packageName !in excludedSet
                    val sizePx = remember { (36 * context.resources.displayMetrics.density).toInt() }
                    val bitmap = remember(app.packageName) {
                        IconUtility.loadIcon(context, app.packageName, app.activityName, sizePx)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val updated = if (isShown) excludedSet + app.packageName
                                    else excludedSet - app.packageName
                                viewModel.prefs.setLockscreenExcludedApps(updated)
                                excludedSet = updated
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIconCircle(bitmap = bitmap, size = 36.dp, borderWidth = 1.5.dp, shape = iconShape)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            app.label,
                            fontSize = 18.sp,
                            fontFamily = LatoFamily,
                            color = LocalInk.current,
                            modifier = Modifier.weight(1f),
                        )
                        val checkShape = RoundedCornerShape(4.dp)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .then(
                                    if (isShown) Modifier.background(LocalInk.current, checkShape)
                                    else Modifier.border(2.5.dp, LocalInk.current, checkShape)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isShown) {
                                Text(
                                    text = "✓",
                                    color = LocalSurface.current,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Box(Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("", fontSize = 18.sp)
                    }
                }
            }
            if (totalPages > 1) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (page > 0) {
                        ArrowButton(
                            iconRes = R.drawable.ic_arrow_left,
                            onClick = { page-- },
                        )
                    } else {
                        Spacer(Modifier.size(ArrowSize))
                    }

                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (i in 0 until totalPages) {
                            Box(
                                Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(6.dp)
                                    .then(
                                        if (i == page) Modifier.background(LocalInk.current, CircleShape)
                                        else Modifier.border(1.5.dp, LocalInk.current, CircleShape)
                                    )
                            )
                        }
                    }

                    if (page < totalPages - 1) {
                        ArrowButton(
                            iconRes = R.drawable.ic_arrow_right,
                            onClick = { page++ },
                        )
                    } else {
                        Spacer(Modifier.size(ArrowSize))
                    }
                }
            }
        }
    }
}

@Composable
fun HiddenAppsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val allApps = remember {
        (AppLoader.loadApps(context) + ShortcutHelper.installedShortcuts(context, viewModel.prefs))
            .sortedBy { it.label.lowercase() }
    }
    var hiddenSet by remember { mutableStateOf(viewModel.prefs.getHiddenApps()) }
    val itemsPerPage = 6
    var page by remember { mutableIntStateOf(0) }
    val totalPages = (allApps.size + itemsPerPage - 1) / itemsPerPage
    val pageApps = remember(page) {
        val start = page * itemsPerPage
        val end = minOf(start + itemsPerPage, allApps.size)
        allApps.subList(start, end)
    }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val hiddenCount = hiddenSet.size
    val iconShape = if (viewModel.roundedIcons) RoundedCornerShape(percent = 26) else CircleShape

    BottomSheet(onDismiss = onDismiss) {
        Text(
            text = if (hiddenCount > 0) stringResource(R.string.hidden_apps_count, hiddenCount) else stringResource(R.string.hidden_apps),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
            color = LocalInk.current,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(
            modifier = Modifier
                .pointerInput(page, totalPages) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onDragEnd = {
                            if (abs(dragAccumulator) > 80f) {
                                if (dragAccumulator < 0 && page < totalPages - 1) page++
                                else if (dragAccumulator > 0 && page > 0) page--
                            }
                        },
                        onHorizontalDrag = { _, amount -> dragAccumulator += amount },
                    )
                },
        ) {
            for (i in 0 until itemsPerPage) {
                if (i < pageApps.size) {
                    val app = pageApps[i]
                    val isHidden = app.key in hiddenSet
                    val sizePx = remember { (36 * context.resources.displayMetrics.density).toInt() }
                    val bitmap = remember(app.key) {
                        if (app.shortcutId.isNotEmpty()) {
                            IconUtility.loadShortcutIcon(context, app.packageName, app.shortcutId, sizePx)
                        } else {
                            IconUtility.loadIcon(context, app.packageName, app.activityName, sizePx)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isHidden) {
                                    viewModel.prefs.unhideApp(app.key)
                                } else {
                                    viewModel.prefs.hideApp(app.key)
                                }
                                hiddenSet = viewModel.prefs.getHiddenApps()
                                viewModel.loadApps()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIconCircle(bitmap = bitmap, size = 36.dp, borderWidth = 1.5.dp, shape = iconShape)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            app.label,
                            fontSize = 18.sp,
                            fontFamily = LatoFamily,
                            color = LocalInk.current,
                            modifier = Modifier.weight(1f),
                        )
                        val checkShape = RoundedCornerShape(4.dp)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .then(
                                    if (isHidden) Modifier.background(LocalInk.current, checkShape)
                                    else Modifier.border(2.5.dp, LocalInk.current, checkShape)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isHidden) {
                                Text(
                                    text = "\u2713",
                                    color = LocalSurface.current,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Box(Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("", fontSize = 18.sp)
                    }
                }
            }
            if (totalPages > 1) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (page > 0) {
                        ArrowButton(
                            iconRes = R.drawable.ic_arrow_left,
                            onClick = { page-- },
                        )
                    } else {
                        Spacer(Modifier.size(ArrowSize))
                    }

                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (i in 0 until totalPages) {
                            Box(
                                Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(6.dp)
                                    .then(
                                        if (i == page) Modifier.background(LocalInk.current, CircleShape)
                                        else Modifier.border(1.5.dp, LocalInk.current, CircleShape)
                                    )
                            )
                        }
                    }

                    if (page < totalPages - 1) {
                        ArrowButton(
                            iconRes = R.drawable.ic_arrow_right,
                            onClick = { page++ },
                        )
                    } else {
                        Spacer(Modifier.size(ArrowSize))
                    }
                }
            }
        }
    }
}

private data class SettingsCategoryInfo(
    val sectionTitleRes: Int,
    val cardTitleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
)

private val settingsCategories = listOf(
    SettingsCategoryInfo(R.string.section_appearance, R.string.section_appearance, R.string.cat_appearance_sub, Icons.Rounded.Palette),
    SettingsCategoryInfo(R.string.section_home, R.string.cat_home, R.string.cat_home_sub, Icons.Rounded.Home),
    SettingsCategoryInfo(R.string.section_all_apps, R.string.section_all_apps, R.string.cat_all_apps_sub, Icons.Rounded.Apps),
    SettingsCategoryInfo(R.string.section_gestures, R.string.section_gestures, R.string.cat_gestures_sub, Icons.Rounded.Gesture),
    SettingsCategoryInfo(R.string.section_notifications, R.string.section_notifications, R.string.cat_notifications_sub, Icons.Rounded.Notifications),
    SettingsCategoryInfo(R.string.section_eink, R.string.section_eink, R.string.cat_eink_sub, Icons.Rounded.Contrast),
    SettingsCategoryInfo(R.string.section_experimental, R.string.cat_lockscreen, R.string.cat_lockscreen_sub, Icons.Rounded.Lock),
    SettingsCategoryInfo(R.string.section_screensaver, R.string.screensaver, R.string.cat_screensaver_sub, Icons.Rounded.Bedtime),
    SettingsCategoryInfo(R.string.section_system, R.string.section_system, R.string.cat_system_sub, Icons.Rounded.Tune),
    SettingsCategoryInfo(R.string.section_about, R.string.section_about, R.string.cat_about_sub, Icons.Rounded.Info),
)

@Composable
private fun SettingsCategoryGrid(
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        settingsCategories.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                pair.forEach { info ->
                    SettingsCategoryCard(
                        info = info,
                        onClick = { onSelect(info.sectionTitleRes) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    info: SettingsCategoryInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = LocalInk.current
    Column(
        modifier = modifier
            .border(2.dp, ink, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = info.icon,
            contentDescription = null,
            tint = ink,
            modifier = Modifier
                .align(Alignment.End)
                .size(30.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(info.cardTitleRes),
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
            color = ink,
            maxLines = 2,
        )
        Text(
            text = stringResource(info.subtitleRes),
            fontSize = 15.sp,
            fontFamily = LatoFamily,
            color = ink,
            maxLines = 2,
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 3.dp),
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
            color = LocalInk.current,
        )
        Spacer(Modifier.height(3.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(LocalInk.current))
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
            Text(
                text = description,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
        Spacer(Modifier.width(16.dp))
        // Toggle: pill track with sliding dot
        val trackShape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 22.dp)
                .then(
                    if (checked) Modifier.background(LocalInk.current, trackShape)
                    else Modifier.border(2.dp, LocalInk.current, trackShape)
                )
                .clickable { onCheckedChange(!checked) },
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(14.dp)
                    .background(
                        if (checked) LocalSurface.current else LocalInk.current,
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun SettingsButtonRow(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
            Text(
                text = description,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
        Spacer(Modifier.width(16.dp))
        val trackShape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .border(2.5.dp, LocalInk.current, trackShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = buttonText,
                fontSize = 12.sp,
                fontFamily = LatoFamily,
                fontWeight = FontWeight.Bold,
                color = LocalInk.current,
            )
        }
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
            Text(
                text = description,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
        Text(
            text = "\u203A",
            fontSize = 24.sp,
            color = LocalInk.current,
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (checked) Icons.Rounded.CheckBox
            else Icons.Rounded.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = LocalInk.current,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
            Text(
                text = description,
                fontSize = 13.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
    }
}

@Composable
private fun MultiChoiceSheet(
    title: String,
    options: List<Triple<String, Boolean, (Boolean) -> Unit>>,
    onDismiss: () -> Unit,
) {
    BottomSheet(onDismiss = onDismiss) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
            color = LocalInk.current,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        options.forEach { (label, checked, onToggle) ->
            BottomSheetOption(
                text = label,
                icon = if (checked) Icons.Rounded.CheckBox
                else Icons.Rounded.CheckBoxOutlineBlank,
            ) {
                onToggle(!checked)
            }
        }
    }
}

@Composable
private fun <T> ChoiceSheet(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheet(onDismiss = onDismiss) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
            color = LocalInk.current,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        options.forEach { (value, label) ->
            BottomSheetOption(
                text = label,
                icon = if (value == selected) Icons.Rounded.RadioButtonChecked
                else Icons.Rounded.RadioButtonUnchecked,
            ) {
                onSelect(value)
                onDismiss()
            }
        }
    }
}

@Composable
private fun SettingsCycleRow(
    title: String,
    description: String,
    value: String = description,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
            Text(
                text = description,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
        val trackShape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .border(2.5.dp, LocalInk.current, trackShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                fontWeight = FontWeight.Bold,
                color = LocalInk.current,
            )
        }
    }
}
