package app.offlinecounter

import android.content.Context
import androidx.core.content.edit

class OverlayPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("overlay_position", Context.MODE_PRIVATE)

    fun save(position: NormalizedPosition, edge: OverlayEdge?) {
        preferences.edit {
            putFloat(POSITION_X, position.x)
            putFloat(POSITION_Y, position.y)
            putString(EDGE, edge?.name ?: "Free")
        }
    }

    fun load(): Pair<NormalizedPosition, OverlayEdge?> =
        NormalizedPosition(
            preferences.getFloat(POSITION_X, 0f),
            preferences.getFloat(POSITION_Y, DEFAULT_Y),
        ) to savedEdge()

    private fun savedEdge(): OverlayEdge? = runCatching {
        OverlayEdge.valueOf(preferences.getString(EDGE, OverlayEdge.Start.name).orEmpty())
    }.getOrNull()

    private companion object {
        const val POSITION_X = "x"
        const val POSITION_Y = "y"
        const val EDGE = "edge"
        const val DEFAULT_Y = 0.18f
    }
}
