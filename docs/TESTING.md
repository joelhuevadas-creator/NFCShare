# Verificación

## Automatizada

`./gradlew testDebugUnitTest lintDebug assembleDebug`

Los tests locales con Robolectric usan API 35; la app compila y apunta a API 36. Los tests JVM validan lógica, no el hardware del teléfono.

## Resultado de esta entrega

- JDK 17, compileSdk 36, targetSdk 36 y minSdk 29 verificados en el APK.
- `testDebugUnitTest lintDebug assembleDebug`: **BUILD SUCCESSFUL**. 34 tests, 0 fallos; lint: 0 errores. Las advertencias son de versiones de dependencias y compatibilidad de recursos/atributos.
- Firma debug del APK comprobada con `apksigner`.
- Workflow **Build Android APK** ejecutado con éxito en GitHub, con artifact `NFCShare-debug`.
- `AppFlowTest.createProfilePreviewQrPersistAndTheme`: **OK (1 test)** en emulador Android 16/API 36. Valida creación, vista previa, QR, persistencia tras recreación de actividad, favoritos y cambio de tema. Capturas en `docs/screenshots`.
- Arranque observado a 1080×2400; recorrido automatizado a 720×1600 con densidad 280. El emulador no expone NFC.

### Repetir la prueba de interfaz

Utiliza un emulador de pruebas con instalación limpia (el test crea un perfil llamado «Mi sitio»). No uses un teléfono con datos que quieras conservar.

```sh
./gradlew connectedDebugAndroidTest
```

Para repetirla en el mismo emulador, borra primero los datos de NFCShare desde Ajustes del emulador. Las capturas se escriben en el directorio externo privado de la app, `files/screenshots`.

## Matriz física pendiente

Estas comprobaciones requieren un teléfono Android 16, etiquetas reales y, para HCE, un segundo teléfono compatible. No deben darse por aprobadas solo porque el APK compile.

| Caso | Resultado esperado |
| --- | --- |
| Teléfono sin NFC | QR, perfiles y biblioteca funcionan; se informa ausencia de NFC |
| Desactivar NFC desde ajustes durante escaneo | Estado actualizado, sin crash |
| NDEF texto UTF-8/UTF-16 y URL | Contenido interpretado, acciones abrir/copiar/guardar/compartir |
| vCard, Smart Poster y Wi-Fi WSC | Interpretación y contraseña Wi-Fi excluida del historial |
| NFC-A/B/F/V, IsoDep, Mifare | Mostrar únicamente tecnologías e información pública disponible |
| Credencial sin NDEF | Explicación sin prometer clonación; ningún intento de autenticación |
| Escritura NDEF vacía y con contenido | Vista previa, capacidad y confirmación; lectura posterior coincide |
| Solo lectura / capacidad insuficiente | Mensaje claro, no escribir |
| NdefFormatable | Advertir capacidad desconocida y pedir confirmación; reescanear para verificar |
| Retirar etiqueta durante escritura | Error de comunicación y sugerencia de comprobación |
| HCE entre dos teléfonos | Selección del AID propio y transferencia íntegra por fragmentos |
| HCE desactivado, expirado o teléfono bloqueado | No entregar contenido |
| Borrar o editar perfil activo | Detener HCE |
| Giro de pantalla y segundo plano | Estado de UI conservado; reader mode se desactiva al salir de primer plano |
| Claro/oscuro, 1080×2400, compacto y tablet | Sin texto cortado ni solapamiento con barras del sistema |
| QR desde otro lector y PNG compartido | Datos correctos y archivo legible |
| Sin red | Operaciones locales funcionan; enlaces externos dependen de otra app |
| Borrar historial completo | Confirmación; perfiles y guardados se conservan |

No hay pagos, emulación de credenciales protegidas ni integración con Wallet que probar.
