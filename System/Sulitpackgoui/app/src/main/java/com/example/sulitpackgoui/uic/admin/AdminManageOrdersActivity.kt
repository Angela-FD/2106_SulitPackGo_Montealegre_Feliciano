
package com.example.sulitpackgoui.uic.admin

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.sulitpackgoui.R
import com.example.sulitpackgoui.admin.AdminDatabase
import com.example.sulitpackgoui.order.OrderData

class AdminManageOrdersActivity : ComponentActivity() {

    private val adminDatabase = AdminDatabase()

    private lateinit var tvNoOrders: TextView
    private lateinit var ordersContainer: LinearLayout

    private var riderNames: Map<String, String> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_orders)

        tvNoOrders = findViewById(R.id.tvNoOrders)
        ordersContainer = findViewById(R.id.ordersContainer)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        loadOrders()
    }

    private fun loadOrders() {
        tvNoOrders.visibility = View.VISIBLE
        tvNoOrders.text = "Loading orders..."
        ordersContainer.removeAllViews()

        adminDatabase.getRiders { riderSuccess, _, riders ->

            riderNames = if (riderSuccess) {
                riders.associate { it.uid to it.name }
            } else {
                emptyMap()
            }

            adminDatabase.getOrders { success, message, orders ->

                if (!success) {
                    showMessage(message)
                    showEmptyState(message)
                    return@getOrders
                }

                displayOrders(orders)
            }
        }
    }

    private fun displayOrders(orders: List<OrderData>) {
        ordersContainer.removeAllViews()

        if (orders.isEmpty()) {
            showEmptyState("No orders found.")
            return
        }

        tvNoOrders.visibility = View.GONE

        orders.forEach { order ->
            ordersContainer.addView(createOrderCard(order))
        }
    }

    private fun createOrderCard(order: OrderData): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = getDrawable(R.drawable.bg_input_outline)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.WHITE)

            isClickable = true
            isFocusable = true

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val idText = TextView(this).apply {
            text = if (order.orderId.isNotBlank()) {
                "Order #${order.orderId}"
            } else {
                "Order ID unavailable"
            }
            textSize = 15f
            setTextColor(Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val statusText = TextView(this).apply {
            text = order.status.ifBlank { "Unknown" }
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getStatusColor(order.status))
        }

        header.addView(idText)
        header.addView(statusText)
        card.addView(header)

        addCardText(
            card,
            "Pickup: ${order.pickupAddress.ifBlank { "Unavailable" }}",
            13f
        )

        addCardText(
            card,
            "Drop-off: ${order.dropAddress.ifBlank { "Unavailable" }}",
            13f
        )

        addCardText(
            card,
            "${formatNumber(order.distance)} km • " +
                    "${formatNumber(order.weight)} kg • " +
                    order.vehicleType.ifBlank { "Vehicle not specified" },
            12f
        )

        val riderName = riderNames[order.riderId]

        addCardText(
            card,
            if (order.riderId.isBlank()) {
                "No rider assigned"
            } else {
                "Rider: ${riderName ?: "Rider information unavailable"}"
            },
            12f
        )

        addCardText(
            card,
            "₱${formatNumber(order.fee)}",
            17f,
            true
        )

        card.setOnClickListener {
            val intent = android.content.Intent(
                this,
                AdminOrderDetailsActivity::class.java
            )

            intent.putExtra("orderId", order.orderId)
            startActivity(intent)
        }

        return card
    }

    private fun showOrderDetails(order: OrderData) {
        val riderName = if (order.riderId.isBlank()) {
            "No rider assigned"
        } else {
            riderNames[order.riderId] ?: "Rider information unavailable"
        }

        val details = """
            Order ID: ${order.orderId}
            
            Status: ${order.status}
            
            Customer ID: ${order.customerId}
            
            Rider: $riderName
            
            Pickup Address:
            ${order.pickupAddress}
            
            Pickup Details:
            ${order.pickupAddressDetails.ifBlank { "None" }}
            
            Drop-off Address:
            ${order.dropAddress}
            
            Drop-off Details:
            ${order.dropAddressDetails.ifBlank { "None" }}
            
            Package Description:
            ${order.description}
            
            Weight: ${formatNumber(order.weight)} kg
            Pieces: ${order.pieces}
            Distance: ${formatNumber(order.distance)} km
            Vehicle: ${order.vehicleType}
            Delivery Fee: ₱${formatNumber(order.fee)}
            
            Instructions:
            ${order.instructions.ifBlank { "None" }}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Order Details")
            .setMessage(details)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun addCardText(
        parent: LinearLayout,
        value: String,
        size: Float,
        bold: Boolean = false
    ) {
        val text = TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(Color.BLACK)

            if (bold) {
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(7)
            }
        }

        parent.addView(text)
    }

    private fun showEmptyState(message: String) {
        ordersContainer.removeAllViews()
        tvNoOrders.text = message
        tvNoOrders.visibility = View.VISIBLE
    }

    private fun getStatusColor(status: String): Int {
        return when (status.trim().lowercase()) {
            "pending" -> Color.rgb(249, 168, 37)
            "accepted", "assigned" -> Color.rgb(21, 101, 192)
            "pick up pending", "out for delivery",
            "picked up", "in transit", "on the way" ->
                Color.rgb(30, 136, 229)
            "delivered", "completed" ->
                Color.rgb(43, 143, 63)
            "cancelled", "canceled", "rejected" ->
                Color.rgb(211, 47, 47)
            else -> Color.DKGRAY
        }
    }

    private fun formatNumber(value: Double): String {
        return String.format(java.util.Locale.US, "%.2f", value)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()

        if (::ordersContainer.isInitialized) {
            loadOrders()
        }
    }
}
