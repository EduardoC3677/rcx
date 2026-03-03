package ca.pkay.rcloneexplorer.RemoteConfig

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import ca.pkay.rcloneexplorer.R
import ca.pkay.rcloneexplorer.Rclone
import com.google.android.material.textfield.TextInputLayout
import es.dmoral.toasty.Toasty

/**
 * Configuration fragment for Amazon S3 and S3-compatible storage.
 * Supports AWS S3, MinIO, DigitalOcean Spaces, Wasabi, etc.
 */
class S3Config : Fragment() {

    private lateinit var rclone: Rclone
    private lateinit var remoteNameInputLayout: TextInputLayout
    private lateinit var accessKeyInputLayout: TextInputLayout
    private lateinit var secretKeyInputLayout: TextInputLayout
    private lateinit var remoteName: EditText
    private lateinit var accessKey: EditText
    private lateinit var secretKey: EditText
    private lateinit var region: EditText
    private lateinit var endpoint: EditText

    companion object {
        @JvmStatic
        fun newInstance() = S3Config()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let { rclone = Rclone(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.remote_config_form, container, false)
        setUpForm(view)
        return view
    }

    private fun setUpForm(view: View) {
        val ctx = context ?: return
        val formContent = view.findViewById<ViewGroup>(R.id.form_content)
        val padding = resources.getDimensionPixelOffset(R.dimen.config_form_template)

        remoteNameInputLayout = view.findViewById(R.id.remote_name_layout)
        remoteNameInputLayout.visibility = View.VISIBLE
        remoteName = view.findViewById(R.id.remote_name)

        // Access Key
        val accessKeyView = View.inflate(ctx, R.layout.config_form_template_edit_text, null)
        accessKeyView.setPadding(0, 0, 0, padding)
        formContent.addView(accessKeyView)
        accessKeyInputLayout = accessKeyView.findViewById(R.id.text_input_layout)
        accessKeyInputLayout.hint = "Access Key ID"
        accessKey = accessKeyView.findViewById(R.id.edit_text)

        // Secret Key
        val secretKeyView = View.inflate(ctx, R.layout.config_form_template_edit_text, null)
        secretKeyView.setPadding(0, 0, 0, padding)
        formContent.addView(secretKeyView)
        secretKeyInputLayout = secretKeyView.findViewById(R.id.text_input_layout)
        secretKeyInputLayout.hint = "Secret Access Key"
        secretKey = secretKeyView.findViewById(R.id.edit_text)

        // Region
        val regionView = View.inflate(ctx, R.layout.config_form_template_edit_text, null)
        regionView.setPadding(0, 0, 0, padding)
        formContent.addView(regionView)
        val regionInputLayout: TextInputLayout = regionView.findViewById(R.id.text_input_layout)
        regionInputLayout.hint = "Region (e.g. us-east-1)"
        region = regionView.findViewById(R.id.edit_text)

        // Endpoint
        val endpointView = View.inflate(ctx, R.layout.config_form_template_edit_text, null)
        endpointView.setPadding(0, 0, 0, padding)
        formContent.addView(endpointView)
        val endpointInputLayout: TextInputLayout = endpointView.findViewById(R.id.text_input_layout)
        endpointInputLayout.hint = "Endpoint (leave empty for AWS)"
        endpoint = endpointView.findViewById(R.id.edit_text)
        endpointView.findViewById<View>(R.id.helper_text).visibility = View.VISIBLE

        view.findViewById<View>(R.id.next).setOnClickListener { setUpRemote() }
        view.findViewById<View>(R.id.cancel).setOnClickListener { activity?.finish() }
    }

    private fun setUpRemote() {
        val name = remoteName.text.toString()
        val accessKeyStr = accessKey.text.toString()
        val secretKeyStr = secretKey.text.toString()
        val regionStr = region.text.toString()
        val endpointStr = endpoint.text.toString()
        var error = false

        if (name.trim().isEmpty()) {
            remoteNameInputLayout.isErrorEnabled = true
            remoteNameInputLayout.error = getString(R.string.remote_name_cannot_be_empty)
            error = true
        } else {
            remoteNameInputLayout.isErrorEnabled = false
        }
        if (accessKeyStr.trim().isEmpty()) {
            accessKeyInputLayout.isErrorEnabled = true
            accessKeyInputLayout.error = getString(R.string.required_field)
            error = true
        } else {
            accessKeyInputLayout.isErrorEnabled = false
        }
        if (secretKeyStr.trim().isEmpty()) {
            secretKeyInputLayout.isErrorEnabled = true
            secretKeyInputLayout.error = getString(R.string.required_field)
            error = true
        } else {
            secretKeyInputLayout.isErrorEnabled = false
        }
        if (error) return

        val options = ArrayList<String>().apply {
            add(name)
            add("s3")
            add("provider")
            add("AWS")
            add("access_key_id")
            add(accessKeyStr)
            add("secret_access_key")
            add(secretKeyStr)
            if (regionStr.trim().isNotEmpty()) {
                add("region")
                add(regionStr)
            }
            if (endpointStr.trim().isNotEmpty()) {
                add("endpoint")
                add(endpointStr)
            }
        }

        RemoteConfigHelper.setupAndWait(context, options)
        activity?.finish()
    }
}
