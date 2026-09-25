package com.nfcshare.app.ui

import android.app.Application
import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.TagLostException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nfcshare.app.NfcShareApplication
import com.nfcshare.app.data.*
import com.nfcshare.app.domain.*
import com.nfcshare.app.hce.HceSession
import com.nfcshare.app.nfc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean

data class ReaderState(val busy: Boolean = false, val result: ScanResult? = null, val writing: NdefMessage? = null, val candidate: WriteCandidate? = null, val writeComplete: Boolean = false)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NfcShareApplication
    val repository = app.repository
    val settings = app.settings.settings
    val items = repository.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _reader = MutableStateFlow(ReaderState())
    val reader = _reader.asStateFlow()
    private val events = Channel<String>(Channel.BUFFERED)
    val messages = events.receiveAsFlow()
    private val feedbackChannel = Channel<Unit>(Channel.BUFFERED)
    val feedback = feedbackChannel.receiveAsFlow()
    private val gate = AtomicBoolean(false)
    private var generation = 0
    fun notify(message: String) { events.trySend(message) }
    fun settings(value: Settings) = app.settings.update(value)
    fun startScan() { generation++; _reader.value = ReaderState() }
    fun startWrite(message: NdefMessage) { generation++; _reader.value = ReaderState(writing = message) }
    fun cancelReader() { generation++; _reader.value = ReaderState() }
    fun show(scan: ScanResult) { generation++; _reader.value = ReaderState(result = scan) }
    fun discovered(tag: Tag, appToApp: Boolean) {
        if (!gate.compareAndSet(false, true)) return
        viewModelScope.launch {
            val before = _reader.value
            val epoch = generation
            try {
                if (before.candidate != null || before.result != null || before.writeComplete) return@launch
                _reader.value = before.copy(busy = true)
                if (before.writing != null) {
                    val candidate = withContext(Dispatchers.IO) { NdefWriter.inspect(tag, before.writing) }
                    if (epoch == generation) _reader.value = before.copy(candidate = candidate)
                } else {
                    val scan = withContext(Dispatchers.IO) { NfcReader.read(tag, appToApp) }
                    if (epoch == generation) {
                        _reader.value = ReaderState(result = scan)
                        feedbackChannel.trySend(Unit)
                        if (settings.value.history) repository.record(scan)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (epoch == generation) { _reader.value = before.copy(busy = false); notify(friendlyError(e)) }
            } finally { gate.set(false) }
        }
    }
    fun dismissWrite() { _reader.value = _reader.value.copy(candidate = null) }
    fun confirmWrite() {
        val state = _reader.value; val candidate = state.candidate ?: return; val message = state.writing ?: return
        if (!gate.compareAndSet(false, true)) return
        val epoch = generation
        viewModelScope.launch {
            _reader.value = state.copy(busy = true)
            try {
                withContext(Dispatchers.IO) { NdefWriter.write(candidate, message) }
                if (epoch == generation) { _reader.value = ReaderState(writeComplete = true); feedbackChannel.trySend(Unit); notify("Contenido escrito correctamente.") }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (epoch == generation) { _reader.value = state.copy(busy = false, candidate = null); notify(friendlyError(e)) }
            } finally { gate.set(false) }
        }
    }
    fun save(scan: ScanResult) = perform("Etiqueta guardada") { repository.save(scan) }
    fun saveProfile(draft: Draft, id: Long = 0, favorite: Boolean = false) = perform("Perfil guardado") { NdefFactory.create(draft); repository.saveProfile(draft, id, favorite); if (id == HceSession.profileId) HceSession.stop() }
    fun update(item: LibraryItem, name: String = item.name, favorite: Boolean = item.favorite) = perform("Biblioteca actualizada") { require(name.isNotBlank()); repository.update(item, name.trim().take(200), favorite) }
    fun delete(item: LibraryItem) = perform("Elemento eliminado") { repository.delete(item); if (item.collection == LibraryCollection.PROFILES && HceSession.profileId == item.id) HceSession.stop() }
    fun clearHistory() = perform("Historial eliminado") { repository.clearHistory() }
    private fun perform(success: String, block: suspend () -> Unit) { viewModelScope.launch {
        try { block(); notify(success) } catch (e: Exception) { if (e is CancellationException) throw e; notify(friendlyError(e)) }
    } }
    private fun friendlyError(e: Exception): String = when (e) {
        is TagLostException -> "La etiqueta se retiró. Acércala y mantenla quieta durante la operación."
        is java.io.IOException -> "Se perdió la comunicación NFC. Si estabas escribiendo, escanea para comprobar el contenido."
        is SecurityException -> "NFC no está disponible. Comprueba que esté activado."
        else -> e.message ?: "No se pudo completar la operación NFC."
    }
}
