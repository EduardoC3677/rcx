package ca.pkay.rcloneexplorer.features

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.pkay.rcloneexplorer.R
import ca.pkay.rcloneexplorer.RuntimeConfiguration
import ca.pkay.rcloneexplorer.databinding.ActivityToolsBinding
import ca.pkay.rcloneexplorer.databinding.ItemToolBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import es.dmoral.toasty.Toasty

/**
 * Activity providing access to advanced rclone tools and features
 * that were previously only available on PC rclone.
 */
class ToolsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityToolsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityToolsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.toolsList.layoutManager = LinearLayoutManager(this)
        binding.toolsList.adapter = ToolsAdapter(this, getToolItems())
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(RuntimeConfiguration.attach(this, newBase))
    }

    private fun getToolItems(): List<ToolItem> = listOf(
        ToolItem(
            name = getString(R.string.bisync),
            description = getString(R.string.bisync_description),
            iconRes = android.R.drawable.stat_notify_sync,
            action = { showBisyncDialog() }
        ),
        ToolItem(
            name = getString(R.string.check),
            description = getString(R.string.check_description),
            iconRes = android.R.drawable.ic_menu_manage,
            action = { showCheckDialog() }
        ),
        ToolItem(
            name = getString(R.string.cleanup),
            description = getString(R.string.cleanup_description),
            iconRes = android.R.drawable.ic_menu_delete,
            action = { showCleanupDialog() }
        ),
        ToolItem(
            name = getString(R.string.dedupe),
            description = getString(R.string.dedupe_description),
            iconRes = android.R.drawable.ic_menu_search,
            action = { showDedupeDialog() }
        ),
        ToolItem(
            name = getString(R.string.config_encryption),
            description = getString(R.string.config_encryption_description),
            iconRes = android.R.drawable.ic_lock_lock,
            action = { showConfigEncryptionDialog() }
        ),
        ToolItem(
            name = getString(R.string.bandwidth_limit),
            description = getString(R.string.bandwidth_limit_description),
            iconRes = android.R.drawable.stat_sys_download,
            action = { showBandwidthDialog() }
        ),
        ToolItem(
            name = getString(R.string.storage_analysis),
            description = getString(R.string.storage_analysis_description),
            iconRes = android.R.drawable.ic_menu_info_details,
            action = { showStorageAnalysisDialog() }
        )
    )

    private fun showBisyncDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.bisync)
            .setMessage(R.string.bisync_description)
            .setPositiveButton(android.R.string.ok, null)
            .show()
        Toasty.info(this, getString(R.string.bisync_description), Toast.LENGTH_SHORT).show()
    }

    private fun showCheckDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.check)
            .setMessage(R.string.check_description)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showCleanupDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.cleanup)
            .setMessage(R.string.cleanup_description)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showDedupeDialog() {
        val modes = arrayOf(
            getString(R.string.dedupe_skip),
            getString(R.string.dedupe_newest),
            getString(R.string.dedupe_oldest),
            getString(R.string.dedupe_largest),
            getString(R.string.dedupe_smallest),
            getString(R.string.dedupe_rename)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dedupe_mode)
            .setItems(modes) { _, which ->
                Toasty.info(this, "Selected: ${modes[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showConfigEncryptionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.config_encryption)
            .setMessage(R.string.config_encryption_description)
            .setPositiveButton(R.string.encrypt_config) { _, _ ->
                Toasty.info(this, getString(R.string.encrypt_config), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.decrypt_config) { _, _ ->
                Toasty.info(this, getString(R.string.decrypt_config), Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton(android.R.string.cancel, null)
            .show()
    }

    private fun showBandwidthDialog() {
        val bwManager = ca.pkay.rcloneexplorer.features.bandwidth.BandwidthManager(this)
        val limits = bwManager.getPredefinedLimits()
        val names = limits.map { it.second }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.bandwidth_limit)
            .setItems(names) { _, which ->
                bwManager.globalLimit = limits[which].first
                bwManager.isEnabled = limits[which].first != "off"
                Toasty.success(this, "Bandwidth: ${names[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showStorageAnalysisDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.storage_analysis)
            .setMessage(R.string.storage_analysis_description)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    data class ToolItem(
        val name: String,
        val description: String,
        val iconRes: Int,
        val action: () -> Unit
    )

    class ToolsAdapter(
        private val context: Context,
        private val tools: List<ToolItem>
    ) : RecyclerView.Adapter<ToolsAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemToolBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemToolBinding.inflate(LayoutInflater.from(context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val tool = tools[position]
            holder.binding.toolName.text = tool.name
            holder.binding.toolDescription.text = tool.description
            holder.binding.toolIcon.setImageResource(tool.iconRes)
            holder.itemView.setOnClickListener { tool.action() }
        }

        override fun getItemCount() = tools.size
    }
}
