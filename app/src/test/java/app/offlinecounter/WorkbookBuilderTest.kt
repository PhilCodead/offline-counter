package app.offlinecounter

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals

class WorkbookBuilderTest {
    @Test
    fun invalidXmlCharactersAreRemovedWithoutLosingUnicode() {
        val status = "готов\u0000\u0001\uFFFF\uFFFE\uD800 ✅"
        val xml = WorkbookBuilder.sheetXml(listOf(Person("Иванов Иван", "01.01.1990", "М", status)))
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        assertEquals("готов ✅", document.getElementsByTagName("t").item(8).textContent.trim())
    }

    @Test
    fun exportPreservesTemplateAndProducesParseableWorksheets() {
        fun entries(bytes: ByteArray): Map<String, ByteArray> {
            val result = linkedMapOf<String, ByteArray>()
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    result[entry.name] = zip.readBytes()
                    entry = zip.nextEntry
                }
            }
            return result
        }
        val template = File("src/main/assets/empty-template-v8.xlsx").readBytes()
        val original = entries(template)
        val result = WorkbookBuilder.replaceSheet(template, listOf(Person("Иванов Иван", "01.01.1990", "М", "")))
        val exported = entries(result)
        assertTrue(WorkbookBuilder.isValid(result))
        assertEquals(original.keys, exported.keys)
        original.filterKeys { it != "xl/worksheets/sheet1.xml" }.forEach { (name, bytes) ->
            assertArrayEquals(name, bytes, exported.getValue(name))
        }
        val parser = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        exported.filterKeys { it.endsWith(".xml") }.values.forEach {
            parser.parse(ByteArrayInputStream(it))
        }
    }

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
    @Test
    fun sexTotalsStayDirectlyUnderGrandTotalForLongLists() {
        val people = List(40) { Person("Пациент Номер", "01.01.1990", if (it % 2 == 0) "Ж" else "М", "") }
        val xml = WorkbookBuilder.sheetXml(people)
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        val rows = document.getElementsByTagName("row")
        fun cellsAt(index: Int): List<String> {
            val cells = rows.item(index).childNodes
            return (0 until cells.length).map { cells.item(it).textContent }
        }
        assertEquals("Общее: 40", cellsAt(0).last())
        assertEquals("М: 20", cellsAt(1).last())
        assertEquals("Ж: 20", cellsAt(2).last())
        assertEquals("Пациент Номер", cellsAt(3).first())
    }

}
