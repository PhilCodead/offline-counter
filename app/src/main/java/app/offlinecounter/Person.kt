package app.offlinecounter

import java.util.Locale

data class Person(
    val fio: String,
    val birthDate: String,
    val sex: String,
    val status: String,
) {
    val normalizedFio: String = fio.trim().split(Regex("\\s+")).joinToString(" ")
    val key: String = "${normalizedFio.lowercase(Locale.ROOT)}|$birthDate"
}
