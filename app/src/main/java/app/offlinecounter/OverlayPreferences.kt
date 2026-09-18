package app.offlinecounter

import android.content.Context

class OverlayPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("overlay_position", Context.MODE_PRIVATE)

    fun save(position: NormalizedPosition, edge: OverlayEdge) {
        preferences.edit().putFloat("x", position.x).putFloat("y", position.y).putString("edge", edge.name).apply()
    }

    fun load(): Pair<NormalizedPosition, OverlayEdge> =
        NormalizedPosition(preferences.getFloat("x", 0f), preferences.getFloat("y", 0.18f)) to
            runCatching { OverlayEdge.valueOf(preferences.getString("edge", OverlayEdge.Start.name)!!) }.getOrDefault(OverlayEdge.Start)
}
