package app.offlinecounter

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ExportFileNameTest {
    @Test
    fun includesCalendarDateTimeAndActualRowCount() {
        val time = LocalDateTime.of(2026, 12, 31, 23, 59, 8, 123_000_000)
        assertEquals(
            "Счётчик_2026-12-31_23-59-08-123_записей-37.xlsx",
            ExportFileName.create(37, time),
        )
        assertEquals(
            "Счётчик_2027-01-01_00-00-00-000_записей-0.xlsx",
            ExportFileName.create(0, LocalDateTime.of(2027, 1, 1, 0, 0)),
        )
    }

    @Test
    fun exportsWithinTheSameSecondHaveDifferentMillisecondNames() {
        val time = LocalDateTime.of(2026, 9, 19, 14, 30)
        assertNotEquals(ExportFileName.create(37, time), ExportFileName.create(37, time.plusNanos(1_000_000)))
    }
}
