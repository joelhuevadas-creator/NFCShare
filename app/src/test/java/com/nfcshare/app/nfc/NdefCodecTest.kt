package com.nfcshare.app.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.nfcshare.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NdefCodecTest {
    private fun roundTrip(draft: Draft) = NdefParser.parse(NdefMessage(NdefFactory.create(draft).toByteArray())).single()
    @Test fun unicodeTextRoundTrips() { assertEquals("¡Hola! 日本語 👋", roundTrip(Draft(text = "¡Hola! 日本語 👋")).content) }
    @Test fun compressedUriRoundTrips() { assertEquals("https://example.com/hola", roundTrip(Draft(kind = ContentKind.URL, url = "https://example.com/hola")).uri) }
    @Test fun utf16WithLanguageCode() {
        val record = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, byteArrayOf(), byteArrayOf(0x82.toByte()) + "es".toByteArray() + "niño".toByteArray(Charsets.UTF_16))
        assertEquals("niño", NdefParser.parse(NdefMessage(arrayOf(record))).single().content)
    }
    @Test fun malformedTextDoesNotCrash() {
        val record = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, byteArrayOf(), byteArrayOf(63, 1))
        assertEquals("Registro inválido", NdefParser.parse(NdefMessage(arrayOf(record))).single().type)
    }
    @Test fun smartPosterDecodesNestedRecords() {
        val nested = NdefMessage(arrayOf(NdefRecord.createUri("https://example.com"), NdefRecord.createTextRecord("es", "Visítame")))
        val poster = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_SMART_POSTER, byteArrayOf(), nested.toByteArray())
        val parsed = NdefParser.parse(NdefMessage(arrayOf(poster))).single()
        assertEquals("Smart Poster", parsed.type); assertEquals("https://example.com", parsed.uri); assertTrue(parsed.content.contains("Visítame"))
    }
    @Test fun contactEscapesNewlineInjection() {
        val parsed = roundTrip(Draft(kind = ContentKind.CONTACT, name = "A\nEND:VCARD", email = "a@example.com"))
        assertEquals("Contacto", parsed.type); assertTrue(parsed.content.contains("FN:A\\nEND:VCARD"))
    }
    @Test fun wifiRoundTripsAndStorageRedactsPassword() {
        val r = roundTrip(Draft(kind = ContentKind.WIFI, ssid = "Mi red", password = "secreto123"))
        assertTrue(r.sensitive); assertTrue(r.content.contains("secreto123"))
        val safe = ScanResult("Wi-Fi", records = listOf(r)).forStorage()
        assertFalse(safe.summary.contains("secreto123")); assertTrue(safe.summary.contains("Mi red"))
    }
    @Test fun externalRecordPreservesType() {
        val r = NdefRecord.createExternal("example.com", "demo", "hola".toByteArray())
        assertEquals("Externo · example.com:demo", NdefParser.parse(NdefMessage(arrayOf(r))).single().type)
    }
    @Test fun binaryMimeHasUsefulHex() {
        val r = NdefRecord.createMime("application/octet-stream", byteArrayOf(0, 1, 2, -1))
        assertEquals("Hex: 000102FF", NdefParser.parse(NdefMessage(arrayOf(r))).single().content)
    }
    @Test fun unknownRecordDoesNotCrash() {
        val r = NdefRecord(NdefRecord.TNF_UNKNOWN, byteArrayOf(), byteArrayOf(), byteArrayOf(0, -1))
        assertTrue(NdefParser.parse(NdefMessage(arrayOf(r))).single().type.startsWith("Desconocido"))
    }
    @Test fun emailAndTelephoneUseCorrectUri() {
        assertTrue(roundTrip(Draft(kind = ContentKind.EMAIL, email = "a@example.com", name = "Hola mundo", text = "a&b")).uri!!.contains("a%26b"))
        assertEquals("tel:+34123456789", roundTrip(Draft(kind = ContentKind.PHONE, phone = "+34123456789")).uri)
    }
    @Test fun coordinatesAreValidated() {
        assertEquals("geo:0,-180", roundTrip(Draft(kind = ContentKind.LOCATION, latitude = "0", longitude = "-180")).uri)
        assertThrows(IllegalArgumentException::class.java) { NdefFactory.create(Draft(kind = ContentKind.LOCATION, latitude = "NaN", longitude = "181")) }
    }
    @Test fun invalidLinksAndOversizeTextRejected() {
        assertThrows(IllegalArgumentException::class.java) { NdefFactory.create(Draft(kind = ContentKind.URL, url = "javascript:alert(1)")) }
        assertThrows(IllegalArgumentException::class.java) { NdefFactory.create(Draft(text = "a".repeat(32768))) }
    }
    @Test fun wifiQrEscapesSpecialCharacters() {
        assertEquals("WIFI:T:WPA;S:a\\;b;P:abc\\:defghi;;", NdefFactory.shareText(Draft(kind = ContentKind.WIFI, ssid = "a;b", password = "abc:defghi")))
    }
    @Test fun malformedWifiNeverLeaksRawPayloadIntoHistory() {
        val record = NdefRecord.createMime("application/vnd.wfa.wsc", "secreto-invalido".toByteArray())
        val parsed = NdefParser.parse(NdefMessage(arrayOf(record))).single()
        assertTrue(parsed.sensitive)
        assertFalse(ScanResult("Wi-Fi", listOf(parsed)).forStorage().summary.contains("secreto-invalido"))
        assertFalse(parsed.content.contains("Hex:"))
    }
}
