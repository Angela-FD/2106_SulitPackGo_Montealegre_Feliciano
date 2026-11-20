
package com.example.sulitpackgoui.uic.admin

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.sulitpackgoui.R
import com.example.sulitpackgoui.admin.VehicleFeeDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class AdminEditVehicleFeeActivity : ComponentActivity() {

    private val feeDatabase = VehicleFeeDatabase()

    private lateinit var vehicleType: String
    private lateinit var tvVehicleName: TextView
    private lateinit var etBaseFee: TextInputEditText
    private lateinit var etRatePerKm: TextInputEditText
    private lateinit var btnSave: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_admin_edit_vehiclefees)

        vehicleType = intent.getStringExtra("vehicleType") ?: ""

        if (vehicleType.isBlank()) {
            Toast.makeText(
                this,
                "Vehicle type is missing.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        setupViews()
        setupButtons()
        loadVehicleFee()
    }

    private fun setupViews() {

        tvVehicleName = findViewById(R.id.tvVehicleName)
        etBaseFee = findViewById(R.id.etBaseFee)
        etRatePerKm = findViewById(R.id.etRatePerKm)
        btnSave = findViewById(R.id.btnSaveVehicle)

        tvVehicleName.text = vehicleType
    }

    private fun setupButtons() {

        findViewById<ImageButton>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        btnSave.setOnClickListener {
            saveVehicleFee()
        }
    }

    private fun loadVehicleFee() {

        btnSave.isEnabled = false

        feeDatabase.getVehicleFee(vehicleType) {
                success, message, vehicleFee ->

            if (success && vehicleFee != null) {

                etBaseFee.setText(
                    String.format(
                        Locale.US,
                        "%.2f",
                        vehicleFee.baseFee
                    )
                )

                etRatePerKm.setText(
                    String.format(
                        Locale.US,
                        "%.2f",
                        vehicleFee.ratePerKm
                    )
                )

                btnSave.isEnabled = true

            } else {

                Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_LONG
                ).show()

                btnSave.isEnabled = true
            }
        }
    }

    private fun saveVehicleFee() {

        val baseFeeText =
            etBaseFee.text?.toString()?.trim().orEmpty()

        val rateText =
            etRatePerKm.text?.toString()?.trim().orEmpty()

        if (baseFeeText.isBlank()) {
            etBaseFee.error = "Enter the base fee."
            return
        }

        if (rateText.isBlank()) {
            etRatePerKm.error = "Enter the rate per KM."
            return
        }

        val baseFee = baseFeeText.toDoubleOrNull()
        val ratePerKm = rateText.toDoubleOrNull()

        if (baseFee == null || baseFee < 0) {
            etBaseFee.error =
                "Enter a valid non-negative amount."
            return
        }

        if (ratePerKm == null || ratePerKm < 0) {
            etRatePerKm.error =
                "Enter a valid non-negative amount."
            return
        }

        btnSave.isEnabled = false

        feeDatabase.updateVehicleFee(
            vehicleType = vehicleType,
            baseFee = baseFee,
            ratePerKm = ratePerKm
        ) { success, message ->

            btnSave.isEnabled = true

            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()

            if (success) {
                finish()
            }
        }
    }
}
