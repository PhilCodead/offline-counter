package app.offlinecounter

import kotlin.math.max

data class OverlayPosition(val x: Int, val y: Int)

data class OverlayBounds(
    val width: Int,
    val height: Int,
    val panelWidth: Int,
    val panelHeight: Int,
    val insetTop: Int,
    val insetBottom: Int,
)

data class NormalizedPosition(val x: Float, val y: Float)

enum class OverlayEdge { Start, End }

object OverlayPlacement {
    fun snapEdge(position: OverlayPosition, bounds: OverlayBounds, threshold: Int): OverlayEdge? {
        val x = clamp(position, bounds).x
        val rightDistance = max(0, bounds.width - bounds.panelWidth) - x
        if (minOf(x, rightDistance) > threshold) return null
        return if (x <= rightDistance) OverlayEdge.Start else OverlayEdge.End
    }

    fun xForEdge(edge: OverlayEdge, bounds: OverlayBounds): Int = when (edge) {
        OverlayEdge.Start -> 0
        OverlayEdge.End -> max(0, bounds.width - bounds.panelWidth)
    }

    fun clamp(position: OverlayPosition, bounds: OverlayBounds): OverlayPosition = OverlayPosition(
        position.x.coerceIn(0, max(0, bounds.width - bounds.panelWidth)),
        position.y.coerceIn(
            bounds.insetTop,
            max(bounds.insetTop, bounds.height - bounds.insetBottom - bounds.panelHeight),
        ),
    )

    fun nearestEdge(position: OverlayPosition, bounds: OverlayBounds): OverlayEdge =
        if (position.x <= (bounds.width - bounds.panelWidth) / 2) OverlayEdge.Start else OverlayEdge.End

    fun normalize(position: OverlayPosition, bounds: OverlayBounds): NormalizedPosition {
        val clamped = clamp(position, bounds)
        val maxX = max(1, bounds.width - bounds.panelWidth)
        val maxY = max(1, bounds.height - bounds.insetBottom - bounds.panelHeight - bounds.insetTop)
        return NormalizedPosition(clamped.x.toFloat() / maxX, (clamped.y - bounds.insetTop).toFloat() / maxY)
    }

    fun denormalize(saved: NormalizedPosition, bounds: OverlayBounds): OverlayPosition = clamp(
        OverlayPosition(
            (saved.x.coerceIn(0f, 1f) * max(0, bounds.width - bounds.panelWidth)).toInt(),
            bounds.insetTop + (
                saved.y.coerceIn(0f, 1f) *
                    max(0, bounds.height - bounds.insetBottom - bounds.panelHeight - bounds.insetTop)
            ).toInt(),
        ),
        bounds,
    )
}
