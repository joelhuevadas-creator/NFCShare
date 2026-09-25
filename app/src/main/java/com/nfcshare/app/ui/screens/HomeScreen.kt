package com.nfcshare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nfcshare.app.data.LibraryItem
import com.nfcshare.app.ui.components.*

@Composable fun HomeScreen(available: Boolean, enabled: Boolean, recent: List<LibraryItem>, scan: () -> Unit, share: () -> Unit, create: () -> Unit, qr: () -> Unit, open: (LibraryItem) -> Unit, settings: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { PageTitle("Comparte en\nun instante.", "Tus datos. Cerca de ti.") }
        item {
            Surface(shape = RoundedCornerShape(32.dp), color = Color(0xFF202820), contentColor = Color(0xFFF5F5ED)) {
                Column(Modifier.fillMaxWidth().padding(26.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFF364032), shape = CircleShape) { Icon(Icons.Outlined.Nfc, null, Modifier.padding(16.dp).size(30.dp), tint = Color(0xFFFF9B51)) }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(7.dp).background(if(enabled) Color(0xFFBADCA2) else Color(0xFFFFBB85), CircleShape))
                            Text(if(enabled) "NFC activo" else if(available) "Desactivado" else "Sin NFC", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if(enabled) "NFC listo" else if(available) "Activa la conexión" else "Siempre puedes usar QR", style = MaterialTheme.typography.headlineMedium)
                        Text(if(enabled) "Acerca una etiqueta y descubre\nlo que tiene para compartir." else if(available) "Activa NFC en los ajustes de tu teléfono." else "Crea perfiles y compártelos con un código QR.", color = Color(0xFFC6CFC1), style = MaterialTheme.typography.bodyLarge)
                    }
                    Button(onClick = if(enabled) scan else if(available) settings else qr, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9B51), contentColor = Color(0xFF361500))) {
                        Icon(if(enabled) Icons.Outlined.Nfc else if(available) Icons.Outlined.Settings else Icons.Outlined.QrCode, null); Spacer(Modifier.width(10.dp)); Text(if(enabled) "Escanear NFC" else if(available) "Activar NFC" else "Crear QR")
                    }
                }
            }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("Compartir", Icons.Outlined.NearMe, Modifier.weight(1f), share)
            QuickAction("Crear etiqueta", Icons.Outlined.Add, Modifier.weight(1f), create)
            QuickAction("QR", Icons.Outlined.QrCode, Modifier.weight(1f), qr)
        } }
        item { Text("Actividad reciente", style = MaterialTheme.typography.titleLarge) }
        if (recent.isEmpty()) item { EmptyState("Tu primer encuentro", "Todavía no has escaneado etiquetas.") }
        items(recent.take(3), key = { it.id }) { LibraryCard(it) { open(it) } }
        item { Text("LOCAL POR NATURALEZA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
@Composable private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier, click: () -> Unit) {
    Surface(onClick = click, modifier = modifier, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(vertical = 20.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
