package com.gezimos.katapult.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gezimos.katapult.MainViewModel
import com.gezimos.katapult.R
import com.gezimos.katapult.model.AppModel
import com.gezimos.katapult.util.IconUtility
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllAppsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var showResetConfirm by remember { mutableStateOf(false) }
    val pageApps = remember(viewModel.currentPage, viewModel.orderedApps) {
        viewModel.getPageApps(viewModel.currentPage)
    }

    val slots = remember(pageApps) {
        val list = pageApps.toMutableList()
        while (list.size < 12) list.add(AppModel("", "", ""))
        list
    }

    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .then(if (!viewModel.prefs.hideStatusBar) Modifier.statusBarsPadding() else Modifier)
            .navigationBarsPadding()
            .padding(PagePadding)
            .pointerInput(viewModel.currentPage, viewModel.totalPages) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDragEnd = {
                        val wrap = viewModel.prefs.infiniteScroll
                        val threshold = 100f
                        if (abs(dragAccumulator) > threshold) {
                            if (dragAccumulator < 0) {
                                val next = if (viewModel.currentPage < viewModel.totalPages - 1)
                                    viewModel.currentPage + 1
                                else if (wrap) 0 else viewModel.currentPage
                                viewModel.showPage(next)
                            } else {
                                val prev = if (viewModel.currentPage > 0)
                                    viewModel.currentPage - 1
                                else if (wrap) viewModel.totalPages - 1 else viewModel.currentPage
                                viewModel.showPage(prev)
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccumulator += dragAmount
                    }
                )
            },
    ) {
        // App grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            for (row in 0 until 4) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 3) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            val idx = row * 3 + col
                            val absoluteIndex = viewModel.currentPage * viewModel.appsPerPage + idx
                            val app = slots[idx]
                            if (app.packageName.isNotEmpty()) {
                                AppGridItem(
                                    app = app,
                                    notificationCount = if (viewModel.prefs.notificationIndicators)
                                        viewModel.notificationCounts[app.packageName] ?: 0 else 0,
                                    isHighlighted = viewModel.reorderMode && absoluteIndex == viewModel.reorderHighlightIndex,
                                    hideLabel = viewModel.prefs.hideAppNames,
                                    onClick = {
                                        if (viewModel.reorderMode) {
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
                                    Box(Modifier.size(IconSize))
                                    Spacer(Modifier.height(4.dp))
                                    Text("", fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Arrow row + dots
        val wrap = viewModel.prefs.infiniteScroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(IconSize), contentAlignment = Alignment.CenterStart) {
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
                            tint = Color.Black,
                            modifier = Modifier
                                .size(ArrowSize)
                                .border(2.5.dp, Color.Black, buttonShape)
                                .clickable { showResetConfirm = true }
                                .padding(8.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.save_uppercase),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LatoFamily,
                            color = Color.Black,
                            maxLines = 1,
                            modifier = Modifier
                                .border(2.5.dp, Color.Black, buttonShape)
                                .clickable { viewModel.finishReorder() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
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
                                        if (i == viewModel.currentPage) Modifier.background(Color.Black, CircleShape)
                                        else Modifier.border(1.5.dp, Color.Black, CircleShape)
                                    )
                            )
                        }
                    }
                }
            }

            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(IconSize), contentAlignment = Alignment.CenterEnd) {
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

    // Context menu bottom sheet (only when NOT renaming and NOT reordering)
    val menuApp = viewModel.contextMenuApp
    if (menuApp != null && !viewModel.showRenameDialog && !viewModel.reorderMode) {
        BottomSheet(onDismiss = { viewModel.contextMenuApp = null }) {
            Text(
                text = menuApp.label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            BottomSheetOption(stringResource(R.string.reorder), icon = Icons.Rounded.SwapVert) {
                viewModel.enterReorderMode(menuApp)
            }
            BottomSheetOption(stringResource(R.string.rename), icon = Icons.Rounded.Edit) {
                viewModel.showRenameDialog = true
            }
            BottomSheetOption(stringResource(R.string.hide), icon = Icons.Rounded.VisibilityOff) {
                viewModel.hideApp(menuApp.packageName)
            }
            BottomSheetOption(stringResource(R.string.app_info), icon = Icons.Rounded.Info) {
                viewModel.contextMenuApp = null
                viewModel.launchIntent(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${menuApp.packageName}")
                })
            }
            BottomSheetOption(stringResource(R.string.notifications), icon = Icons.Rounded.Notifications) {
                viewModel.contextMenuApp = null
                viewModel.launchIntent(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, menuApp.packageName)
                })
            }
            // Only show uninstall for user-installed apps
            val isSystemApp = try {
                val appInfo = context.packageManager.getApplicationInfo(menuApp.packageName, 0)
                appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
            } catch (_: Exception) { true }
            if (!isSystemApp) {
                BottomSheetOption(stringResource(R.string.uninstall), icon = Icons.Rounded.Delete) {
                    viewModel.contextMenuApp = null
                    viewModel.launchIntent(context, Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                        data = Uri.parse("package:${menuApp.packageName}")
                    })
                }
            }
        }
    }

    // Rename dialog (replaces context menu, not stacked)
    if (viewModel.showRenameDialog && menuApp != null) {
        RenameDialog(
            currentName = viewModel.getAppDisplayName(menuApp.packageName),
            onDismiss = {
                viewModel.showRenameDialog = false
                viewModel.contextMenuApp = null
            },
            onConfirm = { newName ->
                viewModel.renameApp(menuApp.packageName, newName)
            },
        )
    }

    if (showResetConfirm) {
        BottomSheet(onDismiss = { showResetConfirm = false }) {
            Text(
                text = stringResource(R.string.reset_order_confirm),
                fontSize = 18.sp,
                fontFamily = LatoFamily,
                color = Color.Black,
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
            color = Color.Black,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        BasicTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 18.sp,
                fontFamily = LatoFamily,
                color = Color.Black,
            ),
            cursorBrush = SolidColor(Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.5.dp, Color.Black)
                .padding(12.dp)
                .focusRequester(focusRequester),
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = stringResource(R.string.cancel),
                fontSize = 18.sp,
                fontFamily = LatoFamily,
                color = Color.Black,
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
                color = Color.Black,
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
private fun AppGridItem(
    app: AppModel,
    notificationCount: Int,
    isHighlighted: Boolean = false,
    hideLabel: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val sizePx = remember { (IconSize.value * context.resources.displayMetrics.density).toInt() }
    val bitmap = remember(app.packageName, app.activityName) {
        IconUtility.loadIcon(context, app.packageName, app.activityName, sizePx)
    }
    val isRounded = LocalIconShape.current != CircleShape

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            AppIconCircle(bitmap = bitmap, size = IconSize)
            if (isHighlighted) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .dashedDotBorder(isRounded = isRounded, outset = 5.dp),
                )
            }

            if (notificationCount > 0) {
                NotificationBadge(
                    count = notificationCount,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
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
            text = if (hideLabel) "" else displayLabel,
            fontSize = 18.sp,
            fontFamily = LatoFamily,
            color = Color.Black,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(110.dp),
        )
    }
}
