package com.gezimos.katapult.util

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.gezimos.katapult.R
import org.xmlpull.v1.XmlPullParser
import java.io.File

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
        "com.android.deskclock" to R.drawable.clock,
        "com.android.fmradio" to R.drawable.radio,
        "com.android.stk" to R.drawable.sim,
        "org.mozilla.firefox" to R.drawable.firefox,
        "org.mozilla.firefox_beta" to R.drawable.firefox,
        "org.mozilla.focus" to R.drawable.firefox,
        "com.fsck.k9" to R.drawable.mail,
        "net.thunderbird.android" to R.drawable.mail,
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
        "com.reddit.frontpage" to R.drawable.reddit,
        "info.plateaukao.einkbro" to R.drawable.einkbro,
        "ws.xsoh.etar" to R.drawable.calendar,
        "org.onekash.kashcal" to R.drawable.calendar,
    )

    data class BundledIcon(val resId: Int, val resName: String, val label: String)

    // Deduplicated set of bundled drawables users can pick for any app.
    val bundledIcons: List<BundledIcon> = listOf(
        BundledIcon(R.drawable.signal, "signal", "Signal"),
        BundledIcon(R.drawable.telegram, "telegram", "Telegram"),
        BundledIcon(R.drawable.viber, "viber", "Viber"),
        BundledIcon(R.drawable.whatsapp, "whatsapp", "WhatsApp"),
        BundledIcon(R.drawable.beeper, "beeper", "Beeper"),
        BundledIcon(R.drawable.aurora, "aurora", "Aurora"),
        BundledIcon(R.drawable.f_droid, "f_droid", "F-Droid"),
        BundledIcon(R.drawable.spotify, "spotify", "Spotify"),
        BundledIcon(R.drawable.gallery, "gallery", "Gallery"),
        BundledIcon(R.drawable.clock, "clock", "Clock"),
        BundledIcon(R.drawable.radio, "radio", "Radio"),
        BundledIcon(R.drawable.sim, "sim", "SIM"),
        BundledIcon(R.drawable.firefox, "firefox", "Firefox"),
        BundledIcon(R.drawable.mail, "mail", "Mail"),
        BundledIcon(R.drawable.files, "files", "Files"),
        BundledIcon(R.drawable.ap, "ap", "AntennaPod"),
        BundledIcon(R.drawable.sms, "sms", "SMS"),
        BundledIcon(R.drawable.newpipe, "newpipe", "NewPipe"),
        BundledIcon(R.drawable.music, "music", "Music"),
        BundledIcon(R.drawable.settings, "settings", "Settings"),
        BundledIcon(R.drawable.chromium, "chromium", "Chromium"),
        BundledIcon(R.drawable.brave, "brave", "Brave"),
        BundledIcon(R.drawable.killapps, "killapps", "Kill Apps"),
        BundledIcon(R.drawable.koreader, "koreader", "KOReader"),
        BundledIcon(R.drawable.book, "book", "Book"),
        BundledIcon(R.drawable.calendar, "calendar", "Calendar"),
        BundledIcon(R.drawable.camera, "camera", "Camera"),
        BundledIcon(R.drawable.car, "car", "Car"),
        BundledIcon(R.drawable.google, "google", "Google"),
        BundledIcon(R.drawable.home, "home", "Home"),
        BundledIcon(R.drawable.lock, "lock", "Lock"),
        BundledIcon(R.drawable.money, "money", "Money"),
        BundledIcon(R.drawable.note, "note", "Note"),
        BundledIcon(R.drawable.phone, "phone", "Phone"),
        BundledIcon(R.drawable.podcast, "podcast", "Podcast"),
        BundledIcon(R.drawable.reddit, "reddit", "Reddit"),
        BundledIcon(R.drawable.rss, "rss", "RSS"),
        BundledIcon(R.drawable.sudoku, "sudoku", "Sudoku"),
        BundledIcon(R.drawable.video, "video", "Video"),
        BundledIcon(R.drawable.einkbro, "einkbro", "Einkbro"),
    )

    private fun resolveBundled(resName: String): Int? =
        bundledIcons.firstOrNull { it.resName == resName }?.resId

    fun clearCacheFor(packageName: String) {
        val prefix = "$packageName:"
        val keys = bitmapCache.snapshot().keys.filter { it.startsWith(prefix) }
        keys.forEach { bitmapCache.remove(it) }
    }

    fun renderDrawableResource(context: Context, resId: Int, sizePx: Int): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, resId) ?: return null
            renderBundledIcon(drawable, sizePx)
        } catch (_: Exception) {
            null
        }
    }

    fun loadIcon(context: Context, packageName: String, activityClass: String, sizePx: Int): Bitmap? {
        if (packageName.isBlank() || sizePx <= 0) return null

        val override = context
            .getSharedPreferences("katapult_prefs", Context.MODE_PRIVATE)
            .getString("icon_override_$packageName", null)

        val cacheKey = "$packageName:$activityClass:$sizePx:${override ?: ""}"
        bitmapCache.get(cacheKey)?.let { return it }

        val bitmap = try {
            val overrideBitmap = override?.let { loadOverrideBitmap(context, it, sizePx) }
            val customRes = customIcons[packageName]
            if (overrideBitmap != null) {
                overrideBitmap
            } else if (customRes != null) {
                val drawable = ContextCompat.getDrawable(context, customRes)!!
                renderBundledIcon(drawable, sizePx)
            } else {
                val appIcon = context.packageManager.getApplicationIcon(packageName)
                val monoLayer = if (appIcon is AdaptiveIconDrawable) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        appIcon.monochrome
                    } else {
                        extractMonochromeFromManifest(context, packageName)
                    }
                } else null

                when {
                    monoLayer != null -> renderMonochromeIcon(monoLayer, sizePx)
                    appIcon is AdaptiveIconDrawable -> renderAdaptiveIcon(appIcon, sizePx)
                    else -> drawableToBitmap(appIcon, sizePx)
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

    private fun loadOverrideBitmap(context: Context, override: String, sizePx: Int): Bitmap? {
        return try {
            when {
                override.startsWith("file:") -> {
                    val path = override.removePrefix("file:")
                    val file = File(path)
                    if (!file.exists()) return null
                    val bmp = BitmapFactory.decodeFile(path) ?: return null
                    drawableToBitmap(BitmapDrawable(context.resources, bmp), sizePx)
                }
                override.startsWith("res:") -> {
                    val resId = resolveBundled(override.removePrefix("res:")) ?: return null
                    renderBundledIcon(ContextCompat.getDrawable(context, resId)!!, sizePx)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    // Android 12 fallback: AdaptiveIconDrawable.getMonochrome() is API 33+, but apps may
    // still ship a <monochrome> element in their adaptive-icon XML.
    private fun extractMonochromeFromManifest(context: Context, packageName: String): Drawable? {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val iconRes = appInfo.icon
            if (iconRes == 0) return null
            val res = pm.getResourcesForApplication(appInfo)
            val androidNs = "http://schemas.android.com/apk/res/android"

            var drawableRes = 0
            val parser: XmlResourceParser = res.getXml(iconRes)
            try {
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.name == "monochrome") {
                        drawableRes = parser.getAttributeResourceValue(androidNs, "drawable", 0)
                        break
                    }
                    event = parser.next()
                }
            } finally {
                parser.close()
            }
            if (drawableRes == 0) null else ResourcesCompat.getDrawable(res, drawableRes, null)
        } catch (_: Exception) {
            null
        }
    }

    private fun renderMonochromeIcon(drawable: Drawable, sizePx: Int): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val layerSize = (size * 108f / 72f).toInt()
        val offset = (layerSize - size) / 2

        val tinted = drawable.mutate()
        tinted.setTint(Color.BLACK)
        tinted.setTintMode(PorterDuff.Mode.SRC_IN)

        val layerBitmap = createBitmap(layerSize, layerSize, Bitmap.Config.ARGB_8888)
        val layerCanvas = Canvas(layerBitmap)
        tinted.setBounds(0, 0, layerSize, layerSize)
        tinted.draw(layerCanvas)

        val result = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(layerBitmap, -offset.toFloat(), -offset.toFloat(), null)
        layerBitmap.recycle()
        return result
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

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    private fun renderBundledIcon(drawable: Drawable, sizePx: Int, fraction: Float = 0.60f): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val maxInner = (size * fraction).toInt().coerceAtLeast(1)
        val iw = drawable.intrinsicWidth.takeIf { it > 0 } ?: maxInner
        val ih = drawable.intrinsicHeight.takeIf { it > 0 } ?: maxInner
        val scale = minOf(maxInner.toFloat() / iw, maxInner.toFloat() / ih)
        val w = (iw * scale).toInt().coerceAtLeast(1)
        val h = (ih * scale).toInt().coerceAtLeast(1)
        val left = (size - w) / 2
        val top = (size - h) / 2

        val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(left, top, left + w, top + h)
        drawable.draw(canvas)
        return bitmap
    }

    fun preloadIcons(context: Context, apps: List<com.gezimos.katapult.model.AppModel>, sizePx: Int) {
        for (app in apps) {
            loadIcon(context, app.packageName, app.activityName, sizePx)
        }
    }

}
