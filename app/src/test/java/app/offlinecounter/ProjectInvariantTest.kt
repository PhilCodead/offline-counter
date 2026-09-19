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
        assertFalse(manifest.contains("android.permission.INTERNET"))
        assertTrue(manifest.contains(".CounterAccessibilityService"))
        assertTrue(manifest.split("android:icon=\"@mipmap/ic_launcher\"").size - 1 >= 3)
    }

    @Test
    fun setupGuideMatchesTheSamsungAccessibilityRoute() {
        val strings = File("src/main/res/values/strings.xml").readText()

        assertTrue(strings.contains("<string name=\"enable_accessibility\">Включить Accessibility</string>"))
        assertTrue(strings.contains("2. Откройте «Установленные приложения»."))
        assertTrue(strings.contains("3. Выберите «Оффлайн-счётчик»."))
        assertTrue(strings.contains("4. Переключите ползунок во включённое положение и нажмите «Разрешить»."))
        assertTrue(strings.contains("5. Вернитесь в исходное приложение и используйте плавающее окно."))
    }

    @Test
    fun developerFooterClearsTheTabletDock() {
        val layout = File("src/main/res/layout/activity_main.xml").readText()

        assertTrue(layout.contains("android:id=\"@+id/developerFooter\""))
        assertTrue(layout.contains("android:layout_marginBottom=\"56dp\""))
    }

    @Test
    fun overlayHasNoBrightStrokeAndLauncherArtworkUsesSafeZone() {
        val foreground = File("src/main/res/drawable/ic_launcher_foreground.xml").readText()

        assertTrue(foreground.contains("android:width=\"72dp\""))
        assertTrue(foreground.contains("android:height=\"72dp\""))
    }
}
