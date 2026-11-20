
package com.example.sulitpackgoui.uic

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.sulitpackgoui.R
import com.example.sulitpackgoui.customer.Customer
import com.example.sulitpackgoui.uic.admin.AdminDashboardActivity
import com.example.sulitpackgoui.uic.customer.CustomerHomeActivity
import com.example.sulitpackgoui.uic.rider.RiderHomeActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseFirestore.getInstance()

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.customer_login)

        setupViews()
        setupButtons()
    }

    private fun setupViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
    }

    private fun setupButtons() {
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {
            login()
        }

        tvForgotPassword.setOnClickListener {
            forgotPassword()
        }

        tvSignUp.setOnClickListener {
            startActivity(
                Intent(this, SignUpActivity::class.java)
            )
        }
    }

    private fun login() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isBlank()) {
            etEmail.error = "Enter your email."
            etEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email."
            etEmail.requestFocus()
            return
        }

        if (password.isBlank()) {
            etPassword.error = "Enter your password."
            etPassword.requestFocus()
            return
        }

        setLoading(true)

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val currentUser = firebaseAuth.currentUser

                if (currentUser == null) {
                    handleLoginFailure("Login failed.")
                } else {
                    checkUserType(currentUser.uid)
                }
            }
            .addOnFailureListener { error ->
                handleLoginFailure(
                    error.message ?: "Login failed."
                )
            }
    }

    private fun checkUserType(uid: String) {
        database.collection("riders")
            .document(uid)
            .get()
            .addOnSuccessListener { riderDocument ->

                if (riderDocument.exists()) {
                    openDashboard(RiderHomeActivity::class.java)
                } else {
                    checkUserCollection(uid)
                }
            }
            .addOnFailureListener {
                handleLoginFailure(
                    "Failed to verify rider account. Please try again."
                )
            }
    }

    private fun checkUserCollection(uid: String) {
        database.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { userDocument ->

                if (!userDocument.exists()) {
                    handleLoginFailure(
                        "Account profile not found."
                    )
                    return@addOnSuccessListener
                }

                val customer = userDocument.toObject(Customer::class.java)

                if (customer == null) {
                    handleLoginFailure(
                        "Unable to read account information."
                    )
                    return@addOnSuccessListener
                }

                when (customer.role.trim().lowercase()) {
                    "customer" -> {
                        openDashboard(CustomerHomeActivity::class.java)
                    }

                    "admin" -> {
                        openDashboard(AdminDashboardActivity::class.java)
                    }

                    else -> {
                        handleLoginFailure(
                            "Unknown account type."
                        )
                    }
                }
            }
            .addOnFailureListener {
                handleLoginFailure(
                    "Failed to load account information. Please try again."
                )
            }
    }

    private fun openDashboard(activityClass: Class<*>) {
        setLoading(false)

        val intent = Intent(this, activityClass)

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    private fun handleLoginFailure(message: String) {
        firebaseAuth.signOut()
        setLoading(false)
        showMessage(message)
    }

    private fun forgotPassword() {
        val email = etEmail.text.toString().trim()

        if (email.isBlank()) {
            etEmail.error = "Enter your email first."
            etEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email."
            etEmail.requestFocus()
            return
        }

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                showMessage(
                    "Password reset email sent. Check your email."
                )
            }
            .addOnFailureListener { error ->
                showMessage(
                    error.message ?: "Failed to send password reset email."
                )
            }
    }

    private fun setLoading(loading: Boolean) {
        btnLogin.isEnabled = !loading
        btnLogin.text = if (loading) {
            "Signing in..."
        } else {
            "Login"
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
