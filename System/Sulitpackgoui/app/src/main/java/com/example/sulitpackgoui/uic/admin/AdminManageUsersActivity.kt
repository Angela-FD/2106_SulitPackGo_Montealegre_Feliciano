
package com.example.sulitpackgoui.uic.admin

import android.graphics.Color
import android.graphics.Typeface
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
import com.example.sulitpackgoui.customer.Customer

class AdminManageUsersActivity : ComponentActivity() {

    private val adminDatabase = AdminDatabase()

    private lateinit var usersContainer: LinearLayout
    private lateinit var tvNoUsers: TextView
    private lateinit var tvUserCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_manage_users)

        usersContainer = findViewById(R.id.usersContainer)
        tvNoUsers = findViewById(R.id.tvNoUsers)
        tvUserCount = findViewById(R.id.tvUserCount)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        loadUsers()
    }

    private fun loadUsers() {
        tvNoUsers.visibility = View.GONE
        usersContainer.removeAllViews()

        adminDatabase.getCustomers { success, message, customers ->

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                if (success) {
                    displayUsers(customers)
                } else {
                    tvUserCount.text = "Unable to load customers"
                    tvNoUsers.text = message
                    tvNoUsers.visibility = View.VISIBLE

                    Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun displayUsers(customers: List<Customer>) {
        usersContainer.removeAllViews()

        tvUserCount.text = "Registered Customers: ${customers.size}"

        if (customers.isEmpty()) {
            tvNoUsers.text = "No customers registered yet."
            tvNoUsers.visibility = View.VISIBLE
            return
        }

        tvNoUsers.visibility = View.GONE

        customers.forEachIndexed { index, customer ->
            usersContainer.addView(
                createUserCard(customer, index)
            )
        }
    }

    private fun createUserCard(
        customer: Customer,
        index: Int
    ): LinearLayout {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
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

        val initialsView = TextView(this).apply {
            text = getInitials(customer.name)
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#2B8F3F"))
            setTypeface(null, Typeface.BOLD)

            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.parseColor("#E8F5E9"))
            }

            layoutParams = LinearLayout.LayoutParams(
                dp(56),
                dp(56)
            )
        }

        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dp(16)
            }
        }

        val nameView = TextView(this).apply {
            text = customer.name.ifBlank {
                "Unnamed Customer"
            }

            textSize = 16f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        }

        val emailView = TextView(this).apply {
            text = customer.email.ifBlank {
                "No email provided"
            }

            textSize = 12f
            setTextColor(Color.parseColor("#666666"))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.topMargin = dp(4)
            layoutParams = params
        }

        val phoneView = TextView(this).apply {
            text = customer.phoneNumber.ifBlank {
                "No phone number provided"
            }

            textSize = 12f
            setTextColor(Color.parseColor("#666666"))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.topMargin = dp(4)
            layoutParams = params
        }

        details.addView(nameView)
        details.addView(emailView)
        details.addView(phoneView)

        card.addView(initialsView)
        card.addView(details)

        return card
    }

    private fun getInitials(name: String): String {
        val words = name.trim()
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "${words.first().first()}${words.last().first()}"
                .uppercase()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()

        if (::usersContainer.isInitialized) {
            loadUsers()
        }
    }
}
