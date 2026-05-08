package de.tadris.flang.ui.fragment

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import de.tadris.flang.R
import de.tadris.flang.databinding.FragmentSettingsBinding
import de.tadris.flang.network.CredentialsStorage
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.util.Sha256
import de.tadris.flang.ui.activity.LoginActivity
import de.tadris.flang.ui.dialog.LoadingDialogViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)!!
        _binding = FragmentSettingsBinding.bind(root)

        setupProfileSection()
        setupAppSection()

        return root
    }

    private fun setupProfileSection() {
        val isLoggedIn = DataRepository.getInstance().credentialsAvailable(requireContext())

        binding.changePasswordOption.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        binding.logoutOption.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        binding.loginOption.visibility = if (!isLoggedIn) View.VISIBLE else View.GONE

        binding.changePasswordOption.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.logoutOption.setOnClickListener {
            showLogoutDialog()
        }

        binding.loginOption.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
    }

    private fun setupAppSection() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val moveConfirmationSwitch = binding.moveConfirmationSwitch

        moveConfirmationSwitch.isChecked = prefs.getBoolean("move_confirmations", true)
        moveConfirmationSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("move_confirmations", isChecked).apply()
        }

        // Make the entire move confirmation row clickable
        binding.moveConfirmationOption.setOnClickListener {
            moveConfirmationSwitch.isChecked = !moveConfirmationSwitch.isChecked
        }

        binding.sourceCodeOption.setOnClickListener {
            openUrl(getString(R.string.sourceCodeUrl))
        }

        binding.privacyPolicyOption.setOnClickListener {
            openUrl(getString(R.string.privacyPolicyUrl))
        }

        binding.changelogOption.setOnClickListener {
            openUrl(getString(R.string.changelogUrl))
        }

        binding.openSourceLibrariesOption.setOnClickListener {
            openUrl(getString(R.string.openSourceLibrariesUrl))
        }

        binding.reportBugOption.setOnClickListener {
            openUrl(getString(R.string.reportBugUrl))
        }

        binding.sendFeedbackOption.setOnClickListener {
            sendFeedbackEmail()
        }

        binding.telegramChatOption.setOnClickListener {
            openUrl(getString(R.string.telegramChatUrl))
        }

        binding.matrixChatOption.setOnClickListener {
            openUrl(getString(R.string.matrixChatUrl))
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_change_password, null)

        val currentPasswordField = dialogView.findViewById<TextInputEditText>(R.id.currentPasswordField)
        val newPasswordField = dialogView.findViewById<TextInputEditText>(R.id.newPasswordField)
        val confirmPasswordField = dialogView.findViewById<TextInputEditText>(R.id.confirmPasswordField)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.changePasswordTitle)
            .setView(dialogView)
            .setPositiveButton(R.string.changePassword) { _, _ ->
                val currentPassword = currentPasswordField.text.toString()
                val newPassword = newPasswordField.text.toString()
                val confirmPassword = confirmPasswordField.text.toString()

                when {
                    currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                        Toast.makeText(requireContext(), R.string.passwordEmpty, Toast.LENGTH_SHORT).show()
                    }
                    newPassword != confirmPassword -> {
                        Toast.makeText(requireContext(), R.string.passwordMismatch, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        changePassword(currentPassword, newPassword)
                    }
                }
            }
            .setNegativeButton(R.string.actionCancel, null)
            .show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val loadingDialog = LoadingDialogViewController(requireContext())
        loadingDialog.setText(getString(R.string.changePassword))

        lifecycleScope.launch {
            try {
                val currentPwdHash = Sha256.getSha256(currentPassword)
                val newPwdHash = Sha256.getSha256(newPassword)

                withContext(Dispatchers.IO) {
                    DataRepository.getInstance().accessRestrictedAPI(requireContext())
                        .changePassword(currentPwdHash, newPwdHash)
                }

                loadingDialog.hide()
                Toast.makeText(requireContext(), R.string.passwordChanged, Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                loadingDialog.hide()
                val errorMessage = getString(R.string.passwordChangeError, e.message)
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.logoutTitle)
            .setMessage(R.string.logoutConfirm)
            .setPositiveButton(R.string.logout) { _, _ ->
                logout()
            }
            .setNegativeButton(R.string.actionCancel, null)
            .show()
    }

    private fun logout() {
        CredentialsStorage(requireContext()).clear()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        activity?.finish()
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not open URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendFeedbackEmail() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.developerEmail)))
                putExtra(Intent.EXTRA_SUBJECT, "Flang Android Feedback")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile section in case login status changed
        setupProfileSection()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}