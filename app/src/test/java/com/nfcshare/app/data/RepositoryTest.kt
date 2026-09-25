package com.nfcshare.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nfcshare.app.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoryTest {
    @Test fun persistenceRenameFavoriteAndSelectiveDelete() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, NfcDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo = LibraryRepository(db.library())
            val scan = ScanResult("Wi-Fi", records = listOf(ParsedRecord("Wi-Fi", "Red: prueba\nContraseña: secreto", 20, sensitive = true)))
            repo.record(scan); repo.save(scan); repo.saveProfile(Draft(name = "Web", kind = ContentKind.URL, url = "https://example.com"))
            val first = repo.items.first(); assertEquals(3, first.size)
            assertFalse(first.filter { it.collection != LibraryCollection.PROFILES }.any { it.json.contains("secreto") })
            val saved = first.first { it.collection == LibraryCollection.SAVED }
            repo.update(saved, "Mi etiqueta", true)
            assertEquals("Mi etiqueta", repo.items.first().first { it.collection == LibraryCollection.SAVED }.name)
            assertTrue(repo.items.first().first { it.collection == LibraryCollection.SAVED }.favorite)
            repo.clearHistory(); assertEquals(2, repo.items.first().size)
            repo.delete(saved); assertEquals(LibraryCollection.PROFILES, repo.items.first().single().collection)
        } finally { db.close() }
    }
}
