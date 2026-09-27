package app.offlinecounter

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object WorkbookBuilder {
    private const val SHEET = "xl/worksheets/sheet1.xml"
    private const val WORKBOOK = "xl/workbook.xml"

    fun sheetXml(people: Collection<Person>): String {
        val records = people.toList()
        val men = records.count { it.sex == "М" }
        val women = records.count { it.sex == "Ж" }
        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
            row(1) {
                cell("A1", "ФИО")
                cell("B1", "Дата рождения")
                cell("C1", "Пол")
                cell("D1", "Статус")
                cell("F1", "Общее: ${records.size}")
            }
            repeat(maxOf(records.size, 2)) { index ->
                val number = index + 2
                row(number) {
                    records.getOrNull(index)?.let { person ->
                        cell("A$number", person.normalizedFio)
                        cell("B$number", person.birthDate)
                        cell("C$number", person.sex)
                        cell("D$number", person.status)
                    }
                    when (number) {
                        2 -> cell("F2", "М: $men")
                        3 -> cell("F3", "Ж: $women")
                    }
                }
            }
            append("</sheetData></worksheet>")
        }
    }

    fun replaceSheet(template: ByteArray, people: Collection<Person>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipInputStream(ByteArrayInputStream(template)).use { input ->
            ZipOutputStream(output).use { zip ->
                var entry = input.nextEntry
                while (entry != null) {
                    val bytes = if (entry.name == SHEET) {
                        sheetXml(people).toByteArray(StandardCharsets.UTF_8)
                    } else {
                        input.readBytes()
                    }
                    zip.putNextEntry(ZipEntry(entry.name))
                    zip.write(bytes)
                    zip.closeEntry()
                    input.closeEntry()
                    entry = input.nextEntry
                }
            }
        }
        return output.toByteArray()
    }

    fun isValid(bytes: ByteArray): Boolean {
        var sheet = false
        var workbook = false
        ZipInputStream(ByteArrayInputStream(bytes)).use { input ->
            var entry = input.nextEntry
            while (entry != null) {
                sheet = sheet || entry.name == SHEET
                workbook = workbook || entry.name == WORKBOOK
                input.closeEntry()
                entry = input.nextEntry
            }
        }
        return sheet && workbook
    }

    private inline fun StringBuilder.row(number: Int, body: StringBuilder.() -> Unit) {
        append("<row r=\"").append(number).append("\">")
        body()
        append("</row>")
    }

    private fun StringBuilder.cell(reference: String, value: String) {
        append("<c r=\"")
            .append(reference)
            .append("\" t=\"inlineStr\"><is><t>")
            .append(value.escapeXml())
            .append("</t></is></c>")
    }

    private fun String.escapeXml() = buildString(length) {
        this@escapeXml.codePoints().forEach { codePoint ->
            when (codePoint) {
                '&'.code -> append("&amp;")
                '<'.code -> append("&lt;")
                '>'.code -> append("&gt;")
                '"'.code -> append("&quot;")
                '\''.code -> append("&apos;")
                0x09, 0x0A, 0x0D,
                in 0x20..0xD7FF, in 0xE000..0xFFFD, in 0x10000..0x10FFFF -> appendCodePoint(codePoint)
            }
        }
    }
}
