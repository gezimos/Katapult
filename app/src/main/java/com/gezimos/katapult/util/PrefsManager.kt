package com.gezimos.katapult.util

import android.content.Context
import android.content.SharedPreferences
import com.gezimos.katapult.model.AppModel
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("katapult_prefs", Context.MODE_PRIVATE)

    init { seedDefaultsOnce() }

    /**
     * Defaults that ship on from 1.5, applied to fresh installs only. Existing users keep
     * whatever they have: a pref reads its default whenever the key is absent, which is just
     * as true for an upgrader who never touched that setting, so the defaults themselves are
     * deliberately left alone and explicit values are written instead. Empty prefs mean a
     * first launch rather than an upgrade, since a user who finished onboarding always has
     * onboarding_complete and notification_indicators written. Clear All Data wipes the file,
     * so it re-seeds, which is the intended behaviour.
     */
    private fun seedDefaultsOnce() {
        if (prefs.contains(KEY_DEFAULTS_SEEDED)) return
        val editor = prefs.edit()
        if (prefs.all.isEmpty()) {
            editor.putBoolean(KEY_HOME_EXTRA_ROW, true)
            editor.putBoolean(KEY_VERTICAL_APP_GESTURES, true)
            editor.putBoolean(KEY_HIDE_ARROW_BUTTONS, true)
            editor.putBoolean(KEY_INFINITE_SCROLL, false)
            editor.putInt(KEY_ICON_SIZE, ICON_SIZE_LARGE)
        }
        editor.putBoolean(KEY_DEFAULTS_SEEDED, true).apply()
    }

    private fun boolPref(key: String, def: Boolean): Boolean = try {
        prefs.getBoolean(key, def)
    } catch (_: ClassCastException) {
        def
    }

    private fun intPref(key: String, def: Int): Int = try {
        prefs.getInt(key, def)
    } catch (_: ClassCastException) {
        def
    }

    private fun strPref(key: String, def: String?): String? = try {
        prefs.getString(key, def)
    } catch (_: ClassCastException) {
        def
    }

    private fun setPref(key: String, def: Set<String>): Set<String>? = try {
        prefs.getStringSet(key, def)
    } catch (_: ClassCastException) {
        def
    }

    fun saveAppOrder(packages: List<String>) {
        val json = JSONArray(packages)
        prefs.edit().putString(KEY_APP_ORDER, json.toString()).apply()
    }

    fun loadAppOrder(): List<String>? {
        val json = strPref(KEY_APP_ORDER, null) ?: return null
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { array.optString(it).ifBlank { null } }
        } catch (_: Exception) {
            null
        }
    }

    fun removeFromAppOrder(key: String) {
        val order = loadAppOrder() ?: return
        if (key in order) saveAppOrder(order.filterNot { it == key })
    }

    fun saveShortcut(slot: String, packageName: String, activityName: String, shortcutId: String = "") {
        val editor = prefs.edit()
            .putString("${slot}_package", packageName)
            .putString("${slot}_activity", activityName)
        if (shortcutId.isEmpty()) editor.remove("${slot}_shortcut_id")
        else editor.putString("${slot}_shortcut_id", shortcutId)
        editor.apply()
    }

    fun loadShortcut(slot: String): Pair<String, String>? {
        val pkg = strPref("${slot}_package", null) ?: return null
        val activity = strPref("${slot}_activity", null) ?: return null
        return Pair(pkg, activity)
    }

    fun loadSlotShortcutId(slot: String): String? =
        strPref("${slot}_shortcut_id", null)?.ifEmpty { null }

    fun clearSlotsForShortcut(packageName: String, shortcutId: String) {
        val editor = prefs.edit()
        var changed = false
        for (slot in HOME_SLOTS) {
            if (loadSlotShortcutId(slot) != shortcutId) continue
            if (strPref("${slot}_package", null) != packageName) continue
            editor.remove("${slot}_package")
                .remove("${slot}_activity")
                .remove("${slot}_shortcut_id")
            changed = true
        }
        if (changed) editor.apply()
    }

    fun getSavedShortcuts(): List<AppModel> {
        val json = strPref(KEY_PINNED_SHORTCUTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val pkg = obj.optString("package").ifEmpty { return@mapNotNull null }
                val id = obj.optString("id").ifEmpty { return@mapNotNull null }
                AppModel(
                    packageName = pkg,
                    label = obj.optString("label").ifEmpty { id },
                    activityName = "",
                    shortcutId = id,
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setSavedShortcuts(shortcuts: List<AppModel>) {
        val array = JSONArray()
        for (s in shortcuts) {
            array.put(
                JSONObject()
                    .put("package", s.packageName)
                    .put("id", s.shortcutId)
                    .put("label", s.label),
            )
        }
        prefs.edit().putString(KEY_PINNED_SHORTCUTS, array.toString()).apply()
    }

    fun addSavedShortcut(packageName: String, shortcutId: String, label: String): Boolean {
        val current = getSavedShortcuts()
        if (current.any { it.packageName == packageName && it.shortcutId == shortcutId }) return false
        setSavedShortcuts(current + AppModel(packageName, label, "", shortcutId))
        return true
    }

    fun removeSavedShortcut(packageName: String, shortcutId: String) {
        setSavedShortcuts(
            getSavedShortcuts().filterNot {
                it.packageName == packageName && it.shortcutId == shortcutId
            },
        )
    }

    fun reconcileOrder(savedOrder: List<String>, currentApps: List<AppModel>): List<AppModel> {
        val appMap = currentApps.associateBy { it.key }.toMutableMap()
        val result = mutableListOf<AppModel>()

        for (pkg in savedOrder) {
            val app = appMap.remove(pkg)
            if (app != null) {
                result.add(app)
            }
        }

        val newApps = appMap.values.sortedBy { it.label.lowercase() }
        for (app in newApps) {
            val label = app.label.lowercase()
            val anchors = longestNonDecreasingIndices(result)
            val insertIndex = when {
                anchors.isEmpty() -> 0
                else -> {
                    val idx = anchors.indexOfFirst { result[it].label.lowercase() > label }
                    if (idx < 0) anchors.last() + 1 else anchors[idx]
                }
            }
            result.add(insertIndex, app)
        }

        return result
    }

    private fun longestNonDecreasingIndices(list: List<AppModel>): List<Int> {
        val n = list.size
        if (n == 0) return emptyList()
        val labels = List(n) { list[it].label.lowercase() }
        val tails = IntArray(n)
        val prev = IntArray(n) { -1 }
        var length = 0
        for (i in 0 until n) {
            val l = labels[i]
            var lo = 0
            var hi = length
            while (lo < hi) {
                val mid = (lo + hi) ushr 1
                if (labels[tails[mid]] > l) hi = mid else lo = mid + 1
            }
            if (lo > 0) prev[i] = tails[lo - 1]
            tails[lo] = i
            if (lo == length) length++
        }
        val out = ArrayDeque<Int>()
        var k = tails[length - 1]
        while (k != -1) {
            out.addFirst(k)
            k = prev[k]
        }
        return out.toList()
    }

    var notificationIndicators: Boolean
        get() = boolPref(KEY_NOTIFICATION_INDICATORS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_INDICATORS, value).apply()

    var clockFormat: String
        get() = strPref(KEY_CLOCK_FORMAT, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_CLOCK_FORMAT, value).apply()

    var dateFormat: String
        get() = strPref(KEY_DATE_FORMAT, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_DATE_FORMAT, value).apply()

    var showBattery: Boolean
        get() = boolPref(KEY_SHOW_BATTERY, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_BATTERY, value).apply()

    var showAlarm: Boolean
        get() = boolPref(KEY_SHOW_ALARM, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_ALARM, value).apply()

    var showWeather: Boolean
        get() = boolPref(KEY_SHOW_WEATHER, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_WEATHER, value).apply()

    var roundedIcons: Boolean
        get() = boolPref(KEY_ROUNDED_ICONS, false)
        set(value) = prefs.edit().putBoolean(KEY_ROUNDED_ICONS, value).apply()

    var darkMode: Boolean
        get() = boolPref(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var hideStatusBar: Boolean
        get() = boolPref(KEY_HIDE_STATUS_BAR, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_STATUS_BAR, value).apply()

    var hideStatusBarClock: Boolean
        get() = boolPref(KEY_HIDE_STATUS_BAR_CLOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_STATUS_BAR_CLOCK, value).apply()

    var einkRefreshOnHome: Boolean
        get() = boolPref(KEY_EINK_REFRESH_HOME, false)
        set(value) = prefs.edit().putBoolean(KEY_EINK_REFRESH_HOME, value).apply()

    var einkHelperMode: Int
        get() = intPref(KEY_EINK_HELPER_MODE, EinkHelper.MEINK_MODE_DISABLED)
        set(value) = prefs.edit().putInt(KEY_EINK_HELPER_MODE, value).apply()

    var doubleTapAction: Int
        get() = intPref(
            KEY_DOUBLE_TAP_ACTION,
            if (boolPref(KEY_DOUBLE_TAP_BRIGHTNESS, false)) DOUBLE_TAP_BRIGHTNESS else DOUBLE_TAP_OFF,
        )
        set(value) = prefs.edit().putInt(KEY_DOUBLE_TAP_ACTION, value).apply()

    var lastBrightness: Int
        get() = intPref(KEY_LAST_BRIGHTNESS, 128)
        set(value) = prefs.edit().putInt(KEY_LAST_BRIGHTNESS, value).apply()

    var infiniteScroll: Boolean
        get() = boolPref(KEY_INFINITE_SCROLL, true)
        set(value) = prefs.edit().putBoolean(KEY_INFINITE_SCROLL, value).apply()

    var homeExtraRow: Boolean
        get() = boolPref(KEY_HOME_EXTRA_ROW, false)
        set(value) = prefs.edit().putBoolean(KEY_HOME_EXTRA_ROW, value).apply()

    var disableMusicWidget: Boolean
        get() = boolPref(KEY_DISABLE_MUSIC_WIDGET, false)
        set(value) = prefs.edit().putBoolean(KEY_DISABLE_MUSIC_WIDGET, value).apply()

    var showKatapultIcon: Boolean
        get() = boolPref(KEY_SHOW_KATAPULT_ICON, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_KATAPULT_ICON, value).apply()

    var hideAppNames: Boolean
        get() = boolPref(KEY_HIDE_APP_NAMES, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_APP_NAMES, value).apply()

    var hideArrowButtons: Boolean
        get() = boolPref(KEY_HIDE_ARROW_BUTTONS, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_ARROW_BUTTONS, value).apply()

    var disableHomeEditing: Boolean
        get() = boolPref(KEY_DISABLE_HOME_EDITING, false)
        set(value) = prefs.edit().putBoolean(KEY_DISABLE_HOME_EDITING, value).apply()

    var hideAllAppsButton: Boolean
        get() = boolPref(KEY_HIDE_ALL_APPS_BUTTON, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_ALL_APPS_BUTTON, value).apply()

    var swipeUpAllApps: Boolean
        get() = boolPref(KEY_SWIPE_UP_ALL_APPS, true)
        set(value) = prefs.edit().putBoolean(KEY_SWIPE_UP_ALL_APPS, value).apply()

    var homeIslands: Boolean
        get() = boolPref(KEY_HOME_ISLANDS, false)
        set(value) = prefs.edit().putBoolean(KEY_HOME_ISLANDS, value).apply()

    var verticalAppGestures: Boolean
        get() = boolPref(KEY_VERTICAL_APP_GESTURES, false)
        set(value) = prefs.edit().putBoolean(KEY_VERTICAL_APP_GESTURES, value).apply()

    var lockscreenWidget: Boolean
        get() = boolPref(KEY_LOCKSCREEN_WIDGET, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCKSCREEN_WIDGET, value).apply()

    var iconSize: Int
        get() = intPref(KEY_ICON_SIZE, ICON_SIZE_SMALL)
        set(value) = prefs.edit().putInt(KEY_ICON_SIZE, value).apply()

    var lockscreenMusicWidget: Boolean
        get() = boolPref(KEY_LOCKSCREEN_MUSIC_WIDGET, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCKSCREEN_MUSIC_WIDGET, value).apply()

    var lockscreenWidgetY: Int
        get() = intPref(KEY_LOCKSCREEN_WIDGET_Y, -1)
        set(value) = prefs.edit().putInt(KEY_LOCKSCREEN_WIDGET_Y, value).apply()

    var lockscreenWidgetRows: Int
        get() = intPref(KEY_LOCKSCREEN_WIDGET_ROWS, 4)
        set(value) = prefs.edit().putInt(KEY_LOCKSCREEN_WIDGET_ROWS, value).apply()

    var lockscreenWidgetLatestFirst: Boolean
        get() = boolPref(KEY_LOCKSCREEN_WIDGET_LATEST_FIRST, true)
        set(value) = prefs.edit().putBoolean(KEY_LOCKSCREEN_WIDGET_LATEST_FIRST, value).apply()

    var screensaverEnabled: Boolean
        get() = boolPref(KEY_SCREENSAVER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREENSAVER_ENABLED, value).apply()

    var screensaverIslands: Boolean
        get() = boolPref(KEY_SCREENSAVER_ISLANDS, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREENSAVER_ISLANDS, value).apply()

    var screensaverWallpaper: Boolean
        get() = boolPref(KEY_SCREENSAVER_WALLPAPER, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREENSAVER_WALLPAPER, value).apply()

    var screensaverWallpaperPath: String?
        get() = strPref(KEY_SCREENSAVER_WALLPAPER_PATH, null)
        set(value) = prefs.edit().putString(KEY_SCREENSAVER_WALLPAPER_PATH, value).apply()

    var screensaverShowClock: Boolean
        get() = boolPref(KEY_SCREENSAVER_SHOW_CLOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_SCREENSAVER_SHOW_CLOCK, value).apply()

    var screensaverShowNotifications: Boolean
        get() = boolPref(KEY_SCREENSAVER_SHOW_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_SCREENSAVER_SHOW_NOTIFICATIONS, value).apply()

    var screensaverDoubleTapBrightness: Boolean
        get() = boolPref(KEY_SCREENSAVER_DOUBLE_TAP_BRIGHTNESS, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREENSAVER_DOUBLE_TAP_BRIGHTNESS, value).apply()

    var screensaverUpdateMode: Int
        get() = intPref(KEY_SCREENSAVER_UPDATE_MODE, SCREENSAVER_MODE_AUTO)
        set(value) = prefs.edit().putInt(KEY_SCREENSAVER_UPDATE_MODE, value).apply()

    var screensaverUpdateMinutes: Int
        get() = intPref(KEY_SCREENSAVER_UPDATE_MINUTES, 5)
        set(value) = prefs.edit().putInt(KEY_SCREENSAVER_UPDATE_MINUTES, value).apply()

    var screensaverEinkRefresh: Boolean
        get() = boolPref(KEY_SCREENSAVER_EINK_REFRESH, true)
        set(value) = prefs.edit().putBoolean(KEY_SCREENSAVER_EINK_REFRESH, value).apply()

    var screensaverOnPower: Boolean
        get() = boolPref(KEY_SCREENSAVER_ON_POWER, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREENSAVER_ON_POWER, value).apply()

    fun getLockscreenExcludedApps(): Set<String> =
        setPref(KEY_LOCKSCREEN_WIDGET_EXCLUDED, emptySet()) ?: emptySet()

    fun setLockscreenExcludedApps(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_LOCKSCREEN_WIDGET_EXCLUDED, packages).apply()
    }

    var onboardingComplete: Boolean
        get() = boolPref(KEY_ONBOARDING_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()

    var wallpaperPath: String?
        get() = strPref(KEY_WALLPAPER_PATH, null)
        set(value) = prefs.edit().putString(KEY_WALLPAPER_PATH, value).apply()

    fun getHiddenApps(): Set<String> {
        return setPref(KEY_HIDDEN_APPS, emptySet()) ?: emptySet()
    }

    fun setHiddenApps(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_HIDDEN_APPS, packages).apply()
    }

    fun hideApp(key: String) {
        setHiddenApps(getHiddenApps() + key)
    }

    fun unhideApp(key: String) {
        setHiddenApps(getHiddenApps() - key)
    }

    fun getIconOverride(key: String): String? {
        return strPref("${KEY_ICON_OVERRIDE_PREFIX}$key", null)
    }

    fun setIconOverride(key: String, value: String) {
        prefs.edit().putString("${KEY_ICON_OVERRIDE_PREFIX}$key", value).apply()
    }

    fun clearIconOverride(key: String) {
        prefs.edit().remove("${KEY_ICON_OVERRIDE_PREFIX}$key").apply()
    }

    fun getAppRename(packageName: String): String? {
        return strPref("${KEY_RENAME_PREFIX}$packageName", null)
    }

    fun setAppRename(packageName: String, name: String?) {
        if (name.isNullOrBlank()) {
            prefs.edit().remove("${KEY_RENAME_PREFIX}$packageName").apply()
        } else {
            prefs.edit().putString("${KEY_RENAME_PREFIX}$packageName", name).apply()
        }
    }

    fun exportToJson(context: Context): String {
        val json = JSONObject()
        json.put(CONFIG_TYPE_KEY, CONFIG_TYPE)
        json.put(CONFIG_VERSION_KEY, appVersionCode(context))
        for ((key, value) in prefs.all) {
            when (value) {
                is Boolean -> json.put(key, value)
                is Int -> json.put(key, value)
                is Long -> json.put(key, value)
                is Float -> json.put(key, value.toDouble())
                is String -> json.put(key, value)
                is Set<*> -> json.put(key, JSONArray(value.filterIsInstance<String>()))
                else -> {}
            }
        }
        return json.toString(2)
    }

    private fun isDeadFileRef(key: String, value: String): Boolean {
        val path = when {
            key == KEY_WALLPAPER_PATH || key == KEY_SCREENSAVER_WALLPAPER_PATH -> value
            key.startsWith(KEY_ICON_OVERRIDE_PREFIX) && value.startsWith("file:") ->
                value.removePrefix("file:")
            else -> return false
        }
        return !java.io.File(path).exists()
    }

    fun importFromJson(text: String): Boolean {
        val json = try {
            JSONObject(text)
        } catch (_: Exception) {
            return false
        }
        if (json.optString(CONFIG_TYPE_KEY) != CONFIG_TYPE) return false
        val existing = prefs.all
        val editor = prefs.edit().clear()
        for (key in json.keys()) {
            if (key == CONFIG_TYPE_KEY || key == CONFIG_VERSION_KEY) continue
            val value = json.opt(key)
            if (!matchesStoredType(existing[key], value)) continue
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Number -> editor.putInt(key, value.toInt())
                is String -> if (isUsableString(key, value) && !isDeadFileRef(key, value)) {
                    editor.putString(key, value)
                }
                is JSONArray -> editor.putStringSet(
                    key,
                    (0 until value.length()).mapNotNull { value.optString(it).ifBlank { null } }.toSet(),
                )
            }
        }
        return editor.commit()
    }

    private fun matchesStoredType(stored: Any?, incoming: Any?): Boolean = when (stored) {
        null -> true
        is Boolean -> incoming is Boolean
        is Int, is Long, is Float -> incoming is Number
        is String -> incoming is String
        is Set<*> -> incoming is JSONArray
        else -> true
    }

    private fun isUsableString(key: String, value: String): Boolean = when (key) {
        KEY_CLOCK_FORMAT, KEY_DATE_FORMAT -> value == "system" || runCatching {
            SimpleDateFormat(value, Locale.getDefault())
        }.isSuccess
        KEY_APP_ORDER -> runCatching { JSONArray(value) }.isSuccess
        else -> true
    }

    private fun appVersionCode(context: Context): Long = try {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
    } catch (_: Exception) {
        0L
    }

    fun clearAll(context: Context) {
        prefs.edit().clear().commit()
        context.filesDir.listFiles()?.forEach { file ->
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        }
    }

    companion object {
        private const val CONFIG_TYPE_KEY = "_type"
        private const val CONFIG_VERSION_KEY = "_version"
        private const val CONFIG_TYPE = "katapult_config"

        val HOME_SLOTS = listOf(
            "clock", "calendar", "phone", "center", "sms",
            "extra_left", "extra_center", "extra_right",
        )

        private const val KEY_DEFAULTS_SEEDED = "defaults_seeded"
        private const val KEY_ICON_SIZE = "icon_size"

        const val ICON_SIZE_SMALL = 72
        const val ICON_SIZE_LARGE = 80
        private const val KEY_APP_ORDER = "app_order"
        private const val KEY_PINNED_SHORTCUTS = "pinned_shortcuts"
        private const val KEY_NOTIFICATION_INDICATORS = "notification_indicators"
        private const val KEY_CLOCK_FORMAT = "clock_format"
        private const val KEY_DATE_FORMAT = "date_format"
        private const val KEY_SHOW_BATTERY = "show_battery"
        private const val KEY_SHOW_ALARM = "show_alarm"
        private const val KEY_SHOW_WEATHER = "show_weather"
        private const val KEY_ROUNDED_ICONS = "rounded_icons"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_HIDE_STATUS_BAR = "hide_status_bar"
        private const val KEY_HIDE_STATUS_BAR_CLOCK = "hide_status_bar_clock"
        private const val KEY_EINK_REFRESH_HOME = "eink_refresh_home"
        private const val KEY_EINK_HELPER_MODE = "eink_helper_mode"
        private const val KEY_DOUBLE_TAP_BRIGHTNESS = "double_tap_brightness"
        private const val KEY_DOUBLE_TAP_ACTION = "double_tap_action"
        private const val KEY_LAST_BRIGHTNESS = "last_brightness"
        private const val KEY_INFINITE_SCROLL = "infinite_scroll"
        private const val KEY_HOME_EXTRA_ROW = "home_extra_row"
        private const val KEY_DISABLE_MUSIC_WIDGET = "disable_music_widget"
        private const val KEY_WALLPAPER_PATH = "wallpaper_path"
        private const val KEY_HIDDEN_APPS = "hidden_apps"
        private const val KEY_RENAME_PREFIX = "rename_"
        private const val KEY_ICON_OVERRIDE_PREFIX = "icon_override_"
        private const val KEY_SHOW_KATAPULT_ICON = "show_katapult_icon"
        private const val KEY_HIDE_APP_NAMES = "hide_app_names"
        private const val KEY_HIDE_ARROW_BUTTONS = "hide_arrow_buttons"
        private const val KEY_DISABLE_HOME_EDITING = "disable_home_editing"
        private const val KEY_HIDE_ALL_APPS_BUTTON = "hide_all_apps_button"
        private const val KEY_SWIPE_UP_ALL_APPS = "swipe_up_all_apps"
        private const val KEY_HOME_ISLANDS = "home_islands"
        private const val KEY_VERTICAL_APP_GESTURES = "vertical_app_gestures"
        private const val KEY_LOCKSCREEN_WIDGET = "lockscreen_widget"
        private const val KEY_LOCKSCREEN_MUSIC_WIDGET = "lockscreen_music_widget"
        private const val KEY_LOCKSCREEN_WIDGET_Y = "lockscreen_widget_y"
        private const val KEY_LOCKSCREEN_WIDGET_ROWS = "lockscreen_widget_rows"
        private const val KEY_LOCKSCREEN_WIDGET_EXCLUDED = "lockscreen_widget_excluded"
        private const val KEY_LOCKSCREEN_WIDGET_LATEST_FIRST = "lockscreen_widget_latest_first"
        private const val KEY_SCREENSAVER_ENABLED = "screensaver_enabled"
        private const val KEY_SCREENSAVER_ISLANDS = "screensaver_islands"
        private const val KEY_SCREENSAVER_WALLPAPER = "screensaver_wallpaper"
        private const val KEY_SCREENSAVER_WALLPAPER_PATH = "screensaver_wallpaper_path"
        private const val KEY_SCREENSAVER_SHOW_CLOCK = "screensaver_show_clock"
        private const val KEY_SCREENSAVER_SHOW_NOTIFICATIONS = "screensaver_show_notifications"
        private const val KEY_SCREENSAVER_DOUBLE_TAP_BRIGHTNESS = "screensaver_double_tap_brightness"
        private const val KEY_SCREENSAVER_UPDATE_MODE = "screensaver_update_mode"
        private const val KEY_SCREENSAVER_UPDATE_MINUTES = "screensaver_update_minutes"
        private const val KEY_SCREENSAVER_EINK_REFRESH = "screensaver_eink_refresh"
        private const val KEY_SCREENSAVER_ON_POWER = "screensaver_on_power"

        const val SCREENSAVER_MODE_AUTO = 0
        const val SCREENSAVER_MODE_INTERVAL = 1
        const val SCREENSAVER_MODE_STATIC = 2

        const val DOUBLE_TAP_OFF = 0
        const val DOUBLE_TAP_BRIGHTNESS = 1
        const val DOUBLE_TAP_LOCK = 2
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }
}
