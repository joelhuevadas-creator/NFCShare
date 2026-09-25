package com.nfcshare.app.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.nfcshare.app.domain.ContentKind
import com.nfcshare.app.domain.Draft

object NdefFactory {
    fun vcard(d: Draft): String {
        fun escape(s: String) = s.replace("\\", "\\\\").replace("\r", "").replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,")
        return listOf("BEGIN:VCARD", "VERSION:3.0", "FN:${escape(d.name)}", "TEL:${escape(d.phone)}", "EMAIL:${escape(d.email)}", "URL:${escape(d.url)}", "NOTE:${escape(listOf(d.description, d.text).filter { it.isNotBlank() }.joinToString("\n"))}", "END:VCARD").joinToString("\r\n")
    }
    fun shareText(d: Draft): String = when (d.kind) {
        ContentKind.CONTACT -> vcard(d)
        ContentKind.WIFI -> {
            fun e(s: String) = s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:").replace("\"", "\\\"")
            "WIFI:T:${if(d.openWifi) "nopass" else "WPA"};S:${e(d.ssid)};P:${if(d.openWifi) "" else e(d.password)};;"
        }
        ContentKind.URL -> d.url.trim()
        ContentKind.EMAIL -> "mailto:${Uri.encode(d.email.trim(), "@")}?subject=${Uri.encode(d.name)}&body=${Uri.encode(d.text)}"
        ContentKind.PHONE -> "tel:${d.phone.trim()}"
        ContentKind.LOCATION -> "geo:${d.latitude.trim()},${d.longitude.trim()}"
        else -> d.text
    }
    fun create(d: Draft): NdefMessage {
        require(d.name.length <= 200 && d.description.length <= 1000) { "Nombre o descripción demasiado largos." }
        val record = when (d.kind) {
            ContentKind.TEXT, ContentKind.CUSTOM -> {
                require(d.text.isNotBlank()) { "Escribe el contenido." }
                NdefRecord.createTextRecord("es", d.text)
            }
            ContentKind.URL -> {
                val uri = Uri.parse(d.url.trim())
                require(uri.scheme in listOf("https", "http") && !uri.host.isNullOrBlank()) { "Usa un enlace completo con https:// o http://." }
                NdefRecord.createUri(uri)
            }
            ContentKind.CONTACT -> {
                require(d.name.isNotBlank()) { "El contacto necesita un nombre." }
                NdefRecord.createMime("text/vcard", vcard(d).toByteArray())
            }
            ContentKind.WIFI -> NdefRecord.createMime("application/vnd.wfa.wsc", WifiCodec.encode(d.ssid, d.password, d.openWifi))
            ContentKind.EMAIL -> {
                require(d.email.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) { "Introduce un correo válido." }
                NdefRecord.createUri(shareText(d))
            }
            ContentKind.PHONE -> {
                require(d.phone.matches(Regex("[+0-9() .-]{3,30}"))) { "Introduce un teléfono válido." }
                NdefRecord.createUri(shareText(d))
            }
            ContentKind.LOCATION -> {
                require(d.latitude.toDoubleOrNull()?.let { it in -90.0..90.0 } == true && d.longitude.toDoubleOrNull()?.let { it in -180.0..180.0 } == true) { "Latitud: −90 a 90. Longitud: −180 a 180." }
                NdefRecord.createUri(shareText(d))
            }
        }
        return NdefMessage(arrayOf(record)).also { require(it.byteArrayLength <= 32768) { "El límite de NFCShare es 32 KiB." } }
    }
}
