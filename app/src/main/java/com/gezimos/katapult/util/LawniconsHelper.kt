package com.gezimos.katapult.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object LawniconsHelper {

    private const val PREFIX = "app.lawnchair.lawnicons"
    private const val THEMED_ICON = "app.lawnchair.icons.THEMED_ICON"

    @Volatile
    var enabled = false

    private val extraLinks: Map<String, String> = mapOf(
        "com.gezimos.katapult/com.gezimos.katapult.ScreensaverAlias" to "generic_moon",
        "com.mudita.dial/com.android.dialer.main.impl.MainActivity" to "generic_dialer",
        "com.mudita.messages/com.android.messaging.ui.conversationlist.ConversationListActivity" to "messages",
        "com.mudita.contacts/com.android.contacts.activities.PeopleActivity" to "generic_contacts",
        "com.mudita.alarm/com.mudita.compact.presentation.ui.main.MainActivity" to "generic_clock",
        "com.mudita.calendar/com.mudita.compact.presentation.ui.main.MainActivity" to "calendar",
        "com.mudita.calc/com.mudita.calc.MainActivity" to "generic_calculator_plus_minus_multi_equal",
        "com.mudita.camera/com.mudita.camera.screens.CameraActivity" to "generic_camera",
        "com.mudita.audio.player/com.mudita.audio.player.platform.AppActivity" to "music_player",
        "com.mudita.notes/com.mudita.notes.screens.MainActivity" to "generic_notes",
        "com.mudita.voicerecorder/com.mudita.voicerecorder.MainActivity" to "generic_voice_recorder_waves",
        "com.mudita.ereader/com.mudita.ebookreader.root.MainActivity" to "generic_book",
        "com.mudita.chess/com.mudita.chess.root.MainActivity" to "generic_chess",
        "com.mudita.maps/net.osmand.plus.activities.MapActivity" to "osmand",
        "com.wanderwildwood.kotozute/com.wanderwildwood.kotozute.feature.main.MainActivity" to "messages",
        "com.wanderwildwood.jimeikin/com.wanderwildwood.jimeikin.MainActivity" to "music_player",
        "com.wanderwildwood.soramoyo/com.wanderwildwood.soramoyo.MainActivity" to "weather",
        "com.caravanfire.calmqr/com.caravanfire.calmqr.MainActivity" to "generic_qr_reader",
        "com.calmapps.calmmusic/com.calmapps.calmmusic.MainActivity" to "music_player",
        "com.calmapps.calmcast/com.calmcast.podcast.MainActivity" to "microphone",
        "com.paperapps.papertunes/moe.rukamori.papertunes.eink.EinkActivity" to "music_player",
        "com.kompaktx.app/com.noti.restore.ui.MainActivity" to "generic_settings",
        "com.berend.transit/com.berend.transit.MainActivity" to "transit",
        "com.monoapps.monomusic/com.monoapps.monomusic.MainActivity" to "music_player",
        "com.monoapps.monomail/net.thunderbird.app.common.MainActivity" to "generic_email",
        "com.monoapps.monoride/com.monoapps.monoride.MainActivity" to "bike_computer",
        "com.monoapps.monoclock/com.monoapps.monoclock.MainActivity" to "generic_clock",
        "com.monoapps.monophone/com.monoapps.monophone.MainActivity" to "generic_dialer",
    ).let { links ->
        links + links.filterKeys { !it.startsWith("com.gezimos.katapult/") }
            .mapKeys { it.key.substringBefore('/') }
    }

    @Volatile
    private var resolved = false

    @Volatile
    private var pkg: String? = null

    @Volatile
    private var res: Resources? = null

    @Volatile
    private var components: Map<String, String>? = null

    @Volatile
    private var drawables: List<String>? = null

    fun isLawnicons(packageName: String): Boolean = packageName.startsWith(PREFIX)

    fun packageName(context: Context): String? {
        if (resolved) return pkg
        pkg = try {
            context.packageManager.queryIntentActivities(Intent(THEMED_ICON), PackageManager.MATCH_ALL)
                .map { it.activityInfo.packageName }
                .firstOrNull { isLawnicons(it) }
        } catch (_: Exception) {
            null
        }
        resolved = true
        return pkg
    }

    fun isInstalled(context: Context): Boolean = packageName(context) != null

    fun active(context: Context): Boolean = enabled && isInstalled(context)

    fun invalidate() {
        resolved = false
        pkg = null
        res = null
        components = null
        drawables = null
    }

    private fun resources(context: Context): Pair<String, Resources>? {
        val p = packageName(context) ?: return null
        res?.let { return p to it }
        return try {
            val loaded = context.packageManager.getResourcesForApplication(p)
            res = loaded
            p to loaded
        } catch (_: Exception) {
            null
        }
    }

    private fun parser(p: String, res: Resources, name: String): XmlPullParser? {
        val id = res.getIdentifier(name, "xml", p)
        if (id != 0) return res.getXml(id)
        return try {
            XmlPullParserFactory.newInstance().newPullParser().apply {
                setInput(res.assets.open("$name.xml"), "UTF-8")
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun componentMap(context: Context): Map<String, String> {
        components?.let { return it }
        val (p, res) = resources(context) ?: return emptyMap()
        val map = HashMap<String, String>()
        try {
            val parser = parser(p, res, "appfilter")
            if (parser != null) {
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType != XmlPullParser.START_TAG || parser.name != "item") continue
                    val component = parser.getAttributeValue(null, "component") ?: continue
                    val drawable = parser.getAttributeValue(null, "drawable")?.takeIf { it.isNotBlank() } ?: continue
                    val inner = component.removePrefix("ComponentInfo{").removeSuffix("}")
                    if (inner.isEmpty() || inner == component) continue
                    map.putIfAbsent(inner, drawable)
                    map.putIfAbsent(inner.substringBefore('/'), drawable)
                }
            }
        } catch (_: Exception) {}
        components = map
        return map
    }

    fun drawableFor(context: Context, packageName: String, activityClass: String): String? {
        val map = componentMap(context)
        if (map.isEmpty()) return null
        val component = "$packageName/$activityClass"
        if (activityClass.isNotEmpty()) {
            map[component]?.let { return it }
            extraLinks[component]?.let { return it }
        }
        return map[packageName] ?: extraLinks[packageName]
    }

    fun allDrawables(context: Context): List<String> {
        drawables?.let { return it }
        val (p, res) = resources(context) ?: return emptyList()
        val names = LinkedHashSet<String>()
        for (file in listOf("drawable", "appfilter")) {
            try {
                val parser = parser(p, res, file) ?: continue
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        parser.getAttributeValue(null, "drawable")?.takeIf { it.isNotBlank() }?.let { names.add(it) }
                    }
                }
            } catch (_: Exception) {}
            if (names.isNotEmpty()) break
        }
        val list = names.toList()
        drawables = list
        return list
    }

    fun glyph(context: Context, name: String): Pair<Drawable, Boolean>? {
        val (p, res) = resources(context) ?: return null
        return try {
            val id = res.getIdentifier(name, "drawable", p)
            if (id == 0) return null
            val drawable = ResourcesCompat.getDrawable(res, id, null) ?: return null
            if (drawable is AdaptiveIconDrawable) {
                val layer = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) drawable.monochrome else null)
                    ?: drawable.foreground ?: return null
                layer to true
            } else {
                drawable to false
            }
        } catch (_: Exception) {
            null
        }
    }
}
