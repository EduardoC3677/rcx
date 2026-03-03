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
 * Configuration fragment for Proton Drive.
 * Proton Drive is an end-to-end encrypted cloud storage service.
 * Added in rclone 1.62+.
 */
class ProtonDriveConfig : Fragment() {

    private lateinit var rclone: Rclone
    private lateinit var remoteNameInputLayout: TextInputLayout
    private lateinit var usernameInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var remoteName: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText

    companion object {
        @JvmStatic
        fun newInstance() = ProtonDriveConfig()
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

        // Username
        val usernameView = View.inflate(ctx, R.layout.config_form_template_edit_text, null)
        usernameView.setPadding(0, 0, 0, padding)
        formContent.addView(usernameView)
        usernameInputLayout = usernameView.findViewById(R.id.text_input_layout)
        usernameInputLayout.hint = "Proton username"
        username = usernameView.findViewById(R.id.edit_text)

        // Password
        val passwordView = View.inflate(ctx, R.layout.config_form_template_edit_text, null)
        passwordView.setPadding(0, 0, 0, padding)
        formContent.addView(passwordView)
        passwordInputLayout = passwordView.findViewById(R.id.text_input_layout)
        passwordInputLayout.hint = "Proton password"
        password = passwordView.findViewById(R.id.edit_text)

        view.findViewById<View>(R.id.next).setOnClickListener { setUpRemote() }
        view.findViewById<View>(R.id.cancel).setOnClickListener { activity?.finish() }
    }

    private fun setUpRemote() {
        val name = remoteName.text.toString()
        val usernameStr = username.text.toString()
        val passwordStr = password.text.toString()
        var error = false

        if (name.trim().isEmpty()) {
            remoteNameInputLayout.isErrorEnabled = true
            remoteNameInputLayout.error = getString(R.string.remote_name_cannot_be_empty)
            error = true
        } else {
            remoteNameInputLayout.isErrorEnabled = false
        }
        if (usernameStr.trim().isEmpty()) {
            usernameInputLayout.isErrorEnabled = true
            usernameInputLayout.error = getString(R.string.required_field)
            error = true
        } else {
            usernameInputLayout.isErrorEnabled = false
        }
        if (passwordStr.trim().isEmpty()) {
            passwordInputLayout.isErrorEnabled = true
            passwordInputLayout.error = getString(R.string.required_field)
            error = true
        } else {
            passwordInputLayout.isErrorEnabled = false
        }
        if (error) return

        val obscuredPassword = rclone.obscure(passwordStr)

        val options = ArrayList<String>().apply {
            add(name)
            add("protondrive")
            add("username")
            add(usernameStr)
            add("password")
            add(obscuredPassword)
        }

        RemoteConfigHelper.setupAndWait(context, options)
        activity?.finish()
    }
}
