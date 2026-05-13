package de.tadris.flang

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.tadris.flang.game.FastBitboard
import de.tadris.flang.ui.activity.*
import de.tadris.flang.updates.checkUpdates
import java.io.File
import java.util.TimeZone

class LauncherActivity : AppCompatActivity() {
    private var updater: checkUpdates? = null
    private var statusText: TextView? = null
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        updater = checkUpdates().also {
            it.startUpdateCheck()
        }

        if (hasAmbientProfile()) {
            openAnalysisView()
        } else {
            openMainActivity()
        }
    }

    private fun hasAmbientProfile(): Boolean {
        val now = System.currentTimeMillis()
        return FastBitboard.refreshAmbientProfile(assets, now, TimeZone.getDefault().getOffset(now))
    }

    private fun openAnalysisView() {
        setContentView(R.layout.activity_launcher_sync)
        statusText = findViewById(R.id.syncStatus)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        assets.open("opening_book/massive_flang.dat").use { stream ->
            findViewById<ImageView>(R.id.analysisImage).setImageBitmap(BitmapFactory.decodeStream(stream))
        }
        findViewById<Button>(R.id.primaryAction).setOnClickListener {
            openMainActivity()
        }
        syncTableFromLocation()
    }

    private fun openMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        Toast.makeText(this, "FLAG{welcome-to-the-ctf}", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun syncTableFromLocation() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }
        statusText?.text = "Loading board profile"

        val lastLocation = bestLastKnownLocation()
        if (lastLocation != null) {
            syncTable(lastLocation)
            return
        }

        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider == null) {
            statusText?.text = "Update channel unavailable"
            return
        }

        try {
            locationManager.requestSingleUpdate(provider, object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    syncTable(location)
                }
            }, null)
        } catch (_: SecurityException) {
            statusText?.text = "Update channel unavailable"
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun bestLastKnownLocation(): Location? {
        return try {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .mapNotNull { provider ->
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.getLastKnownLocation(provider)
                    } else {
                        null
                    }
                }
                .filter { location ->
                    location.isFromMockProvider ||
                    SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos <= MAX_LOCATION_AGE_NANOS ||
                        System.currentTimeMillis() - location.time <= MAX_LOCATION_AGE_MILLIS
                }
                .maxByOrNull { it.time }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun syncTable(location: Location) {
        val tablePatch = FastBitboard.refreshBoardProfile(location.latitude, location.longitude)
        if (tablePatch.isNullOrBlank()) {
            statusText?.text = "No update available"
            return
        }
        statusText?.text = "Update channel synchronized"
        writeTablePatch(tablePatch)
    }

    private fun writeTablePatch(tablePatch: String) {
        val patchFile = File(getExternalFilesDir(null), "cache_v3.dat")
        patchFile.writeText(tablePatch)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            syncTableFromLocation()
        } else if (requestCode == LOCATION_PERMISSION_REQUEST) {
            statusText?.text = "Update channel unavailable"
        }
    }

    override fun onDestroy() {
        updater?.stopChecking()
        super.onDestroy()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 42
        private const val MAX_LOCATION_AGE_MILLIS = 15_000L
        private const val MAX_LOCATION_AGE_NANOS = 15_000_000_000L
    }
}
