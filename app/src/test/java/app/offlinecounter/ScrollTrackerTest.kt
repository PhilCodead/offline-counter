package app.offlinecounter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollTrackerTest {
    @Test
    fun detectsListBoundariesFromIndices() {
        val tracker = ScrollTracker()
        tracker.update(0, 9, 200, -1, -1)
        assertTrue(tracker.isAtTop())
        assertFalse(tracker.isAtBottom())

        tracker.update(190, 199, 200, -1, -1)
        assertFalse(tracker.isAtTop())
        assertTrue(tracker.isAtBottom())
    }

    @Test
    fun detectsBoundariesFromScrollCoordinates() {
        val tracker = ScrollTracker()
        tracker.update(-1, -1, -1, 0, 5000)
        assertTrue(tracker.isAtTop())

        tracker.update(-1, -1, -1, 5000, 5000)
        assertTrue(tracker.isAtBottom())
    }

    @Test
    fun incompleteValuesAreNotBoundaries() {
        val tracker = ScrollTracker()
        tracker.update(-1, -1, -1, -1, -1)
        assertFalse(tracker.isAtTop())
        assertFalse(tracker.isAtBottom())
    }

    @Test
    fun resetClearsEverySignal() {
        val tracker = ScrollTracker()
        tracker.update(0, 9, 10, 0, 100)
        tracker.reset()
        assertFalse(tracker.isAtTop())
        assertFalse(tracker.isAtBottom())
    }
}
