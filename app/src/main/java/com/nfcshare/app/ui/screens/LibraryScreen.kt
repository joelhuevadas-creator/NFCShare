package com.nfcshare.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nfcshare.app.data.*
import com.nfcshare.app.ui.components.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun LibraryScreen(all: List<LibraryItem>, open: (LibraryItem) -> Unit, clear: () -> Unit) {
    var category by rememberSaveable { mutableStateOf("Guardados") }
    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("Todos") }
    var clearConfirm by remember { mutableStateOf(false) }
    val selected = all.filter { when(category) { "Guardados" -> it.collection == LibraryCollection.SAVED; "Perfiles" -> it.collection == LibraryCollection.PROFILES; "Historial" -> it.collection == LibraryCollection.HISTORY; else -> it.favorite } }
    val visible = selected.filter { (type == "Todos" || it.kind == type) && "${it.name} ${it.summary} ${it.kind}".contains(query, ignoreCase = true) }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageTitle("Todo a mano.", "Tus encuentros y lo que quieres conservar.") }
        item { OutlinedTextField(query, { query = it }, label = { Text("Buscar en la biblioteca") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Guardados", "Perfiles", "Historial", "Favoritos").forEach { name -> FilterChip(category == name, { category = name; type = "Todos" }, label = { Text(name) }) } } }
        if(selected.map { it.kind }.distinct().size > 1) item { FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { (listOf("Todos") + selected.map { it.kind }.distinct()).forEach { name -> FilterChip(type == name, { type = name }, label = { Text(name) }) } } }
        if(visible.isEmpty()) item { EmptyState("Un espacio para lo tuyo", if(query.isNotEmpty()) "No hay resultados para esa búsqueda." else "Los elementos de esta categoría aparecerán aquí.", Icons.Outlined.Bookmarks) }
        items(visible, key = { "${it.collection}-${it.id}" }) { LibraryCard(it) { open(it) } }
        if(category == "Historial" && selected.isNotEmpty()) item { TextButton(onClick = { clearConfirm = true }) { Icon(Icons.Outlined.DeleteOutline, null); Spacer(Modifier.width(8.dp)); Text("Borrar historial completo") } }
    }
    if(clearConfirm) AlertDialog(onDismissRequest = { clearConfirm = false }, title = { Text("¿Borrar todo el historial?") }, text = { Text("Las etiquetas guardadas y los perfiles se conservarán. Esta operación no se puede deshacer.") }, confirmButton = { TextButton(onClick = { clear(); clearConfirm = false }) { Text("Borrar historial") } }, dismissButton = { TextButton(onClick = { clearConfirm = false }) { Text("Cancelar") } })
}
