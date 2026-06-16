package com.example.hubrise.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object TimeUtils {

    /** Parses a Django-issued UTC ISO-8601 timestamp (e.g. "...Z" or "...+00:00",
     * with or without microseconds) and renders it as "just now" / "5m" / "3h" / "2d". */
    fun formatRelativeTime(isoDate: String): String {
        return try {
            val datePart = isoDate.substringBefore(".").substringBefore("+").removeSuffix("Z")
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(datePart) ?: return isoDate
            val diff = (System.currentTimeMillis() - date.time).coerceAtLeast(0)
            when {
                diff < 60_000 -> "just now"
                diff < 3_600_000 -> "${diff / 60_000}m"
                diff < 86_400_000 -> "${diff / 3_600_000}h"
                else -> "${diff / 86_400_000}d"
            }
        } catch (e: Exception) {
            isoDate
        }
    }
}
