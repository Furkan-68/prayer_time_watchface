package com.ercan.smartwatch.domain

import com.google.common.truth.Truth.assertThat
import com.ercan.smartwatch.data.api.DateDto
import com.ercan.smartwatch.data.api.GregorianDateDto
import com.ercan.smartwatch.data.api.MetaDto
import com.ercan.smartwatch.data.api.TimingsDataDto
import org.junit.Test

class PrayerTimeParserTest {
    @Test
    fun parsesTimingValuesWithSuffix() {
        val dto = TimingsDataDto(
            timings = mapOf(
                "Fajr" to "05:42 (+03)",
                "Dhuhr" to "12:34",
                "Asr" to "15:27 (TRT)",
                "Maghrib" to "17:58",
                "Isha" to "19:15"
            ),
            date = DateDto(gregorian = GregorianDateDto(date = "13-02-2026")),
            meta = MetaDto(timezone = "Europe/Istanbul")
        )

        val parsed = PrayerTimeParser.parse(dto)

        assertThat(parsed.fajr.hour).isEqualTo(5)
        assertThat(parsed.fajr.minute).isEqualTo(42)
        assertThat(parsed.asr.hour).isEqualTo(15)
        assertThat(parsed.asr.minute).isEqualTo(27)
        assertThat(parsed.timezone).isEqualTo("Europe/Istanbul")
    }
}
