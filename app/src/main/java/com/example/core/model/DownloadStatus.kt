package com.example.core.model

sealed interface DownloadStatus {
    object Idle : DownloadStatus
    object Preparing : DownloadStatus
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadStatus {
        val progressPercentage: Int = (progress * 100).toInt().coerceIn(0, 100)
        val formattedProgress: String
            get() {
                val dlMb = downloadedBytes.toDouble() / (1024 * 1024)
                val totalMb = totalBytes.toDouble() / (1024 * 1024)
                return if (totalBytes > 0) {
                    "%.1f MB / %.1f MB (%d%%)".format(dlMb, totalMb, progressPercentage)
                } else {
                    "%.1f MB".format(dlMb)
                }
            }
    }
    object Completed : DownloadStatus
    data class Failed(val error: String) : DownloadStatus
}
