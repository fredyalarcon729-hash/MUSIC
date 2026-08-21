package com.example.core.model

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class Lyrics(
    val lines: List<LyricLine> = emptyList(),
    val isSynced: Boolean = false,
    val plainText: String? = null
) {
    companion object {
        fun parseLrc(lrc: String): List<LyricLine> {
            val lines = mutableListOf<LyricLine>()
            // Support formats like [00:00.00], [00:00.000], [00:00:00], etc.
            val regex = Regex("\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})\\](.*)")
            lrc.lines().forEach { line ->
                val match = regex.find(line)
                if (match != null) {
                    val min = match.groupValues[1].toLong()
                    val sec = match.groupValues[2].toLong()
                    val msPart = match.groupValues[3]
                    val ms = if (msPart.length == 2) msPart.toLong() * 10 else msPart.toLong()
                    val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
                    val text = match.groupValues[4].trim()
                    if (text.isNotBlank()) {
                        lines.add(LyricLine(totalMs, text))
                    }
                }
            }
            // Ensure correct order and handle multiple lines at same timestamp if any
            return lines.sortedBy { it.timeMs }
        }
    }
}
