package com.nfcshare.app.hce

import android.app.KeyguardManager
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.nfcshare.app.domain.Hex

class NfcShareHceService : HostApduService() {
    private val protocol = ApduProtocol { HceSession.data() }
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (getSystemService(KeyguardManager::class.java).isDeviceLocked) { protocol.reset(); return Hex.decode("6985") }
        return try { protocol.process(commandApdu ?: byteArrayOf()) } catch (_: Exception) { protocol.reset(); Hex.decode("6F00") }
    }
    override fun onDeactivated(reason: Int) { protocol.reset() }
}
