package com.nfcshare.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nfcshare.app.BuildConfig
import com.nfcshare.app.data.Settings
import com.nfcshare.app.ui.components.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun SettingsScreen(value: Settings, update: (Settings) -> Unit, privacy: () -> Unit, wallet: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PageTitle("A tu gusto.", "Pequeños detalles que hacen la diferencia.")
        Text("Apariencia", style = MaterialTheme.typography.titleLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Sistema", "Claro", "Oscuro").forEach { FilterChip(value.theme == it, { update(value.copy(theme = it)) }, label = { Text(it) }) } }
        Toggle("Colores del teléfono", "Dynamic Color · Android 12 o superior", value.dynamicColor) { update(value.copy(dynamicColor = it)) }
        HorizontalDivider()
        Toggle("Vibración", "Una señal breve al detectar una etiqueta", value.vibration) { update(value.copy(vibration = it)) }
        Toggle("Sonido de escaneo", "Respeta el volumen de notificaciones", value.sound) { update(value.copy(sound = it)) }
        Toggle("Guardar historial", "Conservar lecturas en este teléfono", value.history) { update(value.copy(history = it)) }
        Toggle("Opciones avanzadas", "Expandir los detalles técnicos de NFC", value.advanced) { update(value.copy(advanced = it)) }
        HorizontalDivider()
        OutlinedButton(onClick = privacy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Shield, null); Spacer(Modifier.width(8.dp)); Text("Privacidad") }
        OutlinedButton(onClick = wallet, modifier = Modifier.fillMaxWidth()) { Text("Compatibilidad con Wallet") }
        InfoCard("NFCShare ${BuildConfig.VERSION_NAME}", "Una forma sencilla de leer, crear y compartir información propia. Software libre · licencia MIT.")
    }
}
@Composable private fun Toggle(title: String, description: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked, change) }
}
@Composable fun InfoScreen(wallet: Boolean) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        PageTitle(if(wallet) "Compatibilidad con Wallet" else "Tus datos se quedan contigo.", if(wallet) "Cada credencial tiene su propio emisor." else "Privacidad desde el principio.")
        if(wallet) {
            InfoCard("Tarjetas y credenciales", "Las tarjetas bancarias, el transporte y los pases de Google Wallet requieren integraciones admitidas por el emisor, operador o Google Wallet. Leer una etiqueta o su UID no permite añadirla a Wallet.")
            InfoCard("Lo que sí puedes compartir", "Usa enlaces, texto, contactos o códigos QR. NFCShare comparte únicamente datos propios mediante su protocolo HCE; no emula tarjetas bancarias, transporte ni credenciales de acceso.")
        } else {
            InfoCard("Local y sin seguimiento", "NFCShare no tiene permiso de Internet, publicidad, analítica ni rastreadores. El historial, las etiquetas y los perfiles se guardan en el almacenamiento privado de la aplicación, sin copias de seguridad de Android.", Icons.Outlined.Shield)
            InfoCard("Tú eliges qué conservar", "Puedes desactivar el historial y borrar elementos. Las contraseñas de registros Wi-Fi reconocidos no se guardan en lecturas. Los perfiles Wi-Fi que creas sí conservan la contraseña para compartirla; elimínalos cuando dejes de necesitarlos.")
            InfoCard("Compartir es una decisión", "Los QR, NFC y HCE transmiten el contenido a quien lo reciba. HCE caduca a los cinco minutos, exige desbloqueo y se detiene al entrar al escáner. Copiar, guardar un QR, abrir enlaces o compartir con otras apps deja datos fuera de NFCShare.")
            InfoCard("Límites claros", "No extraemos claves, evitamos autenticación ni copiamos credenciales protegidas. Los registros desconocidos se muestran como texto o hexadecimal público. Revisa el contenido antes de guardarlo o compartirlo.")
        }
    }
}
