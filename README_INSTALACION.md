# Sonora — Guía de instalación y uso

Sonora es una aplicación Android para buscar, descargar (MP3/MP4 con metadatos), reproducir, gestionar biblioteca y crear playlists. Usa un servidor Node.js + yt-dlp + ffmpeg para la parte de extracción/descarga.

## Requisitos

- Mac con Node.js 18+ (recomendado 20), yt-dlp y ffmpeg instalados.
- Teléfono Android 7.0+ (API 24) en la misma red WiFi que el Mac.
- yt-dlp: `yt-dlp --version` 
- ffmpeg: `ffmpeg -version`

Si no los tienes en Homebrew: `brew install yt-dlp ffmpeg`

## 1. Iniciar el servidor Node.js

Desde esta carpeta:

```bash
cd servidor
node server.js
```

Deberías ver algo como:
```
  🎵 Servidor de Sonora activo
  URL para la app:  http://192.168.1.132:8080
  Abrir en navegador: http://localhost:8080
```

> Anota la IP (p.ej. http://192.168.1.132:8080). Esa es la que usarás en la app si cambia tu red.

## 2. Instalar el APK en Android

En la carpeta `APKs/` hay el APK listo para instalar:

- `Sonora-debug-v1.0.apk` (recomendado para uso personal)

Pasos:
1. En tu Android, activa "Orígenes desconocidos" / "Instalar apps desconocidas" para el gestor de archivos/navegador.
2. Transfiere el APK al móvil (Airdrop, WhatsApp, correo, cable USB) o abre este enlace desde el móvil.
3. Instala el APK.

## 3. Configurar el servidor en la app

La app guarda la URL base del servidor. Por defecto apunta a `http://192.168.1.132:8080`.

Si tu IP es distinta:
1. Abre Sonora → pestaña Buscar/Biblioteca/Playlists/Para ti (funciona igual).
2. Puedes cambiar la URL editando desde código si quieres, o simplemente usar la que coincida con la que muestra el servidor al arrancar.

> Consejo: si la app no conecta, comprueba que móvil y Mac estén en la misma red WiFi y que el puerto 8080 esté libre.

## 4. Uso de la app

- **Buscar**: escribe canción/artista. Muestra resultados de YouTube y SoundCloud. Pulsa sobre un resultado para ver formatos disponibles (MP3 con kbps, MP4 con resoluciones). Elige uno para iniciar descarga.
- **Biblioteca**: lista de lo ya descargado. Pulsa una canción para reproducirla (mini-reproductor fijo arriba de la navegación).
- **Playlists**: crea, abre (reproduce todas) o borra tus playlists.
- **Para ti**: recomendaciones basadas en lo que tengas en biblioteca (contenido relacionado).

## 5. Compilar desde código (opcional)

Si quieres recompilar la app Android:

```bash
cd sonora-app
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export ANDROID_HOME=~/Library/Android/sdk
export ANDROID_SDK_ROOT=~/Library/Android/sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
chmod +x gradlew
./gradlew assembleDebug
```

El APK quedará en `sonora-app/app/build/outputs/apk/debug/app-debug.apk`.

## 6. Servidor API (endpoints principales)

- `GET /api/search?q=...&limit=...` → resultados multi-fuente unificados
- `GET /api/formats?url=...` → formatos y calidades disponibles
- `GET /api/related?url=...` → recomendaciones
- `POST /api/start` → inicia descarga `{url, format, quality}`
- `GET /api/status/:id` → progreso
- `GET /api/file/:id` → descarga el archivo final

## 7. Aviso legal / uso responsable

- NO se ha implementado ni se recomienda descarga desde Spotify (fuera de alcance por DRM).
- Usa el programa ÚNICAMENTE con contenido del que tengas derechos (propio, dominio público o con licencia Creative Commons).
- Las recomendaciones se basan en contenido relacionado, no en historial personal de Google.

