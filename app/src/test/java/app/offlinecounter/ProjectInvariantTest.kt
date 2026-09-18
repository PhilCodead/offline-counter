package app.offlinecounter

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectInvariantTest {
    @Test
    fun projectUsesReleasePackageAndRemainsOffline() {
        val buildScript = File("build.gradle.kts").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(buildScript.contains("namespace = \"app.offlinecounter\""))
        assertTrue(buildScript.contains("applicationId = \"app.offlinecounter\""))
        assertTrue(buildScript.contains("versionCode = 14"))
        assertTrue(buildScript.contains("versionName = \"0.14\""))
        assertFalse(manifest.contains("android.permission.INTERNET"))
        assertTrue(manifest.contains(".CounterAccessibilityService"))
    }
}
