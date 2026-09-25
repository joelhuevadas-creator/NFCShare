# Protocolo NFCShare v1

Solo datos propios. AID privado de categoría `other`: `F04E46435348415245`. No es EMV ni un applet de transporte/acceso ni NDEF Type 4.

## Sesión

El usuario activa un perfil. NFCShare serializa un `NdefMessage` inmutable de 1–32768 bytes y lo conserva en memoria hasta cinco minutos. El servicio exige dispositivo desbloqueado. El receptor solicita explícitamente Recibir de NFCShare; el lector usa `IsoDep` con timeout de 2500 ms. Entrar al lector detiene la sesión emisora de ese teléfono.

## APDU

1. SELECT: `00 A4 04 00 09 F0 4E 46 43 53 48 41 52 45` (se acepta un `Le=00` final).
2. Respuesta: longitud NDEF unsigned de dos bytes, big endian, seguida de `90 00`.
3. READ BINARY: `00 B0 offsetHi offsetLo length`, con `length` entre 1 y 240.
4. Respuesta: fragmento de hasta `length` bytes seguido de `90 00`.
5. Repite hasta recibir la longitud anunciada; interpreta los bytes como `NdefMessage`.

| SW | Significado |
| --- | --- |
| 9000 | Correcto |
| 6700 | Longitud de comando inválida |
| 6985 | No seleccionado, dispositivo bloqueado o sesión terminada |
| 6A82 | No hay perfil activo |
| 6B00 | Offset/longitud de lectura fuera de rango |
| 6D00 | Instrucción desconocida |
| 6E00 | Clase no soportada |
| 6F00 | Error interno controlado |

La selección fija una instantánea del mensaje para evitar mezclar fragmentos si cambia el perfil. Desactivación del enlace reinicia selección. La caducidad o revocación impide lecturas posteriores. No se aceptan comandos de modificación ni acceso a otros datos.

No es un canal cifrado ni autenticado: cualquier lector cercano que conozca el protocolo puede leer el perfil durante una sesión activa y desbloqueada.
