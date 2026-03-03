package ca.pkay.rcloneexplorer.features.bisync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.app.Service
import ca.pkay.rcloneexplorer.R
import ca.pkay.rcloneexplorer.Rclone
import ca.pkay.rcloneexplorer.Items.RemoteItem
import ca.pkay.rcloneexplorer.util.FLog
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Bisync service that provides bidirectional sync between two remotes.
 * This is a feature from rclone PC (since v1.58) now available on Android.
 * Bisync keeps two directories in sync bidirectionally, handling conflicts.
 */
class BisyncService : Service() {

    companion object {
        private const val TAG = "BisyncService"
        const val EXTRA_REMOTE1 = "extra_remote1"
        const val EXTRA_PATH1 = "extra_path1"
        const val EXTRA_REMOTE2 = "extra_remote2"
        const val EXTRA_PATH2 = "extra_path2"
        const val EXTRA_RESYNC = "extra_resync"
        const val EXTRA_DRY_RUN = "extra_dry_run"
        private const val CHANNEL_ID = "ca.pkay.rcexplorer.BISYNC_CHANNEL"
        private const val NOTIFICATION_ID = 500

        @Volatile
        var isRunning: Boolean = false
        @Volatile
        var progress: String = ""
    }

    private var currentProcess: Process? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            this@BisyncService.stopSelf()
            return Service.START_NOT_STICKY
        }

        val remote1 = intent.getParcelableExtra<RemoteItem>(EXTRA_REMOTE1)
        val path1 = intent.getStringExtra(EXTRA_PATH1) ?: ""
        val remote2 = intent.getParcelableExtra<RemoteItem>(EXTRA_REMOTE2)
        val path2 = intent.getStringExtra(EXTRA_PATH2) ?: ""
        val resync = intent.getBooleanExtra(EXTRA_RESYNC, false)
        val dryRun = intent.getBooleanExtra(EXTRA_DRY_RUN, false)

        if (remote1 == null || remote2 == null) {
            this@BisyncService.stopSelf()
            return Service.START_NOT_STICKY
        }

        startForegroundNotification()
        isRunning = true

        serviceScope.launch {
            runBisync(remote1, path1, remote2, path2, resync, dryRun)
            isRunning = false
            this@BisyncService.stopSelf()
        }

        return Service.START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this@BisyncService, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(this@BisyncService.getString(R.string.app_name))
            .setContentText("Bisync in progress...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this@BisyncService.startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            this@BisyncService.startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun runBisync(
        remote1: RemoteItem, path1: String,
        remote2: RemoteItem, path2: String,
        resync: Boolean, dryRun: Boolean
    ) {
        val rclone = Rclone(this@BisyncService)
        val path1Full = "${remote1.name}:$path1"
        val path2Full = "${remote2.name}:$path2"

        val args = mutableListOf("bisync", path1Full, path2Full)
        if (resync) args.add("--resync")
        if (dryRun) args.add("--dry-run")
        args.add("--verbose")

        try {
            val command = buildCommand(rclone, args)
            val env = rclone.getRcloneEnv()
            currentProcess = Runtime.getRuntime().exec(command, env)

            val reader = BufferedReader(InputStreamReader(currentProcess!!.errorStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    progress = it
                    if (it.startsWith("Transferred:")) {
                        updateNotification(it.substring(12).trim())
                    }
                }
            }
            currentProcess?.waitFor()
        } catch (e: Exception) {
            FLog.e(TAG, "runBisync: error", e)
        }
    }

    private fun buildCommand(rclone: Rclone, args: List<String>): Array<String> {
        val rclonePath = this@BisyncService.applicationInfo.nativeLibraryDir + "/librclone.so"
        val configPath = this@BisyncService.filesDir.path + "/rclone.conf"
        return (listOf(rclonePath, "--config", configPath) + args).toTypedArray()
    }

    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this@BisyncService, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Bisync")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        val nm = this@BisyncService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Bisync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Bidirectional sync operations"
            }
            val nm = this@BisyncService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentProcess?.destroy()
        serviceScope.cancel()
        isRunning = false
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
