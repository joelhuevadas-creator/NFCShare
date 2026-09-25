package com.nfcshare.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.nfcshare.app.domain.*
import com.nfcshare.app.nfc.*
import com.nfcshare.app.ui.components.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun EditorScreen(initial: Draft, profile: Boolean, save: (Draft) -> Unit, write: (Draft) -> Unit, qr: (Draft) -> Unit, notify: (String) -> Unit, draftChanged: (Draft) -> Unit) {
    val gson = remember { Gson() }
    var json by rememberSaveable { mutableStateOf(gson.toJson(initial)) }
    val draft = remember(json) { gson.fromJson(json, Draft::class.java) }
    fun update(d: Draft) { json = gson.toJson(d); draftChanged(d) }
    var preview by remember { mutableStateOf<List<ParsedRecord>?>(null) }
    var previewSize by remember { mutableIntStateOf(0) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    fun validate(action: (Draft) -> Unit) { runCatching { NdefFactory.create(draft); action(draft) }.onFailure { notify(it.message ?: "Revisa los campos") } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageTitle(if(profile) "Tu forma de compartir." else "Crea una etiqueta.", if(profile) "Un perfil, muchas formas de conectar." else "Elige qué quieres llevar en un toque.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ContentKind.entries.forEach { kind -> FilterChip(selected = draft.kind == kind, onClick = { update(draft.copy(kind = kind)) }, label = { Text(kind.label) }) } }
        Field("Nombre", draft.name) { update(draft.copy(name = it)) }
        if(profile) Field("Descripción (opcional)", draft.description) { update(draft.copy(description = it)) }
        when(draft.kind) {
            ContentKind.TEXT, ContentKind.CUSTOM -> Field("Contenido", draft.text, multiline = true) { update(draft.copy(text = it)) }
            ContentKind.URL -> Field("https://tu-pagina.com", draft.url) { update(draft.copy(url = it)) }
            ContentKind.CONTACT -> {
                Field("Teléfono (opcional)", draft.phone) { update(draft.copy(phone = it)) }
                Field("Correo (opcional)", draft.email) { update(draft.copy(email = it)) }
                Field("URL o red social (opcional)", draft.url) { update(draft.copy(url = it)) }
                Field("Texto (opcional)", draft.text, multiline = true) { update(draft.copy(text = it)) }
            }
            ContentKind.WIFI -> {
                Field("Nombre de red (SSID)", draft.ssid) { update(draft.copy(ssid = it)) }
                Row { Checkbox(checked = draft.openWifi, onCheckedChange = { update(draft.copy(openWifi = it)) }); Text("Red abierta", Modifier.padding(top = 14.dp)) }
                if(!draft.openWifi) OutlinedTextField(value = draft.password, onValueChange = { update(draft.copy(password = it)) }, label = { Text("Contraseña WPA2-Personal") }, modifier = Modifier.fillMaxWidth(), visualTransformation = if(showPassword) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { showPassword = !showPassword }) { Icon(if(showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Mostrar u ocultar contraseña") } })
                Text(if(profile) "Al guardar este perfil conservarás la contraseña localmente para poder compartirla. Su QR y su etiqueta permiten leerla." else "La contraseña será legible por quien reciba la etiqueta o el QR.", style = MaterialTheme.typography.bodySmall)
            }
            ContentKind.EMAIL -> { Field("Correo", draft.email) { update(draft.copy(email = it)) }; Field("Mensaje (opcional)", draft.text, multiline = true) { update(draft.copy(text = it)) } }
            ContentKind.PHONE -> Field("Teléfono", draft.phone) { update(draft.copy(phone = it)) }
            ContentKind.LOCATION -> { Field("Latitud", draft.latitude) { update(draft.copy(latitude = it)) }; Field("Longitud", draft.longitude) { update(draft.copy(longitude = it)) } }
        }
        Button(onClick = { validate { val message = NdefFactory.create(it); previewSize = message.byteArrayLength; preview = NdefParser.parse(message) } }, modifier = Modifier.fillMaxWidth()) { Text("Vista previa") }
        if(profile) OutlinedButton(onClick = { validate(save) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.BookmarkAdd, null); Spacer(Modifier.width(8.dp)); Text("Guardar perfil") }
    }
    preview?.let { records ->
        AlertDialog(onDismissRequest = { preview = null }, title = { Text("Vista previa · $previewSize bytes") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                records.forEach { Text(it.type, style = MaterialTheme.typography.labelLarge); Text(it.content) }
                Text("La capacidad disponible depende de la etiqueta. No se escribirá hasta que confirmes.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { preview = null; validate(qr) }) { Icon(Icons.Outlined.QrCode, null); Spacer(Modifier.width(8.dp)); Text("Mostrar QR") }
            }
        }, confirmButton = { TextButton(onClick = { preview = null; validate(write) }) { Text("Escribir en NFC") } }, dismissButton = { TextButton(onClick = { preview = null }) { Text("Volver") } })
    }
}
@Composable private fun Field(label: String, value: String, multiline: Boolean = false, change: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) }, singleLine = !multiline, minLines = if(multiline) 4 else 1, modifier = Modifier.fillMaxWidth())
}
