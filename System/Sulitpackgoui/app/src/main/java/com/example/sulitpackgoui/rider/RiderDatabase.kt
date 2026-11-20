
package com.example.sulitpackgoui.rider

import com.google.firebase.firestore.FirebaseFirestore

class RiderDatabase {

    private val database = FirebaseFirestore.getInstance()

    fun saveRider(
        rider: Rider,
        onResult: (Boolean, String) -> Unit
    ) {
        database.collection("riders")
            .document(rider.uid)
            .set(rider)
            .addOnSuccessListener {
                onResult(true, "Rider saved successfully.")
            }
            .addOnFailureListener { error ->
                onResult(
                    false,
                    error.message ?: "Failed to save rider."
                )
            }
    }

    fun getRider(
        uid: String,
        onResult: (Boolean, String, Rider?) -> Unit
    ) {
        if (uid.isBlank()) {
            onResult(false, "Invalid rider ID.", null)
            return
        }

        database.collection("riders")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                val rider = document.toObject(Rider::class.java)

                if (rider == null) {
                    onResult(
                        false,
                        "Rider profile not found.",
                        null
                    )
                } else {
                    onResult(true, "Rider found.", rider)
                }
            }
            .addOnFailureListener { error ->
                onResult(
                    false,
                    error.message ?: "Failed to get rider.",
                    null
                )
            }
    }

    // Update availability using the latest Firestore data.
    fun updateAvailability(
        rider: Rider,
        isAvailable: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        if (rider.uid.isBlank()) {
            onResult(false, "Rider is not logged in.")
            return
        }

        val riderReference = database
            .collection("riders")
            .document(rider.uid)

        database.runTransaction { transaction ->

            val riderDocument = transaction.get(riderReference)

            if (!riderDocument.exists()) {
                throw IllegalStateException("Rider profile not found.")
            }

            val currentOrderId =
                riderDocument.getString("currentOrderId") ?: ""

            if (currentOrderId.isNotBlank()) {
                throw IllegalStateException(
                    "Finish your current delivery first."
                )
            }

            transaction.update(
                riderReference,
                "isAvailable",
                isAvailable
            )

            isAvailable

        }.addOnSuccessListener { updatedAvailability ->

            rider.isAvailable = updatedAvailability

            if (updatedAvailability) {
                onResult(true, "You are now available.")
            } else {
                onResult(true, "You are now unavailable.")
            }

        }.addOnFailureListener { error ->

            onResult(
                false,
                error.message ?: "Failed to update availability."
            )
        }
    }
}
