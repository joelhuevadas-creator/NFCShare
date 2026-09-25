package com.nfcshare.app.domain

import com.nfcshare.app.nfc.WifiCodec
import org.junit.Assert.*
import org.junit.Test

class UtilitiesTest {
    @Test fun capacityIncludesFullMessageAndAllowsExactFit() { WritePolicy.validate(144, 144, true); assertThrows(IllegalArgumentException::class.java) { WritePolicy.validate(145, 144, true) } }
    @Test fun readOnlyAndEmptyRejected() { assertThrows(IllegalArgumentException::class.java) { WritePolicy.validate(1, 144, false) }; assertThrows(IllegalArgumentException::class.java) { WritePolicy.validate(0, 144, true) } }
    @Test fun hexRoundTripsSignedBytes() { val data = byteArrayOf(0, -1, -128, 127); assertArrayEquals(data, Hex.decode(Hex.encode(data))) }
    @Test fun malformedHexRejected() { listOf("F", "XX", "00 1").forEach { assertThrows(IllegalArgumentException::class.java) { Hex.decode(it) } } }
    @Test fun wifiOpenAndWpa2RoundTrip() {
        assertEquals(WifiCodec.Credential("red", "", 1), WifiCodec.decode(WifiCodec.encode("red", "", true)))
        assertEquals(WifiCodec.Credential("café", "12345678", 32), WifiCodec.decode(WifiCodec.encode("café", "12345678", false)))
    }
    @Test fun malformedWifiLengthRejected() { assertThrows(IllegalArgumentException::class.java) { WifiCodec.decode(byteArrayOf(0x10, 0x0e, 0, 99, 0)) } }
    @Test fun truncatedWifiTlvRejected() { assertThrows(IllegalArgumentException::class.java) { WifiCodec.decode(byteArrayOf(0x10)) } }
    @Test fun wifiValidatesByteLengthAndPassword() {
        assertThrows(IllegalArgumentException::class.java) { WifiCodec.encode("é".repeat(17), "12345678", false) }
        assertThrows(IllegalArgumentException::class.java) { WifiCodec.encode("red", "1234567", false) }
    }
}
