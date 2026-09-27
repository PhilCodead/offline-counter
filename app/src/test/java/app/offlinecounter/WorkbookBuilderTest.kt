package app.offlinecounter

import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkbookBuilderTest {
    @Test
    fun invalidXmlCharactersAreRemovedWithoutLosingUnicode() {
        val status = "готов\u0000\u0001\uFFFF\uFFFE\uD800 ✅"
        val xml = WorkbookBuilder.sheetXml(listOf(Person("Иванов Иван", "01.01.1990", "М", status)))
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        assertEquals("готов ✅", cell(document, "D2"))
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
    fun peopleStartDirectlyUnderHeadersWhileSexTotalsRemainInColumnF() {
        val people = List(40) { Person("Пациент $it", "01.01.1990", if (it % 2 == 0) "Ж" else "М", "") }
        val xml = WorkbookBuilder.sheetXml(people)
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        assertEquals("Общее: 40", cell(document, "F1"))
        assertEquals("Пациент 0", cell(document, "A2"))
        assertEquals("М: 20", cell(document, "F2"))
        assertEquals("Пациент 1", cell(document, "A3"))
        assertEquals("Ж: 20", cell(document, "F3"))
        assertEquals("Пациент 39", cell(document, "A41"))
    }

    private fun cell(document: org.w3c.dom.Document, reference: String): String {
        val cells = document.getElementsByTagName("c")
        return (0 until cells.length).firstNotNullOfOrNull { index ->
            cells.item(index).takeIf { it.attributes.getNamedItem("r").nodeValue == reference }?.textContent
        } ?: error("Missing cell $reference")
    }
}
