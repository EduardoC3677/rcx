package ca.pkay.rcloneexplorer.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ca.pkay.rcloneexplorer.Items.RemoteItem
import ca.pkay.rcloneexplorer.Rclone
import ca.pkay.rcloneexplorer.util.FLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker that performs a scheduled rclone sync between a source and destination.
 * Runs as a CoroutineWorker so it can be cancelled cleanly.
 */
class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_REMOTE_NAME = "remote_name"
        const val KEY_REMOTE_TYPE = "remote_type"
        const val KEY_REMOTE_TYPE_STRING = "remote_type_string"
        const val KEY_SOURCE_PATH = "source_path"
        const val KEY_DEST_PATH = "dest_path"
        const val KEY_SYNC_DIRECTION = "sync_direction"
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val remoteName = inputData.getString(KEY_REMOTE_NAME) ?: return@withContext Result.failure()
        val remoteType = inputData.getInt(KEY_REMOTE_TYPE, -1)
        val remoteTypeString = inputData.getString(KEY_REMOTE_TYPE_STRING) ?: remoteType.toString()
        val sourcePath = inputData.getString(KEY_SOURCE_PATH) ?: return@withContext Result.failure()
        val destPath = inputData.getString(KEY_DEST_PATH) ?: return@withContext Result.failure()
        val syncDirection = inputData.getInt(KEY_SYNC_DIRECTION, Rclone.SYNC_DIRECTION_LOCAL_TO_REMOTE)

        try {
            val rclone = Rclone(applicationContext)
            val remote = RemoteItem(remoteName, remoteTypeString)
            FLog.i(TAG, "Starting scheduled sync: $remoteName $sourcePath -> $destPath")
            val process = rclone.sync(remote, sourcePath, destPath, syncDirection)
            process?.waitFor()
            val exitCode = process?.exitValue() ?: -1
            if (exitCode == 0) {
                FLog.i(TAG, "Scheduled sync completed successfully")
                Result.success()
            } else {
                FLog.w(TAG, "Scheduled sync finished with exit code $exitCode")
                Result.retry()
            }
        } catch (e: Exception) {
            FLog.e(TAG, "Scheduled sync failed", e)
            Result.failure()
        }
    }
}
