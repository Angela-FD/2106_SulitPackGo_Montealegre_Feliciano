
package com.example.sulitpackgoui.rider

import com.example.sulitpackgoui.order.OrderData
import com.google.firebase.firestore.FirebaseFirestore

class RiderOrder {

    private val database = FirebaseFirestore.getInstance()

    fun getPendingOrders(
        rider: Rider,
        onResult: (Boolean, String, List<OrderData>) -> Unit
    ) {
        database.collection("orders")
            .whereEqualTo("status", "Pending")
            .get()
            .addOnSuccessListener { result ->

                val pendingOrders = mutableListOf<OrderData>()

                for (document in result.documents) {
                    val order = document.toObject(OrderData::class.java)

                    if (order != null &&
                        order.vehicleType == rider.vehicleType
                    ) {
                        pendingOrders.add(order)
                    }
                }

                pendingOrders.sortByDescending { it.createdAt }

                onResult(
                    true,
                    "Pending orders found.",
                    pendingOrders
                )
            }
            .addOnFailureListener { error ->
                onResult(
                    false,
                    getErrorMessage(error, "Failed to get pending orders."),
                    emptyList()
                )
            }
    }

    // ACCEPT ORDER: update the order and rider atomically.
    fun acceptOrder(
        rider: Rider,
        orderId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (rider.uid.isBlank()) {
            onResult(false, "Rider is not logged in.")
            return
        }

        if (orderId.isBlank()) {
            onResult(false, "Invalid order.")
            return
        }

        val orderReference = database
            .collection("orders")
            .document(orderId)

        val riderReference = database
            .collection("riders")
            .document(rider.uid)

        database.runTransaction { transaction ->

            val orderDocument = transaction.get(orderReference)
            val riderDocument = transaction.get(riderReference)

            if (!orderDocument.exists()) {
                throw IllegalStateException("Order not found.")
            }

            if (!riderDocument.exists()) {
                throw IllegalStateException("Rider profile not found.")
            }

            val order = orderDocument.toObject(OrderData::class.java)
                ?: throw IllegalStateException("Failed to read order.")

            val latestRider = riderDocument.toObject(Rider::class.java)
                ?: throw IllegalStateException("Failed to read rider profile.")

            if (order.status != "Pending") {
                throw IllegalStateException(
                    "Order is no longer available."
                )
            }

            if (order.vehicleType != latestRider.vehicleType) {
                throw IllegalStateException(
                    "This order requires a ${order.vehicleType}."
                )
            }

            if (!latestRider.isAvailable ||
                latestRider.currentOrderId.isNotBlank()
            ) {
                throw IllegalStateException(
                    "Rider already has an active order."
                )
            }

            transaction.update(
                orderReference,
                "riderId",
                rider.uid,
                "status",
                "Pick Up Pending"
            )

            transaction.update(
                riderReference,
                "isAvailable",
                false,
                "currentOrderId",
                orderId
            )

            "Order accepted successfully."

        }.addOnSuccessListener { message ->

            rider.isAvailable = false
            rider.currentOrderId = orderId

            onResult(true, message)

        }.addOnFailureListener { error ->
            onResult(
                false,
                getErrorMessage(error, "Failed to accept order.")
            )
        }
    }

    fun updateDeliveryStatus(
        rider: Rider,
        onResult: (Boolean, String) -> Unit
    ) {
        if (rider.uid.isBlank()) {
            onResult(false, "Rider is not logged in.")
            return
        }

        if (rider.currentOrderId.isBlank()) {
            onResult(false, "Rider has no active order.")
            return
        }

        val orderId = rider.currentOrderId

        val orderReference = database
            .collection("orders")
            .document(orderId)

        val riderReference = database
            .collection("riders")
            .document(rider.uid)

        database.runTransaction { transaction ->

            // Read both documents before performing any writes.
            val orderDocument = transaction.get(orderReference)
            val riderDocument = transaction.get(riderReference)

            if (!orderDocument.exists()) {
                throw IllegalStateException("Order not found.")
            }

            if (!riderDocument.exists()) {
                throw IllegalStateException("Rider profile not found.")
            }

            val order = orderDocument.toObject(OrderData::class.java)
                ?: throw IllegalStateException("Failed to read order.")

            val latestRider = riderDocument.toObject(Rider::class.java)
                ?: throw IllegalStateException("Failed to read rider profile.")

            if (order.riderId != rider.uid) {
                throw IllegalStateException(
                    "This order does not belong to this rider."
                )
            }

            if (latestRider.currentOrderId != orderId) {
                throw IllegalStateException(
                    "Rider's active order does not match."
                )
            }

            val newStatus = when (order.status) {
                "Pick Up Pending" -> "Out for Delivery"
                "Out for Delivery" -> "Delivered"
                else -> throw IllegalStateException(
                    "No next delivery status."
                )
            }

            // Always update the order status.
            transaction.update(
                orderReference,
                "status",
                newStatus
            )

            // When delivered, release the rider in the same transaction.
            if (newStatus == "Delivered") {
                transaction.update(
                    riderReference,
                    "isAvailable",
                    true,
                    "currentOrderId",
                    ""
                )
            }

            newStatus

        }.addOnSuccessListener { newStatus ->

            if (newStatus == "Delivered") {
                rider.isAvailable = true
                rider.currentOrderId = ""

                onResult(
                    true,
                    "Order delivered successfully."
                )
            } else {
                onResult(
                    true,
                    "Order status updated to $newStatus."
                )
            }

        }.addOnFailureListener { error ->
            onResult(
                false,
                getErrorMessage(
                    error,
                    "Failed to update delivery status."
                )
            )
        }
    }

    fun getCurrentOrder(
        rider: Rider,
        onResult: (Boolean, String, OrderData?) -> Unit
    ) {
        if (rider.currentOrderId.isBlank()) {
            onResult(
                false,
                "Rider has no active order.",
                null
            )
            return
        }

        getOrder(rider.currentOrderId) { success, message, order ->

            if (!success) {
                onResult(false, message, null)
            } else if (order == null) {
                onResult(false, "Order not found.", null)
            } else if (order.riderId != rider.uid) {
                onResult(
                    false,
                    "This order is not assigned to this rider.",
                    null
                )
            } else {
                onResult(true, "Current order found.", order)
            }
        }
    }

    fun getOrder(
        orderId: String,
        onResult: (Boolean, String, OrderData?) -> Unit
    ) {
        if (orderId.isBlank()) {
            onResult(false, "Invalid order ID.", null)
            return
        }

        database.collection("orders")
            .document(orderId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    onResult(false, "Order not found.", null)
                } else {
                    val order = document.toObject(OrderData::class.java)

                    if (order == null) {
                        onResult(
                            false,
                            "Failed to read order.",
                            null
                        )
                    } else {
                        onResult(true, "Order found.", order)
                    }
                }
            }
            .addOnFailureListener { error ->
                onResult(
                    false,
                    getErrorMessage(error, "Failed to get order."),
                    null
                )
            }
    }

    fun getDeliveryHistory(
        rider: Rider,
        onResult: (Boolean, String, List<OrderData>) -> Unit
    ) {
        if (rider.uid.isBlank()) {
            onResult(
                false,
                "Rider is not logged in.",
                emptyList()
            )
            return
        }

        database.collection("orders")
            .whereEqualTo("riderId", rider.uid)
            .get()
            .addOnSuccessListener { result ->

                val history = mutableListOf<OrderData>()

                for (document in result.documents) {
                    val order = document.toObject(OrderData::class.java)

                    if (order != null && order.status == "Delivered") {
                        history.add(order)
                    }
                }

                history.sortByDescending { it.createdAt }

                onResult(
                    true,
                    "Delivery history found.",
                    history
                )
            }
            .addOnFailureListener { error ->
                onResult(
                    false,
                    getErrorMessage(
                        error,
                        "Failed to get delivery history."
                    ),
                    emptyList()
                )
            }
    }

    private fun getErrorMessage(
        error: Exception,
        defaultMessage: String
    ): String {
        return error.message ?: defaultMessage
    }
}
