package com.gezimos.katapult.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Battery0Bar
import androidx.compose.material.icons.rounded.Battery1Bar
import androidx.compose.material.icons.rounded.Battery2Bar
import androidx.compose.material.icons.rounded.Battery3Bar
import androidx.compose.material.icons.rounded.Battery4Bar
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.Battery6Bar
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.gezimos.katapult.MainViewModel
import com.gezimos.katapult.R
import com.gezimos.katapult.Screen
import com.gezimos.katapult.lockscreen.LockscreenWidgetService
import com.gezimos.katapult.util.AudioWidgetHelper
import com.gezimos.katapult.util.BrightnessHelper
import com.gezimos.katapult.util.IconUtility
import com.gezimos.katapult.util.PrefsManager

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, imagePicker: ActivityResultLauncher<String>) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var pickerSlot by remember { mutableStateOf<String?>(null) }
    var showHiddenApps by remember { mutableStateOf(false) }

    val handleSlotLongPress: (String) -> Unit = { slot ->
        if (viewModel.prefs.disableHomeEditing) {
            val pkg = viewModel.getShortcutPackage(slot)
            if (pkg != null) {
                viewModel.launchIntent(
                    context,
                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$pkg")
                    },
                )
            }
        } else {
            pickerSlot = slot
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalSurface.current)
            .pointerInput(viewModel.prefs.verticalAppGestures) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var swipeAcc = 0f
                    var pinchStart = 0f
                    var pinch = false
                    var pinchFired = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (pressed.size >= 2) {
                            pinch = true
                            val distance = (pressed[0].position - pressed[1].position).getDistance()
                            if (pinchStart == 0f) {
                                pinchStart = distance
                            } else if (!pinchFired && viewModel.prefs.screensaverEnabled && pinchStart > 80.dp.toPx() && distance < pinchStart * 0.6f) {
                                pinchFired = true
                                viewModel.startScreensaver(context)
                            }
                        } else if (!pinch) {
                            val delta = if (viewModel.prefs.verticalAppGestures) {
                                pressed[0].positionChange().y
                            } else {
                                pressed[0].positionChange().x
                            }
                            swipeAcc += delta
                        }
                    }
                    if (!pinch && viewModel.prefs.swipeUpAllApps && swipeAcc < -100f) {
                        viewModel.navigateTo(Screen.ALL_APPS)
                    }
                }
            },
    ) {
        if (viewModel.wallpaperBitmap != null) {
            Image(
                bitmap = viewModel.wallpaperBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!viewModel.prefs.hideStatusBar) Modifier.statusBarsPadding() else Modifier)
                .navigationBarsPadding()
                .padding(PagePadding),
        ) {
        val gridRowHeight = if (viewModel.prefs.hideAppNames) AllAppsRowHeightNoLabels else AllAppsRowHeight
        val gridRows = (maxHeight / gridRowHeight).toInt().coerceAtLeast(1)
        val dockRows = if (viewModel.prefs.homeExtraRow) 2 else 1
        val topWeight = (gridRows - dockRows).coerceAtLeast(1).toFloat()
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth().weight(topWeight)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { showMenu = true },
                            onDoubleTap = {
                                when (viewModel.prefs.doubleTapAction) {
                                    PrefsManager.DOUBLE_TAP_BRIGHTNESS ->
                                        BrightnessHelper.toggleBrightness(context, viewModel.prefs)
                                    PrefsManager.DOUBLE_TAP_LOCK ->
                                        LockscreenWidgetService.lockScreen()
                                }
                            },
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ClockDisplay(
                    clockTime = viewModel.clockTime,
                    clockAmPm = viewModel.clockAmPm,
                    clockDate = viewModel.clockDate,
                    alarmTime = viewModel.alarmTime,
                    batteryPercent = viewModel.batteryPercent,
                    isCharging = viewModel.isCharging,
                    showBattery = viewModel.prefs.showBattery,
                    islandsActive = viewModel.prefs.homeIslands,
                    topSpacing = if (viewModel.prefs.hideStatusBar) 32.dp else 8.dp,
                    onClockClick = {
                        val saved = viewModel.prefs.loadShortcut("clock")
                        if (saved != null) {
                            viewModel.launchShortcut(context, "clock")
                        } else if (!viewModel.prefs.disableHomeEditing) {
                            pickerSlot = "clock"
                        }
                    },
                    onClockLongClick = { handleSlotLongPress("clock") },
                    onDateClick = {
                        val saved = viewModel.prefs.loadShortcut("calendar")
                        if (saved != null) {
                            viewModel.launchShortcut(context, "calendar")
                        } else if (!viewModel.prefs.disableHomeEditing) {
                            pickerSlot = "calendar"
                        }
                    },
                    onDateLongClick = { handleSlotLongPress("calendar") },
                    onBatteryClick = {
                        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                context.startActivity(
                                    Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) { }
                        }
                    },
                )
            }

            if (viewModel.mediaInfo != null && !viewModel.prefs.disableMusicWidget) {
                @Suppress("UnusedBoxWithConstraintsScope")
                androidx.compose.foundation.layout.BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    val columnWidth = maxWidth / 3
                    val iconPad = (columnWidth - IconSize) / 2
                    Box(Modifier.fillMaxWidth().padding(horizontal = iconPad)) {
                        viewModel.mediaInfo?.let { media ->
                            MusicWidget(
                                info = media,
                                onOpenApp = { viewModel.mediaOpenApp(context) },
                                onPrevious = { viewModel.mediaPrevious() },
                                onPlayPause = { viewModel.mediaPlayPause() },
                                onNext = { viewModel.mediaNext() },
                                onStop = { viewModel.mediaStop() },
                            )
                        }
                    }
                }
            }
            }

            if (viewModel.prefs.homeExtraRow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ShortcutItem(
                            viewModel = viewModel,
                            slot = "extra_left",
                            defaultLabel = stringResource(R.string.music),
                            refresh = viewModel.shortcutRefresh,
                            onClick = { viewModel.launchShortcut(context, "extra_left") },
                            onLongClick = { handleSlotLongPress("extra_left") },
                        )
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ShortcutItem(
                            viewModel = viewModel,
                            slot = "extra_center",
                            defaultLabel = stringResource(R.string.calendar),
                            refresh = viewModel.shortcutRefresh,
                            onClick = { viewModel.launchShortcut(context, "extra_center") },
                            onLongClick = { handleSlotLongPress("extra_center") },
                        )
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ShortcutItem(
                            viewModel = viewModel,
                            slot = "extra_right",
                            defaultLabel = stringResource(R.string.camera),
                            refresh = viewModel.shortcutRefresh,
                            onClick = { viewModel.launchShortcut(context, "extra_right") },
                            onLongClick = { handleSlotLongPress("extra_right") },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ShortcutItem(
                        viewModel = viewModel,
                        slot = "phone",
                        defaultLabel = stringResource(R.string.phone),
                        refresh = viewModel.shortcutRefresh,
                        onClick = { viewModel.launchShortcut(context, "phone") },
                        onLongClick = { handleSlotLongPress("phone") },
                    )
                }

                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (viewModel.prefs.hideAllAppsButton) {
                        ShortcutItem(
                            viewModel = viewModel,
                            slot = "center",
                            defaultLabel = stringResource(R.string.contacts),
                            refresh = viewModel.shortcutRefresh,
                            onClick = { viewModel.launchShortcut(context, "center") },
                            onLongClick = { handleSlotLongPress("center") },
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { viewModel.navigateTo(Screen.ALL_APPS) },
                        ) {
                            val strokeWidth = 2.5.dp
                            val isRounded = viewModel.roundedIcons
                            val tileShape = if (isRounded) RoundedIconShape else CircleShape
                            Box(
                                modifier = Modifier
                                    .size(IconSize)
                                    .then(
                                        if (viewModel.prefs.homeIslands)
                                            Modifier
                                                .background(LocalSurface.current, tileShape)
                                                .border(strokeWidth, LocalInk.current, tileShape)
                                        else Modifier.dashedDotBorder(strokeWidth = strokeWidth, isRounded = isRounded, color = LocalInk.current)
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        CircleDot(12.dp, borderWidth = strokeWidth)
                                        CircleDot(12.dp, borderWidth = strokeWidth)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        CircleDot(12.dp, borderWidth = strokeWidth)
                                        CircleDot(12.dp, borderWidth = strokeWidth)
                                    }
                                }
                            }
                            if (!viewModel.prefs.hideAppNames) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "",
                                    fontSize = 18.sp,
                                    fontFamily = LatoFamily,
                                    color = LocalInk.current,
                                )
                            }
                        }
                    }
                }

                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ShortcutItem(
                        viewModel = viewModel,
                        slot = "sms",
                        defaultLabel = stringResource(R.string.sms),
                        refresh = viewModel.shortcutRefresh,
                        onClick = { viewModel.launchShortcut(context, "sms") },
                        onLongClick = { handleSlotLongPress("sms") },
                    )
                }
            }
        }
        }
    }

    if (showMenu) {
        BottomSheet(onDismiss = { showMenu = false }) {
            BottomSheetOption(stringResource(R.string.settings), icon = Icons.Rounded.Settings) {
                showMenu = false
                viewModel.navigateTo(Screen.SETTINGS)
            }
            if (viewModel.prefs.hideAllAppsButton) {
                BottomSheetOption(stringResource(R.string.all_apps), icon = Icons.Rounded.Apps) {
                    showMenu = false
                    viewModel.navigateTo(Screen.ALL_APPS)
                }
            }
            BottomSheetOption(stringResource(R.string.hidden_apps), icon = Icons.Rounded.VisibilityOff) {
                showMenu = false
                showHiddenApps = true
            }
            if (viewModel.wallpaperBitmap != null) {
                BottomSheetOption(stringResource(R.string.clear_wallpaper), icon = Icons.Rounded.Wallpaper) {
                    showMenu = false
                    viewModel.clearWallpaper()
                }
            } else {
                BottomSheetOption(stringResource(R.string.set_wallpaper), icon = Icons.Rounded.Wallpaper) {
                    imagePicker.launch("image/*")
                    showMenu = false
                }
            }
            if (viewModel.prefs.screensaverEnabled) {
                BottomSheetOption(stringResource(R.string.screensaver), icon = Icons.Rounded.Bedtime) {
                    showMenu = false
                    viewModel.startScreensaver(context)
                }
            }
            BottomSheetOption(stringResource(R.string.donate_label), icon = Icons.Rounded.FavoriteBorder) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/gezimos")))
                showMenu = false
            }
        }
    }

    if (pickerSlot != null) {
        val slot = pickerSlot!!
        val title = when (slot) {
            "phone" -> stringResource(R.string.set_left_app)
            "sms" -> stringResource(R.string.set_right_app)
            "center" -> stringResource(R.string.set_center_app)
            "clock" -> stringResource(R.string.set_clock_app)
            "calendar" -> stringResource(R.string.set_date_app)
            "extra_left" -> stringResource(R.string.set_extra_left_app)
            "extra_center" -> stringResource(R.string.set_extra_center_app)
            "extra_right" -> stringResource(R.string.set_extra_right_app)
            else -> stringResource(R.string.choose_app)
        }
        AppPickerDialog(
            viewModel = viewModel,
            title = title,
            onDismiss = { pickerSlot = null },
            onSelected = { app ->
                viewModel.saveShortcut(slot, app.packageName, app.activityName, app.shortcutId)
                viewModel.shortcutRefresh++
                pickerSlot = null
            },
        )
    }

    if (showHiddenApps) {
        HiddenAppsDialog(
            viewModel = viewModel,
            onDismiss = { showHiddenApps = false },
        )
    }
}

@Composable
private fun AppPickerDialog(
    viewModel: MainViewModel,
    title: String,
    onDismiss: () -> Unit,
    onSelected: (com.gezimos.katapult.model.AppModel) -> Unit,
) {
    val apps = remember { viewModel.getAllApps() }
    val itemsPerPage = 6
    var page by remember { mutableIntStateOf(0) }
    val totalPages = (apps.size + itemsPerPage - 1) / itemsPerPage
    val pageApps = remember(page) {
        val start = page * itemsPerPage
        val end = minOf(start + itemsPerPage, apps.size)
        apps.subList(start, end)
    }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val iconShape = if (viewModel.roundedIcons) RoundedCornerShape(percent = 26) else CircleShape

    BottomSheet(onDismiss = onDismiss) {
        Text(
            text = title,
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
                            if (kotlin.math.abs(dragAccumulator) > 80f) {
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
                    val context = LocalContext.current
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
                            .clickable { onSelected(app) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIconCircle(bitmap = bitmap, size = 36.dp, borderWidth = 1.5.dp, shape = iconShape)
                        Spacer(Modifier.width(12.dp))
                        Text(app.label, fontSize = 18.sp, fontFamily = LatoFamily, color = LocalInk.current)
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
internal fun MusicWidget(
    info: AudioWidgetHelper.MediaInfo,
    onOpenApp: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    cornerRadius: Dp = 19.dp,
    borderWidth: Dp = 2.5.dp,
) {
    val widgetShape = RoundedCornerShape(cornerRadius)

    val albumArt = remember(info.controller.metadata) {
        try {
            info.controller.metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: info.controller.metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
                ?: info.controller.metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        } catch (_: Exception) { null }
    }

    val noMediaLabel = stringResource(R.string.no_media)
    val songLabel = listOfNotNull(
        info.title?.trim()?.takeIf { it.isNotBlank() },
        info.artist?.trim()?.takeIf { it.isNotBlank() },
    ).joinToString(" - ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(widgetShape)
            .background(LocalSurface.current)
            .border(borderWidth, LocalInk.current, widgetShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.4f)
                .padding(horizontal = 12.dp)
                .clickable { onOpenApp() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = songLabel.ifEmpty { noMediaLabel },
                fontSize = 18.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(LocalInk.current)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val artCorner = cornerRadius
            val artShape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomEnd = 0.dp,
                bottomStart = artCorner,
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(artShape)
                    .background(if (albumArt != null) LocalInk.current else LocalSurface.current)
                    .clickable { onOpenApp() }
                    .padding(bottom = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt.asImageBitmap(),
                        contentDescription = stringResource(R.string.cd_album_art),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = stringResource(R.string.cd_music),
                        tint = LocalInk.current,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Box(Modifier.fillMaxHeight().width(2.5.dp).background(LocalInk.current))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous),
                    tint = LocalInk.current,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onPrevious() },
                )

                Icon(
                    imageVector = if (info.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_pause),
                    tint = LocalInk.current,
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { onPlayPause() },
                )

                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.cd_next),
                    tint = LocalInk.current,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onNext() },
                )

                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = stringResource(R.string.cd_stop),
                    tint = LocalInk.current,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onStop() },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutItem(
    viewModel: MainViewModel,
    slot: String,
    defaultLabel: String,
    refresh: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val pkg = remember(refresh) { viewModel.getShortcutPackage(slot) }
    val activityName = remember(refresh) { viewModel.getShortcutActivity(slot) }
    val slotShortcutId = remember(refresh) { viewModel.prefs.loadSlotShortcutId(slot) }
    val label = remember(refresh) { viewModel.getShortcutLabel(slot, defaultLabel) }
    val sizePx = remember { (IconSize.value * context.resources.displayMetrics.density).toInt() }
    val bitmap = remember(pkg, activityName, refresh) {
        when {
            pkg == null -> null
            slotShortcutId != null -> IconUtility.loadShortcutIcon(context, pkg, slotShortcutId, sizePx)
            else -> IconUtility.loadIcon(context, pkg, activityName, sizePx)
        }
    }

    val notificationCount = if (viewModel.prefs.notificationIndicators && slotShortcutId == null)
        pkg?.let { viewModel.notificationCounts[it] ?: 0 } ?: 0 else 0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box {
            AppIconCircle(bitmap = bitmap, size = IconSize)
            if (notificationCount > 0) {
                NotificationBadge(
                    count = notificationCount,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
        if (!viewModel.prefs.hideAppNames) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                modifier = Modifier.homeIsland(
                    show = viewModel.prefs.homeIslands,
                    surface = LocalSurface.current,
                    ink = LocalInk.current,
                    hPad = 12.dp,
                    vPad = 2.dp,
                ),
                fontSize = 18.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
    }
}

internal fun Modifier.homeIsland(show: Boolean, surface: Color, ink: Color, hPad: Dp = 8.dp, vPad: Dp = 7.dp): Modifier =
    if (!show) this else drawBehind {
        val px = hPad.toPx()
        val py = vPad.toPx()
        val radius = 19.dp.toPx()
        val stroke = 2.5.dp.toPx()
        val corner = CornerRadius(radius, radius)
        val bias = (size.height * 0.05f)
            .coerceAtMost(2.dp.toPx())
            .coerceAtMost(py - 1.dp.toPx())
            .coerceAtLeast(0f)
        val topLeft = Offset(-px, -py + bias)
        val rectSize = Size(size.width + px * 2, size.height + py * 2)
        drawRoundRect(color = surface, topLeft = topLeft, size = rectSize, cornerRadius = corner)
        drawRoundRect(
            color = ink,
            topLeft = Offset(topLeft.x + stroke / 2, topLeft.y + stroke / 2),
            size = Size(rectSize.width - stroke, rectSize.height - stroke),
            cornerRadius = CornerRadius(radius - stroke / 2, radius - stroke / 2),
            style = Stroke(width = stroke),
        )
    }
