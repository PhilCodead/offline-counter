package app.offlinecounter

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayPlacementTest {
    @Test
    fun onlySnapsWithinThresholdAndPreservesMiddlePosition() {
        val b = OverlayBounds(1080, 2200, 300, 160, 80, 120)
        assertEquals(null, OverlayPlacement.snapEdge(OverlayPosition(390, 500), b, 24))
        assertEquals(OverlayEdge.Start, OverlayPlacement.snapEdge(OverlayPosition(24, 500), b, 24))
        assertEquals(null, OverlayPlacement.snapEdge(OverlayPosition(25, 500), b, 24))
        assertEquals(OverlayEdge.End, OverlayPlacement.snapEdge(OverlayPosition(756, 500), b, 24))
        assertEquals(null, OverlayPlacement.snapEdge(OverlayPosition(755, 500), b, 24))
        val middle = OverlayPosition(390, 500)
        assertEquals(middle, OverlayPlacement.denormalize(OverlayPlacement.normalize(middle, b), b))
        assertEquals(OverlayPosition(180, 500), OverlayPlacement.clamp(middle, b.copy(panelWidth = 900)))
    }

    private val bounds = OverlayBounds(1080, 2200, 600, 160, 80, 120)

    @Test
    fun clampsPanelInsideVisibleBounds() {
        assertEquals(OverlayPosition(0, 80), OverlayPlacement.clamp(OverlayPosition(-50, -20), bounds))
        assertEquals(OverlayPosition(480, 1920), OverlayPlacement.clamp(OverlayPosition(900, 2200), bounds))
    }

    @Test
    fun choosesNearestHorizontalEdge() {
        assertEquals(OverlayEdge.Start, OverlayPlacement.nearestEdge(OverlayPosition(100, 500), bounds))
        assertEquals(OverlayEdge.End, OverlayPlacement.nearestEdge(OverlayPosition(450, 500), bounds))
    }

    @Test
    fun keepsSelectedEdgeWhenPanelWidthChanges() {
        val compact = bounds.copy(panelWidth = 300)
        val expanded = bounds.copy(panelWidth = 760)

        assertEquals(0, OverlayPlacement.xForEdge(OverlayEdge.Start, compact))
        assertEquals(780, OverlayPlacement.xForEdge(OverlayEdge.End, compact))
        assertEquals(320, OverlayPlacement.xForEdge(OverlayEdge.End, expanded))
    }
}
