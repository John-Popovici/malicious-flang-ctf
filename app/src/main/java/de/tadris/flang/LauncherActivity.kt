package de.tadris.flang

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import de.tadris.flang.ui.activity.*

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}