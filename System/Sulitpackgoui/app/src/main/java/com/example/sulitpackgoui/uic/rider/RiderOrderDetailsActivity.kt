
package com.example.sulitpackgoui.uic.rider

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.sulitpackgoui.R
import com.example.sulitpackgoui.customer.CustomerDatabase
import com.example.sulitpackgoui.order.OrderData
import com.example.sulitpackgoui.rider.Rider
import com.example.sulitpackgoui.rider.RiderDatabase
import com.example.sulitpackgoui.rider.RiderOrder
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class RiderOrderDetailsActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val riderDatabase = RiderDatabase()
    private val riderOrder = RiderOrder()
    private val customerDatabase = CustomerDatabase()

    private var currentRider = Rider()
    private var currentOrder = OrderData()

    private var riderLoaded = false
    private var orderLoaded = false
    private var acceptingOrder = false

    private lateinit var tvOrderId: TextView
    private lateinit var tvFee: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerPhone: TextView
    private lateinit var tvPickupAddress: TextView
    private lateinit var tvPickupDetails: TextView
    private lateinit var tvDropoffAddress: TextView
    private lateinit var tvDropoffDetails: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvVehicle: TextView

    private lateinit var btnAccept: MaterialButton
    private lateinit var btnDecline: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.rider_order_details)

        setupViews()
        setupButtons()

        val orderId = intent.getStringExtra("orderId").orEmpty()

        if (orderId.isBlank()) {
            showMessage("Order not found.")
            finish()
            return
        }

        btnAccept.isEnabled = false

        loadRider(orderId)
    }

    private fun setupViews() {
        tvOrderId = findViewById(R.id.tvOrderId)
        tvFee = findViewById(R.id.tvFee)
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvCustomerPhone = findViewById(R.id.tvCustomerPhone)
        tvPickupAddress = findViewById(R.id.tvPickupAddress)
        tvPickupDetails = findViewById(R.id.tvPickupDetails)
        tvDropoffAddress = findViewById(R.id.tvDropoffAddress)
        tvDropoffDetails = findViewById(R.id.tvDropoffDetails)
        tvDescription = findViewById(R.id.tvDescription)
        tvWeight = findViewById(R.id.tvWeight)
        tvDistance = findViewById(R.id.tvDistance)
        tvVehicle = findViewById(R.id.tvVehicle)

        btnAccept = findViewById(R.id.btnAccept)
        btnDecline = findViewById(R.id.btnDecline)
    }

    private fun setupButtons() {
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnDecline.setOnClickListener {
            // This only dismisses the details screen.
            // It does not reject or delete the shared order.
            finish()
        }

        btnAccept.setOnClickListener {
            acceptOrder()
        }
    }

    private fun loadRider(orderId: String) {
        val user = auth.currentUser

        if (user == null) {
            showMessage("Rider is not logged in.")
            finish()
            return
        }

        riderDatabase.getRider(user.uid) { success, message, rider ->

            if (!success || rider == null) {
                showMessage(message)
                finish()
                return@getRider
            }

            currentRider = rider
            riderLoaded = true

            loadOrder(orderId)
        }
    }

    private fun loadOrder(orderId: String) {
        riderOrder.getOrder(orderId) { success, message, order ->

            if (!success || order == null) {
                showMessage(message)
                finish()
                return@getOrder
            }

            currentOrder = order
            orderLoaded = true

            showOrder()
            loadCustomer(order.customerId)
            validateOrderAvailability()
        }
    }

    private fun validateOrderAvailability() {
        if (!riderLoaded || !orderLoaded) {
            btnAccept.isEnabled = false
            return
        }

        when {
            currentOrder.status != "Pending" -> {
                btnAccept.isEnabled = false
                showMessage("This order is no longer available.")
            }

            currentOrder.vehicleType != currentRider.vehicleType -> {
                btnAccept.isEnabled = false
                showMessage(
                    "This order requires a ${currentOrder.vehicleType}."
                )
            }

            currentRider.currentOrderId.isNotBlank() -> {
                btnAccept.isEnabled = false
                showMessage("You already have an active delivery.")
            }

            !currentRider.isAvailable -> {
                btnAccept.isEnabled = false
                showMessage("You are currently unavailable.")
            }

            else -> {
                btnAccept.isEnabled = true
            }
        }
    }

    private fun showOrder() {
        tvOrderId.text = "Order #${currentOrder.orderId}"
        tvFee.text = formatMoney(currentOrder.fee)

        tvPickupAddress.text = currentOrder.pickupAddress
        tvPickupDetails.text =
            currentOrder.pickupAddressDetails.ifBlank {
                "No additional pickup details."
            }

        tvDropoffAddress.text = currentOrder.dropAddress
        tvDropoffDetails.text =
            currentOrder.dropAddressDetails.ifBlank {
                "No additional drop-off details."
            }

        tvDescription.text =
            currentOrder.description.ifBlank {
                "No description"
            }

        tvWeight.text =
            "Weight: ${formatNumber(currentOrder.weight)} kg"

        tvDistance.text =
            "Distance: ${formatNumber(currentOrder.distance)} km"

        tvVehicle.text =
            "Vehicle: ${currentOrder.vehicleType}"
    }

    private fun loadCustomer(customerId: String) {
        if (customerId.isBlank()) {
            showUnknownCustomer()
            return
        }

        customerDatabase.getCustomer(customerId) { success, _, customer ->

            if (!success || customer == null) {
                showUnknownCustomer()
                return@getCustomer
            }

            tvCustomerName.text = customer.name

            tvCustomerPhone.text =
                customer.phoneNumber.ifBlank {
                    "Not provided"
                }
        }
    }

    private fun showUnknownCustomer() {
        tvCustomerName.text = "Customer"
        tvCustomerPhone.text = "Not provided"
    }

    private fun acceptOrder() {
        if (acceptingOrder) return

        if (!riderLoaded) {
            showMessage("Rider information is still loading.")
            return
        }

        if (!orderLoaded) {
            showMessage("Order information is still loading.")
            return
        }

        if (currentOrder.status != "Pending") {
            showMessage("This order is no longer available.")
            btnAccept.isEnabled = false
            return
        }

        acceptingOrder = true
        btnAccept.isEnabled = false
        btnDecline.isEnabled = false
        btnAccept.text = "Accepting..."

        riderOrder.acceptOrder(
            currentRider,
            currentOrder.orderId
        ) { success, message ->

            acceptingOrder = false

            if (success) {
                showMessage(message)
                openCurrentDelivery()
            } else {
                btnAccept.text = "Accept Order"
                btnDecline.isEnabled = true

                // Refresh the order and rider before allowing
                // another attempt.
                refreshBeforeRetry(message)
            }
        }
    }

    private fun refreshBeforeRetry(errorMessage: String) {
        val user = auth.currentUser

        if (user == null) {
            showMessage("Rider is not logged in.")
            finish()
            return
        }

        riderDatabase.getRider(user.uid) { riderSuccess, _, rider ->

            if (!riderSuccess || rider == null) {
                showMessage(errorMessage)
                finish()
                return@getRider
            }

            currentRider = rider

            riderOrder.getOrder(currentOrder.orderId) {
                    orderSuccess, _, order ->

                if (!orderSuccess || order == null) {
                    showMessage("This order is no longer available.")
                    finish()
                    return@getOrder
                }

                currentOrder = order

                if (currentOrder.status == "Pending" &&
                    currentRider.isAvailable &&
                    currentRider.currentOrderId.isBlank() &&
                    currentOrder.vehicleType == currentRider.vehicleType
                ) {
                    btnAccept.isEnabled = true
                    showMessage(errorMessage)
                } else {
                    btnAccept.isEnabled = false
                    showMessage("This order is no longer available to you.")
                }
            }
        }
    }

    private fun openCurrentDelivery() {
        val intent = Intent(
            this,
            RiderActiveDeliveryActivity::class.java
        )

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        startActivity(intent)
        finish()
    }

    private fun formatNumber(number: Double): String {
        return String.format("%.2f", number)
    }

    private fun formatMoney(amount: Double): String {
        return "₱${formatNumber(amount)}"
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
