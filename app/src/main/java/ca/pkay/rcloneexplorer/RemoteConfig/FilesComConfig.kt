package ca.pkay.rcloneexplorer.RemoteConfig

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import ca.pkay.rcloneexplorer.R
import ca.pkay.rcloneexplorer.Rclone
import com.google.android.material.textfield.TextInputLayout

/**
 * Configuration fragment for Files.com.
 * Files.com is an enterprise file sharing and governance platform.
 * Added in rclone 1.68.
 */
class FilesComConfig : Fragment() {

    private lateinit var rclone: Rclone
    private lateinit var remoteNameInputLayout: TextInputLayout
    private lateinit var apiKeyInputLayout: TextInputLayout
    private lateinit var remoteName: EditText
    private lateinit var apiKey: EditText
    private lateinit var site: EditText

    companion object {
        @JvmStatic
        fun newInstance() = FilesComConfig()
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

        // Site
        val siteView = View.inflate(ctx, R.layout.config_form_template_edit_text, null)
        siteView.setPadding(0, 0, 0, padding)
        formContent.addView(siteView)
        val siteInputLayout: TextInputLayout = siteView.findViewById(R.id.text_input_layout)
        siteInputLayout.hint = "Site URL (e.g. yoursite.files.com)"
        site = siteView.findViewById(R.id.edit_text)

        // API Key
        val apiKeyView = View.inflate(ctx, R.layout.config_form_template_edit_text, null)
        apiKeyView.setPadding(0, 0, 0, padding)
        formContent.addView(apiKeyView)
        apiKeyInputLayout = apiKeyView.findViewById(R.id.text_input_layout)
        apiKeyInputLayout.hint = "API Key"
        apiKey = apiKeyView.findViewById(R.id.edit_text)

        view.findViewById<View>(R.id.next).setOnClickListener { setUpRemote() }
        view.findViewById<View>(R.id.cancel).setOnClickListener { activity?.finish() }
    }

    private fun setUpRemote() {
        val name = remoteName.text.toString()
        val apiKeyStr = apiKey.text.toString()
        val siteStr = site.text.toString()
        var error = false

        if (name.trim().isEmpty()) {
            remoteNameInputLayout.isErrorEnabled = true
            remoteNameInputLayout.error = getString(R.string.remote_name_cannot_be_empty)
            error = true
        } else {
            remoteNameInputLayout.isErrorEnabled = false
        }
        if (apiKeyStr.trim().isEmpty()) {
            apiKeyInputLayout.isErrorEnabled = true
            apiKeyInputLayout.error = getString(R.string.required_field)
            error = true
        } else {
            apiKeyInputLayout.isErrorEnabled = false
        }
        if (error) return

        val options = ArrayList<String>().apply {
            add(name)
            add("filescom")
            if (siteStr.trim().isNotEmpty()) {
                add("site")
                add(siteStr)
            }
            add("api_key")
            add(apiKeyStr)
        }

        RemoteConfigHelper.setupAndWait(context, options)
        activity?.finish()
    }
}
