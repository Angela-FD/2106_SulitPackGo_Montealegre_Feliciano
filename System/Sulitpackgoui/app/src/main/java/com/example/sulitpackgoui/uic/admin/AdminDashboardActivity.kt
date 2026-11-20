
package com.example.sulitpackgoui.uic.admin

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.sulitpackgoui.R
import com.example.sulitpackgoui.uic.MainActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : ComponentActivity() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseFirestore.getInstance()

    private lateinit var tvTotalOrders: TextView
    private lateinit var tvPendingOrders: TextView
    private lateinit var tvTotalRiders: TextView
    private lateinit var tvTotalUsers: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        setupViews()
        setupButtons()
        loadDashboardStatistics()
    }

    private fun setupViews() {
        tvTotalOrders = findViewById(R.id.tvTotalOrders)
        tvPendingOrders = findViewById(R.id.tvPendingOrders)
        tvTotalRiders = findViewById(R.id.tvTotalRiders)
        tvTotalUsers = findViewById(R.id.tvTotalUsers)

        resetStatistics()
    }

    private fun resetStatistics() {
        tvTotalOrders.text = "..."
        tvPendingOrders.text = "..."
        tvTotalRiders.text = "..."
        tvTotalUsers.text = "..."
    }

    private fun setupButtons() {

        findViewById<MaterialButton>(R.id.btnAdminLogout)
            .setOnClickListener {
                logoutAdmin()
            }

        findViewById<TextView>(R.id.btnManageOrders)
            .setOnClickListener {
                startActivity(
                    Intent(this, AdminManageOrdersActivity::class.java)
                )
            }

        findViewById<TextView>(R.id.btnManageRiders)
            .setOnClickListener {
                startActivity(
                    Intent(this, AdminManageRidersActivity::class.java)
                )
            }

        findViewById<TextView>(R.id.btnManageUsers)
            .setOnClickListener {
                startActivity(
                    Intent(this, AdminManageUsersActivity::class.java)
                )
            }

        findViewById<TextView>(R.id.btnManageFees)
            .setOnClickListener {
                startActivity(
                    Intent(this, AdminVehicleFeesActivity::class.java)
                )
            }

        findViewById<MaterialButton>(R.id.btnCreateRider)
            .setOnClickListener {
                startActivity(
                    Intent(this, AdminCreateRiderActivity::class.java)
                )
            }

    }

    private fun loadDashboardStatistics() {
        loadTotalOrders()
        loadPendingOrders()
        loadTotalRiders()
        loadTotalUsers()
    }

    private fun loadTotalOrders() {
        database.collection("orders")
            .get()
            .addOnSuccessListener { result ->
                tvTotalOrders.text = result.size().toString()
            }
            .addOnFailureListener {
                tvTotalOrders.text = "—"
                showMessage("Failed to load total orders.")
            }
    }

    private fun loadPendingOrders() {
        database.collection("orders")
            .whereEqualTo("status", "Pending")
            .get()
            .addOnSuccessListener { result ->
                tvPendingOrders.text = result.size().toString()
            }
            .addOnFailureListener {
                tvPendingOrders.text = "—"
                showMessage("Failed to load pending orders.")
            }
    }

    private fun loadTotalRiders() {
        database.collection("riders")
            .get()
            .addOnSuccessListener { result ->
                tvTotalRiders.text = result.size().toString()
            }
            .addOnFailureListener {
                tvTotalRiders.text = "—"
                showMessage("Failed to load total riders.")
            }
    }

    private fun loadTotalUsers() {
        database.collection("users")
            .whereEqualTo("role", "customer")
            .get()
            .addOnSuccessListener { result ->
                tvTotalUsers.text = result.size().toString()
            }
            .addOnFailureListener {
                tvTotalUsers.text = "—"
                showMessage("Failed to load total customers.")
            }
    }

    private fun logoutAdmin() {
        firebaseAuth.signOut()

        val intent = Intent(this, MainActivity::class.java)

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onResume() {
        super.onResume()

        if (::tvTotalOrders.isInitialized) {
            loadDashboardStatistics()
        }
    }
}
