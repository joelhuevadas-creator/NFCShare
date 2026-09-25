package com.nfcshare.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nfcshare.app.domain.ScanResult
import com.nfcshare.app.ui.components.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ResultScreen(scan: ScanResult, advanced: Boolean, save: () -> Unit, share: () -> Unit, copy: () -> Unit, open: (String) -> Unit, again: () -> Unit) {
    var expanded by rememberSaveable(scan.timestamp) { mutableStateOf(advanced) }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Icon(Icons.Outlined.CheckCircle, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(12.dp)); PageTitle("Etiqueta detectada", scan.title) }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SuggestionChip(onClick = { expanded = !expanded }, label = { Text(if(scan.hce) "HCE" else if(scan.records.isEmpty()) "Sin NDEF" else "NDEF") })
            if(scan.records.isNotEmpty() && !scan.hce) SuggestionChip(onClick = { expanded = !expanded }, label = { Text(if(scan.writable) "Grabable" else "Solo lectura") })
            if(scan.technologies.contains("IsoDep")) SuggestionChip(onClick = { expanded = !expanded }, label = { Text("ISO-DEP") })
        } }
        if(scan.records.isEmpty()) item { InfoCard("Sin NDEF público", scan.summary) }
        items(scan.records) { record ->
            Card { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(record.type, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                SelectionContainer { Text(record.content, style = MaterialTheme.typography.bodyLarge) }
                Text("${record.size} bytes", style = MaterialTheme.typography.labelSmall)
                record.uri?.takeIf { it.substringBefore(':').lowercase() in setOf("http", "https", "mailto", "tel", "geo") }?.let { uri -> TextButton(onClick = { open(uri) }) { Icon(Icons.Outlined.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("Abrir") } }
            } }
        }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = save) { Icon(Icons.Outlined.BookmarkAdd, null); Spacer(Modifier.width(8.dp)); Text("Guardar") }
            OutlinedButton(onClick = copy) { Text("Copiar") }; OutlinedButton(onClick = share) { Text("Compartir") }
        } }
        if(scan.records.any { it.sensitive }) item { Text("Las contraseñas Wi-Fi no se conservan en el historial ni en las etiquetas guardadas.", style = MaterialTheme.typography.bodySmall) }
        item {
            TextButton(onClick = { expanded = !expanded }) { Text("Información técnica"); Icon(if(expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null) }
            AnimatedVisibility(expanded) { InfoCard("Opciones avanzadas", "UID: ${scan.uid.ifBlank { "No disponible" }}\nTecnologías: ${scan.technologies.joinToString()}\nTipo: ${scan.ndefType}\nCapacidad: ${scan.capacity?.let { "$it bytes" } ?: "No disponible"}\nGrabable: ${scan.writable}\nTamaño NDEF: ${scan.size} bytes\nRegistros: ${scan.records.size}\n\nEl UID puede ser aleatorio. No implica identidad permanente ni capacidad de clonación.") }
        }
        item { TextButton(onClick = again) { Text("Escanear otra etiqueta") } }
    }
}
