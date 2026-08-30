package com.gezimos.katapult.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gezimos.katapult.service.DirectBadgeHelper
import com.gezimos.katapult.service.NotificationListener
import com.gezimos.katapult.ui.AppIconCircle
import com.gezimos.katapult.ui.ClockDisplay
import com.gezimos.katapult.ui.EinkColorFilter
import com.gezimos.katapult.ui.EinkColorFilterDark
import com.gezimos.katapult.ui.LatoFamily
import com.gezimos.katapult.ui.LocalIconFilter
import com.gezimos.katapult.ui.LocalIconShape
import com.gezimos.katapult.ui.LocalInk
import com.gezimos.katapult.ui.LocalSurface
import com.gezimos.katapult.ui.PagePadding
import com.gezimos.katapult.util.DeviceHelper
import com.gezimos.katapult.util.IconUtility
import com.gezimos.katapult.util.PrefsManager
import com.gezimos.katapult.util.WeatherHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val SCREENSAVER_ROWS = 4

private data class SsClock(
    val time: String,
    val amPm: String?,
    val date: String,
    val alarm: String?,
    val battery: Int,
    val charging: Boolean,
    val weather: WeatherHelper.Weather?,
)

private data class SsRow(val count: Int, val label: String, val bitmap: Bitmap?)

@Composable
fun ScreensaverView() {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    val dark = prefs.darkMode

    val doubleTapBrightness = prefs.screensaverDoubleTapBrightness
    val window = (context as? android.app.Activity)?.window
    var lit by remember { mutableStateOf(false) }
    LaunchedEffect(lit) {
        window?.let { w ->
            w.attributes = w.attributes.apply {
                screenBrightness = if (lit) {
                    WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                } else {
                    0f
                }
            }
        }
    }

    val directBadges = remember {
        if (DeviceHelper.isMuditaKompakt()) DirectBadgeHelper(context) else null
    }
    DisposableEffect(Unit) {
        directBadges?.start()
        onDispose { directBadges?.stop() }
    }

    var clock by remember { mutableStateOf(computeClock(context, prefs)) }
    var rowsState by remember { mutableStateOf(computeRows(context, prefs, directBadges)) }

    when (prefs.screensaverUpdateMode) {
        PrefsManager.SCREENSAVER_MODE_INTERVAL -> {
            LaunchedEffect(Unit) {
                val interval = prefs.screensaverUpdateMinutes.coerceIn(1, 60)
                kotlinx.coroutines.delay(300)
                clock = computeClock(context, prefs)
                rowsState = computeRows(context, prefs, directBadges)
                while (true) {
                    kotlinx.coroutines.delay(millisToNextBoundary(interval))
                    clock = computeClock(context, prefs)
                    rowsState = computeRows(context, prefs, directBadges)
                }
            }
        }
        PrefsManager.SCREENSAVER_MODE_STATIC -> {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(300)
                clock = computeClock(context, prefs)
                rowsState = computeRows(context, prefs, directBadges)
            }
        }
        else -> {
            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context, i: Intent) { clock = computeClock(context, prefs) }
                }
                context.registerReceiver(receiver, IntentFilter().apply {
                    addAction(Intent.ACTION_TIME_TICK)
                    addAction(Intent.ACTION_TIME_CHANGED)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                    addAction(Intent.ACTION_BATTERY_CHANGED)
                })
                onDispose { try { context.unregisterReceiver(receiver) } catch (_: Exception) {} }
            }
            DisposableEffect(Unit) {
                val main = Handler(Looper.getMainLooper())
                val refresh = { main.post { rowsState = computeRows(context, prefs, directBadges) } }
                directBadges?.onCountsChanged = { refresh() }
                NotificationListener.onCountsChangedDream = { refresh() }
                onDispose {
                    NotificationListener.onCountsChangedDream = null
                    directBadges?.onCountsChanged = null
                }
            }
        }
    }

    val wallpaper = remember {
        try {
            (prefs.screensaverWallpaperPath ?: prefs.wallpaperPath)?.let { BitmapFactory.decodeFile(it) }
        } catch (_: Exception) { null }
    }

    val iconShape = if (prefs.roundedIcons) RoundedCornerShape(8.dp) else CircleShape
    CompositionLocalProvider(
        LocalIconShape provides iconShape,
        LocalInk provides if (dark) Color.White else Color.Black,
        LocalSurface provides if (dark) Color.Black else Color.White,
        LocalIconFilter provides if (dark) EinkColorFilterDark else EinkColorFilter,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(LocalSurface.current)
                .then(
                    if (doubleTapBrightness) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = { lit = !lit })
                        }
                    } else Modifier
                ),
        ) {
            if (prefs.screensaverWallpaper && wallpaper != null) {
                Image(
                    bitmap = wallpaper.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (!prefs.hideStatusBar) Modifier.statusBarsPadding() else Modifier)
                    .padding(PagePadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (prefs.screensaverShowClock) {
                    ClockDisplay(
                        clockTime = clock.time,
                        clockAmPm = clock.amPm,
                        clockDate = clock.date,
                        alarmTime = clock.alarm,
                        batteryPercent = clock.battery,
                        isCharging = clock.charging,
                        showBattery = prefs.showBattery,
                        showAlarm = prefs.showAlarm,
                        weather = clock.weather,
                        islandsActive = prefs.screensaverIslands,
                    )
                }
                Spacer(Modifier.weight(1f))
                val (rows, overflow) = rowsState
                val showNotifications = prefs.screensaverShowNotifications && rows.isNotEmpty()
                if (showNotifications) {
                    NotificationIsland(rows, overflow)
                }
                if (!prefs.screensaverShowClock && showNotifications) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun NotificationIsland(rows: List<SsRow>, overflow: Int) {
    val ink = LocalInk.current
    val surface = LocalSurface.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(surface, shape)
                .border(2.5.dp, ink, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIconCircle(bitmap = row.bitmap, size = 32.dp, borderWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = row.count.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LatoFamily,
                        color = ink,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = row.label,
                        fontSize = 18.sp,
                        fontFamily = LatoFamily,
                        color = ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (overflow > 0) {
            Text(
                text = "+$overflow",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = ink,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp, y = 12.dp)
                    .background(surface, RoundedCornerShape(8.dp))
                    .border(2.5.dp, ink, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

private fun millisToNextBoundary(intervalMinutes: Int): Long {
    val cal = Calendar.getInstance()
    val minsToNext = intervalMinutes - (cal.get(Calendar.MINUTE) % intervalMinutes)
    return minsToNext * 60_000L - cal.get(Calendar.SECOND) * 1000L - cal.get(Calendar.MILLISECOND)
}

private fun computeClock(context: Context, prefs: PrefsManager): SsClock {
    val now = Date()
    val clockPattern = prefs.clockFormat
    val fullTime = if (clockPattern == "system") DateFormat.getTimeFormat(context).format(now)
        else SimpleDateFormat(clockPattern, Locale.getDefault()).format(now)
    val time = fullTime.replace(Regex("\\s*[AaPp][Mm]\\s*"), "").trim()
    val upper = fullTime.uppercase()
    val amPm = when {
        upper.contains("AM") -> "AM"
        upper.contains("PM") -> "PM"
        else -> null
    }
    val datePattern = prefs.dateFormat
    val date = if (datePattern == "system") DateFormat.getLongDateFormat(context).format(now)
        else SimpleDateFormat(datePattern, Locale.getDefault()).format(now)

    val alarm = (context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager)
        .nextAlarmClock?.let { DateFormat.getTimeFormat(context).format(Date(it.triggerTime)) }

    var battery = -1
    var charging = false
    context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let {
        val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        battery = if (level >= 0) (level * 100) / scale else -1
        val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }
    val weather = if (prefs.showWeather) WeatherHelper.read(context) else null
    return SsClock(time, amPm, date, alarm, battery, charging, weather)
}

private fun computeRows(
    context: Context,
    prefs: PrefsManager,
    directBadges: DirectBadgeHelper?,
): Pair<List<SsRow>, Int> {
    val merged = NotificationListener.getAllCounts().toMutableMap()
    directBadges?.getCounts()?.forEach { (pkg, count) -> merged[pkg] = (merged[pkg] ?: 0) + count }
    val excluded = prefs.getLockscreenExcludedApps()
    val counts = merged.filter { (pkg, count) -> count > 0 && pkg !in excluded }
    val times = NotificationListener.getLastPostTimes()
    val comparator = if (prefs.lockscreenWidgetLatestFirst) {
        compareByDescending<Map.Entry<String, Int>> { times[it.key] ?: 0L }
    } else {
        compareBy<Map.Entry<String, Int>> { times[it.key] ?: Long.MAX_VALUE }
    }
    val sizePx = (32 * context.resources.displayMetrics.density).toInt()
    val top = counts.entries.sortedWith(comparator.thenBy { it.key }).take(SCREENSAVER_ROWS)
    val rows = top.mapNotNull { e ->
        val label = ssLabel(context, prefs, e.key) ?: return@mapNotNull null
        SsRow(e.value, label, IconUtility.loadIcon(context, e.key, "", sizePx))
    }
    return rows to (counts.size - rows.size)
}

private fun ssLabel(context: Context, prefs: PrefsManager, pkg: String): String? {
    prefs.getAppRename(pkg)?.let { return it }
    return try {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Exception) {
        null
    }
}
