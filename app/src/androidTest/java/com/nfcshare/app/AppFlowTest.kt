package com.nfcshare.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import java.io.File

class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(compose.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun createProfilePreviewQrPersistAndTheme() {
        compose.onNodeWithText("Comparte en\nun instante.").assertIsDisplayed()
        screenshot("home-light")
        compose.onAllNodesWithText("Compartir").onLast().performClick()
        compose.onNodeWithText("Crear perfil").performClick()
        compose.onNodeWithText("Enlace").performClick()
        compose.onNode(hasSetTextAction() and hasText("Nombre")).performTextInput("Mi sitio")
        compose.onNode(hasSetTextAction() and hasText("https://tu-pagina.com")).performTextInput("https://example.com")
        compose.onNodeWithText("Vista previa").performScrollTo().performClick()
        compose.onNodeWithText("https://example.com").assertIsDisplayed()
        compose.onNodeWithText("Volver").performClick()
        compose.onNodeWithText("Guardar perfil").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Mi sitio").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Mi sitio").performClick()
        compose.onNodeWithText("Mostrar QR").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Código QR de Mi sitio").fetchSemanticsNodes().isNotEmpty() }
        screenshot("qr")
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Mi sitio").assertIsDisplayed()
        compose.onNodeWithText("Favorito").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Quitar favorito").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Configuración").performClick()
        compose.onNodeWithText("Oscuro").performClick()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Inicio").performClick()
        screenshot("home-dark")
        compose.onNodeWithContentDescription("Configuración").performClick()
        compose.onNodeWithText("Claro").performClick()
    }
}
