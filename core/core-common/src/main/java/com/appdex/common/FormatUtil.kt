package com.appdex.common

import java.text.DecimalFormat

object FormatUtil {

    private val sizeFormat = DecimalFormat("#.##")

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.lastIndex)
        return "${sizeFormat.format(bytes / Math.pow(1024.0, index.toDouble()))} ${units[index]}"
    }

    fun formatTimestamp(millis: Long): String {
        if (millis <= 0) return ""
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }
}
