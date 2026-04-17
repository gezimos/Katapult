package com.gezimos.katapult.ui

import android.app.Activity
import android.content.Context
import android.graphics.Insets
import android.graphics.Outline
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.UUID

@Composable
fun BottomSheet(
    onDismiss: () -> Unit,
    imePadding: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val composition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val currentImePadding by rememberUpdatedState(imePadding)
    val dialogId = rememberSaveable { UUID.randomUUID() }
    val statusBarHidden = remember {
        val prefs = view.context.getSharedPreferences("katapult_prefs", Context.MODE_PRIVATE)
        prefs.getBoolean("hide_status_bar", false)
    }

    val sheetDialog = remember(view, density) {
        BottomSheetDialog(
            onDismiss = onDismiss,
            composeView = view,
            layoutDirection = layoutDirection,
            dialogId = dialogId,
            hideStatusBar = statusBarHidden,
        ).apply {
            setContent(composition) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(if (currentImePadding) Modifier.imePadding() else Modifier)
                        .semantics { dialog() },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(Color.Black)
                        )
                        Column(Modifier.padding(16.dp)) {
                            currentContent()
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(sheetDialog) {
        sheetDialog.show()
        onDispose {
            sheetDialog.dismiss()
            sheetDialog.disposeComposition()
        }
    }

    SideEffect {
        sheetDialog.updateOnDismiss(onDismiss)
    }
}

private class BottomSheetDialogLayout(
    context: Context,
    override val window: Window,
    private val onDismiss: () -> Unit,
) : AbstractComposeView(context), DialogWindowProvider {

    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
        createComposition()
    }

    @Composable
    override fun Content() {
        content()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }
}

private class BottomSheetDialog(
    private var onDismiss: () -> Unit,
    composeView: View,
    layoutDirection: LayoutDirection,
    dialogId: UUID,
    private val hideStatusBar: Boolean,
) : ComponentDialog(
    ContextThemeWrapper(
        composeView.context,
        androidx.compose.material3.R.style.EdgeToEdgeFloatingDialogWindowTheme,
    )
), ViewRootForInspector {

    private val dialogLayout: BottomSheetDialogLayout
    private val activity = composeView.context as? Activity
    private var insetsListener: View.OnApplyWindowInsetsListener? = null

    override val subCompositionView: AbstractComposeView get() = dialogLayout

    init {
        val window = window ?: error("Dialog has no window")
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setWindowAnimations(0)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        dialogLayout = BottomSheetDialogLayout(context, window, onDismiss).apply {
            setTag(androidx.compose.ui.R.id.compose_view_saveable_id_tag, "Dialog:$dialogId")
            clipChildren = false
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, result: Outline) {
                    result.setRect(0, 0, view.width, view.height)
                    result.alpha = 0f
                }
            }
        }

        setContentView(dialogLayout)
        dialogLayout.setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        dialogLayout.setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        dialogLayout.setViewTreeSavedStateRegistryOwner(composeView.findViewTreeSavedStateRegistryOwner())

        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        if (hideStatusBar) {
            // Hide on dialog window
            window.insetsController?.let { controller ->
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsets.Type.statusBars())
            }
            // Hide on activity window
            activity?.window?.insetsController?.let { controller ->
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsets.Type.statusBars())
            }
            // Block status bar insets from re-appearing
            activity?.window?.decorView?.let { decorView ->
                insetsListener = View.OnApplyWindowInsetsListener { v, insets ->
                    val builder = WindowInsets.Builder(insets)
                    builder.setInsets(WindowInsets.Type.statusBars(), Insets.NONE)
                    v.onApplyWindowInsets(builder.build())
                }
                decorView.setOnApplyWindowInsetsListener(insetsListener)
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            onDismiss()
        }
    }

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        dialogLayout.setContent(parent, content)
    }

    fun updateOnDismiss(newOnDismiss: () -> Unit) {
        onDismiss = newOnDismiss
    }

    fun disposeComposition() {
        if (insetsListener != null) {
            activity?.window?.decorView?.setOnApplyWindowInsetsListener(null)
            insetsListener = null
        }
        dialogLayout.disposeComposition()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = super.onTouchEvent(event)
        if (result) onDismiss()
        return result
    }

    override fun cancel() {}
}
