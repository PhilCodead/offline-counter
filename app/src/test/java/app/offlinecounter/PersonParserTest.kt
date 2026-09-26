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
    @Test
    fun parsesAcceptedListWhenDemographicsAndBirthDateAreSeparateNodes() {
        val people = parser.parse(listOf(
            "Принятые",
            "Строганова Маргарита Алексеевна",
            "ж, 58 лет",
            "(20.08.1968)",
            "Флю-М (Вакцина гриппозная инактивированная расщепленная) р-р для в/м 1 доза 0.5",
            "22.09.2026",
            "Кишиневский Николай Андреевич",
            "м, 20 лет",
            "(03.02.2006)",
            "Флю-М (Вакцина гриппозная инактивированная расщепленная) р-р для в/м 1 доза 0.5",
            "22.09.2026",
        ))
        assertEquals(2, people.size)
        assertEquals("Строганова Маргарита Алексеевна", people[0].fio)
        assertEquals("20.08.1968", people[0].birthDate)
        assertEquals("Ж", people[0].sex)
        assertEquals("Кишиневский Николай Андреевич", people[1].fio)
        assertEquals("М", people[1].sex)
    }

}
