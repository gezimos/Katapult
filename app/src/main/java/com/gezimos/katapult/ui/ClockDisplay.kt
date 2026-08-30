package com.gezimos.katapult.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.gezimos.katapult.R
import com.gezimos.katapult.util.WeatherHelper

/**
 * The home clock block — alarm/battery status, the big clock with AM/PM, and the date,
 * with optional "islands" behind each. Shared by the home screen and the screensaver so
 * they render identically. Click handlers are optional (the screensaver passes none).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.ClockDisplay(
    clockTime: String,
    clockAmPm: String?,
    clockDate: String,
    alarmTime: String?,
    batteryPercent: Int,
    isCharging: Boolean,
    showBattery: Boolean,
    showAlarm: Boolean = true,
    weather: WeatherHelper.Weather? = null,
    islandsActive: Boolean,
    onClockClick: (() -> Unit)? = null,
    onClockLongClick: (() -> Unit)? = null,
    onDateClick: (() -> Unit)? = null,
    onDateLongClick: (() -> Unit)? = null,
    onBatteryClick: (() -> Unit)? = null,
    onWeatherClick: (() -> Unit)? = null,
    topSpacing: Dp = 32.dp,
) {
    Spacer(Modifier.height(topSpacing))
    val hasWeather = weather != null
    val hasAlarm = showAlarm && alarmTime != null
    val hasBattery = showBattery && batteryPercent >= 0
    val hasStatus = hasWeather || hasAlarm || hasBattery
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(if (islandsActive) Modifier.zIndex(1f) else Modifier)
            .homeIsland(islandsActive && hasStatus, LocalSurface.current, LocalInk.current, hPad = 12.dp),
    ) {
        if (weather != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onWeatherClick != null) Modifier.clickable(onClick = onWeatherClick) else Modifier,
            ) {
                Icon(
                    imageVector = weatherIcon(weather.conditionId, weather.iconInfo),
                    contentDescription = stringResource(R.string.cd_weather),
                    tint = LocalInk.current,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = stringResource(R.string.weather_temp, weather.temperature, weather.unit),
                    fontSize = 18.sp,
                    fontFamily = LatoFamily,
                    color = LocalInk.current,
                )
            }
        }
        if (showAlarm && alarmTime != null) {
            if (hasWeather) {
                Spacer(Modifier.width(16.dp))
            }
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = stringResource(R.string.cd_alarm),
                tint = LocalInk.current,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = alarmTime,
                fontSize = 18.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
        if (hasBattery) {
            if (hasWeather || hasAlarm) {
                Spacer(Modifier.width(16.dp))
            }
            val batteryIcon = if (isCharging) {
                Icons.Rounded.BatteryChargingFull
            } else {
                when {
                    batteryPercent >= 95 -> Icons.Rounded.BatteryFull
                    batteryPercent >= 85 -> Icons.Rounded.Battery6Bar
                    batteryPercent >= 70 -> Icons.Rounded.Battery5Bar
                    batteryPercent >= 55 -> Icons.Rounded.Battery4Bar
                    batteryPercent >= 40 -> Icons.Rounded.Battery3Bar
                    batteryPercent >= 25 -> Icons.Rounded.Battery2Bar
                    batteryPercent >= 10 -> Icons.Rounded.Battery1Bar
                    else -> Icons.Rounded.Battery0Bar
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onBatteryClick != null) Modifier.clickable(onClick = onBatteryClick) else Modifier,
            ) {
                Icon(
                    imageVector = batteryIcon,
                    contentDescription = stringResource(R.string.cd_battery),
                    tint = LocalInk.current,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = stringResource(R.string.battery_percent, batteryPercent),
                    fontSize = 18.sp,
                    fontFamily = LatoFamily,
                    color = LocalInk.current,
                )
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .homeIsland(islandsActive, LocalSurface.current, LocalInk.current, hPad = 16.dp)
            .then(
                if (onClockClick != null || onClockLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onClockClick?.invoke() },
                        onLongClick = onClockLongClick,
                    )
                } else Modifier,
            ),
    ) {
        Text(
            text = clockTime,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
            color = LocalInk.current,
        )
        if (clockAmPm != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = clockAmPm,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
    }

    Text(
        text = clockDate,
        fontSize = 20.sp,
        fontFamily = LatoFamily,
        color = LocalInk.current,
        modifier = Modifier
            .homeIsland(islandsActive, LocalSurface.current, LocalInk.current, hPad = 12.dp)
            .then(
                if (onDateClick != null || onDateLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onDateClick?.invoke() },
                        onLongClick = onDateLongClick,
                    )
                } else Modifier,
            ),
    )
}

@Composable
private fun weatherIcon(conditionId: Int, iconInfo: String?): ImageVector {
    val day = iconInfo == null || iconInfo.endsWith("d")
    val res = when (conditionId) {
        in 200..232 -> R.drawable.ic_weather_thunderstorm
        in 300..321 -> R.drawable.ic_weather_rainy_light
        511 -> R.drawable.ic_weather_mix
        in 500..531 -> R.drawable.ic_weather_rainy
        in 611..616 -> R.drawable.ic_weather_mix
        in 600..622 -> R.drawable.ic_weather_snowy
        in 701..781 -> R.drawable.ic_weather_foggy
        800, 801 ->
            if (day) R.drawable.ic_weather_clear_day else R.drawable.ic_weather_clear_night
        802, 803 ->
            if (day) R.drawable.ic_weather_partly_cloudy_day
            else R.drawable.ic_weather_partly_cloudy_night
        else -> R.drawable.ic_weather_cloudy
    }
    return ImageVector.vectorResource(res)
}
