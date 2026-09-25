package com.nfcshare.app.hce

import com.nfcshare.app.domain.Hex
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class ApduProtocolTest {
    @Test fun selectAndReadChunksRoundTrip() {
        val data = ByteArray(1024) { it.toByte() }; val service = ApduProtocol { data }
        assertEquals("04009000", Hex.encode(service.process(ApduProtocol.SELECT)))
        val received = ByteArrayOutputStream()
        while(received.size() < data.size) { val response = service.process(ApduProtocol.readCommand(received.size(), 240)); assertEquals("9000", Hex.encode(response.takeLast(2).toByteArray())); received.write(response, 0, response.size - 2) }
        assertArrayEquals(data, received.toByteArray())
    }
    @Test fun readRequiresSelection() { assertEquals("6985", Hex.encode(ApduProtocol { byteArrayOf(1) }.process(ApduProtocol.readCommand(0, 1)))) }
    @Test fun noActiveProfileIsNotFound() { assertEquals("6A82", Hex.encode(ApduProtocol { null }.process(ApduProtocol.SELECT))) }
    @Test fun doesNotSelectPaymentAid() { assertEquals("6D00", Hex.encode(ApduProtocol { byteArrayOf(1) }.process(Hex.decode("00A4040007A0000000031010")))) }
    @Test fun rejectsMalformedAndOutOfBoundsReads() {
        val service = ApduProtocol { byteArrayOf(1, 2) }; service.process(ApduProtocol.SELECT)
        assertEquals("6700", Hex.encode(service.process(byteArrayOf(0))))
        assertEquals("6B00", Hex.encode(service.process(ApduProtocol.readCommand(2, 1))))
        assertEquals("6B00", Hex.encode(service.process(ApduProtocol.readCommand(0, 0))))
        assertEquals("6E00", Hex.encode(service.process(Hex.decode("80B0000001"))))
    }
    @Test fun revokedSessionStopsInProgressRead() {
        var active = true; val service = ApduProtocol { if(active) byteArrayOf(1, 2) else null }
        service.process(ApduProtocol.SELECT); active = false
        assertEquals("6985", Hex.encode(service.process(ApduProtocol.readCommand(0, 1))))
    }
    @Test fun resetRequiresNewSelection() {
        val service = ApduProtocol { byteArrayOf(1) }; service.process(ApduProtocol.SELECT); service.reset()
        assertEquals("6985", Hex.encode(service.process(ApduProtocol.readCommand(0, 1))))
    }
    @Test fun oversizedProfileRejected() { assertEquals("6A82", Hex.encode(ApduProtocol { ByteArray(32769) }.process(ApduProtocol.SELECT))) }
    @Test fun changingProfileRequiresNewSelection() {
        var data = byteArrayOf(1, 2)
        val service = ApduProtocol { data }; service.process(ApduProtocol.SELECT)
        data = byteArrayOf(3, 4)
        assertEquals("6985", Hex.encode(service.process(ApduProtocol.readCommand(0, 2))))
        assertEquals("00029000", Hex.encode(service.process(ApduProtocol.SELECT)))
        assertEquals("03049000", Hex.encode(service.process(ApduProtocol.readCommand(0, 2))))
    }
}
