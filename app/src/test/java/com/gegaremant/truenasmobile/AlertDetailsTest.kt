package com.gegaremant.truenasmobile

import com.gegaremant.truenasmobile.data.models.System
import com.gegaremant.truenasmobile.ui.alerts.AlertDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The complaint these guard: a notification said "backup failed" and stopped.
 * The reason lives in `args`, and nobody could see it.
 */
class AlertDetailsTest {

    private fun alert(
        klass: String = "BackupTaskFailed",
        formatted: String? = "Backup of dataset data/x failed",
        args: Any? = null
    ) = System.AlertResponse(
        uuid = "uuid",
        source = "CRON",
        klass = klass,
        args = args,
        node = "node",
        key = "key",
        datetime = System.MongoDate(0),
        last_occurrence = System.MongoDate(0),
        dismissed = false,
        mail = null,
        text = "Backup failed",
        id = "1",
        level = "ERROR",
        formatted = formatted,
        one_shot = false
    )

    @Test
    fun `flat arguments become readable lines`() {
        val lines = AlertDetails.lines(
            alert(args = mapOf("dataset" to "data/x", "error" to "No space left"))
        )
        assertEquals(
            listOf("dataset: data/x", "error: No space left"),
            lines
        )
    }

    @Test
    fun `nested arguments keep their path`() {
        val lines = AlertDetails.lines(
            alert(args = mapOf("task" to mapOf("name" to "daily-backup", "pool" to "RAID")))
        )
        assertEquals(
            listOf("task.name: daily-backup", "task.pool: RAID"),
            lines
        )
    }

    @Test
    fun `describe puts the headline first and the reasons under it`() {
        val text = AlertDetails.describe(
            alert(args = mapOf("error" to "Snapshot not found"))
        )
        assertTrue(text.startsWith("Backup of dataset data/x failed"))
        assertTrue(text.contains("error: Snapshot not found"))
    }

    @Test
    fun `an alert without arguments still says something`() {
        val text = AlertDetails.describe(alert(args = null))
        assertEquals("Backup of dataset data/x failed", text)
    }

    @Test
    fun `the class reads as words`() {
        assertEquals("Backup task failed", AlertDetails.readableClass("BackupTaskFailed"))
        // Splitting the camel case is all this does: it does not guess a verb
        // to insert "is" - "Pool dataset locked" is what the stand says, and
        // inventing grammar here would only make it vaguer.
        assertEquals(
            "Pool dataset locked",
            AlertDetails.readableClass("PoolDatasetLocked")
        )
    }

    @Test
    fun `blank arguments are dropped rather than shown as empty`() {
        val lines = AlertDetails.lines(alert(args = mapOf("error" to "", "pool" to "RAID")))
        assertEquals(listOf("pool: RAID"), lines)
    }
}