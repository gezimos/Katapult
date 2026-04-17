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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gezimos.katapult.MainViewModel
import com.gezimos.katapult.R
import com.gezimos.katapult.Screen
import com.gezimos.katapult.util.BrightnessHelper
import com.gezimos.katapult.util.IconUtility

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, imagePicker: ActivityResultLauncher<String>) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var pickerSlot by remember { mutableStateOf<String?>(null) }
    var showHiddenApps by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        if (viewModel.wallpaperBitmap != null) {
            Image(
                bitmap = viewModel.wallpaperBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!viewModel.prefs.hideStatusBar) Modifier.statusBarsPadding() else Modifier)
                .navigationBarsPadding()
                .padding(PagePadding),
        ) {
            // Clock area - positioned in upper portion like mockup
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { showMenu = true },
                            onDoubleTap = {
                                if (viewModel.prefs.doubleTapBrightness) {
                                    BrightnessHelper.toggleBrightness(context, viewModel.prefs)
                                }
                            }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(32.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (viewModel.alarmTime != null) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = stringResource(R.string.cd_alarm),
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = viewModel.alarmTime!!,
                            fontSize = 18.sp,
                            fontFamily = LatoFamily,
                            color = Color.Black,
                        )
                    }
                    if (viewModel.prefs.showBattery && viewModel.batteryPercent >= 0) {
                        if (viewModel.alarmTime != null) {
                            Spacer(Modifier.width(16.dp))
                        }
                        val batteryIcon = if (viewModel.isCharging) {
                            Icons.Rounded.BatteryChargingFull
                        } else {
                            when {
                                viewModel.batteryPercent >= 95 -> Icons.Rounded.BatteryFull
                                viewModel.batteryPercent >= 85 -> Icons.Rounded.Battery6Bar
                                viewModel.batteryPercent >= 70 -> Icons.Rounded.Battery5Bar
                                viewModel.batteryPercent >= 55 -> Icons.Rounded.Battery4Bar
                                viewModel.batteryPercent >= 40 -> Icons.Rounded.Battery3Bar
                                viewModel.batteryPercent >= 25 -> Icons.Rounded.Battery2Bar
                                viewModel.batteryPercent >= 10 -> Icons.Rounded.Battery1Bar
                                else -> Icons.Rounded.Battery0Bar
                            }
                        }
                        Icon(
                            imageVector = batteryIcon,
                            contentDescription = stringResource(R.string.cd_battery),
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = stringResource(R.string.battery_percent, viewModel.batteryPercent),
                            fontSize = 18.sp,
                            fontFamily = LatoFamily,
                            color = Color.Black,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            val saved = viewModel.prefs.loadShortcut("clock")
                            if (saved != null) {
                                viewModel.launchPackage(context, saved.first, saved.second)
                            } else {
                                pickerSlot = "clock"
                            }
                        },
                        onLongClick = { pickerSlot = "clock" },
                    ),
                ) {
                    Text(
                        text = viewModel.clockTime,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LatoFamily,
                        color = Color.Black,
                        modifier = Modifier,
                    )
                    if (viewModel.clockAmPm != null && viewModel.prefs.showAmPm) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = viewModel.clockAmPm!!,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LatoFamily,
                            color = Color.Black,
                        )
                    }
                }

                Text(
                    text = viewModel.clockDate,
                    fontSize = 18.sp,
                    fontFamily = LatoFamily,
                    color = Color.Black,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            val saved = viewModel.prefs.loadShortcut("calendar")
                            if (saved != null) {
                                viewModel.launchPackage(context, saved.first, saved.second)
                            } else {
                                pickerSlot = "calendar"
                            }
                        },
                        onLongClick = { pickerSlot = "calendar" },
                    ),
                )

            }

            // Music widget - aligned with app icon edges
            if (viewModel.mediaInfo != null) {
                @Suppress("UnusedBoxWithConstraintsScope")
                androidx.compose.foundation.layout.BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                ) {
                    val columnWidth = maxWidth / 3
                    val iconPad = (columnWidth - IconSize) / 2
                    Box(Modifier.fillMaxWidth().padding(horizontal = iconPad)) {
                        MusicWidget(viewModel)
                    }
                }
            }

            // Extra row (if enabled)
            if (viewModel.prefs.homeExtraRow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ShortcutItem(
                            viewModel = viewModel,
                            slot = "extra_left",
                            defaultLabel = stringResource(R.string.music),
                            refresh = viewModel.shortcutRefresh,
                            onClick = { viewModel.launchShortcut(context, "extra_left") },
                            onLongClick = { pickerSlot = "extra_left" },
                        )
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ShortcutItem(
                            viewModel = viewModel,
                            slot = "extra_center",
                            defaultLabel = stringResource(R.string.calendar),
                            refresh = viewModel.shortcutRefresh,
                            onClick = { viewModel.launchShortcut(context, "extra_center") },
                            onLongClick = { pickerSlot = "extra_center" },
                        )
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ShortcutItem(
                            viewModel = viewModel,
                            slot = "extra_right",
                            defaultLabel = stringResource(R.string.camera),
                            refresh = viewModel.shortcutRefresh,
                            onClick = { viewModel.launchShortcut(context, "extra_right") },
                            onLongClick = { pickerSlot = "extra_right" },
                        )
                    }
                }
            }

            // Shortcuts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ShortcutItem(
                        viewModel = viewModel,
                        slot = "phone",
                        defaultLabel = stringResource(R.string.phone),
                        refresh = viewModel.shortcutRefresh,
                        onClick = { viewModel.launchShortcut(context, "phone") },
                        onLongClick = { pickerSlot = "phone" },
                    )
                }

                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { viewModel.navigateTo(Screen.ALL_APPS) },
                    ) {
                        // Dashed outline with grid dots
                        val strokeWidth = 2.5.dp
                        val isRounded = viewModel.roundedIcons
                        Box(
                            modifier = Modifier
                                .size(IconSize)
                                .dashedDotBorder(strokeWidth = strokeWidth, isRounded = isRounded),
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
                    }
                }

                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ShortcutItem(
                        viewModel = viewModel,
                        slot = "sms",
                        defaultLabel = stringResource(R.string.sms),
                        refresh = viewModel.shortcutRefresh,
                        onClick = { viewModel.launchShortcut(context, "sms") },
                        onLongClick = { pickerSlot = "sms" },
                    )
                }
            }
        }
    }

    // Menu bottom sheet (Dialog-based, handles back + tap-outside natively)
    if (showMenu) {
        BottomSheet(onDismiss = { showMenu = false }) {
            BottomSheetOption(stringResource(R.string.settings), icon = Icons.Rounded.Settings) {
                showMenu = false
                viewModel.navigateTo(Screen.SETTINGS)
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
            BottomSheetOption(stringResource(R.string.donate_label), icon = Icons.Rounded.FavoriteBorder) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/gezimos")))
                showMenu = false
            }
        }
    }

    // App picker bottom sheet
    if (pickerSlot != null) {
        val slot = pickerSlot!!
        val title = when (slot) {
            "phone" -> stringResource(R.string.set_left_app)
            "sms" -> stringResource(R.string.set_right_app)
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
                viewModel.saveShortcut(slot, app.packageName, app.activityName)
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
    val iconShape = LocalIconShape.current

    BottomSheet(onDismiss = onDismiss) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
            color = Color.Black,
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
                    val bitmap = remember(app.packageName) {
                        IconUtility.loadIcon(context, app.packageName, app.activityName, sizePx)
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
                        Text(app.label, fontSize = 18.sp, fontFamily = LatoFamily, color = Color.Black)
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
                                        if (i == page) Modifier.background(Color.Black, CircleShape)
                                        else Modifier.border(1.5.dp, Color.Black, CircleShape)
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
private fun MusicWidget(viewModel: MainViewModel) {
    val info = viewModel.mediaInfo ?: return
    val context = LocalContext.current
    val widgetShape = RoundedIconShape

    // Album art
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
            .background(Color.White)
            .border(2.5.dp, Color.Black, widgetShape),
    ) {
        // Song info row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.4f)
                .padding(horizontal = 12.dp)
                .clickable { viewModel.mediaOpenApp(context) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = songLabel.ifEmpty { noMediaLabel },
                fontSize = 18.sp,
                fontFamily = LatoFamily,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Divider
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(Color.Black)
        )

        // Controls row (2/3 height)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album art - full height, 1:1, bottom-left corner matches container
            val artCorner = 19.dp
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
                    .background(Color.Black)
                    .clickable { viewModel.mediaOpenApp(context) }
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
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            // Separator
            Box(Modifier.fillMaxHeight().width(2.5.dp).background(Color.Black))

            // Controls
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
                    tint = Color.Black,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { viewModel.mediaPrevious() },
                )

                Icon(
                    imageVector = if (info.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_pause),
                    tint = Color.Black,
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { viewModel.mediaPlayPause() },
                )

                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.cd_next),
                    tint = Color.Black,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { viewModel.mediaNext() },
                )

                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = stringResource(R.string.cd_stop),
                    tint = Color.Black,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { viewModel.mediaStop() },
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
    val label = remember(refresh) { viewModel.getShortcutLabel(slot, defaultLabel) }
    val sizePx = remember { (IconSize.value * context.resources.displayMetrics.density).toInt() }
    val bitmap = remember(pkg, activityName, refresh) {
        if (pkg != null) IconUtility.loadIcon(context, pkg, activityName, sizePx) else null
    }

    val notificationCount = if (viewModel.prefs.notificationIndicators)
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
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (viewModel.prefs.hideAppNames) "" else label,
            fontSize = 18.sp,
            fontFamily = LatoFamily,
            color = Color.Black,
        )
    }
}
