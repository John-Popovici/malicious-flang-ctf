package de.tadris.flang.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import de.tadris.flang.R
import de.tadris.flang.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AuthActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.registerSubmit.setOnClickListener {
            val username = binding.registerUsername.text.toString()
            if(username.length < 5){
                showUsernameError(getString(R.string.errorUsernameTooShort))
                return@setOnClickListener
            }else if(username.length > 18){
                showUsernameError(getString(R.string.errorUsernameTooLong))
                return@setOnClickListener
            }else if(!username.matches("[A-Za-z0-9_.-]*".toRegex())){
                showUsernameError(getString(R.string.errorUsernameChars))
                return@setOnClickListener
            }

            lifecycleScope.launch {
                authenticate(username, binding.registerPassword.text.toString(), true)
            }
        }

        binding.consent1.setOnCheckedChangeListener { _, _ -> refresh() }
        binding.consent2.setOnCheckedChangeListener { _, _ -> refresh() }
        binding.consent3.setOnCheckedChangeListener { _, _ -> refresh() }
        binding.consent4.setOnCheckedChangeListener { _, _ -> refresh() }

        findViewById<Button>(R.id.registerPrivacyPolicy).setOnClickListener {
            openPrivacyPolicy()
        }
    }

    private fun showUsernameError(error: String){
        binding.registerUsername.error = error
        binding.registerUsername.requestFocus()
    }

    private fun refresh(){
        binding.registerSubmit.isEnabled =
                    binding.consent1.isChecked &&
                    binding.consent2.isChecked &&
                    binding.consent3.isChecked &&
                    binding.consent4.isChecked
    }

    private fun openPrivacyPolicy(){
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacyPolicyUrl)))
        startActivity(browserIntent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}