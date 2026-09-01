package com.gezimos.katapult.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AppShortcut
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gezimos.katapult.MainViewModel
import com.gezimos.katapult.R
import com.gezimos.katapult.Screen
import com.gezimos.katapult.model.AppModel
import com.gezimos.katapult.service.DirectBadgeHelper
import com.gezimos.katapult.util.DeviceHelper
import com.gezimos.katapult.util.IconUtility
import com.gezimos.katapult.util.ShortcutHelper
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs


@Composable
fun AppContextMenu(
    viewModel: MainViewModel,
    app: AppModel,
    iconPicker: ActivityResultLauncher<Array<String>>,
    showReorder: Boolean,
    showHide: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var changeIconApp by remember { mutableStateOf<AppModel?>(null) }
    var shortcutsApp by remember { mutableStateOf<AppModel?>(null) }

    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.clearMissedCalls() }

    if (!viewModel.showRenameDialog && !viewModel.reorderMode &&
        changeIconApp == null && shortcutsApp == null
    ) {
        BottomSheet(onDismiss = onDismiss) {
            Text(
                text = app.label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            if (showReorder) {
                BottomSheetOption(stringResource(R.string.reorder), icon = Icons.Rounded.SwapVert) {
                    viewModel.enterReorderMode(app)
                }
            }
            BottomSheetOption(stringResource(R.string.rename), icon = Icons.Rounded.Edit) {
                viewModel.showRenameDialog = true
            }
            if (app.shortcutId.isNotEmpty()) {
                BottomSheetOption(stringResource(R.string.change_icon), icon = Icons.Rounded.Image) {
                    changeIconApp = app
                }
                if (showHide) {
                    BottomSheetOption(stringResource(R.string.hide), icon = Icons.Rounded.VisibilityOff) {
                        viewModel.hideApp(app.key)
                    }
                }
                BottomSheetOption(stringResource(R.string.app_info), icon = Icons.Rounded.Info) {
                    onDismiss()
                    viewModel.launchIntent(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                    })
                }
                BottomSheetOption(stringResource(R.string.remove_shortcut), icon = Icons.Rounded.Delete) {
                    viewModel.removeShortcut(app)
                }
            } else {
                BottomSheetOption(stringResource(R.string.change_icon), icon = Icons.Rounded.Image) {
                    changeIconApp = app
                }
                if (showHide) {
                    BottomSheetOption(stringResource(R.string.hide), icon = Icons.Rounded.VisibilityOff) {
                        viewModel.hideApp(app.key)
                    }
                }
                BottomSheetOption(stringResource(R.string.app_shortcuts), icon = Icons.Rounded.AppShortcut) {
                    shortcutsApp = app
                }
                BottomSheetOption(stringResource(R.string.app_info), icon = Icons.Rounded.Info) {
                    onDismiss()
                    viewModel.launchIntent(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                    })
                }
                BottomSheetOption(stringResource(R.string.notifications), icon = Icons.Rounded.Notifications) {
                    onDismiss()
                    viewModel.launchIntent(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, app.packageName)
                    })
                }
                val isDirectBadge = DeviceHelper.isMuditaKompakt() &&
                    app.packageName in DirectBadgeHelper.DIRECT_PACKAGES
                val isMuditaDial = DeviceHelper.isMuditaKompakt() &&
                    app.packageName == DirectBadgeHelper.MUDITA_DIAL
                val canClear = if (isDirectBadge) {
                    isMuditaDial && viewModel.hasMissedCalls()
                } else {
                    (viewModel.notificationCounts[app.packageName] ?: 0) > 0
                }
                if (canClear) {
                    BottomSheetOption(
                        stringResource(R.string.clear_notifications),
                        icon = Icons.Rounded.NotificationsOff,
                    ) {
                        if (isMuditaDial) {
                            if (viewModel.canWriteCallLog()) {
                                viewModel.clearMissedCalls()
                            } else {
                                callLogPermissionLauncher.launch(
                                    android.Manifest.permission.WRITE_CALL_LOG,
                                )
                            }
                        } else {
                            viewModel.clearNotificationsFor(app.packageName)
                        }
                        onDismiss()
                    }
                }
                val isSystemApp = try {
                    val appInfo = context.packageManager.getApplicationInfo(app.packageName, 0)
                    appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
                } catch (_: Exception) { true }
                if (!isSystemApp) {
                    BottomSheetOption(stringResource(R.string.uninstall), icon = Icons.Rounded.Delete) {
                        onDismiss()
                        viewModel.launchIntent(context, Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                            data = Uri.parse("package:${app.packageName}")
                        })
                    }
                }
            }
        }
    }

    shortcutsApp?.let { target ->
        AppShortcutsDialog(
            viewModel = viewModel,
            app = target,
            onDismiss = {
                shortcutsApp = null
                onDismiss()
            },
        )
    }

    if (viewModel.showRenameDialog) {
        RenameDialog(
            currentName = app.label,
            onDismiss = {
                viewModel.showRenameDialog = false
                onDismiss()
            },
            onConfirm = { newName ->
                viewModel.renameApp(app.key, newName, app.label)
            },
        )
    }

    changeIconApp?.let { target ->
        ChangeIconDialog(
            viewModel = viewModel,
            app = target,
            onDismiss = {
                changeIconApp = null
                onDismiss()
            },
            onImportClick = {
                viewModel.beginIconImport(target.key)
                iconPicker.launch(arrayOf("image/png", "image/svg+xml"))
                changeIconApp = null
                onDismiss()
            },
        )
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var textFieldValue by remember {
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(
            text = currentName,
            selection = androidx.compose.ui.text.TextRange(currentName.length),
        ))
    }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    BottomSheet(onDismiss = onDismiss, imePadding = true) {
        Text(
            text = stringResource(R.string.rename),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
            color = LocalInk.current,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        BasicTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 18.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            ),
            cursorBrush = SolidColor(Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.5.dp, LocalInk.current)
                .padding(12.dp)
                .focusRequester(focusRequester),
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = stringResource(R.string.cancel),
                fontSize = 18.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(12.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.save),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier
                    .clickable { onConfirm(textFieldValue.text) }
                    .padding(12.dp),
            )
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalFoundationApi::class)

@Composable
private fun ChangeIconDialog(
    viewModel: MainViewModel,
    app: AppModel,
    onDismiss: () -> Unit,
    onImportClick: () -> Unit,
) {
    val context = LocalContext.current
    val pickerShape = if (viewModel.roundedIcons) RoundedCornerShape(percent = 26) else CircleShape
    val sizePx = remember { (36 * context.resources.displayMetrics.density).toInt() }
    val hasOverride = remember(viewModel.shortcutRefresh) {
        viewModel.prefs.getIconOverride(app.key) != null
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            materialIcons.size
        }
    }

    val bundled = remember { IconUtility.bundledIcons }
    val columns = 5
    val rows = 4
    val itemsPerPage = columns * rows
    var tab by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(0) }
    val totalPages = (bundled.size + itemsPerPage - 1) / itemsPerPage
    val pageIcons = remember(page) {
        val start = page * itemsPerPage
        val end = minOf(start + itemsPerPage, bundled.size)
        bundled.subList(start, end)
    }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    BottomSheet(onDismiss = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = app.label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.weight(1f),
            )
            if (hasOverride) {
                Icon(
                    imageVector = Icons.Rounded.Restore,
                    contentDescription = stringResource(R.string.reset_icon),
                    tint = LocalInk.current,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            viewModel.clearIconOverride(app.key)
                            onDismiss()
                        },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconSourceTab(stringResource(R.string.katapult_icons), tab == 0) { tab = 0 }
            IconSourceTab(stringResource(R.string.material_icons), tab == 1) { tab = 1 }
        }

        if (tab == 0) {
            Text(
                text = stringResource(R.string.import_icon),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            BottomSheetOption(stringResource(R.string.choose_png), icon = Icons.Rounded.Upload) {
                onImportClick()
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.katapult_icons),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 4.dp),
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
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        for (col in 0 until columns) {
                            val idx = row * columns + col
                            if (idx < pageIcons.size) {
                                val icon = pageIcons[idx]
                                val bitmap = remember(icon.resId) {
                                    IconUtility.renderDrawableResource(context, icon.resId, sizePx)
                                }
                                Box(
                                    modifier = Modifier.clickable {
                                        viewModel.setBundledIcon(app.key, icon.resName)
                                        onDismiss()
                                    },
                                ) {
                                    AppIconCircle(
                                        bitmap = bitmap,
                                        size = 48.dp,
                                        borderWidth = 1.5.dp,
                                        shape = pickerShape,
                                    )
                                }
                            } else {
                                Box(Modifier.size(48.dp))
                            }
                        }
                    }
                }
                PickerPageBar(page, totalPages, onPrev = { page-- }, onNext = { page++ })
            }
        } else {
            val density = LocalDensity.current
            var mPage by remember { mutableIntStateOf(0) }
            val mTotalPages = (materialIcons.size + itemsPerPage - 1) / itemsPerPage
            val mPageIcons = remember(mPage) {
                val start = mPage * itemsPerPage
                val end = minOf(start + itemsPerPage, materialIcons.size)
                materialIcons.subList(start, end)
            }
            Column(
                modifier = Modifier
                    .pointerInput(mPage, mTotalPages) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccumulator = 0f },
                            onDragEnd = {
                                if (abs(dragAccumulator) > 80f) {
                                    if (dragAccumulator < 0 && mPage < mTotalPages - 1) mPage++
                                    else if (dragAccumulator > 0 && mPage > 0) mPage--
                                }
                            },
                            onHorizontalDrag = { _, amount -> dragAccumulator += amount },
                        )
                    },
            ) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        for (col in 0 until columns) {
                            val idx = row * columns + col
                            if (idx < mPageIcons.size) {
                                val (name, vector) = mPageIcons[idx]
                                val painter = rememberVectorPainter(vector)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(1.5.dp, LocalInk.current, pickerShape)
                                        .clickable {
                                            viewModel.saveMaterialIcon(
                                                app.key,
                                                rasterizeVectorIcon(painter, density),
                                            )
                                            onDismiss()
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painter,
                                        contentDescription = name,
                                        tint = LocalInk.current,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            } else {
                                Box(Modifier.size(48.dp))
                            }
                        }
                    }
                }
                PickerPageBar(mPage, mTotalPages, onPrev = { mPage-- }, onNext = { mPage++ })
            }
        }
    }
}

@Composable
private fun IconSourceTab(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .then(
                if (selected) Modifier.background(LocalInk.current, shape)
                else Modifier.border(1.5.dp, LocalInk.current, shape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
            color = if (selected) LocalSurface.current else LocalInk.current,
        )
    }
}

@Composable
private fun PickerPageBar(page: Int, totalPages: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    if (totalPages <= 1) return
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (page > 0) {
            ArrowButton(iconRes = R.drawable.ic_arrow_left, onClick = onPrev)
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
            ArrowButton(iconRes = R.drawable.ic_arrow_right, onClick = onNext)
        } else {
            Spacer(Modifier.size(ArrowSize))
        }
    }
}

private fun rasterizeVectorIcon(painter: VectorPainter, density: Density): Bitmap {
    val sizePx = 192
    val image = ImageBitmap(sizePx, sizePx)
    val inner = sizePx * 0.6f
    val inset = (sizePx - inner) / 2f
    CanvasDrawScope().draw(
        density,
        LayoutDirection.Ltr,
        Canvas(image),
        Size(sizePx.toFloat(), sizePx.toFloat()),
    ) {
        translate(inset, inset) {
            with(painter) {
                draw(Size(inner, inner), colorFilter = ColorFilter.tint(Color.Black))
            }
        }
    }
    return image.asAndroidBitmap()
}

@Composable
private fun AppShortcutsDialog(
    viewModel: MainViewModel,
    app: AppModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val hasPermission = remember { ShortcutHelper.hasHostPermission(context) }
    val saved = remember(app.packageName) {
        viewModel.prefs.getSavedShortcuts().filter { it.packageName == app.packageName }
    }
    val initial = remember(app.packageName) {
        val all = ShortcutHelper.queryShortcuts(context, app.packageName)
        val liveIds = all.map { it.id }.toSet()
        val published = all.filter { it.isDynamic || it.isDeclaredInManifest }
            .map { Pair(it.id, ShortcutHelper.label(it)) }
        val stale = if (hasPermission) saved.filterNot { it.shortcutId in liveIds } else emptyList()
        Pair(
            (published + stale.map { Pair(it.shortcutId, it.label) }).sortedBy { it.second.lowercase() },
            stale.map { it.shortcutId }.toSet(),
        )
    }
    var shortcuts by remember(app.packageName) { mutableStateOf(initial.first) }
    val staleIds = initial.second
    var enabledIds by remember {
        mutableStateOf(saved.map { it.shortcutId }.toSet())
    }
    val itemsPerPage = 6
    var page by remember { mutableIntStateOf(0) }
    val totalPages = ((shortcuts.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)
    val pageShortcuts = remember(page, shortcuts) {
        val start = page * itemsPerPage
        val end = minOf(start + itemsPerPage, shortcuts.size)
        if (start < shortcuts.size) shortcuts.subList(start, end) else emptyList()
    }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val iconShape = if (viewModel.roundedIcons) RoundedCornerShape(percent = 26) else CircleShape

    BottomSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.app_shortcuts),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
            color = LocalInk.current,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        if (shortcuts.isEmpty()) {
            Text(
                text = stringResource(
                    if (hasPermission) R.string.no_app_shortcuts_app else R.string.no_app_shortcuts,
                ),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            return@BottomSheet
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
                if (i < pageShortcuts.size) {
                    val (id, label) = pageShortcuts[i]
                    val isEnabled = id in enabledIds
                    val sizePx = remember { (36 * context.resources.displayMetrics.density).toInt() }
                    val bitmap = remember(id) {
                        if (id in staleIds) IconUtility.loadIcon(context, app.packageName, "", sizePx)
                        else IconUtility.loadShortcutIcon(context, app.packageName, id, sizePx)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setShortcutEnabled(app.packageName, id, label, !isEnabled)
                                enabledIds = if (isEnabled) enabledIds - id else enabledIds + id
                                if (isEnabled && id in staleIds) {
                                    shortcuts = shortcuts.filterNot { it.first == id }
                                    if (page > 0 && page * itemsPerPage >= shortcuts.size) page--
                                }
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIconCircle(bitmap = bitmap, size = 36.dp, borderWidth = 1.5.dp, shape = iconShape)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            label,
                            fontSize = 18.sp,
                            fontFamily = LatoFamily,
                            color = LocalInk.current,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        val checkShape = RoundedCornerShape(4.dp)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .then(
                                    if (isEnabled) Modifier.background(LocalInk.current, checkShape)
                                    else Modifier.border(2.5.dp, LocalInk.current, checkShape)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isEnabled) {
                                Text(
                                    text = "✓",
                                    color = LocalSurface.current,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                } else if (totalPages > 1) {
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