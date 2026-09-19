package app.offlinecounter

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.TextView

class OverlayNotice(
    context: Context,
    private val windowManager: WindowManager,
    private val handler: Handler,
) {
    private val themedContext = ContextThemeWrapper(context, R.style.Theme_OfflineCounter_Overlay)
    private val dismiss = Runnable(::hide)
    private val textView = TextView(themedContext).apply {
        gravity = Gravity.CENTER
        textSize = 14f
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        setTextColor(Color.rgb(248, 248, 250))
        minimumWidth = 0
        minWidth = 0
        setPadding(dp(18), dp(12), dp(18), dp(12))
        background = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            setColor(Color.argb(245, 48, 49, 54))
        }
        elevation = dp(3).toFloat()
        clipToOutline = true
    }
    private val card = textView
    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        y = dp(96)
    }
    private var attached = false
    private var animationGeneration = 0

    fun show(message: String) {
        handler.removeCallbacks(dismiss)
        animationGeneration++
        card.animate().setListener(null).withEndAction(null).cancel()
        textView.maxWidth = minOf(dp(360), themedContext.resources.displayMetrics.widthPixels - dp(48))
            .coerceAtLeast(dp(48))
        textView.text = message
        if (!attached) {
            card.alpha = 0f
            windowManager.addView(card, layoutParams)
            attached = true
        } else {
            windowManager.updateViewLayout(card, layoutParams)
        }
        card.animate()
            .alpha(1f)
            .setInterpolator(DecelerateInterpolator())
            .setDuration(180)
            .start()
        handler.postDelayed(dismiss, DISPLAY_DURATION_MS)
    }

    fun destroy() {
        handler.removeCallbacks(dismiss)
        animationGeneration++
        card.animate().setListener(null).withEndAction(null).cancel()
        if (attached) runCatching { windowManager.removeViewImmediate(card) }
        attached = false
    }

    private fun hide() {
        if (!attached) return
        val generation = ++animationGeneration
        card.animate().setListener(null).withEndAction(null).cancel()
        card.animate()
            .alpha(0f)
            .setInterpolator(AccelerateInterpolator())
            .setDuration(220)
            .withEndAction {
                if (generation != animationGeneration || !attached) return@withEndAction
                runCatching { windowManager.removeViewImmediate(card) }
                attached = false
            }
            .start()
    }

    private fun dp(value: Int) = (value * themedContext.resources.displayMetrics.density).toInt()

    private companion object {
        const val DISPLAY_DURATION_MS = 2_800L
    }
}
