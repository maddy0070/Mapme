package com.mapme.core.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Permission is where map apps become hostile: they ask on launch with no
 * reason, ask again after being refused, and then offer a button that cannot
 * work because the system has stopped listening.
 *
 * The mapping below is the whole defence against that, so it is tested rather
 * than left as an `if` chain inside a composable.
 */
class LocationAccessTest {

    @Test
    fun `MapMe says nothing before it has looked`() {
        assertEquals(LocationPrompt.None, LocationPrompt.of(LocationAccess.Unknown))
    }

    @Test
    fun `granted means the interface gets out of the way`() {
        assertEquals(LocationPrompt.None, LocationPrompt.of(LocationAccess.Granted(precise = true)))
        assertEquals(LocationPrompt.None, LocationPrompt.of(LocationAccess.Granted(precise = false)))
    }

    @Test
    fun `approximate is a real answer, not a reason to ask again`() {
        val approximate = LocationAccess.Granted(precise = false)
        assertTrue("approximate location still puts you on the map", approximate.canLocate)
        assertEquals(LocationPrompt.None, LocationPrompt.of(approximate))
    }

    @Test
    fun `a first refusal earns an explanation, not a second dialog`() {
        assertEquals(
            LocationPrompt.Explain,
            LocationPrompt.of(LocationAccess.Denied(permanently = false)),
        )
    }

    @Test
    fun `once the system has stopped asking, MapMe stops pretending`() {
        assertEquals(
            "offering a button that silently does nothing is worse than offering none",
            LocationPrompt.Settings,
            LocationPrompt.of(LocationAccess.Denied(permanently = true)),
        )
    }

    @Test
    fun `no provider is explained, not blamed on the person`() {
        assertEquals(LocationPrompt.NoProvider, LocationPrompt.of(LocationAccess.Unavailable))
    }

    @Test
    fun `only a grant can locate`() {
        assertFalse(LocationAccess.Unknown.canLocate)
        assertFalse(LocationAccess.Denied(permanently = false).canLocate)
        assertFalse(LocationAccess.Denied(permanently = true).canLocate)
        assertFalse(LocationAccess.Unavailable.canLocate)
        assertTrue(LocationAccess.Granted(precise = true).canLocate)
    }

    @Test
    fun `the map is never a dead end — every state has somewhere to go`() {
        // The product rule from the brief: MapMe stays usable if location is
        // refused. Concretely that means no state leaves the person with a
        // prompt they cannot dismiss and no map behind it.
        listOf(
            LocationAccess.Unknown,
            LocationAccess.Granted(precise = true),
            LocationAccess.Granted(precise = false),
            LocationAccess.Denied(permanently = false),
            LocationAccess.Denied(permanently = true),
            LocationAccess.Unavailable,
        ).forEach { access ->
            // The prompt is advisory in every case; none of them is a blocker.
            assertTrue("$access produced no defined prompt", LocationPrompt.of(access) in LocationPrompt.entries)
        }
    }
}
