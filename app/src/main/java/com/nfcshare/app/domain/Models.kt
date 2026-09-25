package com.nfcshare.app.domain

data class ParsedRecord(val type: String, val content: String, val size: Int, val uri: String? = null, val sensitive: Boolean = false)
data class ScanResult(
    val title: String, val records: List<ParsedRecord> = emptyList(), val uid: String = "",
    val technologies: List<String> = emptyList(), val capacity: Int? = null,
    val writable: Boolean = false, val ndefType: String = "Sin NDEF", val size: Int = 0,
    val timestamp: Long = System.currentTimeMillis(), val hce: Boolean = false,
) {
    val summary: String get() = records.joinToString("\n\n") { "${it.type}\n${it.content}" }.ifEmpty {
        "Sin contenido NDEF público. Puede ser una credencial protegida o una etiqueta con formato distinto. NFCShare no copia credenciales protegidas."
    }
    fun forStorage() = copy(records = records.map {
        if (it.sensitive) it.copy(content = it.content.substringBefore("\nContraseña:") + "\nContraseña: no guardada", uri = null) else it
    })
}

enum class ContentKind(val label: String) {
    TEXT("Texto"), URL("Enlace"), CONTACT("Contacto"), WIFI("Wi-Fi"), EMAIL("Correo"), PHONE("Teléfono"), LOCATION("Ubicación"), CUSTOM("Texto personalizado")
}
data class Draft(
    val kind: ContentKind = ContentKind.TEXT, val name: String = "", val description: String = "",
    val text: String = "", val url: String = "", val phone: String = "", val email: String = "",
    val ssid: String = "", val password: String = "", val openWifi: Boolean = false,
    val latitude: String = "", val longitude: String = "",
) {
    fun forStorage(): Draft = if (kind == ContentKind.WIFI) copy(password = if(openWifi) "" else password)
        else copy(ssid = "", password = "", openWifi = false)
}

object Hex {
    fun encode(bytes: ByteArray): String = bytes.joinToString("") { "%02X".format(it.toInt() and 255) }
    fun decode(value: String): ByteArray {
        require(value.length % 2 == 0 && value.all { it in "0123456789abcdefABCDEF" }) { "Hexadecimal inválido" }
        return value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
object WritePolicy {
    fun validate(size: Int, capacity: Int, writable: Boolean) {
        require(writable) { "Esta etiqueta es de solo lectura." }
        require(size > 0) { "El mensaje está vacío." }
        require(size <= capacity) { "El contenido ocupa $size bytes y la etiqueta admite $capacity bytes." }
    }
}
