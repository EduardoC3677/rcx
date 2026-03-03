package ca.pkay.rcloneexplorer.features.configencryption

import android.content.Context
import ca.pkay.rcloneexplorer.Rclone
import ca.pkay.rcloneexplorer.util.FLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Config encryption management - new in rclone 1.68.
 * Allows managing encryption of the rclone.conf file from the app,
 * ensuring config files can be encrypted/decrypted for safe transfer
 * between Android and PC.
 *
 * Commands:
 * - config encryption set: Encrypt the config file
 * - config encryption remove: Decrypt the config file
 * - config encryption check: Check if config is encrypted
 */
class ConfigEncryptionManager(private val context: Context) {

    companion object {
        private const val TAG = "ConfigEncryptionMgr"
    }

    data class EncryptionStatus(
        val isEncrypted: Boolean,
        val message: String
    )

    /**
     * Check if the current config file is encrypted.
     */
    suspend fun checkEncryption(): EncryptionStatus = withContext(Dispatchers.IO) {
        val rclone = Rclone(context)
        try {
            val isEncrypted = rclone.isConfigEncrypted
            EncryptionStatus(
                isEncrypted = isEncrypted,
                message = if (isEncrypted) "Config is encrypted" else "Config is not encrypted"
            )
        } catch (e: Exception) {
            FLog.e(TAG, "checkEncryption: error", e)
            EncryptionStatus(isEncrypted = false, message = "Error checking encryption: ${e.message}")
        }
    }

    /**
     * Set encryption on the config file with the given password.
     * This makes the config portable between Android and PC while keeping it secure.
     */
    suspend fun setEncryption(password: String): Boolean = withContext(Dispatchers.IO) {
        val rclonePath = context.applicationInfo.nativeLibraryDir + "/librclone.so"
        val configPath = context.filesDir.path + "/rclone.conf"

        try {
            val command = arrayOf(rclonePath, "--config", configPath, "config", "encryption", "set")
            val env = arrayOf("RCLONE_CONFIG_PASS=$password")
            val process = Runtime.getRuntime().exec(command, env)
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            FLog.e(TAG, "setEncryption: error", e)
            false
        }
    }

    /**
     * Remove encryption from the config file.
     * Requires the current password to decrypt.
     */
    suspend fun removeEncryption(currentPassword: String): Boolean = withContext(Dispatchers.IO) {
        val rclonePath = context.applicationInfo.nativeLibraryDir + "/librclone.so"
        val configPath = context.filesDir.path + "/rclone.conf"

        try {
            val command = arrayOf(rclonePath, "--config", configPath, "config", "encryption", "remove")
            val env = arrayOf("RCLONE_CONFIG_PASS=$currentPassword")
            val process = Runtime.getRuntime().exec(command, env)
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            FLog.e(TAG, "removeEncryption: error", e)
            false
        }
    }
}
