package app.offlinecounter

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton

class DragHandleButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.imageButtonStyle,
) : AppCompatImageButton(context, attrs, defStyleAttr) {
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
