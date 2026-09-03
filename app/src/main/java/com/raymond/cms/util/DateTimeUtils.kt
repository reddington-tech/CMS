package com.raymond.cms.util

import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    val NAIROBI_ZONE: TimeZone = TimeZone.getTimeZone("Africa/Nairobi")

    fun getFormat(pattern: String, locale: Locale = Locale.getDefault()): SimpleDateFormat {
        return SimpleDateFormat(pattern, locale).apply {
            timeZone = NAIROBI_ZONE
        }
    }

    fun getCalendar(): Calendar {
        return Calendar.getInstance(NAIROBI_ZONE).apply {
            firstDayOfWeek = Calendar.MONDAY
        }
    }
    
    fun getCalendar(timeMillis: Long): Calendar {
        return getCalendar().apply {
            timeInMillis = timeMillis
        }
    }
}
