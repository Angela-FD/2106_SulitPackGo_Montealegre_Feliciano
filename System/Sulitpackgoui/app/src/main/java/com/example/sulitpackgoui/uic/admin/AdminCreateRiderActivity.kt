
package com.example.sulitpackgoui.uic.admin

import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.sulitpackgoui.R
import com.example.sulitpackgoui.rider.Rider
import com.example.sulitpackgoui.rider.RiderDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class AdminCreateRiderActivity : ComponentActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var rgVehicle: RadioGroup
    private lateinit var btnCreate: MaterialButton

    private val riderDatabase = RiderDatabase()

    private val secondaryAppName = "SulitPackGoRiderRegistration"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_rider)

        setupViews()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnCreate.setOnClickListener {
            createRiderAccount()
        }
    }

    private fun setupViews() {
        etName = findViewById(R.id.etRiderName)
        etEmail = findViewById(R.id.etRiderEmail)
        etPhone = findViewById(R.id.etRiderPhone)
        etPassword = findViewById(R.id.etTempPassword)
        rgVehicle = findViewById(R.id.rgVehicle)
        btnCreate = findViewById(R.id.btnCreateAccount)
    }

    private fun createRiderAccount() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val password = etPassword.text.toString()

        val vehicleType = when (rgVehicle.checkedRadioButtonId) {
            R.id.rbMotor -> "Motor"
            R.id.rbTricycle -> "Tricycle"
            R.id.rbVan -> "Van"
            R.id.rbTruck -> "Truck"
            else -> ""
        }

        if (name.isBlank()) {
            etName.error = "Enter rider name"
            return
        }

        if (email.isBlank()) {
            etEmail.error = "Enter rider email"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email address"
            return
        }

        if (phone.isBlank()) {
            etPhone.error = "Enter phone number"
            return
        }

        if (vehicleType.isBlank()) {
            showMessage("Please select a vehicle.")
            return
        }

        if (password.length < 8) {
            etPassword.error = "Password must be at least 8 characters"
            return
        }

        setLoading(true)

        val secondaryAuth = try {
            getSecondaryFirebaseAuth()
        } catch (error: Exception) {
            setLoading(false)
            showMessage(
                error.message ?: "Unable to initialize rider registration."
            )
            return
        }

        secondaryAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val firebaseUser = result.user

                if (firebaseUser == null) {
                    setLoading(false)
                    showMessage("Failed to create rider account.")
                    return@addOnSuccessListener
                }

                val rider = Rider(
                    uid = firebaseUser.uid,
                    name = name,
                    email = email,
                    phoneNumber = phone,
                    vehicleType = vehicleType,
                    isAvailable = true,
                    currentOrderId = ""
                )

                riderDatabase.saveRider(rider) { success, message ->

                    runOnUiThread {
                        if (success) {
                            secondaryAuth.signOut()

                            showMessage(
                                "Rider account created successfully."
                            )

                            finish()
                        } else {
                            // Remove the newly created Auth account
                            // if saving the Firestore profile fails.
                            firebaseUser.delete()
                                .addOnCompleteListener {
                                    secondaryAuth.signOut()

                                    setLoading(false)
                                    showMessage(message)
                                }
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                setLoading(false)

                showMessage(
                    error.message ?: "Failed to create rider account."
                )
            }
    }

    private fun getSecondaryFirebaseAuth(): FirebaseAuth {
        val existingApp = FirebaseApp.getApps(this)
            .firstOrNull { it.name == secondaryAppName }

        val secondaryApp = existingApp ?: FirebaseApp.initializeApp(
            this,
            FirebaseApp.getInstance().options,
            secondaryAppName
        ) ?: throw IllegalStateException(
            "Could not initialize secondary Firebase app."
        )

        return FirebaseAuth.getInstance(secondaryApp)
    }

    private fun setLoading(loading: Boolean) {
        btnCreate.isEnabled = !loading
        btnCreate.text = if (loading) {
            "Creating account..."
        } else {
            "Create account"
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
