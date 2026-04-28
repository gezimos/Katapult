package com.gezimos.katapult.util

import android.content.Context
import android.content.SharedPreferences
import com.gezimos.katapult.model.AppModel
import org.json.JSONArray

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("katapult_prefs", Context.MODE_PRIVATE)

    fun saveAppOrder(packages: List<String>) {
        val json = JSONArray(packages)
        prefs.edit().putString(KEY_APP_ORDER, json.toString()).apply()
    }

    fun loadAppOrder(): List<String>? {
        val json = prefs.getString(KEY_APP_ORDER, null) ?: return null
        val array = JSONArray(json)
        return (0 until array.length()).map { array.getString(it) }
    }

    fun saveShortcut(slot: String, packageName: String, activityName: String) {
        prefs.edit()
            .putString("${slot}_package", packageName)
            .putString("${slot}_activity", activityName)
            .apply()
    }

    fun loadShortcut(slot: String): Pair<String, String>? {
        val pkg = prefs.getString("${slot}_package", null) ?: return null
        val activity = prefs.getString("${slot}_activity", null) ?: return null
        return Pair(pkg, activity)
    }

    fun reconcileOrder(savedOrder: List<String>, currentApps: List<AppModel>): List<AppModel> {
        val appMap = currentApps.associateBy { it.packageName }.toMutableMap()
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
        get() = prefs.getBoolean(KEY_NOTIFICATION_INDICATORS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_INDICATORS, value).apply()

    var showAmPm: Boolean
        get() = prefs.getBoolean(KEY_SHOW_AMPM, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_AMPM, value).apply()

    var showBattery: Boolean
        get() = prefs.getBoolean(KEY_SHOW_BATTERY, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_BATTERY, value).apply()

    var roundedIcons: Boolean
        get() = prefs.getBoolean(KEY_ROUNDED_ICONS, false)
        set(value) = prefs.edit().putBoolean(KEY_ROUNDED_ICONS, value).apply()

    var hideStatusBar: Boolean
        get() = prefs.getBoolean(KEY_HIDE_STATUS_BAR, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_STATUS_BAR, value).apply()

    var einkRefreshOnHome: Boolean
        get() = prefs.getBoolean(KEY_EINK_REFRESH_HOME, false)
        set(value) = prefs.edit().putBoolean(KEY_EINK_REFRESH_HOME, value).apply()

    var einkHelperMode: Int
        get() = prefs.getInt(KEY_EINK_HELPER_MODE, EinkHelper.MEINK_MODE_DISABLED)
        set(value) = prefs.edit().putInt(KEY_EINK_HELPER_MODE, value).apply()

    var doubleTapBrightness: Boolean
        get() = prefs.getBoolean(KEY_DOUBLE_TAP_BRIGHTNESS, false)
        set(value) = prefs.edit().putBoolean(KEY_DOUBLE_TAP_BRIGHTNESS, value).apply()

    var lastBrightness: Int
        get() = prefs.getInt(KEY_LAST_BRIGHTNESS, 128)
        set(value) = prefs.edit().putInt(KEY_LAST_BRIGHTNESS, value).apply()

    var infiniteScroll: Boolean
        get() = prefs.getBoolean(KEY_INFINITE_SCROLL, true)
        set(value) = prefs.edit().putBoolean(KEY_INFINITE_SCROLL, value).apply()

    var homeExtraRow: Boolean
        get() = prefs.getBoolean(KEY_HOME_EXTRA_ROW, false)
        set(value) = prefs.edit().putBoolean(KEY_HOME_EXTRA_ROW, value).apply()

    var showKatapultIcon: Boolean
        get() = prefs.getBoolean(KEY_SHOW_KATAPULT_ICON, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_KATAPULT_ICON, value).apply()

    var hideAppNames: Boolean
        get() = prefs.getBoolean(KEY_HIDE_APP_NAMES, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_APP_NAMES, value).apply()

    var hideArrowButtons: Boolean
        get() = prefs.getBoolean(KEY_HIDE_ARROW_BUTTONS, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_ARROW_BUTTONS, value).apply()

    var disableHomeEditing: Boolean
        get() = prefs.getBoolean(KEY_DISABLE_HOME_EDITING, false)
        set(value) = prefs.edit().putBoolean(KEY_DISABLE_HOME_EDITING, value).apply()

    var hideAllAppsButton: Boolean
        get() = prefs.getBoolean(KEY_HIDE_ALL_APPS_BUTTON, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_ALL_APPS_BUTTON, value).apply()

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()

    var wallpaperPath: String?
        get() = prefs.getString(KEY_WALLPAPER_PATH, null)
        set(value) = prefs.edit().putString(KEY_WALLPAPER_PATH, value).apply()

    // Hidden apps
    fun getHiddenApps(): Set<String> {
        return prefs.getStringSet(KEY_HIDDEN_APPS, emptySet()) ?: emptySet()
    }

    fun setHiddenApps(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_HIDDEN_APPS, packages).apply()
    }

    fun hideApp(packageName: String) {
        setHiddenApps(getHiddenApps() + packageName)
    }

    fun unhideApp(packageName: String) {
        setHiddenApps(getHiddenApps() - packageName)
    }

    // Icon overrides: value is "file:<absolute path>" or "res:<drawable name>", null if none.
    fun getIconOverride(packageName: String): String? {
        return prefs.getString("${KEY_ICON_OVERRIDE_PREFIX}$packageName", null)
    }

    fun setIconOverride(packageName: String, value: String) {
        prefs.edit().putString("${KEY_ICON_OVERRIDE_PREFIX}$packageName", value).apply()
    }

    fun clearIconOverride(packageName: String) {
        prefs.edit().remove("${KEY_ICON_OVERRIDE_PREFIX}$packageName").apply()
    }

    // Renamed apps
    fun getAppRename(packageName: String): String? {
        return prefs.getString("${KEY_RENAME_PREFIX}$packageName", null)
    }

    fun setAppRename(packageName: String, name: String?) {
        if (name.isNullOrBlank()) {
            prefs.edit().remove("${KEY_RENAME_PREFIX}$packageName").apply()
        } else {
            prefs.edit().putString("${KEY_RENAME_PREFIX}$packageName", name).apply()
        }
    }

    companion object {
        private const val KEY_APP_ORDER = "app_order"
        private const val KEY_NOTIFICATION_INDICATORS = "notification_indicators"
        private const val KEY_SHOW_AMPM = "show_ampm"
        private const val KEY_SHOW_BATTERY = "show_battery"
        private const val KEY_ROUNDED_ICONS = "rounded_icons"
        private const val KEY_HIDE_STATUS_BAR = "hide_status_bar"
        private const val KEY_EINK_REFRESH_HOME = "eink_refresh_home"
        private const val KEY_EINK_HELPER_MODE = "eink_helper_mode"
        private const val KEY_DOUBLE_TAP_BRIGHTNESS = "double_tap_brightness"
        private const val KEY_LAST_BRIGHTNESS = "last_brightness"
        private const val KEY_INFINITE_SCROLL = "infinite_scroll"
        private const val KEY_HOME_EXTRA_ROW = "home_extra_row"
        private const val KEY_WALLPAPER_PATH = "wallpaper_path"
        private const val KEY_HIDDEN_APPS = "hidden_apps"
        private const val KEY_RENAME_PREFIX = "rename_"
        private const val KEY_ICON_OVERRIDE_PREFIX = "icon_override_"
        private const val KEY_SHOW_KATAPULT_ICON = "show_katapult_icon"
        private const val KEY_HIDE_APP_NAMES = "hide_app_names"
        private const val KEY_HIDE_ARROW_BUTTONS = "hide_arrow_buttons"
        private const val KEY_DISABLE_HOME_EDITING = "disable_home_editing"
        private const val KEY_HIDE_ALL_APPS_BUTTON = "hide_all_apps_button"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }
}
