package com.nfcshare.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.nfcshare.app.ui.ReaderState
import com.nfcshare.app.ui.components.*

@Composable fun ScanScreen(state: ReaderState, available: Boolean, enabled: Boolean, appToApp: Boolean, changeMode: (Boolean) -> Unit, settings: () -> Unit, rescan: () -> Unit, confirm: () -> Unit, dismiss: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "NFC pulse").animateFloat(0.94f, 1.06f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "pulse")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PageTitle(if(state.writing != null) "Escribir etiqueta" else "Un toque basta.", if(state.writing != null) "${state.writing.byteArrayLength} bytes listos para escribir" else "Lee una etiqueta o recibe un perfil.")
        Spacer(Modifier.height(12.dp))
        Surface(Modifier.size(190.dp).scale(if(enabled && !state.writeComplete) pulse else 1f), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) {
            Box(contentAlignment = Alignment.Center) { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) { Icon(if(state.writeComplete) Icons.Outlined.Check else Icons.Outlined.Nfc, null, Modifier.padding(35.dp).size(54.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) } }
        }
        Text(when { state.writeComplete -> "Etiqueta lista"; !available -> "Este teléfono no tiene NFC"; !enabled -> "NFC desactivado"; state.busy -> "Mantén la etiqueta cerca…"; state.writing != null -> "Acerca una etiqueta compatible"; appToApp -> "Acerca el otro teléfono"; else -> "Acerca una etiqueta NFC" }, style = MaterialTheme.typography.titleLarge)
        Text(if(state.writeComplete) "Ya puedes compartir el contenido." else if(appToApp) "En el otro teléfono, activa HCE en un perfil de NFCShare. Mantén ambas pantallas desbloqueadas." else "Colócala en la parte trasera del teléfono.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if(state.busy) CircularProgressIndicator()
        if(available && !enabled) Button(onClick = settings) { Text("Abrir ajustes NFC") }
        if(state.writeComplete) Button(onClick = rescan) { Text("Escanear etiqueta") }
        if(state.writing == null && !state.writeComplete) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Recibir de NFCShare", style = MaterialTheme.typography.titleMedium); Text("Comunicación HCE entre apps", style = MaterialTheme.typography.bodySmall) }
                Switch(checked = appToApp, onCheckedChange = changeMode)
            }
            InfoCard("Solo lo que la etiqueta comparte", "Leemos NDEF e información técnica pública. Una etiqueta sin NDEF no es necesariamente clonable; puede ser una credencial protegida.")
        }
    }
    state.candidate?.let { candidate ->
        AlertDialog(onDismissRequest = { if(!state.busy) dismiss() }, title = { Text(if(candidate.format) "Formatear como NDEF" else if(candidate.previous != null) "Reemplazar contenido" else "Escribir contenido") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if(candidate.format) "La etiqueta permite formateo NDEF. Android no expone su capacidad antes de formatearla; la operación puede fallar si no cabe." else "Capacidad: ${candidate.capacity} bytes. Mensaje nuevo: ${state.writing?.byteArrayLength} bytes.")
                if(candidate.preview.isNotBlank()) Text("Contenido actual:\n${candidate.preview.take(400)}")
                Text("Confirma para escribir. Mantén esta misma etiqueta junto al teléfono hasta terminar.")
            }
        }, confirmButton = { TextButton(onClick = confirm, enabled = !state.busy) { Text(if(state.busy) "Escribiendo…" else "Confirmar escritura") } }, dismissButton = { TextButton(onClick = dismiss, enabled = !state.busy) { Text("Cancelar") } })
    }
}
