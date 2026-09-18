package app.offlinecounter

import android.view.accessibility.AccessibilityEvent

class ScrollTracker {
    var fromIndex = -1
        private set
    var toIndex = -1
        private set
    var itemCount = -1
        private set
    var scrollY = -1
        private set
    var maxScrollY = -1
        private set
    var serial = 0
        private set

    fun reset() {
        fromIndex = -1
        toIndex = -1
        itemCount = -1
        scrollY = -1
        maxScrollY = -1
        serial = 0
    }

    fun update(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return
        update(event.fromIndex, event.toIndex, event.itemCount, event.scrollY, event.maxScrollY)
    }

    internal fun update(from: Int, to: Int, count: Int, y: Int, maxY: Int) {
        fromIndex = from
        toIndex = to
        itemCount = count
        scrollY = y
        maxScrollY = maxY
        serial++
    }

    fun isAtTop(): Boolean =
        (itemCount > 0 && fromIndex == 0) || (maxScrollY > 0 && scrollY == 0)

    fun isAtBottom(): Boolean =
        (itemCount > 0 && toIndex == itemCount - 1) || (maxScrollY > 0 && scrollY >= maxScrollY)
}
