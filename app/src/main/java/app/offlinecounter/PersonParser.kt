package app.offlinecounter

import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class PersonParser {
    fun snapshot(root: AccessibilityNodeInfo): FrameSnapshot {
        val text = ArrayList<String>(64)
        collectText(root, text)
        return FrameSnapshot(parse(text), text.fold(1) { hash, value -> 31 * hash + value.hashCode() })
    }

    internal fun parse(text: List<String>): List<Person> {
        val result = ArrayList<Person>()
        text.forEachIndexed { index, raw ->
            val open = raw.indexOf('(')
            if (open < 0) return@forEachIndexed
            val close = raw.indexOf(')', open + 1)
            if (close < 0) return@forEachIndexed

            val birthDate = raw.substring(open + 1, close)
            if (!isDate(birthDate)) return@forEachIndexed

            val lower = raw.lowercase(Locale.ROOT)
            val femaleIndex = lower.indexOf("ж,")
            val maleIndex = lower.indexOf("м,")
            val sexIndex = when {
                femaleIndex >= 0 -> femaleIndex
                maleIndex >= 0 -> maleIndex
                else -> return@forEachIndexed
            }
            val sex = if (femaleIndex >= 0) "Ж" else "М"

            val inlineName = raw.substring(0, sexIndex).trim().takeIf(::isName)
            val fio = inlineName ?: findPreviousName(text, index) ?: return@forEachIndexed
            val status = findStatus(text, index)
            result += Person(fio.trim().split(Regex("\\s+")).joinToString(" "), birthDate, sex, status)
        }
        return result
    }

    private fun findPreviousName(text: List<String>, index: Int): String? {
        for (candidateIndex in index - 1 downTo maxOf(0, index - 3)) {
            val candidate = text[candidateIndex].trim()
            if (isName(candidate)) return candidate
        }
        return null
    }

    private fun findStatus(text: List<String>, index: Int): String {
        val end = minOf(text.lastIndex, index + 5)
        for (candidateIndex in index..end) {
            val candidate = text[candidateIndex].lowercase(Locale.ROOT)
            if ("не вакцинирован" in candidate) return "Не вакцинирован"
            if ("вакцинирован" in candidate) return "Вакцинирован"
        }
        return ""
    }

    private fun isDate(value: String): Boolean {
        if (value.length != 10 || value[2] != '.' || value[5] != '.') return false
        return value.indices.all { index -> index == 2 || index == 5 || value[index].isDigit() }
    }

    private fun isName(value: String): Boolean {
        val normalized = value.trim()
        if (normalized.length !in 5..120 || normalized.none(Char::isWhitespace)) return false
        if (normalized.any(Char::isDigit)) return false
        return normalized.split(Regex("\\s+")).size in 2..5
    }

    private fun collectText(node: AccessibilityNodeInfo?, output: MutableList<String>) {
        if (node == null) return
        node.text?.toString()?.trim()?.takeIf(String::isNotEmpty)?.let(output::add)
        for (index in 0 until node.childCount) collectText(node.getChild(index), output)
    }
}
