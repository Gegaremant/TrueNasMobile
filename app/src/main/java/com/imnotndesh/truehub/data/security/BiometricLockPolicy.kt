package com.imnotndesh.truehub.data.security

/**
 * Pure re-lock policy: the app lock is re-armed when the app stayed in the
 * background longer than [autoLockDelayMillis]. A `null` backgroundedAt
 * timestamp (never backgrounded) never re-arms the lock.
 */
fun shouldReLock(
    backgroundedAtMillis: Long?,
    nowMillis: Long,
    autoLockDelayMillis: Long
): Boolean {
    return backgroundedAtMillis != null &&
        nowMillis - backgroundedAtMillis > autoLockDelayMillis
}