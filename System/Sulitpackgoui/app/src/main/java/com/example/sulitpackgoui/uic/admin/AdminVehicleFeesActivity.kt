
package com.example.sulitpackgoui.uic.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.sulitpackgoui.R
import com.example.sulitpackgoui.admin.VehicleFee
import com.example.sulitpackgoui.admin.VehicleFeeDatabase

class AdminVehicleFeesActivity : ComponentActivity() {

    private val feeDatabase = VehicleFeeDatabase()

    private val vehicleTypes = listOf(
        "Motor",
        "Tricycle",
        "Van",
        "Truck"
    )

    private lateinit var baseFeeViews: List<TextView>
    private lateinit var rateViews: List<TextView>
    private lateinit var statusViews: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_vehicle_fees)

        setupViews()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()

        if (::baseFeeViews.isInitialized) {
            loadVehicleFees()
        }
    }

    private fun setupViews() {
        baseFeeViews = listOf(
            findViewById(R.id.tvMotorBaseFee),
            findViewById(R.id.tvTricycleBaseFee),
            findViewById(R.id.tvVanBaseFee),
            findViewById(R.id.tvTruckBaseFee)
        )

        rateViews = listOf(
            findViewById(R.id.tvMotorRate),
            findViewById(R.id.tvTricycleRate),
            findViewById(R.id.tvVanRate),
            findViewById(R.id.tvTruckRate)
        )

        statusViews = listOf(
            findViewById(R.id.tvMotorStatus),
            findViewById(R.id.tvTricycleStatus),
            findViewById(R.id.tvVanStatus),
            findViewById(R.id.tvTruckStatus)
        )
    }

    private fun setupButtons() {
        findViewById<ImageButton>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        val editButtons = listOf(
            R.id.btnEditMotor,
            R.id.btnEditTricycle,
            R.id.btnEditVan,
            R.id.btnEditTruck
        )

        editButtons.forEachIndexed { index, buttonId ->

            findViewById<TextView>(buttonId)
                .setOnClickListener {
                    openEditScreen(vehicleTypes[index])
                }
        }
    }

    private fun loadVehicleFees() {

        vehicleTypes.forEachIndexed { index, vehicleType ->

            statusViews[index].text = "Loading..."
            statusViews[index].visibility = View.VISIBLE

            feeDatabase.getVehicleFee(vehicleType) {
                    success, message, vehicleFee ->

                if (success && vehicleFee != null) {
                    displayFee(index, vehicleFee)
                } else {
                    baseFeeViews[index].text = "Not available"
                    rateViews[index].text = "Not available"

                    statusViews[index].text = message
                    statusViews[index].visibility = View.VISIBLE
                }
            }
        }
    }

    private fun displayFee(index: Int, fee: VehicleFee) {

        baseFeeViews[index].text =
            "₱${String.format("%.2f", fee.baseFee)}"

        rateViews[index].text =
            "₱${String.format("%.2f", fee.ratePerKm)}"

        statusViews[index].visibility = View.GONE
    }

    private fun openEditScreen(vehicleType: String) {

        val intent = Intent(
            this,
            AdminEditVehicleFeeActivity::class.java
        )

        intent.putExtra("vehicleType", vehicleType)

        startActivity(intent)
    }
}
