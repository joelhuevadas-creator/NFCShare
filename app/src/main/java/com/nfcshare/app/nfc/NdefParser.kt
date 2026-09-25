package com.nfcshare.app.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.nfcshare.app.domain.Hex
import com.nfcshare.app.domain.ParsedRecord

object NdefParser {
    fun parse(message: NdefMessage): List<ParsedRecord> = message.records.map { parseRecord(it, 0) }
    private fun parseRecord(r: NdefRecord, depth: Int): ParsedRecord = try {
        require(depth < 5) { "Demasiados registros anidados" }
        val p = r.payload
        val mime = r.toMimeType().orEmpty()
        when {
            r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(NdefRecord.RTD_TEXT) -> {
                require(p.isNotEmpty()) { "Texto vacío" }
                val status = p[0].toInt() and 255; val langLength = status and 63
                require(status and 0x40 == 0 && p.size >= 1 + langLength) { "Idioma de texto inválido" }
                val charset = if (status and 128 != 0) Charsets.UTF_16 else Charsets.UTF_8
                ParsedRecord("Texto", p.copyOfRange(1 + langLength, p.size).toString(charset), r.toByteArray().size)
            }
            r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(NdefRecord.RTD_SMART_POSTER) -> {
                val nested = NdefMessage(p).records.map { parseRecord(it, depth + 1) }
                ParsedRecord("Smart Poster", nested.joinToString("\n") { it.content }, r.toByteArray().size, nested.firstNotNullOfOrNull { it.uri }, nested.any { it.sensitive })
            }
            mime == "application/vnd.wfa.wsc" -> {
                val wifi = WifiCodec.decode(p)
                ParsedRecord("Wi-Fi", "Red: ${wifi.ssid}\nSeguridad: ${when(wifi.auth) { 1 -> "Abierta"; 32 -> "WPA2-Personal"; 2 -> "WPA-Personal"; else -> "0x${wifi.auth.toString(16)}" }}\nContraseña: ${wifi.password}", r.toByteArray().size, sensitive = true)
            }
            mime in listOf("text/vcard", "text/x-vcard") -> ParsedRecord("Contacto", p.toString(Charsets.UTF_8), r.toByteArray().size)
            r.tnf == NdefRecord.TNF_EXTERNAL_TYPE -> ParsedRecord("Externo · ${r.type.toString(Charsets.US_ASCII)}", readable(p), r.toByteArray().size)
            r.toUri() != null -> {
                val uri = r.toUri().toString()
                ParsedRecord(if (uri.startsWith("http")) "URL" else "URI", uri, r.toByteArray().size, uri)
            }
            r.tnf == NdefRecord.TNF_MIME_MEDIA -> ParsedRecord("MIME · $mime", readable(p), r.toByteArray().size)
            else -> ParsedRecord("Desconocido · TNF ${r.tnf}", readable(p), r.toByteArray().size)
        }
    } catch (e: Exception) {
        if (r.toMimeType() == "application/vnd.wfa.wsc") ParsedRecord("Wi-Fi inválido", "Credencial Wi-Fi no interpretable; contenido oculto por privacidad.", r.toByteArray().size, sensitive = true)
        else ParsedRecord("Registro inválido", "${e.message ?: "Formato no reconocido"}\nHex: ${Hex.encode(r.payload.take(512).toByteArray())}", r.toByteArray().size)
    }
    private fun readable(bytes: ByteArray): String {
        val text = bytes.toString(Charsets.UTF_8)
        return if ('\uFFFD' !in text && text.none { it.code < 32 && it !in "\n\r\t" }) text
        else "Hex: ${Hex.encode(bytes.take(512).toByteArray())}${if (bytes.size > 512) "… (${bytes.size} bytes)" else ""}"
    }
}
