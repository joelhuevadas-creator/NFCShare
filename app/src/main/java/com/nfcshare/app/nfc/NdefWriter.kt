package com.nfcshare.app.nfc

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.nfcshare.app.domain.WritePolicy

data class WriteCandidate(val tag: Tag, val previous: ByteArray?, val capacity: Int?, val format: Boolean, val preview: String)
object NdefWriter {
    fun inspect(tag: Tag, message: NdefMessage): WriteCandidate {
        val ndef = Ndef.get(tag)
        if (ndef != null) return ndef.use {
            it.connect()
            WritePolicy.validate(message.byteArrayLength, it.maxSize, it.isWritable)
            val old = it.ndefMessage
            WriteCandidate(tag, old?.toByteArray(), it.maxSize, false, old?.let(NdefParser::parse)?.joinToString("\n") { r -> r.content }.orEmpty())
        }
        require(NdefFormatable.get(tag) != null) { "No es una etiqueta NDEF compatible y grabable. No se pueden copiar credenciales protegidas." }
        return WriteCandidate(tag, null, null, true, "")
    }
    fun write(candidate: WriteCandidate, message: NdefMessage) {
        if (candidate.format) {
            val formatable = NdefFormatable.get(candidate.tag) ?: error("Formato no compatible")
            formatable.use { it.connect(); it.format(message) }
            return
        }
        val ndef = Ndef.get(candidate.tag) ?: error("La etiqueta ya no está disponible")
        ndef.use {
            it.connect()
            WritePolicy.validate(message.byteArrayLength, it.maxSize, it.isWritable)
            val current = it.ndefMessage?.toByteArray()
            require((current == null && candidate.previous == null) || (current != null && candidate.previous != null && current.contentEquals(candidate.previous))) { "El contenido cambió. Vuelve a acercar la etiqueta para confirmarlo." }
            it.writeNdefMessage(message)
            val readBack = it.ndefMessage?.toByteArray()
            check(readBack != null && readBack.contentEquals(message.toByteArray())) { "No se pudo verificar la escritura. Escanea la etiqueta para comprobarla." }
        }
    }
}
