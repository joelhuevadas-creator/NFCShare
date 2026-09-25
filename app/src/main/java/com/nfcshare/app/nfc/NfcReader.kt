package com.nfcshare.app.nfc

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import com.nfcshare.app.domain.*
import com.nfcshare.app.hce.ApduProtocol
import java.io.ByteArrayOutputStream

object NfcReader {
    fun read(tag: Tag, appToApp: Boolean): ScanResult {
        val base = ScanResult(title = "Etiqueta NFC", uid = Hex.encode(tag.id), technologies = tag.techList.map { it.substringAfterLast('.') })
        val ndef = Ndef.get(tag)
        if (ndef != null) return ndef.use {
            it.connect()
            val message = it.ndefMessage
            base.copy(title = message?.records?.firstOrNull()?.let { r -> NdefParser.parse(NdefMessage(arrayOf(r))).first().type } ?: "Etiqueta vacía",
                records = message?.let(NdefParser::parse).orEmpty(), capacity = it.maxSize, writable = it.isWritable,
                ndefType = it.type, size = message?.byteArrayLength ?: 0)
        }
        if (appToApp) {
            val iso = IsoDep.get(tag) ?: error("Este dispositivo o etiqueta no admite ISO-DEP para NFCShare.")
            return iso.use {
                it.connect(); it.timeout = 2500
                val selected = it.transceive(ApduProtocol.SELECT)
                require(selected.size == 4 && selected.takeLast(2).toByteArray().contentEquals(ApduProtocol.OK)) { "No hay un perfil NFCShare activo. Una credencial protegida no se puede copiar." }
                val size = ((selected[0].toInt() and 255) shl 8) or (selected[1].toInt() and 255)
                require(size in 1..ApduProtocol.MAX_SIZE) { "Longitud HCE inválida" }
                val out = ByteArrayOutputStream()
                while (out.size() < size) {
                    val count = minOf(240, size - out.size())
                    val response = it.transceive(ApduProtocol.readCommand(out.size(), count))
                    require(response.size == count + 2 && response.takeLast(2).toByteArray().contentEquals(ApduProtocol.OK)) { "La transferencia HCE se interrumpió." }
                    out.write(response, 0, count)
                }
                base.copy(title = "Perfil NFCShare", records = NdefParser.parse(NdefMessage(out.toByteArray())), ndefType = "HCE", size = size, hce = true)
            }
        }
        return base.copy(title = if (base.technologies.contains("IsoDep")) "Credencial ISO-DEP · sin NDEF" else "Etiqueta sin NDEF")
    }
}
