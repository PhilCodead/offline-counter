package com.example.offlinecounter

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ExcelExporter {
    private const val MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private const val TEMPLATE = "empty-template-v8.xlsx"
    private const val SHEET = "xl/worksheets/sheet1.xml"

    fun export(context: Context, people: Collection<Person>): Uri {
        val bytes = buildWorkbook(context, people)
        require(isValidWorkbook(bytes))

        val name = "patients_export_${System.currentTimeMillis()}.xlsx"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/OfflineCounter")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        try {
            resolver.openOutputStream(uri, "w")!!.use { output ->
                output.write(bytes)
                output.flush()
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    internal fun sheetXml(people: Collection<Person>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
        row(1) {
            cell("A1", "ФИО")
            cell("B1", "Дата рождения")
            cell("C1", "Пол")
            cell("D1", "Статус")
            cell("F1", "Общее: ${people.size}")
        }
        people.forEachIndexed { index, person ->
            val row = index + 2
            row(row) {
                cell("A$row", person.fio)
                cell("B$row", person.birthDate)
                cell("C$row", person.sex)
                cell("D$row", person.status)
                if (row == 2) cell("F2", "М: ${people.count { it.sex == "М" }}")
                if (row == 3) cell("F3", "Ж: ${people.count { it.sex == "Ж" }}")
            }
        }
        if (people.isEmpty()) {
            row(2) { cell("F2", "М: 0") }
            row(3) { cell("F3", "Ж: 0") }
        } else if (people.size == 1) {
            row(3) { cell("F3", "Ж: ${people.count { it.sex == "Ж" }}") }
        }
        append("</sheetData></worksheet>")
    }

    private fun buildWorkbook(context: Context, people: Collection<Person>): ByteArray {
        val template = context.assets.open(TEMPLATE)
        val output = ByteArrayOutputStream()
        ZipInputStream(template).use { input ->
            ZipOutputStream(output).use { zip ->
                var entry = input.nextEntry
                while (entry != null) {
                    val replacement = if (entry.name == SHEET) sheetXml(people).toByteArray(StandardCharsets.UTF_8) else input.readBytes()
                    zip.putNextEntry(ZipEntry(entry.name))
                    zip.write(replacement)
                    zip.closeEntry()
                    input.closeEntry()
                    entry = input.nextEntry
                }
            }
        }
        return output.toByteArray()
    }

    private fun isValidWorkbook(bytes: ByteArray): Boolean {
        var hasSheet = false
        var hasWorkbook = false
        ZipInputStream(ByteArrayInputStream(bytes)).use { input ->
            var entry = input.nextEntry
            while (entry != null) {
                if (entry.name == SHEET) hasSheet = true
                if (entry.name == "xl/workbook.xml") hasWorkbook = true
                input.closeEntry()
                entry = input.nextEntry
            }
        }
        return hasSheet && hasWorkbook
    }

    private inline fun StringBuilder.row(number: Int, body: StringBuilder.() -> Unit) {
        append("<row r=\"").append(number).append("\">")
        body()
        append("</row>")
    }

    private fun StringBuilder.cell(ref: String, value: String) {
        append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t>")
        append(value.xmlEscape())
        append("</t></is></c>")
    }

    private fun String.xmlEscape(): String = buildString(length) {
        for (char in this@xmlEscape) when (char) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(char)
        }
    }
}
