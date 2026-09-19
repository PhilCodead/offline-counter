package app.offlinecounter

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

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
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        setTextColor(MaterialColors.getColor(themedContext, com.google.android.material.R.attr.colorOnSurface, Color.WHITE))
        setPadding(dp(18), dp(12), dp(18), dp(12))
    }
    private val card = MaterialCardView(themedContext).apply {
        radius = dp(18).toFloat()
        cardElevation = dp(10).toFloat()
        setCardBackgroundColor(
            ColorUtils.setAlphaComponent(
                MaterialColors.getColor(themedContext, com.google.android.material.R.attr.colorSurface, Color.rgb(24, 31, 43)),
                250,
            ),
        )
        addView(textView)
    }
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
        textView.text = message
        if (!attached) {
            windowManager.addView(card, layoutParams)
            attached = true
            card.alpha = 0f
            card.scaleX = 0.96f
            card.scaleY = 0.96f
        }
        card.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setInterpolator(DecelerateInterpolator())
            .setDuration(180)
            .start()
        handler.postDelayed(dismiss, DISPLAY_DURATION_MS)
    }

    fun destroy() {
        handler.removeCallbacks(dismiss)
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
