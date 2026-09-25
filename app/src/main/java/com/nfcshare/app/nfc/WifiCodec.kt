package com.nfcshare.app.nfc

import java.io.ByteArrayOutputStream

/** NFC Forum Wi-Fi Simple Configuration credential TLVs, network index 1. */
object WifiCodec {
    data class Credential(val ssid: String, val password: String, val auth: Int)
    private fun tlv(type: Int, bytes: ByteArray): ByteArray = byteArrayOf((type shr 8).toByte(), type.toByte(), (bytes.size shr 8).toByte(), bytes.size.toByte()) + bytes
    fun encode(ssid: String, password: String, open: Boolean): ByteArray {
        require(ssid.toByteArray().size in 1..32) { "El nombre de red debe ocupar entre 1 y 32 bytes." }
        require(open || password.toByteArray().size in 8..63 || password.matches(Regex("[0-9a-fA-F]{64}"))) { "WPA2 requiere 8–63 bytes o 64 caracteres hexadecimales." }
        val credential = ByteArrayOutputStream().apply {
            write(tlv(0x1026, byteArrayOf(1)))
            write(tlv(0x1045, ssid.toByteArray()))
            write(tlv(0x1003, byteArrayOf(0, if (open) 1 else 0x20)))
            write(tlv(0x100F, byteArrayOf(0, if (open) 1 else 8)))
            write(tlv(0x1027, if (open) byteArrayOf() else password.toByteArray()))
            write(tlv(0x1020, ByteArray(6) { 0xff.toByte() }))
        }.toByteArray()
        return tlv(0x100E, credential)
    }
    private fun fields(data: ByteArray): Map<Int, ByteArray> {
        val out = mutableMapOf<Int, ByteArray>(); var offset = 0
        while (offset < data.size) {
            require(offset + 4 <= data.size) { "Cabecera Wi-Fi incompleta" }
            val type = ((data[offset].toInt() and 255) shl 8) or (data[offset + 1].toInt() and 255)
            val length = ((data[offset + 2].toInt() and 255) shl 8) or (data[offset + 3].toInt() and 255)
            offset += 4
            require(length <= data.size - offset) { "Longitud Wi-Fi inválida" }
            out[type] = data.copyOfRange(offset, offset + length); offset += length
        }
        return out
    }
    fun decode(data: ByteArray): Credential {
        val outer = fields(data)
        val values = fields(requireNotNull(outer[0x100E]) { "Sin credencial Wi-Fi" })
        val ssid = requireNotNull(values[0x1045]) { "Falta nombre de red" }.toString(Charsets.UTF_8)
        val auth = values[0x1003]?.takeIf { it.size == 2 }?.let { ((it[0].toInt() and 255) shl 8) or (it[1].toInt() and 255) } ?: 0
        return Credential(ssid, values[0x1027]?.toString(Charsets.UTF_8).orEmpty(), auth)
    }
}
