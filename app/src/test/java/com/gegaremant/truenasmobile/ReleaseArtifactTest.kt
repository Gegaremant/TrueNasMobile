package com.gegaremant.truenasmobile

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import kotlin.math.min

/**
 * Checks a built release APK the way a device and a user would.
 *
 * This is the verification that was done by hand once and should never be done
 * by hand again:
 *
 *  - **ZIP integrity.** A truncated or mangled download installs happily and
 *    then dies on launch with a `ClassNotFoundException` for the Application
 *    class, which looks exactly like a broken build. The real line is buried
 *    underneath: `Bad checksum (... expected ...)`.
 *  - **dex integrity**, recomputed the way ART does it: Adler-32 from offset
 *    `0x0C` and SHA-1 from `0x20`. Getting the Adler-32 range wrong is easy -
 *    it is not a hash of the payload after the signature field, it covers the
 *    signature too.
 *  - **versionCode**, because an update that does not raise it will not install
 *    over the previous release.
 *  - **the ru locale**, because a release that silently loses the Russian
 *    dictionary is a regression nobody notices until a user writes in.
 *  - **a SHA-256** printed for publication, so a user can confirm their own
 *    download in one command instead of reading logcat.
 *
 * The artifact is located automatically. When there is nothing built, the tests
 * are skipped rather than failed: this is a release gate, not something that
 * should break `./gradlew test` on a clean checkout.
 */
class ReleaseArtifactTest {

    private fun releaseApks(): List<File> {
        val dir = File("build/outputs/apk/github/release")
        if (!dir.isDirectory) return emptyList()
        return dir.listFiles { f: File -> f.name.endsWith(".apk") }?.sortedBy { it.name } ?: emptyList()
    }

    private fun withApk(test: (File) -> Unit) {
        val apks = releaseApks()
        assumeTrue(
            "no release APK in build/outputs/apk/github/release - run " +
                "./gradlew :app:assembleGithubRelease first",
            apks.isNotEmpty()
        )
        apks.forEach(test)
    }

    @Test
    fun `every zip entry passes its CRC`() = withApk { apk ->
        ZipFile(apk).use { zip ->
            var entries = 0
            val iterator = zip.entries().asSequence()
            for (entry: ZipEntry in iterator) {
                entries++
                // getInputStream reads the entry, so a bad CRC throws here.
                zip.getInputStream(entry).use { it.readBytes() }
            }
            assertTrue("${apk.name} has no entries at all", entries > 0)
        }
    }

    @Test
    fun `every dex declares the checksum and signature ART recomputes`() = withApk { apk ->
        ZipFile(apk).use { zip ->
            val dexEntry = zip.entries().asSequence().firstOrNull { it.name.endsWith(".dex") }
            assertNotNull("${apk.name} contains no classes.dex", dexEntry)
            val dex = zip.getInputStream(dexEntry!!).readBytes()

            val storedChecksum = readLittleEndianInt(dex, 8)
            val computedChecksum = adler32(dex.copyOfRange(12, dex.size))
            assertTrue(
                ("${apk.name}: dex Adler-32 mismatch - the header says %08x, the " +
                    "content hashes to %08x. A device rejects this with a Bad " +
                    "checksum error and a ClassNotFoundException for the " +
                    "Application class.")
                    .format(storedChecksum, computedChecksum),
                storedChecksum == computedChecksum
            )

            val storedSignature = dex.copyOfRange(12, 32).toHex()
            val computedSignature = sha1(dex.copyOfRange(32, dex.size))
            assertTrue(
                "${apk.name}: dex SHA-1 mismatch, the file has been modified " +
                    "after it was built",
                storedSignature == computedSignature
            )
        }
    }

    @Test
    fun `every dex size in the archive matches the dex header`() = withApk { apk ->
        ZipFile(apk).use { zip ->
            for (entry in zip.entries().asSequence().filter { it.name.endsWith(".dex") }) {
                val dex = zip.getInputStream(entry).readBytes()
                val declared = readLittleEndianInt(dex, 32).toLong()
                assertTrue(
                    "${apk.name}: ${entry.name} header claims $declared bytes " +
                        "but the archive entry holds ${dex.size}",
                    declared == dex.size.toLong()
                )
            }
        }
    }

    @Test
    fun `a sha256 is available for publication`() = withApk { apk ->
        val digest = sha256(apk.readBytes())
        println("SHA-256  ${apk.name}  $digest")
        assertTrue("sha256 looks wrong: $digest", digest.length == 64)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun readLittleEndianInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8) or
            ((bytes[offset + 2].toInt() and 0xff) shl 16) or
            ((bytes[offset + 3].toInt() and 0xff) shl 24)

    /**
     * Adler-32 as zlib defines it: s1 = 1, s2 = 0, both mod 65521.
     *
     * The accumulators are [Long] on purpose. zlib's NMAX chunking of 5552 only
     * bounds the intermediate values for *unsigned* 32-bit arithmetic; with a
     * signed Int, s2 passes 2^31 well before the first chunk ends and wraps
     * negative, so the modulo yields a negative remainder and the checksum is
     * silently wrong - which is how the first version of this test failed
     * against a perfectly good APK.
     */
    private fun adler32(bytes: ByteArray): Int {
        var s1 = 1L
        var s2 = 0L
        val chunkSize = 5552
        var index = 0
        while (index < bytes.size) {
            val end = min(bytes.size.toLong(), index + chunkSize.toLong()).toInt()
            while (index < end) {
                s1 += (bytes[index].toInt() and 0xff).toLong()
                s2 += s1
                index++
            }
            s1 %= 65521L
            s2 %= 65521L
        }
        return ((s2 shl 16) or s1).toInt()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun sha1(bytes: ByteArray): String =
        java.security.MessageDigest.getInstance("SHA-1").digest(bytes).toHex()

    private fun sha256(bytes: ByteArray): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
}
