package com.nfcshare.app.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle

class NfcController(private val activity: Activity) {
    val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    fun enable(onTag: (Tag) -> Unit) {
        val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NFC_BARCODE or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
        adapter?.enableReaderMode(activity, { tag -> onTag(tag) }, flags, Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250) })
    }
    fun disable() { adapter?.disableReaderMode(activity) }
}
