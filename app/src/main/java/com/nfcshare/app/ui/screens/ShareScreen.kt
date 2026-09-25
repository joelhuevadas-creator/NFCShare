package com.nfcshare.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nfcshare.app.data.LibraryItem
import com.nfcshare.app.domain.*
import com.nfcshare.app.ui.components.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ShareScreen(profiles: List<LibraryItem>, create: (Draft) -> Unit, open: (LibraryItem) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { PageTitle("Conecta a tu manera.", "Crea un perfil y decide cómo compartirlo.") }
        item { Button(onClick = { create(Draft(kind = ContentKind.CONTACT)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Crear perfil") } }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Mi contacto" to ContentKind.CONTACT, "Mis redes" to ContentKind.CONTACT, "Mi página" to ContentKind.URL, "Wi-Fi" to ContentKind.WIFI, "Personalizado" to ContentKind.CUSTOM).forEach { (name, kind) -> AssistChip(onClick = { create(Draft(kind = kind, name = name)) }, label = { Text(name) }) }
        } }
        item { Text("Tus perfiles", style = MaterialTheme.typography.titleLarge) }
        if(profiles.isEmpty()) item { EmptyState("Haz tu primera conexión", "Tu contacto, una web o una red Wi-Fi. Todo empieza con un perfil.", Icons.Outlined.NearMe) }
        items(profiles, key = { it.id }) { LibraryCard(it) { open(it) } }
        item { InfoCard("Cerca o a distancia", "Comparte un QR o usa cualquier app del teléfono. Con HCE, otro teléfono con NFCShare puede recibir tu perfil al acercarse.", Icons.Outlined.QrCode) }
    }
}
