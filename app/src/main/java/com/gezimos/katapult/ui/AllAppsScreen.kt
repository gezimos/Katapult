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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllAppsScreen(viewModel: MainViewModel, iconPicker: ActivityResultLauncher<Array<String>>) {
    val context = LocalContext.current
    var showResetConfirm by remember { mutableStateOf(false) }

    var showReorderHint by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel.reorderMode) {
        if (viewModel.reorderMode) {
            showReorderHint = true
            kotlinx.coroutines.delay(4000)
            showReorderHint = false
        } else {
            showReorderHint = false
        }
    }

    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalSurface.current)
            .then(if (!viewModel.prefs.hideStatusBar) Modifier.statusBarsPadding() else Modifier)
            .navigationBarsPadding()
            .padding(PagePadding)
            .pointerInput(viewModel.currentPage, viewModel.totalPages, viewModel.prefs.verticalAppGestures) {
                val handleDragEnd = {
                    val wrap = viewModel.prefs.infiniteScroll
                    val threshold = 100f
                    if (abs(dragAccumulator) > threshold) {
                        if (dragAccumulator < 0) {
                            val next = if (viewModel.currentPage < viewModel.totalPages - 1)
                                viewModel.currentPage + 1
                            else if (wrap) 0 else viewModel.currentPage
                            viewModel.showPage(next)
                        } else {
                            if (!wrap && viewModel.currentPage == 0) {
                                viewModel.navigateTo(Screen.HOME)
                            } else {
                                val prev = if (viewModel.currentPage > 0)
                                    viewModel.currentPage - 1
                                else viewModel.totalPages - 1
                                viewModel.showPage(prev)
                            }
                        }
                    }
                }
                if (viewModel.prefs.verticalAppGestures) {
                    detectVerticalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onDragEnd = handleDragEnd,
                        onVerticalDrag = { _, dragAmount -> dragAccumulator += dragAmount },
                    )
                } else {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onDragEnd = handleDragEnd,
                        onHorizontalDrag = { _, dragAmount -> dragAccumulator += dragAmount },
                    )
                }
            },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val columns = viewModel.gridColumns
            val rowHeight = if (viewModel.prefs.hideAppNames) AllAppsRowHeightNoLabels else AllAppsRowHeight
            val measuredRows = (maxHeight / rowHeight).toInt().coerceAtLeast(1)
            val perPage = if (viewModel.reorderMode) viewModel.appsPerPage else measuredRows * columns
            val rows = perPage / columns
            if (!viewModel.reorderMode && viewModel.appsPerPage != perPage) {
                viewModel.updateAppsPerPage(perPage)
            }
            val pageApps = remember(viewModel.currentPage, viewModel.orderedApps, perPage) {
                val start = viewModel.currentPage * perPage
                val end = minOf(start + perPage, viewModel.orderedApps.size)
                if (start < viewModel.orderedApps.size) viewModel.orderedApps.subList(start, end) else emptyList()
            }
            val slots = remember(pageApps, perPage) {
                val list = pageApps.toMutableList()
                while (list.size < perPage) list.add(AppModel("", "", ""))
                list
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (col in 0 until columns) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            val idx = row * columns + col
                            val absoluteIndex = viewModel.currentPage * perPage + idx
                            val app = slots.getOrElse(idx) { AppModel("", "", "") }
                            if (app.packageName.isNotEmpty()) {
                                AppGridItem(
                                    app = app,
                                    notificationCount = if (viewModel.prefs.notificationIndicators &&
                                        app.shortcutId.isEmpty())
                                        viewModel.notificationCounts[app.packageName] ?: 0 else 0,
                                    isHighlighted = viewModel.reorderMode && absoluteIndex == viewModel.reorderHighlightIndex,
                                    hideLabel = viewModel.prefs.hideAppNames,
                                    refresh = viewModel.shortcutRefresh,
                                    onClick = {
                                        if (viewModel.reorderMode) {
                                            showReorderHint = false
                                            viewModel.reorderTap(absoluteIndex)
                                        } else {
                                            viewModel.launchApp(context, app)
                                        }
                                    },
                                    onLongClick = {
                                        if (!viewModel.reorderMode) {
                                            viewModel.contextMenuApp = app
                                        }
                                    },
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.size(LocalIconSize.current))
                                    Spacer(Modifier.height(4.dp))
                                    Text("", fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
            }
        }

        val wrap = viewModel.prefs.infiniteScroll
        if (viewModel.reorderMode || (!viewModel.prefs.hideArrowButtons && viewModel.totalPages > 1)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(LocalIconSize.current), contentAlignment = Alignment.CenterStart) {
                        val canGoPrev = viewModel.currentPage > 0 || wrap
                        if (canGoPrev) {
                            ArrowButton(
                                iconRes = R.drawable.ic_arrow_left,
                                onClick = {
                                    val prev = if (viewModel.currentPage > 0)
                                        viewModel.currentPage - 1 else viewModel.totalPages - 1
                                    viewModel.showPage(prev)
                                },
                            )
                        }
                    }
                }
            }

            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (viewModel.reorderMode) {
                    val buttonShape = LocalSmallIconShape.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = stringResource(R.string.cd_reset_order),
                            tint = LocalInk.current,
                            modifier = Modifier
                                .size(ArrowSize)
                                .border(2.5.dp, LocalInk.current, buttonShape)
                                .clickable { showResetConfirm = true }
                                .padding(8.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .height(ArrowSize)
                                .border(2.5.dp, LocalInk.current, buttonShape)
                                .clickable { viewModel.finishReorder() }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.save_uppercase),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LatoFamily,
                                color = LocalInk.current,
                                maxLines = 1,
                            )
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (i in 0 until viewModel.totalPages) {
                            Box(
                                Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(8.dp)
                                    .then(
                                        if (i == viewModel.currentPage) Modifier.background(LocalInk.current, CircleShape)
                                        else Modifier.border(1.5.dp, LocalInk.current, CircleShape)
                                    )
                            )
                        }
                    }
                }
            }

            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(LocalIconSize.current), contentAlignment = Alignment.CenterEnd) {
                        val canGoNext = viewModel.currentPage < viewModel.totalPages - 1 || wrap
                        if (canGoNext) {
                            ArrowButton(
                                iconRes = R.drawable.ic_arrow_right,
                                onClick = {
                                    val next = if (viewModel.currentPage < viewModel.totalPages - 1)
                                        viewModel.currentPage + 1 else 0
                                    viewModel.showPage(next)
                                },
                            )
                        }
                    }
                }
            }
        }
        }
    }

    if (showReorderHint) {
        androidx.compose.ui.window.Popup(alignment = Alignment.TopCenter) {
            Box(Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp)) {
                Box(
                    modifier = Modifier
                        .background(LocalSurface.current, RoundedCornerShape(12.dp))
                        .border(2.5.dp, LocalInk.current, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.reorder_hint),
                        fontSize = 14.sp,
                        fontFamily = LatoFamily,
                        color = LocalInk.current,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    viewModel.contextMenuApp?.let { menuApp ->
        AppContextMenu(
            viewModel = viewModel,
            app = menuApp,
            iconPicker = iconPicker,
            showReorder = true,
            showHide = true,
            onDismiss = { viewModel.contextMenuApp = null },
        )
    }


    if (showResetConfirm) {
        BottomSheet(onDismiss = { showResetConfirm = false }) {
            Text(
                text = stringResource(R.string.reset_order_confirm),
                fontSize = 18.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            BottomSheetOption(stringResource(R.string.reset), icon = Icons.Rounded.RestartAlt) {
                viewModel.resetOrder()
                showResetConfirm = false
            }
        }
    }
}

@Composable
private fun AppGridItem(
    app: AppModel,
    notificationCount: Int,
    isHighlighted: Boolean = false,
    hideLabel: Boolean = false,
    refresh: Int = 0,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val iconSize = LocalIconSize.current
    val sizePx = remember(iconSize) { (iconSize.value * context.resources.displayMetrics.density).toInt() }
    val bitmap = remember(app.key, refresh, sizePx) {
        if (app.shortcutId.isNotEmpty()) {
            IconUtility.loadShortcutIcon(context, app.packageName, app.shortcutId, sizePx)
        } else {
            IconUtility.loadIcon(context, app.packageName, app.activityName, sizePx)
        }
    }
    val isRounded = LocalIconShape.current != CircleShape

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            AppIconCircle(bitmap = bitmap, size = iconSize)
            if (isHighlighted) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .dashedDotBorder(isRounded = isRounded, outset = 5.dp, color = LocalInk.current),
                )
            }

            if (notificationCount > 0) {
                NotificationBadge(
                    count = notificationCount,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
        if (!hideLabel) {
            Spacer(Modifier.height(4.dp))
            val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
            val maxWidth = with(androidx.compose.ui.platform.LocalDensity.current) { 110.dp.toPx() }
            val style = TextStyle(fontSize = 18.sp, fontFamily = LatoFamily)
            val displayLabel = remember(app.label) {
                val full = textMeasurer.measure(app.label, style, maxLines = 1)
                if (full.size.width <= maxWidth.toInt()) {
                    app.label
                } else {
                    var end = app.label.length
                    while (end > 1) {
                        end--
                        val truncated = app.label.take(end) + "."
                        val measured = textMeasurer.measure(truncated, style, maxLines = 1)
                        if (measured.size.width <= maxWidth.toInt()) return@remember truncated
                    }
                    "."
                }
            }
            Text(
                text = displayLabel,
                fontSize = 18.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(110.dp),
            )
        }
    }
}
