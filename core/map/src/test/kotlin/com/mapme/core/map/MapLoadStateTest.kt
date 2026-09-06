package com.mapme.core.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapLoadStateTest {

    @Test
    fun `a retry is offered only where it could actually work`() {
        assertTrue("no network now may be network in a minute", MapFailure.Offline.retryable)
        assertTrue("a tile fetch is worth another go", MapFailure.Tiles.retryable)
        assertFalse(
            "a style that will not parse will not parse the second time either",
            MapFailure.Style.retryable,
        )
    }

    @Test
    fun `failure carries a reason, so the screen can say something true`() {
        val failed = MapLoadState.Failed(MapFailure.Offline)
        assertTrue(failed.reason.retryable)
    }
}
