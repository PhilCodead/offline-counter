package app.offlinecounter

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 35, 36])
class PrivacyConfigurationTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun mergedManifestDoesNotStartDownloadableEmojiFonts() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA,
        )
        packageInfo.providers.orEmpty().forEach { provider ->
            assertFalse(
                "${provider.name} must not initialize downloadable emoji fonts",
                provider.metaData?.containsKey("androidx.emoji2.text.EmojiCompatInitializer") == true,
            )
        }
    }

    @Test
    fun cloudBackupExcludesEveryStorageDomain() {
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
        assertEveryDomainExcluded("cloud-backup")
    }

    @Test
    fun deviceTransferExcludesEveryStorageDomain() {
        assertEveryDomainExcluded("device-transfer")
    }

    private fun assertEveryDomainExcluded(section: String) {
        val excluded = mutableSetOf<String>()
        var insideSection = false
        context.resources.getXml(R.xml.data_extraction_rules).use { parser ->
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> when {
                        parser.name == section -> insideSection = true
                        insideSection && parser.name == "exclude" &&
                            parser.getAttributeValue(null, "path") == "." -> {
                            excluded += parser.getAttributeValue(null, "domain")
                        }
                    }
                    XmlPullParser.END_TAG -> if (parser.name == section) insideSection = false
                }
            }
        }
        assertEquals(
            "$section must exclude all credential-protected and device-protected storage",
            setOf(
                "root", "file", "database", "sharedpref", "external",
                "device_root", "device_file", "device_database", "device_sharedpref",
            ),
            excluded,
        )
    }
}
