package de.tadris.flang.ui.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import de.tadris.flang.R
import de.tadris.flang.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AuthActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.loginSubmit.setOnClickListener {
            lifecycleScope.launch {
                authenticate(binding.loginUsername.text.toString(), binding.loginPassword.text.toString(), false)
            }
        }

        AlertDialog.Builder(this)
                .setTitle(R.string.questionLoginTitle)
                .setMessage(R.string.questionLoginMessage)
                .setPositiveButton(R.string.actionRegister) { _: DialogInterface, _: Int -> startRegisterActivity() }
                .setNegativeButton(R.string.actionLogin, null)
                .setOnCancelListener { finish() }
                .show()

    }

    private fun startRegisterActivity(){
        startActivity(Intent(this, RegisterActivity::class.java))
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}