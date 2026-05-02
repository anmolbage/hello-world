package com.tourregister.ui.visits

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.tourregister.R
import com.tourregister.data.entity.DetectedStop
import com.tourregister.data.entity.Visit
import com.tourregister.data.entity.VisitPurpose
import com.tourregister.data.repository.TourRepository
import com.tourregister.util.DateUtils
import com.tourregister.util.PrefsManager
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VisitDetailActivity : AppCompatActivity() {
    private lateinit var repository: TourRepository
    private lateinit var prefs: PrefsManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var stop: DetectedStop? = null
    private var selectedPurpose: VisitPurpose? = null
    private var photoPath: String = ""
    private var photoUri: Uri? = null
    private val chipToPurpose = mutableMapOf<Int, VisitPurpose>()

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            val ivPhoto = findViewById<ImageView>(R.id.ivPhoto)
            ivPhoto.setImageURI(photoUri); ivPhoto.visibility = android.view.View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visit_detail)
        repository = TourRepository(this); prefs = PrefsManager(this)
        val stopId = intent.getLongExtra(EXTRA_STOP_ID, -1)
        if (stopId == -1L) { finish(); return }
        setupChipMapping(); setupListeners(); loadStop(stopId)
    }

    private fun setupChipMapping() {
        chipToPurpose[R.id.chipGovtMeeting] = VisitPurpose.GOVT_MEETING
        chipToPurpose[R.id.chipOfficialMeeting] = VisitPurpose.OFFICIAL_MEETING
        chipToPurpose[R.id.chipCustomerMeeting] = VisitPurpose.CUSTOMER_MEETING
        chipToPurpose[R.id.chipPreSanction] = VisitPurpose.PRE_SANCTION_INSPECTION
        chipToPurpose[R.id.chipPostSanction] = VisitPurpose.POST_SANCTION_INSPECTION
        chipToPurpose[R.id.chipLeadFollowup] = VisitPurpose.LEAD_FOLLOWUP
        chipToPurpose[R.id.chipNoticeServe] = VisitPurpose.NOTICE_SERVE
        chipToPurpose[R.id.chipRecoveryVisit] = VisitPurpose.RECOVERY_VISIT
        chipToPurpose[R.id.chipOthers] = VisitPurpose.OTHERS
    }

    private fun setupListeners() {
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ChipGroup>(R.id.chipGroupPurpose).setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) selectedPurpose = chipToPurpose[checkedIds[0]]
        }
        findViewById<MaterialButton>(R.id.btnAttachPhoto).setOnClickListener { takePhoto() }
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { saveVisit() }
    }

    private fun loadStop(stopId: Long) {
        scope.launch {
            val loadedStop = withContext(Dispatchers.IO) { repository.getStopById(stopId) }
            if (loadedStop == null) { finish(); return@launch }
            stop = loadedStop; populateStopInfo(loadedStop)
        }
    }

    private fun populateStopInfo(stop: DetectedStop) {
        findViewById<TextView>(R.id.tvTimeIn).text = DateUtils.formatTime(stop.arrivalTime)
        val departure = if (stop.departureTime > 0) stop.departureTime else System.currentTimeMillis()
        findViewById<TextView>(R.id.tvTimeOut).text = if (stop.departureTime > 0) DateUtils.formatTime(stop.departureTime) else "Ongoing"
        findViewById<TextView>(R.id.tvDuration).text = DateUtils.formatDurationFromMillis(departure - stop.arrivalTime)
        findViewById<TextView>(R.id.tvAddress).text = stop.address.ifEmpty { "Unknown" }
    }

    private fun takePhoto() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val photoFile = File.createTempFile("VISIT_${timeStamp}_", ".jpg", storageDir)
        photoPath = photoFile.absolutePath
        photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
        takePictureLauncher.launch(photoUri)
    }

    private fun saveVisit() {
        val currentStop = stop ?: return
        val purpose = selectedPurpose
        if (purpose == null) { Toast.makeText(this, "Please select a purpose", Toast.LENGTH_SHORT).show(); return }
        val notes = findViewById<TextInputEditText>(R.id.etNotes).text?.toString() ?: ""
        val departure = if (currentStop.departureTime > 0) currentStop.departureTime else System.currentTimeMillis()
        val durationMinutes = DateUtils.durationMinutes(currentStop.arrivalTime, departure)

        scope.launch {
            val distance = withContext(Dispatchers.IO) {
                val lastPoint = repository.getLastLocationPoint()
                if (lastPoint != null) repository.calculateDistance(lastPoint.latitude, lastPoint.longitude, currentStop.latitude, currentStop.longitude).toDouble() / 1000.0
                else repository.calculateDistance(prefs.getGeofenceLatitude(), prefs.getGeofenceLongitude(), currentStop.latitude, currentStop.longitude).toDouble() / 1000.0
            }
            val visit = Visit(stopId = currentStop.id, date = currentStop.date, timeIn = currentStop.arrivalTime, timeOut = departure,
                durationMinutes = durationMinutes, distanceKm = distance, latitude = currentStop.latitude, longitude = currentStop.longitude,
                address = currentStop.address, purpose = purpose.name, notes = notes, photoPath = photoPath)
            withContext(Dispatchers.IO) { repository.insertVisit(visit); repository.markStopClassified(currentStop.id); repository.updateDailyStats(currentStop.date) }
            Toast.makeText(this@VisitDetailActivity, R.string.visit_saved, Toast.LENGTH_SHORT).show(); finish()
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }

    companion object { const val EXTRA_STOP_ID = "extra_stop_id" }
}
