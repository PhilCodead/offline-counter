package app.offlinecounter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ExportFileName {
    private val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS", Locale.ROOT)

    fun create(count: Int, time: LocalDateTime = LocalDateTime.now()): String {
        require(count >= 0)
        return "Счётчик_${time.format(timestamp)}_записей-$count.xlsx"
    }
}
