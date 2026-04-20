package biz.smt_life.android.feature.outbound.proxyshipment

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object ProxyShipmentDateFormatter {
    private val apiFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val displayFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.JAPAN)
    private val tokyoZone = ZoneId.of("Asia/Tokyo")

    fun toDisplay(apiDate: String): String = try {
        LocalDate.parse(apiDate, apiFormatter).format(displayFormatter)
    } catch (_: DateTimeParseException) {
        apiDate.replace('-', '/')
    }

    fun fromEpochMillis(epochMillis: Long?): String? {
        if (epochMillis == null) return null
        return Instant.ofEpochMilli(epochMillis)
            .atZone(tokyoZone)
            .toLocalDate()
            .format(apiFormatter)
    }
}
