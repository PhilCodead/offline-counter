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

    @Test
    fun rejectsMalformedDatesAndPreservesStatus() {
        val people = parser.parse(listOf(
            "Анна Смирнова ж, 34 года (31-12-1990)",
            "Ирина Волкова ж, 30 лет (01.02.1994)",
            "Не вакцинирован",
        ))
        assertEquals(1, people.size)
        assertEquals("Не вакцинирован", people.single().status)
    }

    @Test
    fun normalizedKeyCollapsesWhitespaceAndCase() {
        assertEquals(
            Person(" Анна  Смирнова ", "01.01.1990", "Ж", "").key,
            Person("анна смирнова", "01.01.1990", "Ж", "").key,
        )
    }
}
