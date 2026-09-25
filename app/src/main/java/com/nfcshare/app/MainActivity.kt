package com.nfcshare.app

import android.content.*
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nfcshare.app.hce.HceSession
import com.nfcshare.app.hce.NfcShareHceService
import com.nfcshare.app.nfc.NfcController
import com.nfcshare.app.ui.MainViewModel
import com.nfcshare.app.ui.NfcShareApp
import com.nfcshare.app.utils.Feedback
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val model: MainViewModel by viewModels()
    private lateinit var nfc: NfcController
    private var resumed = false
    private var readerWanted = false
    private var appToApp = false
    private var available by mutableStateOf(false)
    private var enabled by mutableStateOf(false)
    private val receiver = object : BroadcastReceiver() { override fun onReceive(context: Context?, intent: Intent?) { refresh(); syncReader() } }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfc = NfcController(this); refresh()
        ContextCompat.registerReceiver(this, receiver, IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED), ContextCompat.RECEIVER_EXPORTED)
        lifecycleScope.launch { model.feedback.collect { Feedback.play(this@MainActivity, model.settings.value) } }
        setContent { NfcShareApp(model, available, enabled) { wanted, peer ->
            if (readerWanted != wanted || appToApp != peer) { readerWanted = wanted; appToApp = peer; syncReader() }
        } }
    }
    private fun refresh() { available = nfc.adapter != null; enabled = nfc.adapter?.isEnabled == true }
    private fun syncReader() {
        if (!resumed) return
        try {
            if (readerWanted && enabled) { HceSession.stop(); nfc.enable { model.discovered(it, appToApp) } } else nfc.disable()
        } catch (e: Exception) { model.notify(e.message ?: "NFC no disponible") }
    }
    override fun onResume() {
        super.onResume(); resumed = true; refresh(); syncReader()
        if (HceSession.supported(this) && nfc.adapter != null) runCatching {
            CardEmulation.getInstance(nfc.adapter).setPreferredService(this, ComponentName(this, NfcShareHceService::class.java))
        }
    }
    override fun onPause() {
        runCatching { nfc.disable() }
        if (HceSession.supported(this) && nfc.adapter != null) runCatching { CardEmulation.getInstance(nfc.adapter).unsetPreferredService(this) }
        resumed = false; super.onPause()
    }
    override fun onDestroy() { unregisterReceiver(receiver); super.onDestroy() }
}
