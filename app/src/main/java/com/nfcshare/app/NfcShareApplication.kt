package com.nfcshare.app

import android.app.Application
import androidx.room.Room
import com.nfcshare.app.data.*

class NfcShareApplication : Application() {
    val database by lazy { Room.databaseBuilder(this, NfcDatabase::class.java, "nfcshare.db").build() }
    val repository by lazy { LibraryRepository(database.library()) }
    val settings by lazy { SettingsStore(this) }
}
