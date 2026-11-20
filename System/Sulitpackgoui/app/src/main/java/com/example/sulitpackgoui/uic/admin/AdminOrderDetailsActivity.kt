
package com.example.sulitpackgoui.uic.admin

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.sulitpackgoui.R
import com.example.sulitpackgoui.admin.AdminDatabase
import com.example.sulitpackgoui.order.OrderData

class AdminOrderDetailsActivity : ComponentActivity() {

    private val adminDatabase = AdminDatabase()

    private lateinit var tvOrderId: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvCustomer: TextView
    private lateinit var tvRider: TextView
    private lateinit var tvPickupAddress: TextView
    private lateinit var tvPickupExtra: TextView
    private lateinit var tvDropAddress: TextView
    private lateinit var tvDropExtra: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvWeightPieces: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var tvVehicle: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvFee: TextView

    private var orderId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_order_details)

        connectViews()

        findViewById<Button>(R.id.btnCloseDetails).setOnClickListener {
            finish()
        }

        orderId = intent.getStringExtra("orderId").orEmpty()

        if (orderId.isBlank()) {
            Toast.makeText(
                this,
                "Order ID is missing.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        loadOrderDetails()
    }

    private fun connectViews() {
        tvOrderId = findViewById(R.id.tvDetailOrderId)
        tvStatus = findViewById(R.id.tvDetailStatus)
        tvCustomer = findViewById(R.id.tvDetailCustomer)
        tvRider = findViewById(R.id.tvDetailRider)
        tvPickupAddress = findViewById(R.id.tvDetailPickupAddress)
        tvPickupExtra = findViewById(R.id.tvDetailPickupExtra)
        tvDropAddress = findViewById(R.id.tvDetailDropAddress)
        tvDropExtra = findViewById(R.id.tvDetailDropExtra)
        tvDescription = findViewById(R.id.tvDetailDescription)
        tvWeightPieces = findViewById(R.id.tvDetailWeightPieces)
        tvInstructions = findViewById(R.id.tvDetailInstructions)
        tvVehicle = findViewById(R.id.tvDetailVehicle)
        tvDistance = findViewById(R.id.tvDetailDistance)
        tvFee = findViewById(R.id.tvDetailFee)
    }

    private fun loadOrderDetails() {
        adminDatabase.getOrders { success, message, orders ->

            if (isFinishing || isDestroyed) return@getOrders

            if (!success) {
                Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_LONG
                ).show()
                finish()
                return@getOrders
            }

            val order = orders.find {
                it.orderId == orderId
            }

            if (order == null) {
                Toast.makeText(
                    this,
                    "This order could not be found.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
                return@getOrders
            }

            displayOrder(order)
            loadPeople(order)
        }
    }

    private fun loadPeople(order: OrderData) {
        adminDatabase.getCustomers { customerSuccess, _, customers ->

            val customer = if (customerSuccess) {
                customers.find { it.uid == order.customerId }
            } else {
                null
            }

            tvCustomer.text = if (customer != null) {
                "${customer.name}\n${customer.email}\n${customer.phoneNumber}"
            } else {
                "Customer ID: ${order.customerId}"
            }

            adminDatabase.getRiders { riderSuccess, _, riders ->

                val rider = if (riderSuccess && order.riderId.isNotBlank()) {
                    riders.find { it.uid == order.riderId }
                } else {
                    null
                }

                tvRider.text = when {
                    order.riderId.isBlank() ->
                        "No rider assigned"

                    rider != null ->
                        "${rider.name}\n${rider.email}\n${rider.phoneNumber}"

                    else ->
                        "Rider ID: ${order.riderId}"
                }
            }
        }
    }

    private fun displayOrder(order: OrderData) {
        tvOrderId.text = "Order ID: ${order.orderId}"

        tvStatus.text = order.status.ifBlank {
            "Unknown"
        }

        tvStatus.setTextColor(getStatusColor(order.status))

        tvPickupAddress.text = showValue(order.pickupAddress)
        tvPickupExtra.text = showValue(order.pickupAddressDetails)

        tvDropAddress.text = showValue(order.dropAddress)
        tvDropExtra.text = showValue(order.dropAddressDetails)

        tvDescription.text = showValue(order.description)

        tvWeightPieces.text =
            "Weight: ${formatNumber(order.weight)} kg\n" +
                    "Number of pieces: ${order.pieces}"

        tvInstructions.text = showValue(order.instructions)

        tvVehicle.text =
            "Vehicle type: ${showValue(order.vehicleType)}"

        tvDistance.text =
            "Delivery distance: ${formatNumber(order.distance)} km"

        tvFee.text = "₱${formatNumber(order.fee)}"
    }

    private fun showValue(value: String): String {
        return value.ifBlank { "Not provided" }
    }

    private fun formatNumber(value: Double): String {
        return String.format(
            java.util.Locale.US,
            "%.2f",
            value
        )
    }

    private fun getStatusColor(status: String): Int {
        return when (status.trim().lowercase()) {
            "pending" -> Color.rgb(249, 168, 37)
            "accepted", "assigned" -> Color.rgb(21, 101, 192)
            "pick up pending",
            "picked up",
            "out for delivery",
            "in transit",
            "on the way" -> Color.rgb(30, 136, 229)
            "delivered", "completed" -> Color.rgb(43, 143, 63)
            "cancelled", "canceled", "rejected" ->
                Color.rgb(211, 47, 47)
            else -> Color.DKGRAY
        }
    }
}
