package app.offlinecounter

data class OverlayPresentation(
    val showDataActions: Boolean,
) {
    companion object {
        fun forTotal(total: Int) = OverlayPresentation(showDataActions = total > 0)
    }
}
