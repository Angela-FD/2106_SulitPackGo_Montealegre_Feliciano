
package com.example.sulitpackgoui.uic.rider

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.sulitpackgoui.R
import com.example.sulitpackgoui.rider.Rider
import com.example.sulitpackgoui.rider.RiderDatabase
import com.example.sulitpackgoui.rider.RiderOrder
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class RiderHomeActivity : ComponentActivity() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val riderDatabase = RiderDatabase()
    private val riderOrder = RiderOrder()

    private var currentRider = Rider()
    private var riderLoaded = false
    private var availabilityUpdating = false

    private lateinit var tvRiderName: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvCurrentTitle: TextView
    private lateinit var tvCurrentSubtitle: TextView
    private lateinit var switchOnline: SwitchCompat
    private lateinit var btnViewAvailableOrders: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.rider_home)

        setupViews()
        setupButtons()
        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        loadRider()
    }

    private fun setupViews() {
        tvRiderName = findViewById(R.id.tvRiderName)
        tvStatus = findViewById(R.id.tvStatus)
        tvCurrentTitle = findViewById(R.id.tvCurrentTitle)
        tvCurrentSubtitle = findViewById(R.id.tvCurrentSubtitle)
        switchOnline = findViewById(R.id.switchOnline)
        btnViewAvailableOrders = findViewById(R.id.btnViewAvailableOrders)
    }

    private fun setupButtons() {
        btnViewAvailableOrders.setOnClickListener {
            openDeliveryScreen()
        }

        switchOnline.setOnClickListener {
            if (!riderLoaded || availabilityUpdating) {
                switchOnline.isChecked = currentRider.isAvailable
                return@setOnClickListener
            }

            changeAvailability(switchOnline.isChecked)
        }
    }

    private fun setupNavigation() {
        findViewById<TextView>(R.id.navSettings).setOnClickListener {
            startActivity(
                Intent(this, RiderProfileActivity::class.java)
            )
        }

        findViewById<TextView>(R.id.navHome).setOnClickListener {
            loadRider()
        }

        findViewById<TextView>(R.id.navHistory).setOnClickListener {
            startActivity(
                Intent(this, RiderHistoryActivity::class.java)
            )
        }
    }

    private fun openDeliveryScreen() {
        if (!riderLoaded) {
            showMessage("Rider information is still loading.")
            return
        }

        if (currentRider.currentOrderId.isNotBlank()) {
            startActivity(
                Intent(this, RiderActiveDeliveryActivity::class.java)
            )
            return
        }

        if (!currentRider.isAvailable) {
            showMessage("Turn your availability ON first.")
            return
        }

        startActivity(
            Intent(this, RiderAvailableOrdersActivity::class.java)
        )
    }

    private fun loadRider() {
        val currentUser = firebaseAuth.currentUser

        if (currentUser == null) {
            showMessage("Rider is not logged in.")
            finish()
            return
        }

        riderLoaded = false

        riderDatabase.getRider(currentUser.uid) { success, message, rider ->

            if (!success || rider == null) {
                showMessage(message)
                return@getRider
            }

            currentRider = rider
            riderLoaded = true

            showRiderInformation()
            loadCurrentDelivery()
        }
    }

    private fun showRiderInformation() {
        tvRiderName.text = currentRider.name

        switchOnline.isChecked = currentRider.isAvailable

        if (availabilityUpdating) {
            switchOnline.isEnabled = false
            return
        }

        if (currentRider.currentOrderId.isNotBlank()) {
            tvStatus.text = "You have an active delivery."
            switchOnline.isEnabled = false
        } else {
            switchOnline.isEnabled = true

            tvStatus.text = if (currentRider.isAvailable) {
                "Available for delivery requests."
            } else {
                "You are currently unavailable."
            }
        }
    }

    private fun changeAvailability(isAvailable: Boolean) {
        if (availabilityUpdating) return

        availabilityUpdating = true
        switchOnline.isEnabled = false

        riderDatabase.updateAvailability(
            currentRider,
            isAvailable
        ) { success, message ->

            availabilityUpdating = false

            if (success) {
                showRiderInformation()
            } else {
                switchOnline.isChecked = currentRider.isAvailable

                switchOnline.isEnabled =
                    currentRider.currentOrderId.isBlank()
            }

            showMessage(message)
        }
    }

    private fun loadCurrentDelivery() {
        if (currentRider.currentOrderId.isBlank()) {
            tvCurrentTitle.text = "No active delivery"

            tvCurrentSubtitle.text =
                "Browse available orders and accept one when you are ready."

            btnViewAvailableOrders.text = "View available orders"
            return
        }

        riderOrder.getCurrentOrder(currentRider) { success, message, order ->

            if (!success || order == null) {
                showCurrentOrderError(message)
                return@getCurrentOrder
            }

            tvCurrentTitle.text = order.status

            tvCurrentSubtitle.text =
                "${order.pickupAddress} → ${order.dropAddress}"

            btnViewAvailableOrders.text = "Open current delivery"
        }
    }

    private fun showCurrentOrderError(message: String) {
        tvCurrentTitle.text = "Unable to load delivery"
        tvCurrentSubtitle.text = message
        btnViewAvailableOrders.text = "Open current delivery"
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
