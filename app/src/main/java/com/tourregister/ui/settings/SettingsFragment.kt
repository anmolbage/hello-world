package com.tourregister.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.tourregister.R
import com.tourregister.ui.auth.PinActivity
import com.tourregister.ui.export.ExportActivity
import com.tourregister.ui.geofence.GeofenceSetupActivity
import com.tourregister.util.PrefsManager

class SettingsFragment : Fragment() {

    private lateinit var prefs: PrefsManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = PrefsManager(requireContext())

        val etName = view.findViewById<TextInputEditText>(R.id.etSettingsName)
        val etBranch = view.findViewById<TextInputEditText>(R.id.etSettingsBranch)

        etName.setText(prefs.getUserName())
        etBranch.setText(prefs.getBranchName())

        view.findViewById<MaterialButton>(R.id.btnSaveProfile).setOnClickListener {
            prefs.setUserName(etName.text?.toString() ?: "Branch Manager")
            prefs.setBranchName(etBranch.text?.toString() ?: "")
            Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<MaterialButton>(R.id.btnEditGeofence).setOnClickListener {
            startActivity(Intent(requireContext(), GeofenceSetupActivity::class.java))
        }

        view.findViewById<MaterialButton>(R.id.btnChangePin).setOnClickListener {
            startActivity(Intent(requireContext(), PinActivity::class.java))
        }

        view.findViewById<MaterialButton>(R.id.btnExportSettings).setOnClickListener {
            startActivity(Intent(requireContext(), ExportActivity::class.java))
        }
    }
}
