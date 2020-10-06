/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.search.region

import kotlinx.coroutines.runBlocking
import mozilla.components.service.location.LocationService
import mozilla.components.support.test.fakes.FakeClock
import mozilla.components.support.test.fakes.FakeContext
import mozilla.components.support.test.fakes.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RegionManagerTest {
    @Test
    fun `Initial state`() {
        val regionManager = RegionManager(
            context = FakeContext(),
            locationService = FakeLocationService(),
            currentTime = FakeClock()::time,
            preferences = FakeSharedPreferences()
        )

        assertNull(regionManager.region())
    }

    @Test
    fun `First update`() {
        val locationService = FakeLocationService(
            region = LocationService.Region("DE", "Germany")
        )

        val regionManager = RegionManager(
            context = FakeContext(),
            locationService = locationService,
            currentTime = FakeClock()::time,
            preferences = FakeSharedPreferences()
        )

        val updatedRegion = runBlocking { regionManager.update() }
        assertNotNull(updatedRegion!!)
        assertEquals("DE", updatedRegion.current)
        assertEquals("DE", updatedRegion.home)
    }

    @Test
    fun `Updating to new home region`() {
        val clock = FakeClock()

        val locationService = FakeLocationService(
            region = LocationService.Region("DE", "Germany")
        )

        val regionManager = RegionManager(
            context = FakeContext(),
            locationService = locationService,
            currentTime = clock::time,
            preferences = FakeSharedPreferences()
        )

        runBlocking { regionManager.update() }

        locationService.region = LocationService.Region("FR", "France")

        // Should not be updated since the "home" region didn't change
        assertNull(runBlocking { regionManager.update() })
        assertEquals("DE", regionManager.region()?.home)
        assertEquals("FR", regionManager.region()?.current)

        // Let's jump one week into the future!
        clock.advanceBy(60 * 60 * 24 * 7 * 1000)

        // Still not updated because we switch after two weeks
        assertNull(runBlocking { regionManager.update() })
        assertEquals("DE", regionManager.region()?.home)
        assertEquals("FR", regionManager.region()?.current)

        // Let's move the clock 8 more days into the future
        clock.advanceBy(60 * 60 * 24 * 8 * 1000)

        val updatedRegion = (runBlocking { regionManager.update() })
        assertNotNull(updatedRegion!!)
        assertEquals("FR", updatedRegion.home)
        assertEquals("FR", updatedRegion.current)
        assertEquals("FR", regionManager.region()?.home)
        assertEquals("FR", regionManager.region()?.current)
    }

    @Test
    fun `Switching back to home region after staying in different region shortly`() {
        val clock = FakeClock()

        val locationService = FakeLocationService(
            region = LocationService.Region("DE", "Germany")
        )

        val regionManager = RegionManager(
            context = FakeContext(),
            locationService = locationService,
            currentTime = clock::time,
            preferences = FakeSharedPreferences()
        )

        runBlocking { regionManager.update() }

        // Let's jump one week into the future!
        clock.advanceBy(60 * 60 * 24 * 7 * 1000)

        locationService.region = LocationService.Region("FR", "France")

        // Should not be updated since the "home" region didn't change
        assertNull(runBlocking { regionManager.update() })
        assertEquals("DE", regionManager.region()?.home)
        assertEquals("FR", regionManager.region()?.current)

        // Next day, we are back in the home region
        clock.advanceBy(60 * 60 * 24 * 1000)

        locationService.region = LocationService.Region("DE", "Germany")
        assertNull(runBlocking { regionManager.update() })
        assertEquals("DE", regionManager.region()?.home)
        assertEquals("DE", regionManager.region()?.current)

        // Another week forward, we are back in France
        clock.advanceBy(60 * 60 * 24 * 7 * 1000)

        locationService.region = LocationService.Region("FR", "France")

        // The "home" region should not have changed since we haven't been in the other region the
        // whole time.
        assertNull(runBlocking { regionManager.update() })
        assertEquals("DE", regionManager.region()?.home)
        assertEquals("FR", regionManager.region()?.current)
    }
}

class FakeLocationService(
    var region: LocationService.Region? = null,
    private val hasRegionCached: Boolean = false
) : LocationService {
    override suspend fun fetchRegion(readFromCache: Boolean): LocationService.Region? = region
    override fun hasRegionCached(): Boolean = hasRegionCached
}
