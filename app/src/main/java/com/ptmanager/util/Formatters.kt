package com.ptmanager.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val BYTE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB", "PB")

    fun bytes(value: Long): String {
        if (value < 0) return "-"
        var v = value.toDouble()
        var unit = 0
        while (v >= 1024 && unit < BYTE_UNITS.size - 1) {
            v /= 1024
            unit++
        }
        return if (unit == 0) "${v.toLong()} ${BYTE_UNITS[unit]}" else String.format(Locale.US, "%.2f %s", v, BYTE_UNITS[unit])
    }

    fun speed(value: Long): String = if (value >= 0) "${bytes(value)}/s" else "-"

    fun ratio(value: Double): String = if (value == Double.POSITIVE_INFINITY) "∞" else String.format(Locale.US, "%.3f", value)

    fun percent(progress: Double): String = String.format(Locale.US, "%.1f%%", progress * 100)

    fun eta(seconds: Long): String {
        if (seconds < 0) return "-"
        if (seconds >= 8640000L || seconds >= 86400L * 100) return "∞"
        val d = seconds / 86400
        val h = (seconds % 86400) / 3600
        val m = (seconds % 3600) / 60
        return when {
            d > 0 -> "${d}天${h}时"
            h > 0 -> "${h}时${m}分"
            else -> "${m}分"
        }
    }

    fun date(epochSeconds: Long): String {
        if (epochSeconds <= 0) return "-"
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(epochSeconds * 1000))
    }

    fun magnetHash(magnet: String): String {
        val match = Regex("btih:([a-fA-F0-9]{40})").find(magnet)
        return match?.groupValues?.get(1)?.uppercase(Locale.US).orEmpty()
    }

    fun looksLikeUrlOrMagnet(text: String): Boolean =
        text.contains("magnet:?") || text.matches(Regex("https?://.+"))
}

object StateColor {
    val downloading = 0xFF4CAF50.toInt()
    val seeding = 0xFF2196F3.toInt()
    val paused = 0xFFFF9800.toInt()
    val error = 0xFFF44336.toInt()
    val checking = 0xFF9C27B0.toInt()
    val queued = 0xFF795548.toInt()
    val default = 0xFF9E9E9E.toInt()

    fun colorFor(state: String): Int = when {
        state == "downloading" || state == "forcedDL" || state == "metaDL" -> downloading
        state == "uploading" || state == "forcedUP" || state == "stalledUP" -> seeding
        state.startsWith("paused") -> paused
        state == "error" || state == "missingFiles" -> error
        state.startsWith("checking") || state == "moving" || state == "allocating" -> checking
        state.startsWith("queued") -> queued
        else -> default
    }
}