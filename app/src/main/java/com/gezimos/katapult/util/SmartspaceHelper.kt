package com.gezimos.katapult.util

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import android.widget.TextView

object SmartspaceHelper {

    private const val PACKAGE = "com.google.android.googlequicksearchbox"
    private const val CLASS_HINT = "SmartspaceWidgetProvider"
    private const val HOST_ID = 4711
    private const val KEY_WIDGET_ID = "smartspace_widget_id"

    private val TEMPERATURE = Regex("(-?\\d{1,3})\\s*°\\s*([CF](?![A-Za-z]))?")

    private const val HEIGHT_DP = 100

    private var host: AppWidgetHost? = null
    private var lastView: AppWidgetHostView? = null
    private var listening = false

    var latest: WeatherHelper.Weather? = null
        private set

    var onWeather: ((WeatherHelper.Weather) -> Unit)? = null

    private fun host(context: Context): AppWidgetHost =
        host ?: ReaderHost(context.applicationContext).also { host = it }

    private fun prefs(context: Context) =
        context.getSharedPreferences("katapult_prefs", Context.MODE_PRIVATE)

    fun provider(context: Context): AppWidgetProviderInfo? = try {
        AppWidgetManager.getInstance(context).installedProviders.firstOrNull {
            it.provider.packageName == PACKAGE && it.provider.className.contains(CLASS_HINT)
        }
    } catch (_: Exception) {
        null
    }

    fun isAvailable(context: Context): Boolean = provider(context) != null

    private fun boundId(context: Context): Int {
        val info = provider(context) ?: return -1
        val id = prefs(context).getInt(KEY_WIDGET_ID, -1)
        if (id == -1) return -1
        val bound = try {
            AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider
        } catch (_: Exception) {
            null
        }
        return if (bound == info.provider) id else -1
    }

    fun bindIntent(context: Context): Intent? {
        val info = provider(context) ?: return null
        if (boundId(context) != -1) return null
        val stale = prefs(context).getInt(KEY_WIDGET_ID, -1)
        if (stale != -1) {
            try { host(context).deleteAppWidgetId(stale) } catch (_: Exception) {}
        }
        val id = try { host(context).allocateAppWidgetId() } catch (_: Exception) { return null }
        prefs(context).edit().putInt(KEY_WIDGET_ID, id).apply()
        val bound = try {
            AppWidgetManager.getInstance(context).bindAppWidgetIdIfAllowed(id, info.provider)
        } catch (_: Exception) {
            false
        }
        if (bound) return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
    }

    fun start(context: Context) {
        if (listening) return
        val id = boundId(context)
        if (id == -1) return
        try {
            val appContext = context.applicationContext
            val widgetHost = host(appContext)
            widgetHost.startListening()
            listening = true
            val info = AppWidgetManager.getInstance(appContext).getAppWidgetInfo(id) ?: return
            val view = widgetHost.createView(appContext, id, info)
            lastView = view
            reportSize(appContext, view)
            (view as? ReaderView)?.onUpdate = { parse(it) }
            parse(view)
        } catch (_: Exception) {
        }
    }

    private fun reportSize(context: Context, view: AppWidgetHostView) {
        val metrics = context.resources.displayMetrics
        val widthDp = metrics.widthPixels / metrics.density
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                view.updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp, HEIGHT_DP.toFloat())))
            } else {
                @Suppress("DEPRECATION")
                view.updateAppWidgetSize(Bundle(), widthDp.toInt(), HEIGHT_DP, widthDp.toInt(), HEIGHT_DP)
            }
        } catch (_: Exception) {
        }
    }

    private fun parse(view: AppWidgetHostView) {
        try {
            val metrics = view.context.resources.displayMetrics
            view.measure(
                View.MeasureSpec.makeMeasureSpec(metrics.widthPixels.coerceAtLeast(1), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec((HEIGHT_DP * metrics.density).toInt(), View.MeasureSpec.AT_MOST),
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            val texts = mutableListOf<String>()
            collectTexts(view, texts)
            val weather = weatherFrom(texts) ?: return
            latest = weather
            onWeather?.invoke(weather)
        } catch (_: Exception) {
        }
    }

    internal fun weatherFrom(texts: List<String>): WeatherHelper.Weather? {
        val match = texts.asReversed().firstNotNullOfOrNull { TEMPERATURE.find(it) } ?: return null
        return WeatherHelper.Weather(
            location = null,
            temperature = match.groupValues[1].toInt(),
            low = null,
            high = null,
            conditionId = -1,
            iconInfo = null,
            unit = match.groupValues[2],
        )
    }

    fun click(): Boolean {
        val target = lastView?.let { findWeatherTarget(it) } ?: return false
        return try { target.performClick() } catch (_: Exception) { false }
    }

    private fun findWeatherTarget(view: View): View? {
        if (view is TextView && view.text?.contains('°') == true) {
            var current: View? = view
            while (current != null) {
                if (current.hasOnClickListeners()) return current
                current = current.parent as? View
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findWeatherTarget(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private class ReaderHost(context: Context) : AppWidgetHost(context, HOST_ID) {
        override fun onCreateView(
            context: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?,
        ): AppWidgetHostView = ReaderView(context)
    }

    private class ReaderView(context: Context) : AppWidgetHostView(context) {
        var onUpdate: ((AppWidgetHostView) -> Unit)? = null

        override fun updateAppWidget(remoteViews: RemoteViews?) {
            super.updateAppWidget(remoteViews)
            onUpdate?.invoke(this)
        }
    }

    private fun collectTexts(view: View, out: MutableList<String>) {
        if (view is TextView) {
            view.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { out.add(it) }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) collectTexts(view.getChildAt(i), out)
        }
    }
}
