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
import com.google.android.gms.location.FusedLocationProviderClient
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
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var selectedLat: Double? = null
    private var selectedLon: Double? = null

    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) fetchCurrentLocation()
        else Toast.makeText(this, R.string.location_permission_rationale, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geofence_setup)
        prefs = PrefsManager(this); repository = TourRepository(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val etBranchName = findViewById<TextInputEditText>(R.id.etBranchName)
        val etManagerName = findViewById<TextInputEditText>(R.id.etManagerName)
        val tvCoordinates = findViewById<TextView>(R.id.tvCoordinates)
        val tvRadiusValue = findViewById<TextView>(R.id.tvRadiusValue)
        val sliderRadius = findViewById<Slider>(R.id.sliderRadius)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)

        if (prefs.isGeofenceSet()) {
            selectedLat = prefs.getGeofenceLatitude(); selectedLon = prefs.getGeofenceLongitude()
            tvCoordinates.text = String.format("%.6f, %.6f", selectedLat, selectedLon)
            sliderRadius.value = prefs.getGeofenceRadiusKm(); btnSave.isEnabled = true
        }
        etBranchName.setText(prefs.getBranchName()); etManagerName.setText(prefs.getUserName())

        sliderRadius.addOnChangeListener { _, value, _ -> tvRadiusValue.text = String.format("%.1f km", value) }

        findViewById<MaterialButton>(R.id.btnUseCurrentLocation).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) fetchCurrentLocation()
            else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }

        btnSave.setOnClickListener {
            val lat = selectedLat; val lon = selectedLon
            if (lat != null && lon != null) {
                prefs.setGeofence(lat, lon, sliderRadius.value)
                prefs.setBranchName(etBranchName.text?.toString() ?: "")
                prefs.setUserName(etManagerName.text?.toString() ?: "Branch Manager")
                Toast.makeText(this, R.string.geofence_saved, Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java)); finish()
            }
        }
    }

    private fun fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val tvCoordinates = findViewById<TextView>(R.id.tvCoordinates)
        val tvAddress = findViewById<TextView>(R.id.tvAddress)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        tvCoordinates.text = getString(R.string.loading)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    selectedLat = location.latitude; selectedLon = location.longitude
                    tvCoordinates.text = String.format("%.6f, %.6f", location.latitude, location.longitude)
                    btnSave.isEnabled = true
                    scope.launch { tvAddress.text = repository.reverseGeocode(location.latitude, location.longitude) }
                } else tvCoordinates.text = "Could not get location. Try again."
            }.addOnFailureListener { tvCoordinates.text = "Location error. Try again." }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
