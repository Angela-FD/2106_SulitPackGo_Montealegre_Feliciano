
package com.example.sulitpackgoui.uic.admin

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.sulitpackgoui.R
import com.example.sulitpackgoui.admin.AdminDatabase
import com.example.sulitpackgoui.rider.Rider

class AdminManageRidersActivity : ComponentActivity() {

    private val adminDatabase = AdminDatabase()

    private lateinit var ridersContainer: LinearLayout
    private lateinit var tvNoRiders: TextView
    private lateinit var tvRiderCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_manage_riders)

        ridersContainer = findViewById(R.id.ridersContainer)
        tvNoRiders = findViewById(R.id.tvNoRiders)
        tvRiderCount = findViewById(R.id.tvRiderCount)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        loadRiders()
    }

    private fun loadRiders() {
        tvNoRiders.visibility = View.GONE
        ridersContainer.removeAllViews()

        adminDatabase.getRiders { success, message, riders ->

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                if (success) {
                    displayRiders(riders)
                } else {
                    tvRiderCount.text = "Unable to load riders"
                    tvNoRiders.text = message
                    tvNoRiders.visibility = View.VISIBLE

                    Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun displayRiders(riders: List<Rider>) {
        ridersContainer.removeAllViews()

        tvRiderCount.text = "Registered Riders: ${riders.size}"

        if (riders.isEmpty()) {
            tvNoRiders.text = "No riders registered yet."
            tvNoRiders.visibility = View.VISIBLE
            return
        }

        tvNoRiders.visibility = View.GONE

        riders.forEachIndexed { index, rider ->
            val riderCard = createRiderCard(rider, index)
            ridersContainer.addView(riderCard)
        }
    }

    private fun createRiderCard(
        rider: Rider,
        index: Int
    ): LinearLayout {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundResource(R.drawable.bg_input_outline)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.WHITE)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.topMargin = if (index == 0) dp(16) else dp(12)
            layoutParams = params
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val nameView = TextView(this).apply {
            text = rider.name.ifBlank { "Unnamed Rider" }
            textSize = 16f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val statusView = TextView(this).apply {
            text = if (rider.isAvailable) "Available" else "Unavailable"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)

            setTextColor(
                if (rider.isAvailable) {
                    Color.parseColor("#2B8F3F")
                } else {
                    Color.parseColor("#F9A825")
                }
            )
        }

        header.addView(nameView)
        header.addView(statusView)
        card.addView(header)

        addInfoText(
            card,
            "Vehicle: ${rider.vehicleType.ifBlank { "Not specified" }}",
            8
        )

        addInfoText(
            card,
            "Phone: ${rider.phoneNumber.ifBlank { "Not provided" }}",
            4
        )

        addInfoText(
            card,
            "Email: ${rider.email.ifBlank { "Not provided" }}",
            4
        )

        addInfoText(
            card,
            if (rider.currentOrderId.isNotBlank()) {
                "Current Order: ${rider.currentOrderId}"
            } else {
                "Current Order: None"
            },
            4
        )

        val deleteButton = Button(this).apply {
            text = "Delete Rider"
            setTextColor(Color.WHITE)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#D32F2F")
                )

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.topMargin = dp(12)
            layoutParams = params

            setOnClickListener {
                confirmDeleteRider(rider)
            }
        }

        card.addView(deleteButton)

        return card
    }

    private fun addInfoText(
        parent: LinearLayout,
        value: String,
        topMargin: Int
    ) {
        val textView = TextView(this).apply {
            text = value
            textSize = 13f
            setTextColor(Color.parseColor("#666666"))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.topMargin = dp(topMargin)
            layoutParams = params
        }

        parent.addView(textView)
    }

    private fun confirmDeleteRider(rider: Rider) {
        AlertDialog.Builder(this)
            .setTitle("Delete Rider")
            .setMessage(
                "Are you sure you want to delete ${rider.name}?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteRider(rider)
            }
            .show()
    }

    private fun deleteRider(rider: Rider) {
        if (rider.uid.isBlank()) {
            Toast.makeText(
                this,
                "Rider ID is missing. Cannot delete this rider.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        adminDatabase.deleteRider(rider.uid) { success, message ->

            runOnUiThread {
                Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_LONG
                ).show()

                if (success) {
                    loadRiders()
                }
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()

        if (::ridersContainer.isInitialized) {
            loadRiders()
        }
    }
}
