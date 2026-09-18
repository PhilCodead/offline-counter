package app.offlinecounter

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonParserTest {
    private val parser = PersonParser()

    @Test
    fun parsesInlineAndSeparatedNames() {
        val result = parser.parse(
            listOf(
                "Шелапутина Ирина Олеговна ж, 50 лет (28.04.1976)",
                "Вакцинирован",
                "Светлицкая Екатерина Викторовна",
                "м, 61 год (13.01.1965)",
                "Вакцинирован",
            ),
        )

        assertEquals(2, result.size)
        assertEquals("Ж", result[0].sex)
        assertEquals("28.04.1976", result[0].birthDate)
        assertEquals("Светлицкая Екатерина Викторовна", result[1].fio)
        assertEquals("М", result[1].sex)
    }
}
