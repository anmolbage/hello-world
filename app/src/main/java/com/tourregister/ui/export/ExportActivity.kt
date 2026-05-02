package com.tourregister.ui.export

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.tourregister.R
import com.tourregister.data.entity.Visit
import com.tourregister.data.entity.VisitPurpose
import com.tourregister.data.repository.TourRepository
import com.tourregister.util.DateUtils
import com.tourregister.util.PrefsManager
import kotlinx.coroutines.*
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ExportActivity : AppCompatActivity() {

    private lateinit var repository: TourRepository
    private lateinit var prefs: PrefsManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var startDate: String = ""
    private var endDate: String = ""
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export)

        repository = TourRepository(this)
        prefs = PrefsManager(this)

        val btnStartDate = findViewById<MaterialButton>(R.id.btnStartDate)
        val btnEndDate = findViewById<MaterialButton>(R.id.btnEndDate)

        // Default to this month
        setThisMonth()
        btnStartDate.text = DateUtils.formatDate(startDate)
        btnEndDate.text = DateUtils.formatDate(endDate)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        btnStartDate.setOnClickListener { showDatePicker(true) }
        btnEndDate.setOnClickListener { showDatePicker(false) }

        findViewById<MaterialButton>(R.id.btnThisWeek).setOnClickListener {
            setThisWeek()
            btnStartDate.text = DateUtils.formatDate(startDate)
            btnEndDate.text = DateUtils.formatDate(endDate)
        }

        findViewById<MaterialButton>(R.id.btnThisMonth).setOnClickListener {
            setThisMonth()
            btnStartDate.text = DateUtils.formatDate(startDate)
            btnEndDate.text = DateUtils.formatDate(endDate)
        }

        findViewById<MaterialButton>(R.id.btnExportExcel).setOnClickListener {
            exportExcel()
        }

        findViewById<MaterialButton>(R.id.btnExportPdf).setOnClickListener {
            exportPdf()
        }
    }

    private fun showDatePicker(isStart: Boolean) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isStart) "Start Date" else "End Date")
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            val date = dateFormat.format(Date(millis))
            if (isStart) {
                startDate = date
                findViewById<MaterialButton>(R.id.btnStartDate).text = DateUtils.formatDate(date)
            } else {
                endDate = date
                findViewById<MaterialButton>(R.id.btnEndDate).text = DateUtils.formatDate(date)
            }
        }

        picker.show(supportFragmentManager, "date_picker")
    }

    private fun setThisWeek() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        startDate = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_WEEK, 6)
        endDate = dateFormat.format(cal.time)
    }

    private fun setThisMonth() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        startDate = dateFormat.format(cal.time)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        endDate = dateFormat.format(cal.time)
    }

    private fun exportExcel() {
        val tvStatus = findViewById<TextView>(R.id.tvExportStatus)
        tvStatus.text = "Generating Excel..."
        tvStatus.visibility = View.VISIBLE

        scope.launch {
            try {
                val visits = withContext(Dispatchers.IO) {
                    repository.getVisitsInRange(startDate, endDate)
                }

                if (visits.isEmpty()) {
                    tvStatus.text = "No visits found in selected range"
                    return@launch
                }

                val file = withContext(Dispatchers.IO) { generateExcel(visits) }

                tvStatus.text = "Export successful!"
                shareFile(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

            } catch (e: Exception) {
                tvStatus.text = "Export failed: ${e.message}"
            }
        }
    }

    private fun generateExcel(visits: List<Visit>): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Tour Register")

        // Header style
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.ROYAL_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont()
            font.bold = true
            font.color = IndexedColors.WHITE.index
            setFont(font)
        }

        // Title row
        val titleRow = sheet.createRow(0)
        titleRow.createCell(0).apply {
            setCellValue("TOUR REGISTER - ${prefs.getBranchName()}")
            cellStyle = headerStyle
        }

        val subTitleRow = sheet.createRow(1)
        subTitleRow.createCell(0).setCellValue("Officer: ${prefs.getUserName()}")
        subTitleRow.createCell(3).setCellValue("Period: ${DateUtils.formatDate(startDate)} to ${DateUtils.formatDate(endDate)}")

        // Column headers
        val headers = listOf("S.No", "Date", "Time Out", "Time In", "Duration", "Place Visited", "Distance (km)", "Purpose", "Notes")
        val headerRow = sheet.createRow(3)
        headers.forEachIndexed { i, header ->
            headerRow.createCell(i).apply {
                setCellValue(header)
                cellStyle = headerStyle
            }
        }

        // Data rows
        var totalDistance = 0.0
        var totalMinutes = 0
        visits.forEachIndexed { index, visit ->
            val row = sheet.createRow(index + 4)
            row.createCell(0).setCellValue((index + 1).toDouble())
            row.createCell(1).setCellValue(DateUtils.formatDate(visit.date))
            row.createCell(2).setCellValue(DateUtils.formatTime(visit.timeIn))
            row.createCell(3).setCellValue(DateUtils.formatTime(visit.timeOut))
            row.createCell(4).setCellValue(DateUtils.formatDuration(visit.durationMinutes))
            row.createCell(5).setCellValue(visit.address)
            row.createCell(6).setCellValue(visit.distanceKm)
            row.createCell(7).setCellValue(VisitPurpose.fromName(visit.purpose).displayName)
            row.createCell(8).setCellValue(visit.notes)

            totalDistance += visit.distanceKm
            totalMinutes += visit.durationMinutes
        }

        // Summary row
        val summaryRow = sheet.createRow(visits.size + 5)
        summaryRow.createCell(0).apply {
            setCellValue("TOTAL")
            cellStyle = headerStyle
        }
        summaryRow.createCell(4).setCellValue(DateUtils.formatDuration(totalMinutes))
        summaryRow.createCell(6).setCellValue(totalDistance)

        // Auto-size columns
        for (i in headers.indices) {
            sheet.setColumnWidth(i, 4000)
        }
        sheet.setColumnWidth(5, 8000) // Address wider

        val exportDir = File(getExternalFilesDir(null), "Exports")
        exportDir.mkdirs()
        val file = File(exportDir, "TourRegister_${startDate}_to_${endDate}.xlsx")
        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()
        return file
    }

    private fun exportPdf() {
        val tvStatus = findViewById<TextView>(R.id.tvExportStatus)
        tvStatus.text = "Generating PDF..."
        tvStatus.visibility = View.VISIBLE

        scope.launch {
            try {
                val visits = withContext(Dispatchers.IO) {
                    repository.getVisitsInRange(startDate, endDate)
                }

                if (visits.isEmpty()) {
                    tvStatus.text = "No visits found in selected range"
                    return@launch
                }

                val file = withContext(Dispatchers.IO) { generatePdf(visits) }

                tvStatus.text = "Export successful!"
                shareFile(file, "application/pdf")

            } catch (e: Exception) {
                tvStatus.text = "Export failed: ${e.message}"
            }
        }
    }

    private fun generatePdf(visits: List<Visit>): File {
        val exportDir = File(getExternalFilesDir(null), "Exports")
        exportDir.mkdirs()
        val file = File(exportDir, "TourRegister_${startDate}_to_${endDate}.pdf")

        val writer = com.itextpdf.kernel.pdf.PdfWriter(file)
        val pdf = com.itextpdf.kernel.pdf.PdfDocument(writer)
        val document = com.itextpdf.layout.Document(pdf, com.itextpdf.kernel.geom.PageSize.A4.rotate())

        // Title
        document.add(
            com.itextpdf.layout.element.Paragraph("TOUR REGISTER")
                .setFontSize(18f)
                .setBold()
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
        )

        document.add(
            com.itextpdf.layout.element.Paragraph("Branch: ${prefs.getBranchName()} | Officer: ${prefs.getUserName()}")
                .setFontSize(11f)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
        )

        document.add(
            com.itextpdf.layout.element.Paragraph("Period: ${DateUtils.formatDate(startDate)} to ${DateUtils.formatDate(endDate)}")
                .setFontSize(10f)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(12f)
        )

        // Table
        val table = com.itextpdf.layout.element.Table(
            floatArrayOf(1f, 2f, 2f, 2f, 1.5f, 4f, 1.5f, 2.5f, 3f)
        ).useAllAvailableWidth()

        val headers = listOf("S.No", "Date", "Time Out", "Time In", "Duration", "Place Visited", "KM", "Purpose", "Remarks")
        val headerBg = com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY

        for (header in headers) {
            table.addHeaderCell(
                com.itextpdf.layout.element.Cell().add(
                    com.itextpdf.layout.element.Paragraph(header).setFontSize(8f).setBold()
                        .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE)
                ).setBackgroundColor(headerBg)
                    .setPadding(4f)
            )
        }

        var totalDistance = 0.0
        var totalMinutes = 0
        visits.forEachIndexed { index, visit ->
            table.addCell(com.itextpdf.layout.element.Cell().add(
                com.itextpdf.layout.element.Paragraph("${index + 1}").setFontSize(8f)).setPadding(3f))
            table.addCell(com.itextpdf.layout.element.Cell().add(
                com.itextpdf.layout.element.Paragraph(DateUtils.formatDate(visit.date)).setFontSize(8f)).setPadding(3f))
            table.addCell(com.itextpdf.layout.element.Cell().add(
                com.itextpdf.layout.element.Paragraph(DateUtils.formatTime(visit.timeIn)).setFontSize(8f)).setPadding(3f))
            table.addCell(com.itextpdf.layout.element.Cell().add(
                com.itextpdf.layout.element.Paragraph(DateUtils.formatTime(visit.timeOut)).setFontSize(8f)).setPadding(3f))
            table.addCell(com.itextpdf.layout.element.Cell().add(
                com.itextpdf.layout.element.Paragraph(DateUtils.formatDuration(visit.durationMinutes)).setFontSize(8f)).setPadding(3f))
            table.addCell(com.itextpdf.layout.element.Cell().add(
                com.itextpdf.layout.element.Paragraph(visit.address).setFontSize(8f)).setPadding(3f))
            table.addCell(com.itextpdf.layout.element.Cell().add(
                com.itextpdf.layout.element.Paragraph(String.format("%.1f", visit.distanceKm)).setFontSize(8f)).setPadding(3f))
            table.addCell(com.itextpdf.layout.element.Cell().add(
                com.itextpdf.layout.element.Paragraph(VisitPurpose.fromName(visit.purpose).displayName).setFontSize(8f)).setPadding(3f))
            table.addCell(com.itextpdf.layout.element.Cell().add(
                com.itextpdf.layout.element.Paragraph(visit.notes).setFontSize(8f)).setPadding(3f))

            totalDistance += visit.distanceKm
            totalMinutes += visit.durationMinutes
        }

        // Total row
        table.addCell(com.itextpdf.layout.element.Cell(1, 4).add(
            com.itextpdf.layout.element.Paragraph("TOTAL").setFontSize(9f).setBold()).setPadding(4f))
        table.addCell(com.itextpdf.layout.element.Cell().add(
            com.itextpdf.layout.element.Paragraph(DateUtils.formatDuration(totalMinutes)).setFontSize(9f).setBold()).setPadding(4f))
        table.addCell(com.itextpdf.layout.element.Cell().add(
            com.itextpdf.layout.element.Paragraph("").setFontSize(9f)).setPadding(4f))
        table.addCell(com.itextpdf.layout.element.Cell().add(
            com.itextpdf.layout.element.Paragraph(String.format("%.1f", totalDistance)).setFontSize(9f).setBold()).setPadding(4f))
        table.addCell(com.itextpdf.layout.element.Cell(1, 2).add(
            com.itextpdf.layout.element.Paragraph("").setFontSize(9f)).setPadding(4f))

        document.add(table)

        // Signature block
        document.add(
            com.itextpdf.layout.element.Paragraph("\n\n\nSignature of Officer: _____________________     Date: _______________")
                .setFontSize(10f)
                .setMarginTop(30f)
        )

        document.close()
        return file
    }

    private fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Tour Register"))
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
