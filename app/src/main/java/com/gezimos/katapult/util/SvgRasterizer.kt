package com.gezimos.katapult.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Xml
import androidx.core.graphics.PathParser
import androidx.core.graphics.createBitmap
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import kotlin.math.max

object SvgRasterizer {

    fun rasterize(input: InputStream, sizePx: Int): Bitmap? {
        if (sizePx <= 0) return null

        var viewBoxX = 0f
        var viewBoxY = 0f
        var viewBoxW = 24f
        var viewBoxH = 24f
        val paths = mutableListOf<android.graphics.Path>()

        try {
            val parser = Xml.newPullParser()
            parser.setInput(input, null)

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "svg" -> {
                            val vb = parser.getAttributeValue(null, "viewBox")
                            if (vb != null) {
                                val parts = vb.trim().split(Regex("[\\s,]+"))
                                if (parts.size >= 4) {
                                    viewBoxX = parts[0].toFloatOrNull() ?: 0f
                                    viewBoxY = parts[1].toFloatOrNull() ?: 0f
                                    viewBoxW = parts[2].toFloatOrNull() ?: 24f
                                    viewBoxH = parts[3].toFloatOrNull() ?: 24f
                                }
                            } else {
                                parser.getAttributeValue(null, "width")?.let {
                                    stripUnit(it)?.let { v -> viewBoxW = v }
                                }
                                parser.getAttributeValue(null, "height")?.let {
                                    stripUnit(it)?.let { v -> viewBoxH = v }
                                }
                            }
                        }
                        "path" -> {
                            val d = parser.getAttributeValue(null, "d")
                            if (!d.isNullOrBlank()) {
                                try {
                                    paths.add(PathParser.createPathFromPathData(d))
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
            return null
        }

        if (paths.isEmpty()) return null

        val bitmap = createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Render at 60% of canvas, centered.
        val targetSpan = sizePx * 0.6f
        val scale = targetSpan / max(viewBoxW, viewBoxH)
        val drawnW = viewBoxW * scale
        val drawnH = viewBoxH * scale
        val offsetX = (sizePx - drawnW) / 2f
        val offsetY = (sizePx - drawnH) / 2f

        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        canvas.translate(-viewBoxX, -viewBoxY)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.BLACK
        }
        for (path in paths) {
            canvas.drawPath(path, paint)
        }
        return bitmap
    }

    private fun stripUnit(raw: String): Float? {
        val digits = raw.trim().takeWhile { it.isDigit() || it == '.' || it == '-' }
        return digits.toFloatOrNull()
    }
}
