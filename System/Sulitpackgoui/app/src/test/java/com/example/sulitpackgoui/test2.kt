
package com.example.sulitpackgoui.logic

import com.example.sulitpackgoui.admin.VehicleFee
import org.junit.Assert.*
import org.junit.Test

class FeeCalculatorTest {

    @Test
    fun calculateBaseFee_returnsCorrectAmount() {
        val fee = VehicleFee(
            "Motor",
            50.0,
            10.0
        )

        val calculator = FeeCalculator(fee, 5.0)

        assertEquals(
            50.0,
            calculator.calculateBaseFee(),
            0.0
        )
    }

    @Test
    fun calculateDistanceFee_returnsCorrectAmount() {
        val fee = VehicleFee(
            "Motor",
            50.0,
            10.0
        )

        val calculator = FeeCalculator(fee, 5.0)

        assertEquals(
            50.0,
            calculator.calculateDistanceFee(),
            0.0
        )
    }

    @Test
    fun calculateTotalFee_returnsCorrectAmount() {
        val fee = VehicleFee(
            "Motor",
            50.0,
            10.0
        )

        val calculator = FeeCalculator(fee, 5.0)

        assertEquals(
            100.0,
            calculator.calculateTotalFee(),
            0.0
        )
    }

    @Test
    fun calculateTotalFee_withZeroDistance_returnsBaseFee() {
        val fee = VehicleFee(
            "Motor",
            50.0,
            10.0
        )

        val calculator = FeeCalculator(fee, 0.0)

        assertEquals(
            50.0,
            calculator.calculateTotalFee(),
            0.0
        )
    }

    @Test
    fun calculateTotalFee_withZeroFees_returnsZero() {
        val fee = VehicleFee(
            "Motor",
            0.0,
            0.0
        )

        val calculator = FeeCalculator(fee, 10.0)

        assertEquals(
            0.0,
            calculator.calculateTotalFee(),
            0.0
        )
    }
}
