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
    private val tasks = NavbarDestination.TASKS
    private val storage = NavbarDestination.STORAGE
    private val containers = NavbarDestination.CONTAINERS
    private val vms = NavbarDestination.VMS

    @Test
    fun `home is required and unique`() {
        assertEquals(1, NavbarDestination.required.size)
        assertEquals(home, NavbarDestination.required.single())
    }

    @Test
    fun `defaults are the three tabs the owner asked for`() {
        val defaults = NavbarDestination.defaults
        // Details, Storage, Tasks - in that order. Apps/Containers/VMs became
        // sub-tabs of Tasks, and Performance is no longer a tab at all.
        assertEquals(
            listOf(NavbarDestination.HOME, NavbarDestination.STORAGE, NavbarDestination.TASKS),
            defaults
        )
        assertEquals(home, defaults.first())
        assertTrue(
            "Performance must not be a bar entry any more",
            NavbarDestination.entries.none { it.route == "performance" }
        )
    }

    @Test
    fun `effective destinations preserve the user's order`() {
        val result = PersonalizationManager.effectiveDestinations(
            listOf(home, containers, tasks, vms)
        )
        assertEquals(listOf(home, containers, tasks, vms), result)
    }

    @Test
    fun `effective destinations always put home first even if it is absent or last`() {
        assertEquals(
            home,
            PersonalizationManager.effectiveDestinations(listOf(tasks, vms)).first()
        )
        assertEquals(
            listOf(home, tasks, vms),
            PersonalizationManager.effectiveDestinations(listOf(tasks, vms, home))
        )
    }

    @Test
    fun `effective destinations collapse duplicates and survive an empty selection`() {
        assertEquals(
            listOf(home, tasks),
            PersonalizationManager.effectiveDestinations(listOf(home, tasks, tasks, home))
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
        assertEquals(NavbarDestination.TASKS, moved[1])
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
