package com.gezimos.katapult.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.gezimos.katapult.util.IconUtility
import kotlin.math.abs

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
    var showAmPm by remember { mutableStateOf(prefs.showAmPm) }
    var showBattery by remember { mutableStateOf(prefs.showBattery) }
    var roundedIcons by remember { mutableStateOf(prefs.roundedIcons) }
    var hideStatusBar by remember { mutableStateOf(prefs.hideStatusBar) }
    var einkRefreshOnHome by remember { mutableStateOf(prefs.einkRefreshOnHome) }
    var einkHelperMode by remember { mutableIntStateOf(prefs.einkHelperMode) }
    var doubleTapBrightness by remember { mutableStateOf(prefs.doubleTapBrightness) }
    var homeExtraRow by remember { mutableStateOf(prefs.homeExtraRow) }
    var infiniteScroll by remember { mutableStateOf(prefs.infiniteScroll) }
    var showKatapultIcon by remember { mutableStateOf(prefs.showKatapultIcon) }
    var hideAppNames by remember { mutableStateOf(prefs.hideAppNames) }
    var hideArrowButtons by remember { mutableStateOf(prefs.hideArrowButtons) }
    var disableHomeEditing by remember { mutableStateOf(prefs.disableHomeEditing) }
    var hideAllAppsButton by remember { mutableStateOf(prefs.hideAllAppsButton) }

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
        }
    }

    var currentPage by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(1) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pageCount) {
        if (currentPage >= pageCount) currentPage = (pageCount - 1).coerceAtLeast(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .then(if (!hideStatusBar) Modifier.statusBarsPadding() else Modifier)
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = Color.Black,
                modifier = Modifier.weight(1f),
            )
            if (pageCount > 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (i in 0 until pageCount) {
                        Box(
                            Modifier
                                .padding(horizontal = 2.dp)
                                .size(6.dp)
                                .then(
                                    if (i == currentPage) Modifier.background(Color.Black, CircleShape)
                                    else Modifier.border(1.5.dp, Color.Black, CircleShape)
                                )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val rows = buildList<@Composable () -> Unit> {
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
            if (isMudita) {
                add {
                    SettingsCycleRow(
                        title = stringResource(R.string.eink_auto_mode),
                        description = EinkHelper.modeName(einkHelperMode),
                        onClick = {
                            val next = EinkHelper.nextMode(einkHelperMode)
                            einkHelperMode = next
                            prefs.einkHelperMode = next
                            (context as? MainActivity)?.setMeinkMode(next)
                        },
                    )
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
                SettingsToggleRow(
                    title = stringResource(R.string.hide_status_bar),
                    description = stringResource(R.string.hide_status_bar_desc),
                    checked = hideStatusBar,
                    onCheckedChange = {
                        hideStatusBar = it
                        prefs.hideStatusBar = it
                        viewModel.applyStatusBar(context)
                    },
                )
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
                    title = stringResource(R.string.double_tap_brightness),
                    description = stringResource(R.string.double_tap_brightness_desc),
                    checked = doubleTapBrightness,
                    onCheckedChange = {
                        doubleTapBrightness = it
                        prefs.doubleTapBrightness = it
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.show_ampm),
                    description = stringResource(R.string.show_ampm_desc),
                    checked = showAmPm,
                    onCheckedChange = {
                        showAmPm = it
                        prefs.showAmPm = it
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
                    title = stringResource(R.string.hide_app_names),
                    description = stringResource(R.string.hide_app_names_desc),
                    checked = hideAppNames,
                    onCheckedChange = {
                        hideAppNames = it
                        prefs.hideAppNames = it
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
                    title = stringResource(R.string.disable_home_editing),
                    description = stringResource(R.string.disable_home_editing_desc),
                    checked = disableHomeEditing,
                    onCheckedChange = {
                        disableHomeEditing = it
                        prefs.disableHomeEditing = it
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LatoFamily,
                            color = Color.Black,
                        )
                        Text(
                            text = versionName,
                            fontSize = 14.sp,
                            fontFamily = LatoFamily,
                            color = Color.Black,
                        )
                    }
                }
            }
        }

        SubcomposeLayout(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
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
            val rowPlaceables = rows.mapIndexed { i, content ->
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

@Composable
fun HiddenAppsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val allApps = remember { AppLoader.loadApps(context) }
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
    val iconShape = LocalIconShape.current

    BottomSheet(onDismiss = onDismiss) {
        Text(
            text = if (hiddenCount > 0) stringResource(R.string.hidden_apps_count, hiddenCount) else stringResource(R.string.hidden_apps),
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
                    val isHidden = app.packageName in hiddenSet
                    val sizePx = remember { (36 * context.resources.displayMetrics.density).toInt() }
                    val bitmap = remember(app.packageName) {
                        IconUtility.loadIcon(context, app.packageName, app.activityName, sizePx)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isHidden) {
                                    viewModel.prefs.unhideApp(app.packageName)
                                } else {
                                    viewModel.prefs.hideApp(app.packageName)
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
                            color = Color.Black,
                            modifier = Modifier.weight(1f),
                        )
                        val checkShape = RoundedCornerShape(4.dp)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .then(
                                    if (isHidden) Modifier.background(Color.Black, checkShape)
                                    else Modifier.border(2.5.dp, Color.Black, checkShape)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isHidden) {
                                Text(
                                    text = "\u2713",
                                    color = Color.White,
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
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = Color.Black,
            )
            Text(
                text = description,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = Color.Black,
            )
        }
        Spacer(Modifier.width(16.dp))
        // Toggle: pill track with sliding dot
        val trackShape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 22.dp)
                .then(
                    if (checked) Modifier.background(Color.Black, trackShape)
                    else Modifier.border(2.dp, Color.Black, trackShape)
                )
                .clickable { onCheckedChange(!checked) },
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(14.dp)
                    .background(
                        if (checked) Color.White else Color.Black,
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
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = Color.Black,
            )
            Text(
                text = description,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = Color.Black,
            )
        }
        Spacer(Modifier.width(16.dp))
        val trackShape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .border(2.5.dp, Color.Black, trackShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = buttonText,
                fontSize = 12.sp,
                fontFamily = LatoFamily,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
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
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = Color.Black,
            )
            Text(
                text = description,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = Color.Black,
            )
        }
        Text(
            text = "\u203A",
            fontSize = 24.sp,
            color = Color.Black,
        )
    }
}

@Composable
private fun SettingsCycleRow(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = Color.Black,
            )
            Text(
                text = description,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = Color.Black,
            )
        }
        val trackShape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .border(2.5.dp, Color.Black, trackShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = description,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
        }
    }
}
