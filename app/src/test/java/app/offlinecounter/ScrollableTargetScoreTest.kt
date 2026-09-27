package app.offlinecounter

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollableTargetScoreTest {
    @Test
    fun prefersTheNestedPatientListOverAnOuterScrollablePage() {
        val outer = ScrollableTargetScore(visiblePeople = 5, depth = 0, vertical = true)
        val list = ScrollableTargetScore(visiblePeople = 5, depth = 2, vertical = true)
        assertEquals(list, maxOf(outer, list))
    }

    @Test
    fun prefersRecognizedRecordsOverUnrelatedScrollableTabs() {
        val tabs = ScrollableTargetScore(visiblePeople = 0, depth = 3, vertical = false)
        val list = ScrollableTargetScore(visiblePeople = 5, depth = 1, vertical = false)
        assertEquals(list, maxOf(tabs, list))
    }

    @Test
    fun usesVerticalScrollingWhenNeitherNodeHasRecords() {
        val tabs = ScrollableTargetScore(visiblePeople = 0, depth = 3, vertical = false)
        val list = ScrollableTargetScore(visiblePeople = 0, depth = 1, vertical = true)
        assertEquals(list, maxOf(tabs, list))
    }
}
