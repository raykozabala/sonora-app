package com.sonora.app

/**
 * Modelos de datos de Sonora.
 * Todos los campos son opcionales porque vienen de la red.
 */

/** Resultado de búsqueda unificado (YouTube, SoundCloud…). */
data class Resultado(
    val source: String?,
    val id: String?,
    val title: String?,
    val uploader: String?,
    val duration: Double?,
    val thumbnail: String?,
    val url: String?
)

/** Ficha completa de una pista, con los formatos disponibles. */
data class Ficha(
    val title: String?,
    val uploader: String?,
    val duration: Double?,
    val thumbnail: String?,
    val webpage_url: String?,
    val audio: List<FormatoAudio>?,
    val video: List<FormatoVideo>?
)

data class FormatoAudio(
    val ext: String?,
    val abr: Double?,
    val asr: Double?,
    val note: String?,
    val filesize: Double?
)

data class FormatoVideo(
    val ext: String?,
    val height: Int?,
    val width: Int?,
    val fps: Double?,
    val vcodec: String?,
    val note: String?,
    val filesize: Double?
)

/** Estado de un trabajo de descarga. */
data class Trabajo(
    val id: String?,
    val state: String?,
    val progress: Int?,
    val error: String?,
    val format: String?,
    val info: InfoPista?,
    val filename: String?
)

data class InfoPista(
    val id: String?,
    val title: String?,
    val uploader: String?,
    val duration: Double?,
    val thumbnail: String?,
    val webpage_url: String?,
    val view_count: Double?,
    val album: String? = null
)

/** Pista guardada en la biblioteca local. */
data class Pista(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Double,
    val thumbnail: String,
    val source: String,
    val filePath: String,
    val format: String,
    val addedAt: Long = System.currentTimeMillis(),
    /** Metadatos extra (opcionales, para el reproductor) */
    val album: String = "",
    val enlace: String = ""
)

/** Estado del servidor remoto (respuesta a /api/ping). */
data class EstadoServidor(
    val ok: Boolean,
    val nombre: String? = null,
    val version: String? = null,
    val ytDlp: String? = null,
    val ffmpeg: String? = null,
    val ip: String? = null
)

/** Letras de una canción (sincronizadas en formato LRC y/o texto plano). */
data class Letras(
    val sync: String?,
    val plain: String?,
    val album: String?,
    val fuente: String?
)
