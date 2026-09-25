package com.gegaremant.truenasmobile

import com.gegaremant.truenasmobile.data.helpers.NavbarDestination
import com.gegaremant.truenasmobile.data.helpers.PersonalizationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bottom navigation bar used to be a hardcoded list in `MainScreen`, while a
 * whole personalization subsystem - save, load, order, toggle - sat next to it
 * with no call sites at all. These tests pin the behaviour the UI now depends
 * on, because "user reorders the bar and nothing happens" is exactly the bug
 * that was there before.
 */
class NavbarDestinationTest {

    private val home = NavbarDestination.HOME
    private val apps = NavbarDestination.APPS
    private val storage = NavbarDestination.STORAGE
    private val containers = NavbarDestination.CONTAINERS
    private val vms = NavbarDestination.VMS

    @Test
    fun `home is required and unique`() {
        assertEquals(1, NavbarDestination.required.size)
        assertEquals(home, NavbarDestination.required.single())
    }

    @Test
    fun `defaults keep the classic bar and expose apps`() {
        val defaults = NavbarDestination.defaults
        // Home/Storage/Tasks/Performance were the hardcoded bar; losing them
        // would be a regression for existing users.
        listOf(home, storage, NavbarDestination.TASKS, NavbarDestination.PERFORMANCE)
            .forEach { assertTrue("$it missing from defaults", it in defaults) }
        assertTrue("apps should be reachable by default", apps in defaults)
        assertEquals(home, defaults.first())
    }

    @Test
    fun `effective destinations preserve the user's order`() {
        val result = PersonalizationManager.effectiveDestinations(
            listOf(home, containers, apps, vms)
        )
        assertEquals(listOf(home, containers, apps, vms), result)
    }

    @Test
    fun `effective destinations always put home first even if it is absent or last`() {
        assertEquals(
            home,
            PersonalizationManager.effectiveDestinations(listOf(apps, vms)).first()
        )
        assertEquals(
            listOf(home, apps, vms),
            PersonalizationManager.effectiveDestinations(listOf(apps, vms, home))
        )
    }

    @Test
    fun `effective destinations collapse duplicates and survive an empty selection`() {
        assertEquals(
            listOf(home, apps),
            PersonalizationManager.effectiveDestinations(listOf(home, apps, apps, home))
        )
        assertEquals(
            listOf(home),
            PersonalizationManager.effectiveDestinations(emptyList())
        )
    }

    @Test
    fun `toggling switches a destination on and off`() {
        val defaults = PersonalizationManager.defaultsForTest
        assertTrue(vms !in defaults)

        val off = PersonalizationManager.toggleDestination(defaults, vms)
        assertTrue(vms in off)
        assertEquals(defaults.size + 1, off.size)

        // Turning it back off must restore the bar exactly.
        val on = PersonalizationManager.toggleDestination(off, vms)
        assertTrue(vms !in on)
        assertEquals(defaults, on)
    }

    @Test
    fun `toggling home is a no-op`() {
        val defaults = PersonalizationManager.defaultsForTest
        assertEquals(defaults, PersonalizationManager.toggleDestination(defaults, home))
    }

    @Test
    fun `moving respects the home slot and the list bounds`() {
        val bar = PersonalizationManager.defaultsForTest
        assertEquals(1, bar.indexOf(storage))

        // Nothing may move into the first slot: that is Home.
        assertEquals(bar, PersonalizationManager.moveDestination(bar, storage, -1))
        assertEquals(home, PersonalizationManager.moveDestination(bar, storage, -1).first())

        // The last item cannot move further down.
        val last = bar.last()
        assertEquals(bar, PersonalizationManager.moveDestination(bar, last, 1))

        // A real move swaps neighbours.
        val moved = PersonalizationManager.moveDestination(bar, storage, 1)
        assertEquals(apps, moved[1])
        assertEquals(storage, moved[2])
    }

    @Test
    fun `a moved destination can be moved back`() {
        val bar = PersonalizationManager.defaultsForTest
        val there = PersonalizationManager.moveDestination(bar, storage, 1)
        val back = PersonalizationManager.moveDestination(there, storage, -1)
        assertEquals(bar, back)
    }
}

private val PersonalizationManager.defaultsForTest: List<NavbarDestination>
    get() = PersonalizationManager.effectiveDestinations(NavbarDestination.defaults)
