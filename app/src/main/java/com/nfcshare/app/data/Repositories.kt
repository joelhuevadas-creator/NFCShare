package com.nfcshare.app.data

import com.google.gson.Gson
import com.nfcshare.app.domain.Draft
import com.nfcshare.app.domain.ScanResult
import kotlinx.coroutines.flow.combine

enum class LibraryCollection { SAVED, PROFILES, HISTORY }
data class LibraryItem(val id: Long, val collection: LibraryCollection, val name: String, val kind: String, val summary: String, val json: String, val date: Long, val favorite: Boolean)

class LibraryRepository(private val dao: LibraryDao) {
    private val gson = Gson()
    val items = combine(dao.history(), dao.saved(), dao.profiles()) { h, s, p ->
        (h.map { LibraryItem(it.id, LibraryCollection.HISTORY, it.name, it.kind, it.summary, it.json, it.date, it.favorite) } +
            s.map { LibraryItem(it.id, LibraryCollection.SAVED, it.name, it.kind, it.summary, it.json, it.date, it.favorite) } +
            p.map { LibraryItem(it.id, LibraryCollection.PROFILES, it.name, it.kind, it.summary, it.json, it.date, it.favorite) }).sortedByDescending { it.date }
    }
    suspend fun record(scan: ScanResult) {
        val safe = scan.forStorage()
        dao.insert(ScanHistory(name = safe.title, kind = safe.ndefType, summary = safe.summary, json = gson.toJson(safe), date = safe.timestamp))
    }
    suspend fun save(scan: ScanResult) {
        val safe = scan.forStorage()
        dao.insert(SavedTag(name = safe.title, kind = safe.ndefType, summary = safe.summary, json = gson.toJson(safe), date = safe.timestamp))
    }
    suspend fun saveProfile(draft: Draft, id: Long = 0, favorite: Boolean = false) = dao.insert(ShareProfile(id = id, name = draft.name.ifBlank { draft.kind.label }, kind = draft.kind.label,
        summary = draft.description.ifBlank { when { draft.kind == com.nfcshare.app.domain.ContentKind.WIFI -> draft.ssid; draft.url.isNotBlank() -> draft.url; else -> draft.text.take(120) } }, json = gson.toJson(draft.forStorage()), date = System.currentTimeMillis(), favorite = favorite))
    fun profile(item: LibraryItem): Draft = gson.fromJson(item.json, Draft::class.java)
    fun scan(item: LibraryItem): ScanResult = gson.fromJson(item.json, ScanResult::class.java).copy(title = item.name)
    suspend fun update(item: LibraryItem, name: String = item.name, favorite: Boolean = item.favorite) {
        when(item.collection) {
            LibraryCollection.HISTORY -> dao.updateHistory(item.id, name, favorite)
            LibraryCollection.SAVED -> dao.updateSaved(item.id, name, favorite)
            LibraryCollection.PROFILES -> dao.updateProfile(item.id, name, favorite)
        }
    }
    suspend fun delete(item: LibraryItem) { when(item.collection) {
        LibraryCollection.HISTORY -> dao.deleteHistory(item.id)
        LibraryCollection.SAVED -> dao.deleteSaved(item.id)
        LibraryCollection.PROFILES -> dao.deleteProfile(item.id)
    } }
    suspend fun clearHistory() = dao.clearHistory()
}
