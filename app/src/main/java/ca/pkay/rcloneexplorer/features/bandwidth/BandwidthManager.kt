package ca.pkay.rcloneexplorer.features.bandwidth

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import ca.pkay.rcloneexplorer.R

/**
 * Bandwidth management for rclone transfers.
 * Allows setting upload/download speed limits, equivalent to
 * rclone --bwlimit flag on PC.
 *
 * Supports:
 * - Global bandwidth limit
 * - Separate upload/download limits
 * - Schedule-based limits (timetable format)
 * - Real-time bandwidth adjustment
 */
class BandwidthManager(private val context: Context) {

    companion object {
        const val PREF_KEY_BWLIMIT = "pref_key_bwlimit"
        const val PREF_KEY_BWLIMIT_UPLOAD = "pref_key_bwlimit_upload"
        const val PREF_KEY_BWLIMIT_DOWNLOAD = "pref_key_bwlimit_download"
        const val PREF_KEY_BWLIMIT_ENABLED = "pref_key_bwlimit_enabled"
    }

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * Whether bandwidth limiting is enabled.
     */
    var isEnabled: Boolean
        get() = prefs.getBoolean(PREF_KEY_BWLIMIT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(PREF_KEY_BWLIMIT_ENABLED, value).apply()

    /**
     * Global bandwidth limit in format understood by rclone (e.g., "1M", "500k", "off").
     */
    var globalLimit: String
        get() = prefs.getString(PREF_KEY_BWLIMIT, "off") ?: "off"
        set(value) = prefs.edit().putString(PREF_KEY_BWLIMIT, value).apply()

    /**
     * Upload bandwidth limit.
     */
    var uploadLimit: String
        get() = prefs.getString(PREF_KEY_BWLIMIT_UPLOAD, "off") ?: "off"
        set(value) = prefs.edit().putString(PREF_KEY_BWLIMIT_UPLOAD, value).apply()

    /**
     * Download bandwidth limit.
     */
    var downloadLimit: String
        get() = prefs.getString(PREF_KEY_BWLIMIT_DOWNLOAD, "off") ?: "off"
        set(value) = prefs.edit().putString(PREF_KEY_BWLIMIT_DOWNLOAD, value).apply()

    /**
     * Get rclone --bwlimit flag value for commands.
     * Format: "upload:download" or single value for both.
     */
    fun getBwLimitFlag(): String? {
        if (!isEnabled) return null

        val upload = uploadLimit
        val download = downloadLimit

        return when {
            upload != "off" && download != "off" -> "$upload:$download"
            upload != "off" -> upload
            download != "off" -> download
            globalLimit != "off" -> globalLimit
            else -> null
        }
    }

    /**
     * Get the command-line arguments to append for bandwidth limiting.
     */
    fun getCommandArgs(): List<String> {
        val limit = getBwLimitFlag() ?: return emptyList()
        return listOf("--bwlimit", limit)
    }

    /**
     * Predefined bandwidth options for UI selection.
     */
    fun getPredefinedLimits(): List<Pair<String, String>> {
        return listOf(
            "off" to "Unlimited",
            "128k" to "128 KB/s",
            "256k" to "256 KB/s",
            "512k" to "512 KB/s",
            "1M" to "1 MB/s",
            "2M" to "2 MB/s",
            "5M" to "5 MB/s",
            "10M" to "10 MB/s",
            "20M" to "20 MB/s",
            "50M" to "50 MB/s",
            "100M" to "100 MB/s"
        )
    }
}
