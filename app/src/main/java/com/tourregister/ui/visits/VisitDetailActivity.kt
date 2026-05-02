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
        if (success && photoUri != null) { val iv = findViewById<ImageView>(R.id.ivPhoto); iv.setImageURI(photoUri); iv.visibility = android.view.View.VISIBLE }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visit_detail)
        repository = TourRepository(this); prefs = PrefsManager(this)
        val stopId = intent.getLongExtra(EXTRA_STOP_ID, -1); if (stopId == -1L) { finish(); return }
        chipToPurpose[R.id.chipGovtMeeting] = VisitPurpose.GOVT_MEETING; chipToPurpose[R.id.chipOfficialMeeting] = VisitPurpose.OFFICIAL_MEETING
        chipToPurpose[R.id.chipCustomerMeeting] = VisitPurpose.CUSTOMER_MEETING; chipToPurpose[R.id.chipPreSanction] = VisitPurpose.PRE_SANCTION_INSPECTION
        chipToPurpose[R.id.chipPostSanction] = VisitPurpose.POST_SANCTION_INSPECTION; chipToPurpose[R.id.chipLeadFollowup] = VisitPurpose.LEAD_FOLLOWUP
        chipToPurpose[R.id.chipNoticeServe] = VisitPurpose.NOTICE_SERVE; chipToPurpose[R.id.chipRecoveryVisit] = VisitPurpose.RECOVERY_VISIT
        chipToPurpose[R.id.chipOthers] = VisitPurpose.OTHERS

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ChipGroup>(R.id.chipGroupPurpose).setOnCheckedStateChangeListener { _, ids -> if (ids.isNotEmpty()) selectedPurpose = chipToPurpose[ids[0]] }
        findViewById<MaterialButton>(R.id.btnAttachPhoto).setOnClickListener { takePhoto() }
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { saveVisit() }

        scope.launch {
            val loaded = withContext(Dispatchers.IO) { repository.getStopById(stopId) }
            if (loaded == null) { finish(); return@launch }
            stop = loaded
            findViewById<TextView>(R.id.tvTimeIn).text = DateUtils.formatTime(loaded.arrivalTime)
            val dep = if (loaded.departureTime > 0) loaded.departureTime else System.currentTimeMillis()
            findViewById<TextView>(R.id.tvTimeOut).text = if (loaded.departureTime > 0) DateUtils.formatTime(loaded.departureTime) else "Ongoing"
            findViewById<TextView>(R.id.tvDuration).text = DateUtils.formatDurationFromMillis(dep - loaded.arrivalTime)
            findViewById<TextView>(R.id.tvAddress).text = loaded.address.ifEmpty { "Unknown" }
        }
    }

    private fun takePhoto() {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File.createTempFile("VISIT_${ts}_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        photoPath = file.absolutePath
        photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        takePictureLauncher.launch(photoUri)
    }

    private fun saveVisit() {
        val s = stop ?: return; val p = selectedPurpose
        if (p == null) { Toast.makeText(this, "Please select a purpose", Toast.LENGTH_SHORT).show(); return }
        val notes = findViewById<TextInputEditText>(R.id.etNotes).text?.toString() ?: ""
        val dep = if (s.departureTime > 0) s.departureTime else System.currentTimeMillis()
        scope.launch {
            val dist = withContext(Dispatchers.IO) {
                val last = repository.getLastLocationPoint()
                if (last != null) repository.calculateDistance(last.latitude, last.longitude, s.latitude, s.longitude).toDouble() / 1000.0
                else repository.calculateDistance(prefs.getGeofenceLatitude(), prefs.getGeofenceLongitude(), s.latitude, s.longitude).toDouble() / 1000.0
            }
            val visit = Visit(stopId = s.id, date = s.date, timeIn = s.arrivalTime, timeOut = dep, durationMinutes = DateUtils.durationMinutes(s.arrivalTime, dep),
                distanceKm = dist, latitude = s.latitude, longitude = s.longitude, address = s.address, purpose = p.name, notes = notes, photoPath = photoPath)
            withContext(Dispatchers.IO) { repository.insertVisit(visit); repository.markStopClassified(s.id); repository.updateDailyStats(s.date) }
            Toast.makeText(this@VisitDetailActivity, R.string.visit_saved, Toast.LENGTH_SHORT).show(); finish()
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
    companion object { const val EXTRA_STOP_ID = "extra_stop_id" }
}
