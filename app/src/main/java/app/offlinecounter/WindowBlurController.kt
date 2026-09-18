package app.offlinecounter

import android.os.Build
import android.view.WindowManager

class WindowBlurController(
    private val windowManager: WindowManager,
    private val params: WindowManager.LayoutParams,
    private val update: () -> Unit,
    private val availabilityChanged: (Boolean) -> Unit,
) {
    private var expanded = false
    private val listener = java.util.function.Consumer<Boolean> { apply(expanded) }

    fun attach() {
        if (Build.VERSION.SDK_INT >= 31) windowManager.addCrossWindowBlurEnabledListener(listener)
        apply(false)
    }

    fun apply(expanded: Boolean) {
        this.expanded = expanded
        val available = Build.VERSION.SDK_INT >= 31 && windowManager.isCrossWindowBlurEnabled
        availabilityChanged(available)
        if (available) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            params.setBlurBehindRadius(if (expanded) 120 else 72)
        } else {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
        }
        update()
    }

    fun detach() {
        if (Build.VERSION.SDK_INT >= 31) windowManager.removeCrossWindowBlurEnabledListener(listener)
    }
}
