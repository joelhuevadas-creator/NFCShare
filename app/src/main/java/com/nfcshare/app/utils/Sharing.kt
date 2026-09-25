package com.nfcshare.app.utils

import android.content.*
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.nfcshare.app.data.Settings
import java.io.File

object Sharing {
    fun text(context: Context, text: String) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Compartir con")) }
    fun qr(text: String): Bitmap {
        require(text.toByteArray().size <= 2000) { "El contenido es demasiado grande para un QR legible. Comparte el texto con otra app." }
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 900, 900, mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 4))
        return Bitmap.createBitmap(900, 900, Bitmap.Config.ARGB_8888).apply { setPixels(IntArray(900 * 900) { if (matrix[it % 900, it / 900]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }, 0, 900, 0, 0, 900, 900) }
    }
    fun shareImage(context: Context, bitmap: Bitmap) {
        val directory = File(context.cacheDir, "qr").apply { mkdirs() }
        val file = File(directory, "NFCShare-${System.currentTimeMillis()}.png")
        directory.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 24 * 60 * 60_000 }?.forEach { it.delete() }
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); clipData = ClipData.newRawUri("QR", uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Compartir QR"))
    }
    fun saveImage(context: Context, bitmap: Bitmap) {
        val resolver = context.contentResolver
        val values = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, "NFCShare-${System.currentTimeMillis()}.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png"); put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NFCShare"); put(MediaStore.Images.Media.IS_PENDING, 1) }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("No se pudo guardar la imagen")
        try {
            requireNotNull(resolver.openOutputStream(uri)).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        } catch(e: Exception) { resolver.delete(uri, null, null); throw e }
    }
    fun open(context: Context, uri: String) {
        val value = android.net.Uri.parse(uri)
        require(value.scheme?.lowercase() in setOf("http", "https", "mailto", "tel", "geo")) { "Este tipo de enlace no se abre automáticamente. Puedes copiarlo." }
        context.startActivity(Intent(if(value.scheme == "tel") Intent.ACTION_DIAL else Intent.ACTION_VIEW, value))
    }
    fun copy(context: Context, text: String) { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("NFCShare", text)) }
}
object Feedback {
    fun play(context: Context, settings: Settings) {
        if (settings.vibration) { @Suppress("DEPRECATION") val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator; vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)) }
        if (settings.sound) runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55)
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 100)
            Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 250)
        }
    }
}
