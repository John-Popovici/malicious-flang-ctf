package de.tadris.flang

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import de.tadris.flang.ui.activity.*
import de.tadris.flang.updates.checkUpdates
import android.util.Log

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        startActivity(Intent(this, MainActivity::class.java))
        Toast.makeText(this, "FLAG{welcome-to-the-ctf}", Toast.LENGTH_LONG).show()
        finish()
    }
}