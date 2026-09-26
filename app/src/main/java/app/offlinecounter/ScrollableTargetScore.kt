package app.offlinecounter

internal data class ScrollableTargetScore(
    val visiblePeople: Int,
    val depth: Int,
    val vertical: Boolean,
) : Comparable<ScrollableTargetScore> {
    override fun compareTo(other: ScrollableTargetScore): Int =
        compareValuesBy(this, other, { it.visiblePeople > 0 }, { it.vertical }, { it.depth }, { it.visiblePeople })
}
