package app.offlinecounter

class NoticeDispatcher(
    private val systemChannel: (String) -> Unit,
    private val overlayChannel: (String) -> Unit,
) {
    fun show(message: String) {
        systemChannel(message)
        overlayChannel(message)
    }
}
