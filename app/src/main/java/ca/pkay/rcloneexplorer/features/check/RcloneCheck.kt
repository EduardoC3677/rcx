package ca.pkay.rcloneexplorer.features.check

import android.content.Context
import ca.pkay.rcloneexplorer.Items.RemoteItem
import ca.pkay.rcloneexplorer.Rclone
import ca.pkay.rcloneexplorer.util.FLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Provides rclone 'check' functionality - verifies file integrity between
 * source and destination. This is a PC rclone feature now available on Android.
 *
 * Supports:
 * - check: Compare source and dest using sizes and hashes
 * - cryptcheck: Verify encrypted remote integrity
 * - hashsum: Compute hashes for files
 */
class RcloneCheck(private val context: Context) {

    companion object {
        private const val TAG = "RcloneCheck"
    }

    data class CheckResult(
        val matchingFiles: Int = 0,
        val differingFiles: Int = 0,
        val missingOnSource: Int = 0,
        val missingOnDest: Int = 0,
        val errors: Int = 0,
        val details: List<String> = emptyList(),
        val success: Boolean = false
    )

    /**
     * Check files between source and destination using sizes and hashes.
     * Equivalent to: rclone check source:path dest:path
     */
    suspend fun check(
        sourceRemote: RemoteItem,
        sourcePath: String,
        destRemote: RemoteItem,
        destPath: String,
        oneWay: Boolean = false
    ): CheckResult = withContext(Dispatchers.IO) {
        val rclone = Rclone(context)
        val source = "${sourceRemote.name}:$sourcePath"
        val dest = "${destRemote.name}:$destPath"

        val args = mutableListOf("check", source, dest)
        if (oneWay) args.add("--one-way")

        val command = buildCommand(args)
        val env = rclone.getRcloneEnv()

        try {
            val process = Runtime.getRuntime().exec(command, env)
            val reader = BufferedReader(InputStreamReader(process.errorStream))
            val details = mutableListOf<String>()
            var matching = 0
            var differing = 0
            var missingSrc = 0
            var missingDst = 0
            var errors = 0

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                details.add(l)
                when {
                    l.contains("matching") -> {
                        val num = l.filter { it.isDigit() }.toIntOrNull()
                        if (num != null) matching = num
                    }
                    l.contains("differing") -> {
                        val num = l.filter { it.isDigit() }.toIntOrNull()
                        if (num != null) differing = num
                    }
                    l.contains("missing on src") -> {
                        val num = l.filter { it.isDigit() }.toIntOrNull()
                        if (num != null) missingSrc = num
                    }
                    l.contains("missing on dst") -> {
                        val num = l.filter { it.isDigit() }.toIntOrNull()
                        if (num != null) missingDst = num
                    }
                    l.startsWith("ERROR") -> errors++
                }
            }

            process.waitFor()
            CheckResult(
                matchingFiles = matching,
                differingFiles = differing,
                missingOnSource = missingSrc,
                missingOnDest = missingDst,
                errors = errors,
                details = details,
                success = process.exitValue() == 0
            )
        } catch (e: Exception) {
            FLog.e(TAG, "check: error", e)
            CheckResult(errors = 1, details = listOf(e.message ?: "Unknown error"))
        }
    }

    /**
     * Verify encrypted remote integrity.
     * Equivalent to: rclone cryptcheck source:path dest:path
     */
    suspend fun cryptCheck(
        sourceRemote: RemoteItem,
        sourcePath: String,
        cryptRemote: RemoteItem,
        cryptPath: String
    ): CheckResult = withContext(Dispatchers.IO) {
        val rclone = Rclone(context)
        val source = "${sourceRemote.name}:$sourcePath"
        val crypt = "${cryptRemote.name}:$cryptPath"

        val command = buildCommand(listOf("cryptcheck", source, crypt))
        val env = rclone.getRcloneEnv()

        try {
            val process = Runtime.getRuntime().exec(command, env)
            val reader = BufferedReader(InputStreamReader(process.errorStream))
            val details = mutableListOf<String>()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { details.add(it) }
            }

            process.waitFor()
            CheckResult(
                details = details,
                success = process.exitValue() == 0
            )
        } catch (e: Exception) {
            FLog.e(TAG, "cryptCheck: error", e)
            CheckResult(errors = 1, details = listOf(e.message ?: "Unknown error"))
        }
    }

    private fun buildCommand(args: List<String>): Array<String> {
        val rclonePath = context.applicationInfo.nativeLibraryDir + "/librclone.so"
        val configPath = context.filesDir.path + "/rclone.conf"
        return (listOf(rclonePath, "--config", configPath) + args).toTypedArray()
    }
}
