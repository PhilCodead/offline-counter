package app.offlinecounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayUiStateTest {
    @Test
    fun primaryLabelReflectsPhaseAndCount() {
        assertEquals("Подсчёт", OverlayUiState().primaryLabel)
        assertEquals("42", OverlayUiState(phase = CountingPhase.Collecting, total = 42).primaryLabel)
        assertEquals("Готово", OverlayUiState(phase = CountingPhase.Completed, total = 42).primaryLabel)
    }

    @Test
    fun onlyClearRequiresConfirmation() {
        assertTrue(OverlayAction.Clear.requiresConfirmation)
        assertFalse(OverlayAction.Disable.requiresConfirmation)
        assertFalse(OverlayAction.Share.requiresConfirmation)
    }

    @Test
    fun panelUsesOpaqueSurfacesInBothStates() {
        assertEquals(GlassMode.CompactOpaque, glassMode(false))
        assertEquals(GlassMode.ExpandedOpaque, glassMode(true))
    }
}
