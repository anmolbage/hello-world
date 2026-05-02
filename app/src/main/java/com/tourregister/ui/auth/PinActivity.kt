package com.tourregister.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tourregister.R
import com.tourregister.ui.geofence.GeofenceSetupActivity
import com.tourregister.ui.main.MainActivity
import com.tourregister.util.PrefsManager

class PinActivity : AppCompatActivity() {
    private lateinit var prefs: PrefsManager
    private var enteredPin = StringBuilder()
    private var isSettingPin = false
    private var firstPin: String? = null
    private lateinit var dots: List<View>
    private lateinit var tvPrompt: TextView
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)
        prefs = PrefsManager(this)
        isSettingPin = !prefs.isPinSet()
        tvPrompt = findViewById(R.id.tvPinPrompt)
        tvError = findViewById(R.id.tvError)
        dots = listOf(findViewById(R.id.dot1), findViewById(R.id.dot2), findViewById(R.id.dot3), findViewById(R.id.dot4))
        updatePrompt()
        setupNumberPad()
    }

    private fun updatePrompt() {
        tvPrompt.text = when {
            isSettingPin && firstPin == null -> getString(R.string.set_pin)
            isSettingPin && firstPin != null -> getString(R.string.confirm_pin)
            else -> getString(R.string.enter_pin)
        }
    }

    private fun setupNumberPad() {
        val buttonIds = listOf(R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9)
        for (id in buttonIds) {
            findViewById<View>(id).setOnClickListener { v ->
                val digit = (v as? com.google.android.material.button.MaterialButton)?.text?.toString() ?: return@setOnClickListener
                onDigitEntered(digit)
            }
        }
        findViewById<View>(R.id.btnBackspace).setOnClickListener { onBackspace() }
    }

    private fun onDigitEntered(digit: String) {
        if (enteredPin.length >= 4) return
        enteredPin.append(digit)
        updateDots()
        tvError.visibility = View.GONE
        if (enteredPin.length == 4) handlePinComplete(enteredPin.toString())
    }

    private fun onBackspace() {
        if (enteredPin.isNotEmpty()) { enteredPin.deleteCharAt(enteredPin.length - 1); updateDots() }
    }

    private fun updateDots() {
        for (i in dots.indices) {
            dots[i].setBackgroundResource(if (i < enteredPin.length) R.drawable.pin_dot_filled else R.drawable.pin_dot_empty)
        }
    }

    private fun handlePinComplete(pin: String) {
        if (isSettingPin) {
            if (firstPin == null) { firstPin = pin; resetInput(); updatePrompt() }
            else {
                if (pin == firstPin) { prefs.setPin(pin); navigateToApp() }
                else { showError(getString(R.string.pin_mismatch)); firstPin = null; resetInput(); updatePrompt() }
            }
        } else {
            if (prefs.verifyPin(pin)) navigateToApp()
            else { showError(getString(R.string.invalid_pin)); resetInput() }
        }
    }

    private fun showError(message: String) { tvError.text = message; tvError.visibility = View.VISIBLE }
    private fun resetInput() { enteredPin.clear(); updateDots() }

    private fun navigateToApp() {
        val target = if (!prefs.isGeofenceSet()) GeofenceSetupActivity::class.java else MainActivity::class.java
        startActivity(Intent(this, target)); finish()
    }
}
