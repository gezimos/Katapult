package com.gezimos.katapult.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

object BitmapHelper {

    fun screenMaxSide(context: Context): Int {
        val m = context.resources.displayMetrics
        return maxOf(m.widthPixels, m.heightPixels).coerceAtLeast(1)
    }

    private fun sampleFor(srcW: Int, srcH: Int, maxSide: Int): Int {
        var sample = 1
        while (srcW / sample > maxSide || srcH / sample > maxSide) sample *= 2
        return sample
    }

    fun decodeFile(path: String, maxSide: Int): Bitmap? {
        return try {
            if (!File(path).exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) return null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleFor(srcW, srcH, maxSide)
            }
            BitmapFactory.decodeFile(path, opts)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeUri(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) return null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleFor(srcW, srcH, maxSide)
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (_: Exception) {
            null
        }
    }
}
