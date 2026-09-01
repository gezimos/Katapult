package com.gezimos.katapult.ui

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.gezimos.katapult.R
import androidx.compose.ui.unit.dp
import com.gezimos.katapult.MainViewModel
import com.gezimos.katapult.Screen

@Composable
fun App(
    viewModel: MainViewModel,
    imagePicker: ActivityResultLauncher<String>,
    iconPicker: ActivityResultLauncher<Array<String>>,
) {
    val iconShape = if (viewModel.roundedIcons) RoundedIconShape else CircleShape
    val smallShape = if (viewModel.roundedIcons) RoundedSmallShape else CircleShape
    val badgeShape = if (viewModel.roundedIcons) RoundedBadgeShape else CircleShape
    val dark = viewModel.darkMode
    CompositionLocalProvider(
        androidx.compose.foundation.LocalIndication provides NoIndication,
        LocalIconSize provides viewModel.iconSize.dp,
        LocalIconShape provides iconShape,
        LocalSmallIconShape provides smallShape,
        LocalBadgeShape provides badgeShape,
        LocalSheetDismissSignal provides viewModel.sheetDismissSignal,
        LocalInk provides if (dark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black,
        LocalSurface provides if (dark) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White,
        LocalIconFilter provides if (dark) EinkColorFilterDark else EinkColorFilter,
    ) {
        when (viewModel.screen) {
            Screen.ONBOARDING -> OnboardingScreen(viewModel)
            Screen.HOME -> HomeScreen(viewModel, imagePicker, iconPicker)
            Screen.ALL_APPS -> AllAppsScreen(viewModel, iconPicker)
            Screen.SETTINGS -> SettingsScreen(viewModel)
        }
        com.gezimos.katapult.lockscreen.LockscreenReEnableSheet(viewModel)
        if (viewModel.iconImportError) {
            val context = LocalContext.current
            val message = stringResource(R.string.icon_import_failed)
            LaunchedEffect(Unit) {
                android.widget.Toast.makeText(
                    context, message, android.widget.Toast.LENGTH_LONG,
                ).show()
                viewModel.iconImportError = false
            }
        }
    }
}
