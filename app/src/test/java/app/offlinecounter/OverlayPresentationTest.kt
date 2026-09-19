package app.offlinecounter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayPresentationTest {
    @Test
    fun dataActionsStayHiddenUntilPeopleAreFound() {
        assertFalse(OverlayPresentation.forTotal(0).showDataActions)
        assertTrue(OverlayPresentation.forTotal(1).showDataActions)
    }
}
