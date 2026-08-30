package com.gezimos.katapult.util

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri

object WeatherHelper {

    data class Weather(
        val location: String?,
        val temperature: Int,
        val low: Int?,
        val high: Int?,
        val conditionId: Int,
        val iconInfo: String?,
        val unit: String,
    )

    const val PACKAGE = "com.mudita.weather"

    private val WidgetUri: Uri = Uri.parse("content://$PACKAGE/widget_weather")

    fun openApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun read(context: Context): Weather? = try {
        context.contentResolver.query(WidgetUri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else if (intOrNull(cursor, "is_error") != 0) {
                null
            } else {
                val temperature = intOrNull(cursor, "temperature")
                if (temperature == null) {
                    null
                } else {
                    Weather(
                        location = strOrNull(cursor, "location_name"),
                        temperature = temperature,
                        low = intOrNull(cursor, "lowest_temperature"),
                        high = intOrNull(cursor, "highest_temperature"),
                        conditionId = intOrNull(cursor, "weather_icon_id") ?: 0,
                        iconInfo = strOrNull(cursor, "weather_icon_info"),
                        unit = strOrNull(cursor, "measurement_unit").orEmpty(),
                    )
                }
            }
        }
    } catch (_: Exception) {
        null
    }

    private fun intOrNull(cursor: Cursor, name: String): Int? {
        val index = cursor.getColumnIndex(name)
        return if (index < 0 || cursor.isNull(index)) null else cursor.getInt(index)
    }

    private fun strOrNull(cursor: Cursor, name: String): String? {
        val index = cursor.getColumnIndex(name)
        return if (index < 0 || cursor.isNull(index)) null else cursor.getString(index)
    }
}
