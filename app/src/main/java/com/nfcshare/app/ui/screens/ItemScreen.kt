package com.nfcshare.app.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nfcshare.app.data.*
import com.nfcshare.app.domain.Draft
import com.nfcshare.app.hce.HceSession
import com.nfcshare.app.nfc.NdefFactory
import com.nfcshare.app.ui.components.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ItemScreen(item: LibraryItem, draft: Draft?, canHce: Boolean, nfcEnabled: Boolean, share: () -> Unit, qr: () -> Unit, edit: () -> Unit, write: () -> Unit, details: () -> Unit, rename: (String) -> Unit, favorite: () -> Unit, delete: () -> Unit, notify: (String) -> Unit) {
    var remaining by remember { mutableLongStateOf(0) }
    var name by remember(item.name) { mutableStateOf(item.name) }
    var renameDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var hceConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(item.id) { while(true) { remaining = if(HceSession.profileId == item.id && HceSession.data() != null) ((HceSession.expiresAt - SystemClock.elapsedRealtime()) / 1000).coerceAtLeast(0) else 0; delay(500) } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PageTitle(item.name, item.kind)
        InfoCard(if(draft != null) "Tu perfil" else "Contenido guardado", item.summary.ifBlank { "Listo para compartir" })
        Button(onClick = qr, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Outlined.QrCode, null); Spacer(Modifier.width(10.dp)); Text("Mostrar QR") }
        OutlinedButton(onClick = share, modifier = Modifier.fillMaxWidth()) { Text("Compartir con otra app") }
        if(draft != null) {
            OutlinedButton(onClick = write, modifier = Modifier.fillMaxWidth()) { Text("Escribir en etiqueta NFC") }
            if(canHce) {
                InfoCard(if(remaining > 0) "HCE activo · ${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}" else "Compartir por proximidad", "Otro teléfono con NFCShare debe abrir Escanear y activar Recibir de NFCShare. Mantén el teléfono desbloqueado. La sesión caduca a los cinco minutos.", Icons.Outlined.Nfc)
                FilledTonalButton(onClick = { if(remaining > 0) { HceSession.stop(); remaining = 0 } else hceConfirm = true }, enabled = nfcEnabled || remaining > 0, modifier = Modifier.fillMaxWidth()) { Text(if(remaining > 0) "Detener HCE" else if(nfcEnabled) "Activar HCE por 5 minutos" else "Activa NFC para usar HCE") }
            } else InfoCard("HCE no disponible", "Este teléfono no admite emulación de tarjetas. Puedes compartir el perfil mediante QR u otra app.")
        } else OutlinedButton(onClick = details, modifier = Modifier.fillMaxWidth()) { Text("Ver lectura e información técnica") }
        HorizontalDivider()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = favorite) { Icon(if(item.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, null); Spacer(Modifier.width(6.dp)); Text(if(item.favorite) "Quitar favorito" else "Favorito") }
            if(draft != null) TextButton(onClick = edit) { Text("Editar") }
            TextButton(onClick = { renameDialog = true }) { Text("Renombrar") }
            TextButton(onClick = { deleteDialog = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Eliminar") }
        }
    }
    if(renameDialog) AlertDialog(onDismissRequest = { renameDialog = false }, title = { Text("Renombrar") }, text = { OutlinedTextField(name, { name = it.take(200) }, label = { Text("Nombre") }) }, confirmButton = { TextButton(onClick = { rename(name); renameDialog = false }, enabled = name.isNotBlank()) { Text("Guardar") } }, dismissButton = { TextButton(onClick = { renameDialog = false }) { Text("Cancelar") } })
    if(deleteDialog) AlertDialog(onDismissRequest = { deleteDialog = false }, title = { Text("¿Eliminar ${item.name}?") }, text = { Text("Esta operación no se puede deshacer.") }, confirmButton = { TextButton(onClick = { deleteDialog = false; delete() }) { Text("Eliminar") } }, dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("Cancelar") } })
    if(hceConfirm && draft != null) AlertDialog(onDismissRequest = { hceConfirm = false }, title = { Text("Compartir este perfil por HCE") }, text = { Text("Durante cinco minutos un lector cercano que use el protocolo NFCShare podrá recibir este contenido. Incluye la contraseña si es un perfil Wi-Fi. Actívalo cuando estés listo para compartir.") }, confirmButton = { TextButton(onClick = { runCatching { HceSession.start(item.id, NdefFactory.create(draft).toByteArray()); remaining = 300 }.onFailure { notify(it.message ?: "No se pudo activar HCE") }; hceConfirm = false }) { Text("Activar") } }, dismissButton = { TextButton(onClick = { hceConfirm = false }) { Text("Cancelar") } })
}
