package io.github.ktor_batterypack.validation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import java.time.chrono.JapaneseDate
import java.time.chrono.MinguoDate
import java.time.chrono.ThaiBuddhistDate
import java.time.temporal.Temporal
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

class TemporalConstraintsTest {

    companion object {
        private val NOW = Instant.parse("2026-06-15T12:00:00Z")
        private val CLOCK = Clock.fixed(NOW, ZoneOffset.UTC)
        private val TODAY = LocalDate.of(2026, 6, 15)
        private val PLUS_TWO = ZoneOffset.ofHours(2)

        private val TEMPORAL_SAMPLES: List<Sample<Temporal>> = listOf(
            Sample(NOW.minusSeconds(1), NOW, NOW.plusSeconds(1)),
            Sample(TODAY.minusDays(1), TODAY, TODAY.plusDays(1)),
            Sample(
                LocalDateTime.of(2026, 6, 15, 11, 59),
                LocalDateTime.of(2026, 6, 15, 12, 0),
                LocalDateTime.of(2026, 6, 15, 12, 1)
            ),
            Sample(LocalTime.of(11, 59), LocalTime.of(12, 0), LocalTime.of(12, 1)),
            Sample(
                OffsetDateTime.of(2026, 6, 15, 13, 59, 0, 0, PLUS_TWO),
                OffsetDateTime.of(2026, 6, 15, 14, 0, 0, 0, PLUS_TWO),
                OffsetDateTime.of(2026, 6, 15, 14, 1, 0, 0, PLUS_TWO)
            ),
            Sample(
                OffsetTime.of(11, 59, 0, 0, ZoneOffset.UTC),
                OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC),
                OffsetTime.of(12, 1, 0, 0, ZoneOffset.UTC)
            ),
            Sample(Year.of(2025), Year.of(2026), Year.of(2027)),
            Sample(YearMonth.of(2026, 5), YearMonth.of(2026, 6), YearMonth.of(2026, 7)),
            Sample(
                ZonedDateTime.of(2026, 6, 15, 13, 59, 0, 0, ZoneId.of("Europe/Warsaw")),
                ZonedDateTime.of(2026, 6, 15, 14, 0, 0, 0, ZoneId.of("Europe/Warsaw")),
                ZonedDateTime.of(2026, 6, 15, 14, 1, 0, 0, ZoneId.of("Europe/Warsaw"))
            ),
            Sample(
                HijrahDate.from(TODAY.minusDays(1)),
                HijrahDate.from(TODAY),
                HijrahDate.from(TODAY.plusDays(1))
            ),
            Sample(
                JapaneseDate.from(TODAY.minusDays(1)),
                JapaneseDate.from(TODAY),
                JapaneseDate.from(TODAY.plusDays(1))
            ),
            Sample(
                MinguoDate.from(TODAY.minusDays(1)),
                MinguoDate.from(TODAY),
                MinguoDate.from(TODAY.plusDays(1))
            ),
            Sample(
                ThaiBuddhistDate.from(TODAY.minusDays(1)),
                ThaiBuddhistDate.from(TODAY),
                ThaiBuddhistDate.from(TODAY.plusDays(1))
            ),
        )

        private val DATE_SAMPLE = Sample(
            Date.from(NOW.minusSeconds(1)),
            Date.from(NOW),
            Date.from(NOW.plusSeconds(1))
        )

        private val CALENDAR_SAMPLE = Sample(
            calendarAt(NOW.minusSeconds(1)),
            calendarAt(NOW),
            calendarAt(NOW.plusSeconds(1))
        )

        private val MONTH_DAY_SAMPLE = Sample(MonthDay.of(6, 14), MonthDay.of(6, 15), MonthDay.of(6, 16))

        private fun calendarAt(instant: Instant): Calendar {
            val calendar = GregorianCalendar(TimeZone.getTimeZone("Asia/Tokyo"))
            calendar.timeInMillis = instant.toEpochMilli()
            return calendar
        }
    }

    @Test
    fun `Past accepts only temporals before the clock`() {
        for (sample in TEMPORAL_SAMPLES) {
            assertThat(validOutcomes(sample) { prop, date, call -> Constraints.checkPast(prop, date, call) })
                .describedAs(sample.past::class.simpleName)
                .containsExactly(true, false, false)
        }
    }

    @Test
    fun `PastOrPresent accepts temporals up to the clock`() {
        for (sample in TEMPORAL_SAMPLES) {
            assertThat(validOutcomes(sample) { prop, date, call -> Constraints.checkPastOrPresent(prop, date, call) })
                .describedAs(sample.past::class.simpleName)
                .containsExactly(true, true, false)
        }
    }

    @Test
    fun `Future accepts only temporals after the clock`() {
        for (sample in TEMPORAL_SAMPLES) {
            assertThat(validOutcomes(sample) { prop, date, call -> Constraints.checkFuture(prop, date, call) })
                .describedAs(sample.past::class.simpleName)
                .containsExactly(false, false, true)
        }
    }

    @Test
    fun `FutureOrPresent accepts temporals from the clock on`() {
        for (sample in TEMPORAL_SAMPLES) {
            assertThat(validOutcomes(sample) { prop, date, call -> Constraints.checkFutureOrPresent(prop, date, call) })
                .describedAs(sample.past::class.simpleName)
                .containsExactly(false, true, true)
        }
    }

    @Test
    fun `java util Date is compared with the clock instant`() {
        assertThat(validOutcomes(DATE_SAMPLE) { prop, date, call -> Constraints.checkPast(prop, date, call) })
            .containsExactly(true, false, false)
        assertThat(validOutcomes(DATE_SAMPLE) { prop, date, call -> Constraints.checkPastOrPresent(prop, date, call) })
            .containsExactly(true, true, false)
        assertThat(validOutcomes(DATE_SAMPLE) { prop, date, call -> Constraints.checkFuture(prop, date, call) })
            .containsExactly(false, false, true)
        assertThat(validOutcomes(DATE_SAMPLE) { prop, date, call -> Constraints.checkFutureOrPresent(prop, date, call) })
            .containsExactly(false, true, true)
    }

    @Test
    fun `Calendar is compared with the clock instant regardless of its time zone`() {
        assertThat(validOutcomes(CALENDAR_SAMPLE) { prop, date, call -> Constraints.checkPast(prop, date, call) })
            .containsExactly(true, false, false)
        assertThat(validOutcomes(CALENDAR_SAMPLE) { prop, date, call -> Constraints.checkPastOrPresent(prop, date, call) })
            .containsExactly(true, true, false)
        assertThat(validOutcomes(CALENDAR_SAMPLE) { prop, date, call -> Constraints.checkFuture(prop, date, call) })
            .containsExactly(false, false, true)
        assertThat(validOutcomes(CALENDAR_SAMPLE) { prop, date, call -> Constraints.checkFutureOrPresent(prop, date, call) })
            .containsExactly(false, true, true)
    }

    @Test
    fun `MonthDay is compared with the clock date`() {
        assertThat(validOutcomes(MONTH_DAY_SAMPLE) { prop, date, call -> Constraints.checkPast(prop, date, call) })
            .containsExactly(true, false, false)
        assertThat(validOutcomes(MONTH_DAY_SAMPLE) { prop, date, call -> Constraints.checkPastOrPresent(prop, date, call) })
            .containsExactly(true, true, false)
        assertThat(validOutcomes(MONTH_DAY_SAMPLE) { prop, date, call -> Constraints.checkFuture(prop, date, call) })
            .containsExactly(false, false, true)
        assertThat(validOutcomes(MONTH_DAY_SAMPLE) { prop, date, call -> Constraints.checkFutureOrPresent(prop, date, call) })
            .containsExactly(false, true, true)
    }

    @Test
    fun `temporal violations are reported under the constraint name`() {
        val call = ValidationCall(clock = CLOCK)

        Constraints.checkPast("issuedAt", TODAY, call)
        Constraints.checkFutureOrPresent("dueAt", TODAY.minusDays(1), call)

        val properties = call.finishObject()?.properties
        assertThat(properties?.get("issuedAt")).isEqualTo(FieldConstraintError(listOf(SingleConstraintError("Past"))))
        assertThat(properties?.get("dueAt")).isEqualTo(FieldConstraintError(listOf(SingleConstraintError("FutureOrPresent"))))
    }

    @Test
    fun `nested and list item calls use the clock of the root call`() {
        val call = ValidationCall(clock = CLOCK)

        val nested = call.nestedProperty("contract")
        val item = nested.listItem()

        assertThat(nested.clock).isSameAs(CLOCK)
        assertThat(item.clock).isSameAs(CLOCK)
    }

    private fun <T> validOutcomes(sample: Sample<T>, check: (String, T, ValidationCall) -> Unit): List<Boolean> {
        return listOf(sample.past, sample.present, sample.future).map { value ->
            val call = ValidationCall(clock = CLOCK)
            check("date", value, call)
            call.finishObject() == null
        }
    }

    private data class Sample<T>(val past: T, val present: T, val future: T)
}
