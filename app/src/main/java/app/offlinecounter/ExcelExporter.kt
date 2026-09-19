package app.offlinecounter

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore

object ExcelExporter {
    private const val MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private const val TEMPLATE = "empty-template-v8.xlsx"

    fun export(context: Context, people: Collection<Person>): Uri {
        val bytes = WorkbookBuilder.replaceSheet(context.assets.open(TEMPLATE).use { it.readBytes() }, people)
        require(WorkbookBuilder.isValid(bytes))

        val name = ExportFileName.create(people.size)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/OfflineCounter")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        try {
            requireNotNull(resolver.openOutputStream(uri, "w")).use { output ->
                output.write(bytes)
                output.flush()
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            check(resolver.update(uri, values, null, null) == 1) { "Unable to publish exported file" }
            return uri
        } catch (error: Exception) {
            runCatching { resolver.delete(uri, null, null) }
                .exceptionOrNull()?.let(error::addSuppressed)
            throw error
        }
    }
}
