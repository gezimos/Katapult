package com.gezimos.katapult.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.LruCache
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.gezimos.katapult.R

object IconUtility {

    private val bitmapCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(
        maxOf(
            (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt(),
            50 * 128 * 128 * 4 / 1024
        )
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    private val customIcons = mapOf(
        "org.thoughtcrime.securesms" to R.drawable.signal,
        "org.telegram.messenger" to R.drawable.telegram,
        "com.viber.voip" to R.drawable.viber,
        "com.whatsapp" to R.drawable.whatsapp,
        "com.beeper.android" to R.drawable.beeper,
        "com.aurora.store" to R.drawable.aurora,
        "org.fdroid.fdroid" to R.drawable.f_droid,
        "com.spotify.music" to R.drawable.spotify,
        "com.android.gallery3d" to R.drawable.gallery,
        "org.futo.inputmethod.latin" to R.drawable.futo,
        "com.android.deskclock" to R.drawable.clock,
        "com.android.fmradio" to R.drawable.radio,
        "com.android.stk" to R.drawable.sim,
        "org.mozilla.firefox" to R.drawable.firefox,
        "org.mozilla.firefox_beta" to R.drawable.firefox,
        "org.mozilla.focus" to R.drawable.firefox,
        "com.fsck.k9" to R.drawable.mail,
        "net.thunderbird.android" to R.drawable.mail,
        "ch.protonmail.android" to R.drawable.mail,
        "com.android.documentsui" to R.drawable.files,
        "de.danoeh.antennapod" to R.drawable.ap,
        "dev.octoshrimpy.quik" to R.drawable.sms,
        "org.schabi.newpipe" to R.drawable.newpipe,
        "org.fossify.musicplayer" to R.drawable.music,
        "org.oxycblt.auxio" to R.drawable.music,
        "com.foobar2000.foobar2000" to R.drawable.music,
        "org.videolan.vlc" to R.drawable.music,
        "com.android.settings" to R.drawable.settings,
        "org.chromium.webview_shell" to R.drawable.chromium,
        "com.brave.browser" to R.drawable.brave,
        "com.zsemberi.killapps" to R.drawable.killapps,
        "org.koreader.launcher" to R.drawable.koreader,
        "org.koreader.launcher.fdroid" to R.drawable.koreader,
    )

    fun loadIcon(context: Context, packageName: String, activityClass: String, sizePx: Int): Bitmap? {
        if (packageName.isBlank() || sizePx <= 0) return null

        val cacheKey = "$packageName:$activityClass:$sizePx"
        bitmapCache.get(cacheKey)?.let { return it }

        val bitmap = try {
            val customRes = customIcons[packageName]
            if (customRes != null) {
                val drawable = ContextCompat.getDrawable(context, customRes)!!
                drawableToBitmap(drawable, sizePx)
            } else {
                val appIcon = context.packageManager.getApplicationIcon(packageName)
                if (appIcon is AdaptiveIconDrawable) {
                    renderAdaptiveIcon(appIcon, sizePx)
                } else {
                    drawableToBitmap(appIcon, sizePx)
                }
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Exception) {
            null
        }

        if (bitmap != null) bitmapCache.put(cacheKey, bitmap)
        return bitmap
    }

    private fun renderAdaptiveIcon(icon: AdaptiveIconDrawable, sizePx: Int): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val layerSize = (size * 108f / 72f).toInt()
        val offset = (layerSize - size) / 2

        val layerBitmap = createBitmap(layerSize, layerSize, Bitmap.Config.ARGB_8888)
        val layerCanvas = Canvas(layerBitmap)

        icon.background?.let { bg ->
            bg.setBounds(0, 0, layerSize, layerSize)
            bg.draw(layerCanvas)
        }
        icon.foreground?.let { fg ->
            fg.setBounds(0, 0, layerSize, layerSize)
            fg.draw(layerCanvas)
        }

        val result = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(layerBitmap, -offset.toFloat(), -offset.toFloat(), null)
        layerBitmap.recycle()
        return result
    }

    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable, sizePx: Int): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    fun preloadIcons(context: Context, apps: List<com.gezimos.katapult.model.AppModel>, sizePx: Int) {
        for (app in apps) {
            loadIcon(context, app.packageName, app.activityName, sizePx)
        }
    }

}
