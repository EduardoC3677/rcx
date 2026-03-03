package ca.pkay.rcloneexplorer.features.cleanup

import android.content.Context
import ca.pkay.rcloneexplorer.Items.RemoteItem
import ca.pkay.rcloneexplorer.Rclone
import ca.pkay.rcloneexplorer.util.FLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Provides rclone 'cleanup' and 'dedupe' commands.
 * These are PC rclone features now available on Android.
 *
 * - cleanup: Remove unfinished uploads and empty directories
 * - dedupe: Find and remove duplicate files
 */
class RcloneCleanup(private val context: Context) {

    companion object {
        private const val TAG = "RcloneCleanup"
    }

    data class CleanupResult(
        val output: List<String> = emptyList(),
        val success: Boolean = false,
        val itemsCleaned: Int = 0
    )

    data class DedupeResult(
        val duplicatesFound: Int = 0,
        val duplicatesRemoved: Int = 0,
        val output: List<String> = emptyList(),
        val success: Boolean = false
    )

    enum class DedupeMode(val flag: String) {
        INTERACTIVE("interactive"),
        SKIP("skip"),
        FIRST("first"),
        NEWEST("newest"),
        OLDEST("oldest"),
        LARGEST("largest"),
        SMALLEST("smallest"),
        RENAME("rename")
    }

    /**
     * Clean up remote storage - remove incomplete uploads, trash, etc.
     * Equivalent to: rclone cleanup remote:path
     */
    suspend fun cleanup(
        remote: RemoteItem,
        path: String = ""
    ): CleanupResult = withContext(Dispatchers.IO) {
        val rclone = Rclone(context)
        val remotePath = "${remote.name}:$path"
        val command = buildCommand(listOf("cleanup", remotePath))
        val env = rclone.rcloneEnv

        try {
            val process = Runtime.getRuntime().exec(command, env)
            val reader = BufferedReader(InputStreamReader(process.errorStream))
            val output = mutableListOf<String>()
            var cleaned = 0

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    output.add(it)
                    if (it.contains("Deleted") || it.contains("Removed")) cleaned++
                }
            }

            process.waitFor()
            CleanupResult(
                output = output,
                success = process.exitValue() == 0,
                itemsCleaned = cleaned
            )
        } catch (e: Exception) {
            FLog.e(TAG, "cleanup: error", e)
            CleanupResult(output = listOf(e.message ?: "Unknown error"))
        }
    }

    /**
     * Find and handle duplicate files.
     * Equivalent to: rclone dedupe [mode] remote:path
     */
    suspend fun dedupe(
        remote: RemoteItem,
        path: String = "",
        mode: DedupeMode = DedupeMode.SKIP,
        dryRun: Boolean = false
    ): DedupeResult = withContext(Dispatchers.IO) {
        val rclone = Rclone(context)
        val remotePath = "${remote.name}:$path"
        val args = mutableListOf("dedupe", "--dedupe-mode", mode.flag, remotePath)
        if (dryRun) args.add("--dry-run")
        args.add("-v")

        val command = buildCommand(args)
        val env = rclone.rcloneEnv

        try {
            val process = Runtime.getRuntime().exec(command, env)
            val reader = BufferedReader(InputStreamReader(process.errorStream))
            val output = mutableListOf<String>()
            var found = 0
            var removed = 0

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    output.add(it)
                    if (it.contains("duplicate")) found++
                    if (it.contains("Deleted") || it.contains("Removed")) removed++
                }
            }

            process.waitFor()
            DedupeResult(
                duplicatesFound = found,
                duplicatesRemoved = removed,
                output = output,
                success = process.exitValue() == 0
            )
        } catch (e: Exception) {
            FLog.e(TAG, "dedupe: error", e)
            DedupeResult(output = listOf(e.message ?: "Unknown error"))
        }
    }

    private fun buildCommand(args: List<String>): Array<String> {
        val rclonePath = context.applicationInfo.nativeLibraryDir + "/librclone.so"
        val configPath = context.filesDir.path + "/rclone.conf"
        return (listOf(rclonePath, "--config", configPath) + args).toTypedArray()
    }
}
