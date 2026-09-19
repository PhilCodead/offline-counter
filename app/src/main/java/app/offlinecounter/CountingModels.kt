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
    val fingerprint: Int,
)

data class OverlayUiState(
    val phase: CountingPhase = CountingPhase.Idle,
    val total: Int = 0,
    val women: Int = 0,
    val men: Int = 0,
    val status: String = "",
    val expanded: Boolean = false,
)
