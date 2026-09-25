package com.nfcshare.app.hce

import com.nfcshare.app.domain.Hex

/** Private application protocol v1. SELECT returns a 2-byte length; READ BINARY returns <=240 bytes. */
class ApduProtocol(private val content: () -> ByteArray?) {
    private var selected: ByteArray? = null
    fun reset() { selected = null }
    fun process(command: ByteArray): ByteArray {
        if (command.size < 4) return Hex.decode("6700")
        if (command.contentEquals(SELECT) || command.contentEquals(SELECT + byteArrayOf(0))) {
            val value = content()
            if (value == null || value.isEmpty() || value.size > MAX_SIZE) { reset(); return Hex.decode("6A82") }
            selected = value.copyOf()
            return byteArrayOf((value.size shr 8).toByte(), value.size.toByte()) + OK
        }
        if (command[0] != 0.toByte()) return Hex.decode("6E00")
        if (command[1] != 0xB0.toByte()) return Hex.decode("6D00")
        if (command.size != 5) return Hex.decode("6700")
        val current = content()
        if (current == null) { reset(); return Hex.decode("6985") }
        val data = selected ?: return Hex.decode("6985")
        if (!current.contentEquals(data)) { reset(); return Hex.decode("6985") }
        val offset = ((command[2].toInt() and 255) shl 8) or (command[3].toInt() and 255)
        val length = command[4].toInt() and 255
        if (length !in 1..240 || offset >= data.size) return Hex.decode("6B00")
        return data.copyOfRange(offset, minOf(offset + length, data.size)) + OK
    }
    companion object {
        const val MAX_SIZE = 32768
        val SELECT: ByteArray get() = Hex.decode("00A4040009F04E46435348415245")
        val OK: ByteArray get() = Hex.decode("9000")
        fun readCommand(offset: Int, length: Int) = byteArrayOf(0, 0xB0.toByte(), (offset shr 8).toByte(), offset.toByte(), length.toByte())
    }
}
