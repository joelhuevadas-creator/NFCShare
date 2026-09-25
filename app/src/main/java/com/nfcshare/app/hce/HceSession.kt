package com.nfcshare.app.hce

import android.content.Context

/** Memory-only: a process restart always stops sharing, and nothing private is written to preferences. */
object HceSession {
    @Volatile private var payload: ByteArray? = null
    @Volatile var profileId: Long? = null
        private set
    @Volatile var expiresAt: Long = 0
        private set
    fun start(id: Long, data: ByteArray) {
        require(data.size in 1..ApduProtocol.MAX_SIZE)
        profileId = id; expiresAt = android.os.SystemClock.elapsedRealtime() + 5 * 60_000; payload = data.copyOf()
    }
    fun stop() { payload = null; profileId = null; expiresAt = 0 }
    fun data(): ByteArray? = if (android.os.SystemClock.elapsedRealtime() < expiresAt) payload else null
    fun supported(context: Context) = context.packageManager.hasSystemFeature("android.hardware.nfc.hce")
}
