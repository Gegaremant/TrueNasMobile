package com.imnotndesh.truehub.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricLockPolicyTest {

    private val delayMs = 30_000L

    @Test
    fun neverBackgrounded_doesNotReLock() {
        assertFalse(shouldReLock(backgroundedAtMillis = null, nowMillis = 100_000L, autoLockDelayMillis = delayMs))
    }

    @Test
    fun exactlyAtThreshold_doesNotReLock() {
        // Lock arms strictly after the delay (a quick foreground flip must not re-lock).
        assertFalse(shouldReLock(backgroundedAtMillis = 70_000L, nowMillis = 100_000L, autoLockDelayMillis = delayMs))
    }

    @Test
    fun oneMsOverThreshold_reLocks() {
        assertTrue(shouldReLock(backgroundedAtMillis = 69_999L, nowMillis = 100_000L, autoLockDelayMillis = delayMs))
    }

    @Test
    fun longBackground_reLocks() {
        assertTrue(shouldReLock(backgroundedAtMillis = 1_000L, nowMillis = 100_000L, autoLockDelayMillis = delayMs))
    }

    @Test
    fun zeroDelayReLocksOnAnySpentTime() {
        assertTrue(shouldReLock(backgroundedAtMillis = 100_000L, nowMillis = 100_001L, autoLockDelayMillis = 0L))
    }
}