package com.nfcshare.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nfcshare.app.ui.components.*
import com.nfcshare.app.utils.Sharing
import kotlinx.coroutines.*

@Composable fun QrScreen(content: String, title: String, notify: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember(content) { mutableStateOf<Bitmap?>(null) }
    var error by remember(content) { mutableStateOf<String?>(null) }
    LaunchedEffect(content) { try { bitmap = withContext(Dispatchers.Default) { Sharing.qr(content) } } catch(e: Exception) { if(e is CancellationException) throw e; error = e.message } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PageTitle("Listo para conectar.", title)
        bitmap?.let { image ->
            Image(image.asImageBitmap(), "Código QR de $title", Modifier.fillMaxWidth().aspectRatio(1f).background(Color.White))
            Text("La otra persona solo tiene que escanearlo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { scope.launch { runCatching { Sharing.shareImage(context, image) }.onFailure { notify(it.message ?: "No se pudo compartir") } } }, modifier = Modifier.fillMaxWidth()) { Text("Compartir imagen QR") }
            OutlinedButton(onClick = { scope.launch { try { withContext(Dispatchers.IO) { Sharing.saveImage(context, image) }; notify("QR guardado en Imágenes/NFCShare") } catch(e: Exception) { if(e is CancellationException) throw e; notify(e.message ?: "No se pudo guardar") } } }, modifier = Modifier.fillMaxWidth()) { Text("Guardar imagen") }
        } ?: if(error == null) CircularProgressIndicator() else InfoCard("No se puede generar este QR", error!!)
        if(error != null) Button(onClick = { runCatching { Sharing.text(context, content) }.onFailure { notify("No hay una app para compartir") } }) { Text("Compartir contenido") }
    }
}
