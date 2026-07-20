package com.gezimos.katapult.lockscreen

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.gezimos.katapult.R
import com.gezimos.katapult.service.DirectBadgeHelper
import com.gezimos.katapult.service.NotificationListener
import com.gezimos.katapult.ui.EinkTintMatrix
import com.gezimos.katapult.ui.EinkTintMatrixDark
import com.gezimos.katapult.util.DeviceHelper
import com.gezimos.katapult.util.IconUtility
import com.gezimos.katapult.util.PrefsManager
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Experimental: shows per-app notification counts on the lock screen.
 *
 * An accessibility service may add TYPE_ACCESSIBILITY_OVERLAY windows, which the system
 * composites above the keyguard — the same mechanism zacharee/LockscreenWidgets uses.
 * Shown only while the pref is on, the keyguard is locked, the screen is interactive,
 * there is something to show, and the bouncer isn't up.
 *
 * Adjusting (like Lockscreen Widgets): long-press the widget to enter edit mode —
 * the border turns dashed and a drag handle appears at the bottom. In edit mode a
 * one-finger drag moves the widget, dragging the bottom handle resizes it (rows), and
 * a single tap (or 8s of no touches) exits. Position and row count are remembered.
 *
 * The whole feature is contained here so it can be scrapped easily. To remove it, delete:
 * this package, the <service> entry in AndroidManifest.xml, res/xml/lockscreen_widget_service.xml,
 * the Experimental section in SettingsScreen (toggle + Lockscreen Notification Apps dialog),
 * the lockscreenWidget prefs in PrefsManager, the onCountsChangedExtra hook in
 * NotificationListener, and the lockscreen_widget and section_experimental strings.
 */
class LockscreenWidgetService : AccessibilityService() {

    companion object {
        private const val MIN_ROWS = 1
        private const val MAX_ROWS = 8
        private const val LONG_PRESS_MS = 400L
        private const val EDIT_TIMEOUT_MS = 8000L

        @Volatile
        private var instance: LockscreenWidgetService? = null

        /**
         * Locks the device via the accessibility service, if it's running. Used by the
         * screensaver to lock the screen so Home/Back can't return to the launcher.
         * Returns false when the service isn't enabled (nothing an app can do then).
         */
        fun lockScreen(): Boolean = try {
            instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false
        } catch (_: Exception) {
            false
        }

        /** Whether the user has enabled this service in system accessibility settings. */
        fun isEnabled(context: Context): Boolean {
            val component = ComponentName(context, LockscreenWidgetService::class.java)
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            return enabled.split(':').any {
                it.equals(component.flattenToString(), ignoreCase = true) ||
                    it.equals(component.flattenToShortString(), ignoreCase = true)
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlay: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var shownRows: List<Pair<String, Int>> = emptyList()
    private var directBadges: DirectBadgeHelper? = null
    private lateinit var prefs: PrefsManager

    // Edit-mode state for the current overlay
    private var editing = false
    private var islandView: View? = null
    private var rowViews: List<View> = emptyList()
    private var overflowView: TextView? = null
    private var handleView: View? = null
    private var visibleRows = 0
    private var totalCount = 0
    private val editTimeout = Runnable { exitEdit() }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) maybeStartScreensaver()
            evaluate()
        }
    }

    private fun maybeStartScreensaver() {
        if (!::prefs.isInitialized) return
        if (!prefs.screensaverEnabled || !prefs.screensaverOnPower) return
        if (ScreensaverActivity.isShowing || ScreensaverActivity.recentlyActive()) return
        try {
            startActivity(
                Intent(this, ScreensaverActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(ScreensaverActivity.EXTRA_FROM_POWER, true),
            )
        } catch (_: Exception) {}
    }

    override fun onServiceConnected() {
        instance = this
        prefs = PrefsManager(this)
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        })
        if (DeviceHelper.isMuditaKompakt()) {
            directBadges = DirectBadgeHelper(this).apply {
                onCountsChanged = { mainHandler.post { evaluate() } }
                start()
            }
        }
        NotificationListener.onCountsChangedExtra = { mainHandler.post { evaluate() } }
        evaluate()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        evaluate()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance === this) instance = null
        NotificationListener.onCountsChangedExtra = null
        directBadges?.let {
            it.onCountsChanged = null
            it.stop()
        }
        directBadges = null
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        removeOverlay()
        super.onDestroy()
    }

    private fun evaluate() {
        if (!::prefs.isInitialized) return
        val keyguard = getSystemService(KeyguardManager::class.java)
        val power = getSystemService(PowerManager::class.java)
        val counts = currentCounts()
        val shouldShow = prefs.lockscreenWidget &&
            keyguard.isKeyguardLocked &&
            power.isInteractive &&
            counts.isNotEmpty() &&
            !KatapultDreamService.isDreaming &&
            !ScreensaverActivity.isShowing &&
            !inCall() &&
            !pinShowing()
        if (!shouldShow) {
            removeOverlay()
            return
        }
        // Don't rebuild under the user's fingers while they're adjusting the widget.
        if (editing) return
        // Skip redraws when nothing changed — every rebuild flashes the e-ink panel.
        val rows = orderedRows(counts)
        if (overlay != null && rows == shownRows) return
        removeOverlay()
        addOverlay(rows, counts.size)
    }

    /**
     * The PIN screen is in front when a visible input field holds focus. Polled instead of
     * event-driven: re-showing the PIN doesn't re-fire focus events when its field kept focus.
     */
    private fun pinShowing(): Boolean = try {
        val focus = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val showing = focus != null && focus.isVisibleToUser &&
            focus.className?.contains("EditText") == true
        @Suppress("DEPRECATION")
        focus?.recycle()
        showing
    } catch (_: Exception) {
        false
    }

    /** An incoming or active call shows its own UI over the lock screen — hide the widget then. */
    private fun inCall(): Boolean = try {
        when (getSystemService(AudioManager::class.java).mode) {
            AudioManager.MODE_RINGTONE,
            AudioManager.MODE_IN_CALL,
            AudioManager.MODE_IN_COMMUNICATION -> true
            else -> false
        }
    } catch (_: Exception) {
        false
    }

    private fun currentCounts(): Map<String, Int> {
        val merged = NotificationListener.getAllCounts().toMutableMap()
        directBadges?.getCounts()?.forEach { (pkg, count) -> merged[pkg] = count }
        val excluded = prefs.getLockscreenExcludedApps()
        return merged.filter { (pkg, count) -> count > 0 && pkg !in excluded }
    }

    /**
     * Rows ordered by each app's latest notification time (direction per the sort
     * setting). Apps without a known time — the Mudita call/SMS badges — sort last.
     */
    private fun orderedRows(counts: Map<String, Int>): List<Pair<String, Int>> {
        val times = NotificationListener.getLastPostTimes()
        val comparator = if (prefs.lockscreenWidgetLatestFirst) {
            compareByDescending<Map.Entry<String, Int>> { times[it.key] ?: 0L }
        } else {
            compareBy<Map.Entry<String, Int>> { times[it.key] ?: Long.MAX_VALUE }
        }
        return counts.entries
            .sortedWith(comparator.thenBy { it.key })
            .take(MAX_ROWS)
            .map { it.key to it.value }
    }

    private fun addOverlay(rows: List<Pair<String, Int>>, total: Int) {
        val view = buildWidget(rows, total) ?: return
        val savedY = prefs.lockscreenWidgetY
        val params = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            width = resources.displayMetrics.widthPixels - dp(28f)
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = if (savedY >= 0) savedY else (resources.displayMetrics.heightPixels * 0.62f).toInt()
        }
        attachGestures(view, params)
        try {
            getSystemService(WindowManager::class.java).addView(view, params)
            overlay = view
            overlayParams = params
            shownRows = rows
        } catch (_: Exception) {
            overlay = null
            overlayParams = null
            shownRows = emptyList()
        }
    }

    private fun removeOverlay() {
        mainHandler.removeCallbacks(editTimeout)
        editing = false
        overlay?.let {
            try { getSystemService(WindowManager::class.java).removeView(it) } catch (_: Exception) {}
        }
        overlay = null
        overlayParams = null
        shownRows = emptyList()
        islandView = null
        rowViews = emptyList()
        overflowView = null
        handleView = null
    }

    // --- Edit mode (two-finger long-press) ---

    private fun widgetBackground(dashed: Boolean): GradientDrawable {
        val ink = if (prefs.darkMode) Color.WHITE else Color.BLACK
        val surface = if (prefs.darkMode) Color.BLACK else Color.WHITE
        return GradientDrawable().apply {
            setColor(surface)
            cornerRadius = dp(14f).toFloat()
            if (dashed) {
                setStroke(dp(2.5f), ink, dp(10f).toFloat(), dp(7f).toFloat())
            } else {
                setStroke(dp(2.5f), ink)
            }
        }
    }

    private fun enterEdit() {
        val view = overlay ?: return
        editing = true
        islandView?.background = widgetBackground(dashed = true)
        handleView?.visibility = View.VISIBLE
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        mainHandler.removeCallbacks(editTimeout)
        mainHandler.postDelayed(editTimeout, EDIT_TIMEOUT_MS)
    }

    private fun exitEdit() {
        if (!editing) return
        editing = false
        mainHandler.removeCallbacks(editTimeout)
        islandView?.background = widgetBackground(dashed = false)
        handleView?.visibility = View.GONE
        prefs.lockscreenWidgetRows = visibleRows.coerceIn(MIN_ROWS, MAX_ROWS)
        overlayParams?.let { prefs.lockscreenWidgetY = it.y }
    }

    /** Show the first [target] rows, hide the rest, and refresh the +N overflow line. */
    private fun applyRowVisibility(target: Int) {
        val clamped = target.coerceIn(1, rowViews.size)
        if (clamped == visibleRows) return
        visibleRows = clamped
        rowViews.forEachIndexed { i, row ->
            row.visibility = if (i < clamped) View.VISIBLE else View.GONE
        }
        val remaining = totalCount - clamped
        overflowView?.apply {
            text = "+$remaining"
            visibility = if (remaining > 0) View.VISIBLE else View.GONE
        }
    }

    private object GestureMode {
        const val NONE = 0
        const val MOVE = 1
        const val RESIZE = 2
        const val TAP = 3
    }

    private fun attachGestures(view: View, params: WindowManager.LayoutParams) {
        var startRawX = 0f
        var startRawY = 0f
        var startParamY = 0
        var startRows = 0
        var mode = GestureMode.NONE
        val slop = dp(12f).toFloat()
        val pendingEdit = Runnable {
            if (!editing) enterEdit()
            mode = GestureMode.NONE
        }

        view.setOnTouchListener { v, event ->
            if (editing) {
                mainHandler.removeCallbacks(editTimeout)
                mainHandler.postDelayed(editTimeout, EDIT_TIMEOUT_MS)
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startRawX = event.rawX
                    startRawY = event.rawY
                    startParamY = params.y
                    startRows = visibleRows
                    mode = if (editing) {
                        if (event.y >= v.height - dp(44f)) GestureMode.RESIZE else GestureMode.TAP
                    } else {
                        // Long-press (one finger, held still) enters edit mode.
                        mainHandler.postDelayed(pendingEdit, LONG_PRESS_MS)
                        GestureMode.NONE
                    }
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    // A second finger means this isn't a long-press.
                    mainHandler.removeCallbacks(pendingEdit)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startRawX
                    val dy = event.rawY - startRawY
                    if (!editing) {
                        // Fingers wandered — this isn't a long-press, cancel activation.
                        if (hypot(dx, dy) > slop) mainHandler.removeCallbacks(pendingEdit)
                        return@setOnTouchListener true
                    }
                    when (mode) {
                        GestureMode.TAP -> if (hypot(dx, dy) > slop) mode = GestureMode.MOVE
                        GestureMode.RESIZE -> {
                            val target = startRows + (dy / dp(40f).toFloat()).roundToInt()
                            applyRowVisibility(target)
                        }
                    }
                    if (mode == GestureMode.MOVE) {
                        val maxY = (resources.displayMetrics.heightPixels - v.height).coerceAtLeast(0)
                        val newY = (startParamY + dy).toInt().coerceIn(0, maxY)
                        // Move in 4dp steps so the e-ink panel isn't redrawn for every pixel.
                        if (abs(newY - params.y) >= dp(4f)) {
                            params.y = newY
                            try {
                                getSystemService(WindowManager::class.java).updateViewLayout(v, params)
                            } catch (_: Exception) {}
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {
                    mainHandler.removeCallbacks(pendingEdit)
                    if (editing && event.actionMasked == MotionEvent.ACTION_UP) {
                        when (mode) {
                            GestureMode.TAP -> exitEdit()
                            GestureMode.MOVE -> prefs.lockscreenWidgetY = params.y
                            GestureMode.RESIZE -> prefs.lockscreenWidgetRows = visibleRows.coerceIn(MIN_ROWS, MAX_ROWS)
                        }
                    }
                    if (event.actionMasked != MotionEvent.ACTION_POINTER_UP) mode = GestureMode.NONE
                    true
                }
                else -> false
            }
        }
    }

    // --- Widget view ---

    private fun buildWidget(rows: List<Pair<String, Int>>, total: Int): View? {
        val ink = if (prefs.darkMode) Color.WHITE else Color.BLACK
        val lato = try { ResourcesCompat.getFont(this, R.font.lato) } catch (_: Exception) { null }

        // Transparent column: the island box on top, the resize cue below it (outside the box).
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val island = FrameLayout(this).apply {
            background = widgetBackground(dashed = false)
            // Rows carry 4dp of their own vertical padding, so 16dp horizontal here
            // matches the effective 12+4dp vertical inset.
            setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Build up to MAX_ROWS rows so edit mode can grow the widget without rebuilding;
        // rows beyond the configured count start out hidden.
        val iconSize = dp(32f)
        val builtRows = mutableListOf<View>()
        for ((pkg, count) in rows) {
            val label = appLabel(pkg) ?: continue
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(4f), 0, dp(4f))
            }
            iconBitmap(pkg, iconSize, ink)?.let { bmp ->
                row.addView(ImageView(this).apply {
                    setImageBitmap(bmp)
                }, LinearLayout.LayoutParams(iconSize, iconSize))
            }
            row.addView(TextView(this).apply {
                text = count.toString()
                typeface = Typeface.create(lato, Typeface.BOLD)
                textSize = 18f
                setTextColor(ink)
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginStart = dp(10f)
            })
            row.addView(TextView(this).apply {
                text = label
                typeface = lato
                textSize = 18f
                setTextColor(ink)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(8f)
            })
            container.addView(row)
            builtRows.add(row)
        }
        if (builtRows.isEmpty()) return null

        island.addView(container, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        ))

        // "+N" overflow badge pinned to the widget's top-right corner.
        val overflow = TextView(this).apply {
            typeface = Typeface.create(lato, Typeface.BOLD)
            textSize = 14f
            setTextColor(ink)
            background = GradientDrawable().apply {
                setColor(if (prefs.darkMode) Color.BLACK else Color.WHITE)
                cornerRadius = dp(8f).toFloat()
                setStroke(dp(2.5f), ink)
            }
            setPadding(dp(8f), dp(2f), dp(8f), dp(2f))
        }
        island.addView(overflow, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.END,
        ))

        root.addView(island, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        // Resize cue below the box (outside it), visible only in edit mode.
        val handle = View(this).apply {
            background = GradientDrawable().apply {
                setColor(ink)
                cornerRadius = dp(3f).toFloat()
            }
            visibility = View.GONE
        }
        root.addView(handle, LinearLayout.LayoutParams(dp(48f), dp(5f)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(6f)
        })

        islandView = island
        rowViews = builtRows
        overflowView = overflow
        handleView = handle
        totalCount = total
        visibleRows = 0
        applyRowVisibility(prefs.lockscreenWidgetRows.coerceIn(MIN_ROWS, MAX_ROWS))
        return root
    }

    /**
     * The launcher icon rendered the way AppIconCircle does it: surface fill in the icon
     * shape, the monochrome silhouette via the e-ink tint matrix, and an ink border with
     * an inset corner radius so the stroke stays concentric with the fill.
     */
    private fun iconBitmap(pkg: String, sizePx: Int, ink: Int): Bitmap? {
        val src = IconUtility.loadIcon(this, pkg, "", sizePx) ?: return null
        val surface = if (prefs.darkMode) Color.BLACK else Color.WHITE
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val size = sizePx.toFloat()
        val radius = if (prefs.roundedIcons) size * 0.26f else size / 2f
        val shape = Path().apply { addRoundRect(RectF(0f, 0f, size, size), radius, radius, Path.Direction.CW) }
        canvas.drawPath(shape, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surface })
        canvas.save()
        canvas.clipPath(shape)
        val matrix = if (prefs.darkMode) EinkTintMatrixDark.values else EinkTintMatrix.values
        canvas.drawBitmap(src, null, RectF(0f, 0f, size, size), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        })
        canvas.restore()
        val strokeWidth = dp(2.5f).toFloat()
        val inset = strokeWidth / 2f
        canvas.drawRoundRect(
            RectF(inset, inset, size - inset, size - inset),
            radius - inset,
            radius - inset,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                color = ink
            },
        )
        return out
    }

    private fun appLabel(pkg: String): String? {
        prefs.getAppRename(pkg)?.let { return it }
        return try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun dp(v: Float): Int = (v * resources.displayMetrics.density).toInt()
}
