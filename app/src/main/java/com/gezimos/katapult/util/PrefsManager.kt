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

        // Insert newly installed apps in alphabetical position
        for (app in appMap.values.sortedBy { it.label.lowercase() }) {
            val label = app.label.lowercase()
            val insertIndex = result.indexOfFirst { it.label.lowercase() > label }
            if (insertIndex >= 0) {
                result.add(insertIndex, app)
            } else {
                result.add(app)
            }
        }

        return result
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
        private const val KEY_DOUBLE_TAP_BRIGHTNESS = "double_tap_brightness"
        private const val KEY_LAST_BRIGHTNESS = "last_brightness"
        private const val KEY_INFINITE_SCROLL = "infinite_scroll"
        private const val KEY_HOME_EXTRA_ROW = "home_extra_row"
        private const val KEY_WALLPAPER_PATH = "wallpaper_path"
        private const val KEY_HIDDEN_APPS = "hidden_apps"
        private const val KEY_RENAME_PREFIX = "rename_"
        private const val KEY_SHOW_KATAPULT_ICON = "show_katapult_icon"
        private const val KEY_HIDE_APP_NAMES = "hide_app_names"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }
}
