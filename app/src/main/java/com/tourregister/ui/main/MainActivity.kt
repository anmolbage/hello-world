package com.tourregister.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tourregister.R
import com.tourregister.service.LocationTrackingService
import com.tourregister.ui.dashboard.DashboardFragment
import com.tourregister.ui.settings.SettingsFragment
import com.tourregister.ui.stops.StopsFragment
import com.tourregister.ui.visits.VisitsFragment
import com.tourregister.worker.DailySummaryWorker

class MainActivity : AppCompatActivity() {
    private val backgroundLocationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(this, R.string.background_location_rationale, Toast.LENGTH_LONG).show()
    }
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupBottomNavigation()
        requestPermissions()
        DailySummaryWorker.schedule(this)
        val navigateTo = intent.getStringExtra("navigate_to")
        if (navigateTo == "stops") {
            loadFragment(StopsFragment())
            findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.nav_stops
        } else if (savedInstanceState == null) loadFragment(DashboardFragment())
    }

    private fun setupBottomNavigation() {
        findViewById<BottomNavigationView>(R.id.bottomNav).setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_stops -> StopsFragment()
                R.id.nav_visits -> VisitsFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> return@setOnItemSelectedListener false
            }
            loadFragment(fragment); true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder(this).setTitle("Background Location")
                    .setMessage(getString(R.string.background_location_rationale))
                    .setPositiveButton(R.string.ok) { _, _ -> backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
                    .setNegativeButton(R.string.cancel, null).show()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun startTracking() {
        startForegroundService(Intent(this, LocationTrackingService::class.java).apply { action = LocationTrackingService.ACTION_START })
    }

    fun stopTracking() {
        startService(Intent(this, LocationTrackingService::class.java).apply { action = LocationTrackingService.ACTION_STOP })
    }
}
