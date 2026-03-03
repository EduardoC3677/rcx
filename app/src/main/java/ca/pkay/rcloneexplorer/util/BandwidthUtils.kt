package ca.pkay.rcloneexplorer.util

object BandwidthUtils {
    /**
     * Converts a bandwidth limit preference value to an rclone --bwlimit flag value.
     * Returns null if no limit should be applied (value is 0 or "off").
     */
    fun toBwlimitArg(bytesPerSecond: Long): String? {
        if (bytesPerSecond <= 0) return null
        return when {
            bytesPerSecond >= 1_000_000_000L -> "${bytesPerSecond / 1_000_000_000L}G"
            bytesPerSecond >= 1_000_000L -> "${bytesPerSecond / 1_000_000L}M"
            bytesPerSecond >= 1_000L -> "${bytesPerSecond / 1_000L}k"
            else -> "$bytesPerSecond"
        }
    }

    val PRESETS = listOf(
        0L to "Off",
        512_000L to "500 KB/s",
        1_048_576L to "1 MB/s",
        2_097_152L to "2 MB/s",
        5_242_880L to "5 MB/s",
        10_485_760L to "10 MB/s",
        20_971_520L to "20 MB/s",
        52_428_800L to "50 MB/s",
        104_857_600L to "100 MB/s"
    )
}
