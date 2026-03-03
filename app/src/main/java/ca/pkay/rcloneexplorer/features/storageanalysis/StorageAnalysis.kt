package ca.pkay.rcloneexplorer.features.storageanalysis

import android.content.Context
import ca.pkay.rcloneexplorer.Items.RemoteItem
import ca.pkay.rcloneexplorer.Rclone
import ca.pkay.rcloneexplorer.util.FLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Storage analysis tool - equivalent to 'rclone ncdu' and 'rclone size' on PC.
 * Provides directory size analysis with breakdown by folder/file.
 */
class StorageAnalysis(private val context: Context) {

    companion object {
        private const val TAG = "StorageAnalysis"
    }

    data class SizeInfo(
        val totalBytes: Long = 0,
        val totalCount: Long = 0,
        val formattedSize: String = "0 B"
    )

    data class DirectorySize(
        val path: String,
        val name: String,
        val size: Long,
        val count: Int,
        val isDir: Boolean
    )

    /**
     * Get total size of a remote path.
     * Equivalent to: rclone size remote:path --json
     */
    suspend fun getSize(
        remote: RemoteItem,
        path: String = ""
    ): SizeInfo = withContext(Dispatchers.IO) {
        val rclone = Rclone(context)
        val remotePath = "${remote.name}:$path"
        val command = buildCommand(listOf("size", "--json", remotePath))
        val env = rclone.rcloneEnv

        try {
            val process = Runtime.getRuntime().exec(command, env)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line)
            }
            process.waitFor()

            if (process.exitValue() == 0) {
                val json = JSONObject(output.toString())
                SizeInfo(
                    totalBytes = json.optLong("bytes", 0),
                    totalCount = json.optLong("count", 0),
                    formattedSize = formatSize(json.optLong("bytes", 0))
                )
            } else {
                SizeInfo()
            }
        } catch (e: Exception) {
            FLog.e(TAG, "getSize: error", e)
            SizeInfo()
        }
    }

    /**
     * Get directory listing with sizes for analysis.
     * Uses lsjson with recursive=false for top-level analysis.
     */
    suspend fun analyzeDirectory(
        remote: RemoteItem,
        path: String = ""
    ): List<DirectorySize> = withContext(Dispatchers.IO) {
        val rclone = Rclone(context)
        val remotePath = "${remote.name}:$path"
        val command = buildCommand(listOf("lsjson", "--dirs-only", "-R", "--no-modtime", remotePath))
        val env = rclone.rcloneEnv

        try {
            val process = Runtime.getRuntime().exec(command, env)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line)
            }
            process.waitFor()

            if (process.exitValue() == 0) {
                val jsonArray = JSONArray(output.toString())
                val results = mutableListOf<DirectorySize>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    results.add(
                        DirectorySize(
                            path = obj.optString("Path", ""),
                            name = obj.optString("Name", ""),
                            size = obj.optLong("Size", -1),
                            count = 0,
                            isDir = obj.optBoolean("IsDir", false)
                        )
                    )
                }
                results
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            FLog.e(TAG, "analyzeDirectory: error", e)
            emptyList()
        }
    }

    /**
     * Get detailed tree-like output for a path.
     * Equivalent to: rclone tree remote:path
     */
    suspend fun getTree(
        remote: RemoteItem,
        path: String = "",
        maxDepth: Int = 2
    ): String = withContext(Dispatchers.IO) {
        val rclone = Rclone(context)
        val remotePath = "${remote.name}:$path"
        val command = buildCommand(listOf("tree", "--level", maxDepth.toString(), remotePath))
        val env = rclone.rcloneEnv

        try {
            val process = Runtime.getRuntime().exec(command, env)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
            process.waitFor()
            output.toString()
        } catch (e: Exception) {
            FLog.e(TAG, "getTree: error", e)
            "Error: ${e.message}"
        }
    }

    private fun buildCommand(args: List<String>): Array<String> {
        val rclonePath = context.applicationInfo.nativeLibraryDir + "/librclone.so"
        val configPath = context.filesDir.path + "/rclone.conf"
        return (listOf(rclonePath, "--config", configPath) + args).toTypedArray()
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        if (gb < 1024) return String.format("%.1f GB", gb)
        val tb = gb / 1024.0
        return String.format("%.1f TB", tb)
    }
}
