package app.offlinecounter

enum class CountingPhase { Idle, Rewinding, Collecting, Completed, Error }

sealed interface CountingCommand {
    data object ScrollBackward : CountingCommand
    data object ScrollForward : CountingCommand
    data object Wait : CountingCommand
    data object Complete : CountingCommand
    data class Fail(val message: String) : CountingCommand
}

data class FrameSnapshot(
    val people: List<Person>,
    val atTop: Boolean,
    val atBottom: Boolean,
    val fingerprint: Int,
)

data class OverlayUiState(
    val phase: CountingPhase = CountingPhase.Idle,
    val total: Int = 0,
    val women: Int = 0,
    val men: Int = 0,
    val status: String = "",
    val expanded: Boolean = false,
) {
    val primaryLabel: String
        get() = when (phase) {
            CountingPhase.Collecting, CountingPhase.Rewinding -> total.toString()
            CountingPhase.Completed -> "Готово"
            else -> "Подсчёт"
        }
}

enum class OverlayAction(val requiresConfirmation: Boolean) {
    Count(false), Export(false), Share(false), Expand(false), StopOrRecount(false), Clear(true), Disable(true)
}

enum class GlassMode { CompactBlur, ExpandedBlur, CompactOpaque, ExpandedOpaque }

fun glassMode(expanded: Boolean, blurAvailable: Boolean): GlassMode = when {
    expanded && blurAvailable -> GlassMode.ExpandedBlur
    expanded -> GlassMode.ExpandedOpaque
    blurAvailable -> GlassMode.CompactBlur
    else -> GlassMode.CompactOpaque
}
