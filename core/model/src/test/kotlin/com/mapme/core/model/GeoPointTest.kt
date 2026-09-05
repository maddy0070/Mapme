package com.mapme.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GeoPointTest {

    private val london = GeoPoint(51.5074, -0.1278)
    private val paris = GeoPoint(48.8566, 2.3522)

    @Test
    fun `distance between two cities matches the known great-circle value`() {
        // 343.6 km by the same spherical model. A kilometre of tolerance is
        // generous; it is here to catch a broken formula, not rounding.
        assertEquals(343_556.5, london.distanceTo(paris), 1_000.0)
    }

    @Test
    fun `distance is symmetric`() {
        assertEquals(london.distanceTo(paris), paris.distanceTo(london), 0.001)
    }

    @Test
    fun `a point is no distance from itself`() {
        // Naive haversine implementations return NaN here when floating point
        // pushes the argument of asin just past 1.
        assertEquals(0.0, london.distanceTo(london), 0.0001)
    }

    @Test
    fun `antipodal points do not produce NaN`() {
        val here = GeoPoint(0.0, 0.0)
        val opposite = GeoPoint(0.0, 180.0)
        assertEquals(20_015_114.0, here.distanceTo(opposite), 100.0)
    }

    @Test
    fun `a degree of latitude is about 111 kilometres anywhere`() {
        assertEquals(111_195.0, GeoPoint(0.0, 0.0).distanceTo(GeoPoint(1.0, 0.0)), 50.0)
        assertEquals(111_195.0, GeoPoint(60.0, 25.0).distanceTo(GeoPoint(61.0, 25.0)), 50.0)
    }

    @Test
    fun `bearing due north is zero and due east is ninety`() {
        val origin = GeoPoint(51.5074, -0.1278)
        assertEquals(0.0, origin.bearingTo(GeoPoint(51.5084, -0.1278)), 0.01)
        assertEquals(90.0, GeoPoint(0.0, 0.0).bearingTo(GeoPoint(0.0, 1.0)), 0.01)
    }

    @Test
    fun `coordinates off the planet are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { GeoPoint(91.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { GeoPoint(0.0, 181.0) }
    }
}
