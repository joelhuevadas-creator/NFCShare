package com.nfcshare.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.Gson
import com.nfcshare.app.data.*
import com.nfcshare.app.domain.*
import com.nfcshare.app.hce.HceSession
import com.nfcshare.app.nfc.NdefFactory
import com.nfcshare.app.ui.screens.*
import com.nfcshare.app.ui.theme.NfcShareTheme
import com.nfcshare.app.utils.Sharing

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun NfcShareApp(model: MainViewModel, available: Boolean, enabled: Boolean, readerMode: (Boolean, Boolean) -> Unit) {
    val settings by model.settings.collectAsStateWithLifecycle()
    val items by model.items.collectAsStateWithLifecycle()
    val reader by model.reader.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val gson = remember { Gson() }
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var route by rememberSaveable { mutableStateOf("main") }
    var qrBack by rememberSaveable { mutableStateOf("main") }
    var profileEditor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableLongStateOf(0) }
    var editingFavorite by rememberSaveable { mutableStateOf(false) }
    var draftJson by rememberSaveable { mutableStateOf(gson.toJson(Draft())) }
    var selectedId by rememberSaveable { mutableLongStateOf(0) }
    var selectedCollection by rememberSaveable { mutableStateOf(LibraryCollection.SAVED.name) }
    var qrText by rememberSaveable { mutableStateOf("") }
    var qrTitle by rememberSaveable { mutableStateOf("") }
    var peer by rememberSaveable { mutableStateOf(false) }
    val selected = items.find { it.id == selectedId && it.collection.name == selectedCollection }
    val scanning = route == "write" || (route == "main" && tab == 1 && reader.result == null && !reader.writeComplete)
    DisposableEffect(scanning, peer) { readerMode(scanning, peer); onDispose { readerMode(false, false) } }
    LaunchedEffect(Unit) { model.messages.collect { snackbar.showSnackbar(it) } }
    fun safely(action: () -> Unit) { runCatching(action).onFailure { model.notify(it.message ?: "No se pudo completar la acción") } }
    fun nfcSettings() = safely { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
    fun startEditor(draft: Draft, profile: Boolean, id: Long = 0, favorite: Boolean = false) { draftJson = gson.toJson(draft); profileEditor = profile; editingId = id; editingFavorite = favorite; route = "editor" }
    fun showQr(text: String, name: String) { qrText = text; qrTitle = name; qrBack = route; route = "qr" }
    fun open(item: LibraryItem) { selectedId = item.id; selectedCollection = item.collection.name; route = "item" }
    fun write(draft: Draft) { safely { model.startWrite(NdefFactory.create(draft)); route = "write" } }
    fun back() { when(route) { "qr" -> route = qrBack; "privacy", "wallet" -> route = "settings"; "details" -> route = "item"; "write" -> { model.cancelReader(); route = "main" }; else -> route = "main" } }
    BackHandler(enabled = route != "main" || tab != 0 || reader.result != null) {
        if(route != "main") back() else if(tab == 1 && reader.result != null) model.startScan() else { tab = 0; model.cancelReader() }
    }
    NfcShareTheme(settings) {
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("NFCShare", style = MaterialTheme.typography.titleLarge) }, navigationIcon = {
                if(route != "main") IconButton(onClick = ::back) { Icon(Icons.Outlined.ArrowBack, "Volver") }
                else Icon(Icons.Outlined.Nfc, null, Modifier.padding(start = 24.dp), tint = MaterialTheme.colorScheme.primary)
            }, actions = { if(route != "settings") IconButton(onClick = { route = "settings" }) { Icon(Icons.Outlined.Tune, "Configuración") } }) },
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = { if(route == "main") NavigationBar {
                val labels = listOf("Inicio", "Escanear", "Compartir", "Biblioteca")
                val icons = listOf(Icons.Outlined.Home, Icons.Outlined.Nfc, Icons.Outlined.NearMe, Icons.Outlined.Bookmarks)
                labels.forEachIndexed { index, label -> NavigationBarItem(selected = tab == index, onClick = { if(index == 1 && tab != 1) model.startScan(); tab = index }, icon = { Icon(icons[index], null) }, label = { Text(label) }) }
            } }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                    Crossfade(targetState = route, label = "screen") { screen ->
                        when(screen) {
                            "settings" -> SettingsScreen(settings, model::settings, { route = "privacy" }, { route = "wallet" })
                            "privacy", "wallet" -> InfoScreen(screen == "wallet")
                            "editor" -> EditorScreen(gson.fromJson(draftJson, Draft::class.java), profileEditor,
                                { model.saveProfile(it, editingId, editingFavorite); route = "main"; tab = 2 }, ::write, { showQr(NdefFactory.shareText(it), it.name.ifBlank { it.kind.label }) }, model::notify, { draftJson = gson.toJson(it) })
                            "qr" -> QrScreen(qrText, qrTitle, model::notify)
                            "write" -> ScanScreen(reader, available, enabled, false, {}, ::nfcSettings, { model.startScan(); route = "main"; tab = 1 }, model::confirmWrite, model::dismissWrite)
                            "item" -> selected?.let { item ->
                                val draft = if(item.collection == LibraryCollection.PROFILES) model.repository.profile(item).copy(name = item.name) else null
                                fun content() = if(draft != null) NdefFactory.shareText(draft) else model.repository.scan(item).records.joinToString("\n") { it.uri ?: it.content }.ifBlank { item.summary }
                                ItemScreen(item, draft, HceSession.supported(context), enabled,
                                    { safely { Sharing.text(context, content()) } }, { showQr(content(), item.name) },
                                    { if(draft != null) startEditor(draft, true, item.id, item.favorite) },
                                    { if(draft != null) write(draft) }, { route = "details" },
                                    { model.update(item, name = it) }, { model.update(item, favorite = !item.favorite) },
                                    { model.delete(item); route = "main" }, model::notify)
                            }
                            "details" -> selected?.let { item ->
                                val scan = model.repository.scan(item)
                                ResultScreen(scan, settings.advanced, { model.save(scan) }, { safely { Sharing.text(context, scan.summary) } }, { safely { Sharing.copy(context, scan.summary); model.notify("Contenido copiado") } }, { safely { Sharing.open(context, it) } }, { model.startScan(); route = "main"; tab = 1 })
                            }
                            else -> when(tab) {
                                0 -> HomeScreen(available, enabled, items.filter { it.collection == LibraryCollection.HISTORY }, { model.startScan(); tab = 1 }, { tab = 2 }, { startEditor(Draft(), false) }, { startEditor(Draft(), true) }, ::open, ::nfcSettings)
                                1 -> reader.result?.let { scan -> ResultScreen(scan, settings.advanced, { model.save(scan) }, { safely { Sharing.text(context, scan.summary) } }, { safely { Sharing.copy(context, scan.summary); model.notify("Contenido copiado") } }, { safely { Sharing.open(context, it) } }, model::startScan) }
                                    ?: ScanScreen(reader, available, enabled, peer, { peer = it; model.startScan() }, ::nfcSettings, model::startScan, model::confirmWrite, model::dismissWrite)
                                2 -> ShareScreen(items.filter { it.collection == LibraryCollection.PROFILES }, { startEditor(it, true) }, ::open)
                                3 -> LibraryScreen(items, ::open, model::clearHistory)
                            }
                        }
                    }
                }
            }
        }
    }
}
