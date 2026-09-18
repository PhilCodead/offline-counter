package app.offlinecounter

import org.junit.Assert.assertTrue
import org.junit.Test

class WorkbookBuilderTest {
    @Test
    fun escapesXmlAndWritesTotals() {
        val xml = WorkbookBuilder.sheetXml(listOf(Person("Иванов & Петров", "01.01.1990", "М", "<готов>")))
        assertTrue(xml.contains("Иванов &amp; Петров"))
        assertTrue(xml.contains("&lt;готов&gt;"))
        assertTrue(xml.contains("Общее: 1"))
        assertTrue(xml.contains("М: 1"))
        assertTrue(xml.contains("Ж: 0"))
    }

    @Test
    fun emptyDataStillWritesBothSexTotals() {
        val xml = WorkbookBuilder.sheetXml(emptyList())
        assertTrue(xml.contains("М: 0"))
        assertTrue(xml.contains("Ж: 0"))
    }
}
