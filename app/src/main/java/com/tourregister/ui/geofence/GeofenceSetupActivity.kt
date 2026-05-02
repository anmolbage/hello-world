package com.tourregister.ui.geofence

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.tourregister.R
import com.tourregister.data.repository.TourRepository
import com.tourregister.ui.main.MainActivity
import com.tourregister.util.PrefsManager
import kotlinx.coroutines.*

class GeofenceSetupActivity : AppCompatActivity() {
    private lateinit var prefs: PrefsManager
    private lateinit var repository: TourRepository
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var selectedLat: Double? = null
    private var selectedLon: Double? = null

    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) fetchCurrentLocation()
        else Toast.makeText(this, R.string.location_permission_rationale, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geofence_setup)
        prefs = PrefsManager(this)
        repository = TourRepository(this)

        val etBranch = findViewById<TextInputEditText>(R.id.etBranchName)
        val etManager = findViewById<TextInputEditText>(R.id.etManagerName)
        val tvCoords = findViewById<TextView>(R.id.tvCoordinates)
        val tvAddr = findViewById<TextView>(R.id.tvAddress)
        val tvRadius = findViewById<TextView>(R.id.tvRadiusValue)
        val slider = findViewById<Slider>(R.id.sliderRadius)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)

        if (prefs.isGeofenceSet()) {
            selectedLat = prefs.getGeofenceLatitude(); selectedLon = prefs.getGeofenceLongitude()
            tvCoords.text = String.format("%.6f, %.6f", selectedLat, selectedLon)
            slider.value = prefs.getGeofenceRadiusKm(); btnSave.isEnabled = true
        }
        etBranch.setText(prefs.getBranchName()); etManager.setText(prefs.getUserName())
        slider.addOnChangeListener { _, value, _ -> tvRadius.text = String.format("%.1f km", value) }
        findViewById<MaterialButton>(R.id.btnUseCurrentLocation).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) fetchCurrentLocation()
            else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        btnSave.setOnClickListener {
            val lat = selectedLat; val lon = selectedLon
            if (lat != null && lon != null) {
                prefs.setGeofence(lat, lon, slider.value)
                prefs.setBranchName(etBranch.text?.toString() ?: "")
                prefs.setUserName(etManager.text?.toString() ?: "Branch Manager")
                Toast.makeText(this, R.string.geofence_saved, Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java)); finish()
            }
        }
    }

    private fun fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val tvCoords = findViewById<TextView>(R.id.tvCoordinates)
        val tvAddr = findViewById<TextView>(R.id.tvAddress)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        tvCoords.text = getString(R.string.loading)
        LocationServices.getFusedLocationProviderClient(this)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    selectedLat = loc.latitude; selectedLon = loc.longitude
                    tvCoords.text = String.format("%.6f, %.6f", loc.latitude, loc.longitude); btnSave.isEnabled = true
                    scope.launch { tvAddr.text = repository.reverseGeocode(loc.latitude, loc.longitude) }
                } else tvCoords.text = "Could not get location. Try again."
            }.addOnFailureListener { tvCoords.text = "Location error. Try again." }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
