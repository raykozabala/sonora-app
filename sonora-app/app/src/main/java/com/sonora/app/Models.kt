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
    val view_count: Double?
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
    val addedAt: Long = System.currentTimeMillis()
)
