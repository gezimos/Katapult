package com.gezimos.katapult.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.gezimos.katapult.MainViewModel
import com.gezimos.katapult.R
import com.gezimos.katapult.Screen
import com.gezimos.katapult.util.DeviceHelper

@Composable
fun OnboardingScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val ink = LocalInk.current
    val surface = LocalSurface.current
    val isMudita = remember { DeviceHelper.isMuditaKompakt() }

    var hasNotificationPermission by remember { mutableStateOf(false) }
    var isDefaultLauncher by remember { mutableStateOf(false) }
    var hasCallSmsPermission by remember { mutableStateOf(false) }
    var showHighlight by remember { mutableStateOf(false) }

    // Flash animation for asterisks
    val flashTransition = rememberInfiniteTransition(label = "flash")
    val flashAlpha by flashTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "flashAlpha",
    )

    // Poll permission state
    LaunchedEffect(context) {
        while (true) {
            hasNotificationPermission = NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)

            isDefaultLauncher = DeviceHelper.isDefaultLauncher(context)

            hasCallSmsPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

            kotlinx.coroutines.delay(1000)
        }
    }

    val allDone = hasNotificationPermission && isDefaultLauncher &&
        (!isMudita || hasCallSmsPermission)
    LaunchedEffect(allDone) {
        if (allDone) showHighlight = false
    }

    val notifAsterisk = " *"
    val launcherAsterisk = " *"
    val callSmsAsterisk = if (isMudita) " *" else ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        // Logo
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.katapult_logo),
                contentDescription = null,
                tint = ink,
                modifier = Modifier.width(160.dp).height(43.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        // 1. Notification Indicators
        SettingsToggleRow(
            title = stringResource(R.string.notification_indicators) + notifAsterisk,
            description = stringResource(R.string.onboarding_notification_desc),
            checked = hasNotificationPermission,
            onCheckedChange = {
                try {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (_: Exception) {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    })
                }
            },
        )

        // 2. Call & SMS Badges (Mudita Kompakt only)
        if (isMudita) {
            val callSmsPermissions = remember {
                arrayOf(
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.WRITE_CALL_LOG,
                    Manifest.permission.READ_SMS,
                )
            }
            val openAppSettings = {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                } catch (_: Exception) {}
            }
            val callSmsLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                if (result.values.any { !it }) {
                    val activity = context as? Activity
                    val canAskAgain = activity != null && callSmsPermissions.any {
                        ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
                    }
                    if (!canAskAgain) openAppSettings()
                }
            }
            SettingsToggleRow(
                title = stringResource(R.string.call_sms_badges) + callSmsAsterisk,
                description = stringResource(R.string.call_sms_badges_desc),
                checked = hasCallSmsPermission,
                onCheckedChange = {
                    if (hasCallSmsPermission) openAppSettings()
                    else callSmsLauncher.launch(callSmsPermissions)
                },
            )
        }

        // 3. Default Launcher
        SettingsToggleRow(
            title = stringResource(R.string.set_default_launcher) + launcherAsterisk,
            description = stringResource(R.string.onboarding_launcher_desc),
            checked = isDefaultLauncher,
            onCheckedChange = {
                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            },
        )

        // Required hint
        Text(
            text = stringResource(R.string.required_hint),
            fontSize = 14.sp,
            fontFamily = LatoFamily,
            color = ink.copy(alpha = if (showHighlight && !allDone) flashAlpha else 1f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.weight(1f))

        // Finish button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (allDone) Modifier
                        .border(2.5.dp, ink, RoundedCornerShape(10.dp))
                        .clickable {
                            viewModel.prefs.onboardingComplete = true
                            viewModel.prefs.notificationIndicators = true
                            viewModel.navigateTo(Screen.HOME)
                        }
                    else Modifier
                        .drawBehind {
                            val stroke = 1.5.dp.toPx()
                            val dash = 8.dp.toPx()
                            val gap = 6.dp.toPx()
                            val cr = 10.dp.toPx()
                            drawRoundRect(
                                color = ink,
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(cr, cr),
                                style = Stroke(
                                    width = stroke,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap)),
                                ),
                            )
                        }
                        .clickable { showHighlight = true }
                )
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.finish),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = ink,
            )
        }

    }
}
