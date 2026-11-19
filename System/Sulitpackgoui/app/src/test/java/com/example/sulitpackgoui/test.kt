
package com.example.sulitpackgoui.logic

import org.junit.Assert.*
import org.junit.Test

class VehicleTest {

    @Test
    fun selectMotor_returnsCorrectWeightLimit() {
        val vehicle = Vehicle()

        vehicle.selectVehicle("Motor")

        assertEquals("Motor", vehicle.vehicleType)
        assertEquals(40.0, vehicle.weightLimit, 0.0)
    }

    @Test
    fun selectTricycle_returnsCorrectWeightLimit() {
        val vehicle = Vehicle()

        vehicle.selectVehicle("Tricycle")

        assertEquals("Tricycle", vehicle.vehicleType)
        assertEquals(80.0, vehicle.weightLimit, 0.0)
    }

    @Test
    fun selectVan_returnsCorrectWeightLimit() {
        val vehicle = Vehicle()

        vehicle.selectVehicle("Van")

        assertEquals("Van", vehicle.vehicleType)
        assertEquals(300.0, vehicle.weightLimit, 0.0)
    }

    @Test
    fun selectTruck_returnsCorrectWeightLimit() {
        val vehicle = Vehicle()

        vehicle.selectVehicle("Truck")

        assertEquals("Truck", vehicle.vehicleType)
        assertEquals(1000.0, vehicle.weightLimit, 0.0)
    }

    @Test
    fun checkWeight_withValidWeight_returnsTrue() {
        val vehicle = Vehicle()
        vehicle.selectVehicle("Motor")

        assertTrue(vehicle.checkWeight(30.0))
    }

    @Test
    fun checkWeight_exceedingWeightLimit_returnsFalse() {
        val vehicle = Vehicle()
        vehicle.selectVehicle("Motor")

        assertFalse(vehicle.checkWeight(50.0))
    }

    @Test
    fun selectInvalidVehicle_resetsVehicleInformation() {
        val vehicle = Vehicle()

        vehicle.selectVehicle("Airplane")

        assertEquals("", vehicle.vehicleType)
        assertEquals(0.0, vehicle.weightLimit, 0.0)
    }
}
