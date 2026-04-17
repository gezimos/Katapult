package com.gezimos.katapult.ui

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.gezimos.katapult.MainViewModel
import com.gezimos.katapult.Screen

@Composable
fun App(viewModel: MainViewModel, imagePicker: ActivityResultLauncher<String>) {
    val noIndication = object : androidx.compose.foundation.IndicationNodeFactory {
        override fun create(interactionSource: androidx.compose.foundation.interaction.InteractionSource): androidx.compose.ui.Modifier.Node {
            return object : androidx.compose.ui.Modifier.Node() {}
        }
        override fun hashCode() = 0
        override fun equals(other: Any?) = other === this
    }
    val iconShape = if (viewModel.roundedIcons) RoundedIconShape else CircleShape
    val smallShape = if (viewModel.roundedIcons) RoundedSmallShape else CircleShape
    val badgeShape = if (viewModel.roundedIcons) RoundedBadgeShape else CircleShape
    CompositionLocalProvider(
        androidx.compose.foundation.LocalIndication provides noIndication,
        LocalIconShape provides iconShape,
        LocalSmallIconShape provides smallShape,
        LocalBadgeShape provides badgeShape,
    ) {
        when (viewModel.screen) {
            Screen.ONBOARDING -> OnboardingScreen(viewModel)
            Screen.HOME -> HomeScreen(viewModel, imagePicker)
            Screen.ALL_APPS -> AllAppsScreen(viewModel)
            Screen.SETTINGS -> SettingsScreen(viewModel)
        }
    }
}
