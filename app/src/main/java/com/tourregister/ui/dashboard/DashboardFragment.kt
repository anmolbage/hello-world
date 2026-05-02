package com.tourregister.ui.dashboard

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.tourregister.R
import com.tourregister.data.repository.TourRepository
import com.tourregister.ui.export.ExportActivity
import com.tourregister.ui.geofence.GeofenceSetupActivity
import com.tourregister.ui.main.MainActivity
import com.tourregister.util.DateUtils
import com.tourregister.util.PrefsManager

class DashboardFragment : Fragment() {
    private lateinit var prefs: PrefsManager
    private lateinit var repository: TourRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsManager(requireContext()); repository = TourRepository(requireContext())
        val today = DateUtils.today(); val monthPrefix = DateUtils.currentMonthPrefix()
        view.findViewById<TextView>(R.id.tvDate).text = DateUtils.formatDate(today)

        val btnToggle = view.findViewById<MaterialButton>(R.id.btnToggleTracking)
        val tvStatus = view.findViewById<TextView>(R.id.tvTrackingStatus)
        val indicator = view.findViewById<View>(R.id.statusIndicator)
        updateTrackingUI(btnToggle, tvStatus, indicator)
        btnToggle.setOnClickListener {
            val act = requireActivity() as MainActivity
            if (prefs.isTrackingActive()) { act.stopTracking(); prefs.setTrackingActive(false) }
            else { act.startTracking(); prefs.setTrackingActive(true) }
            updateTrackingUI(btnToggle, tvStatus, indicator)
        }

        repository.getDailyDistance(today).observe(viewLifecycleOwner) { view.findViewById<TextView>(R.id.tvDailyKm).text = String.format("%.1f", it ?: 0.0) }
        repository.getDailyFieldMinutes(today).observe(viewLifecycleOwner) { view.findViewById<TextView>(R.id.tvDailyHours).text = DateUtils.formatDuration(it ?: 0) }
        repository.getMonthlyDistance(monthPrefix).observe(viewLifecycleOwner) { view.findViewById<TextView>(R.id.tvMonthlyKm).text = String.format("%.1f", it ?: 0.0) }
        repository.getMonthlyFieldMinutes(monthPrefix).observe(viewLifecycleOwner) { view.findViewById<TextView>(R.id.tvMonthlyHours).text = DateUtils.formatDuration(it ?: 0) }

        val cardUncl = view.findViewById<MaterialCardView>(R.id.cardUnclassified)
        val tvUncl = view.findViewById<TextView>(R.id.tvUnclassifiedCount)
        repository.getUnclassifiedCount().observe(viewLifecycleOwner) { count ->
            if (count > 0) { cardUncl.visibility = View.VISIBLE; tvUncl.text = "$count stops need classification" } else cardUncl.visibility = View.GONE
        }
        view.findViewById<MaterialButton>(R.id.btnClassifyStops).setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.nav_stops
        }
        view.findViewById<MaterialButton>(R.id.btnExport).setOnClickListener { startActivity(Intent(requireContext(), ExportActivity::class.java)) }
        view.findViewById<MaterialButton>(R.id.btnGeofence).setOnClickListener { startActivity(Intent(requireContext(), GeofenceSetupActivity::class.java)) }
    }

    private fun updateTrackingUI(btn: MaterialButton, tvStatus: TextView, indicator: View) {
        if (prefs.isTrackingActive()) {
            btn.text = getString(R.string.stop_tracking); btn.setBackgroundColor(resources.getColor(R.color.tracking_inactive, null))
            tvStatus.text = getString(R.string.tracking_active); tvStatus.setTextColor(resources.getColor(R.color.tracking_active, null))
            indicator.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.tracking_active, null))
        } else {
            btn.text = getString(R.string.start_tracking); btn.setBackgroundColor(resources.getColor(R.color.primary, null))
            tvStatus.text = getString(R.string.tracking_inactive); tvStatus.setTextColor(resources.getColor(R.color.tracking_inactive, null))
            indicator.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.tracking_inactive, null))
        }
    }
}
