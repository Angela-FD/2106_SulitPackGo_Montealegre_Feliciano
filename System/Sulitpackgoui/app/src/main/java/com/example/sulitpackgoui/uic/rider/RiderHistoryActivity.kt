
package com.example.sulitpackgoui.uic.rider

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.sulitpackgoui.R
import com.example.sulitpackgoui.order.OrderData
import com.example.sulitpackgoui.rider.Rider
import com.example.sulitpackgoui.rider.RiderDatabase
import com.example.sulitpackgoui.rider.RiderOrder
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RiderHistoryActivity : ComponentActivity() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val riderDatabase = RiderDatabase()
    private val riderOrder = RiderOrder()

    private lateinit var tvNoHistory: TextView
    private lateinit var tvHistoryCount: TextView
    private lateinit var historyContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.rider_history)

        setupViews()
        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        loadRider()
    }

    private fun setupViews() {
        tvNoHistory = findViewById(R.id.tvNoHistory)
        tvHistoryCount = findViewById(R.id.tvHistoryCount)
        historyContainer = findViewById(R.id.historyContainer)
    }

    private fun setupNavigation() {
        findViewById<TextView>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, RiderProfileActivity::class.java))
        }

        findViewById<TextView>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, RiderHomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        findViewById<TextView>(R.id.navHistory).setOnClickListener {
            loadRider()
        }
    }

    private fun loadRider() {
        val currentUser = firebaseAuth.currentUser

        if (currentUser == null) {
            showMessage("Rider is not logged in.")
            finish()
            return
        }

        riderDatabase.getRider(currentUser.uid) { success, message, rider ->
            if (!success || rider == null) {
                showEmptyHistory(
                    if (message.isBlank()) {
                        "Unable to load rider information."
                    } else {
                        message
                    }
                )
                return@getRider
            }

            loadHistory(rider)
        }
    }

    private fun loadHistory(rider: Rider) {
        riderOrder.getDeliveryHistory(rider) { success, message, orders ->
            if (success) {
                showHistory(orders)
            } else {
                showEmptyHistory(message)
                showMessage(message)
            }
        }
    }

    private fun showHistory(orders: List<OrderData>) {
        historyContainer.removeAllViews()

        if (orders.isEmpty()) {
            showEmptyHistory("No completed deliveries yet.")
            return
        }

        tvNoHistory.visibility = View.GONE
        tvHistoryCount.visibility = View.VISIBLE
        tvHistoryCount.text = "Completed deliveries: ${orders.size}"

        orders.forEach { order ->
            historyContainer.addView(createHistoryCard(order))
        }
    }

    private fun createHistoryCard(order: OrderData): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dpToPx(16),
                dpToPx(16),
                dpToPx(16),
                dpToPx(16)
            )
            setBackgroundResource(R.drawable.bg_input_outline)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                getColor(android.R.color.white)
            )
        }

        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(12)
        }

        card.addView(
            createText(
                "Order #${order.orderId}",
                16f,
                true,
                android.R.color.black
            )
        )

        card.addView(
            createText(
                "${order.pickupAddress} → ${order.dropAddress}",
                14f,
                false,
                android.R.color.black
            )
        )

        card.addView(
            createText(
                "₱${String.format(Locale.getDefault(), "%.2f", order.fee)}",
                16f,
                true,
                android.R.color.black
            )
        )

        card.addView(
            createText(
                "Delivered • ${formatDate(order.createdAt)}",
                12f,
                true,
                android.R.color.holo_green_dark
            )
        )

        return card
    }

    private fun createText(
        value: String,
        size: Float,
        bold: Boolean,
        colorResource: Int
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(getColor(colorResource))
            setPadding(0, dpToPx(5), 0, dpToPx(5))

            if (bold) {
                setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun showEmptyHistory(message: String) {
        historyContainer.removeAllViews()

        tvHistoryCount.visibility = View.GONE
        tvNoHistory.visibility = View.VISIBLE
        tvNoHistory.text = message
    }

    private fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat(
            "MMM dd, yyyy • hh:mm a",
            Locale.getDefault()
        )

        return formatter.format(Date(timestamp))
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
