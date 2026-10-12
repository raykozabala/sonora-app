package com.sonora.app

import android.media.MediaPlayer

/**
 * Reproductor global de Sonora.
 * - Una única instancia compartida por toda la app.
 * - Cola de reproducción: siguiente / anterior automático.
 * - Callbacks para que la interfaz se actualice sola.
 */
object PlayerManager {

    private var player: MediaPlayer? = null
    private var actual: Pista? = null

    /** Cola actual (pistas pendientes de reproducir) */
    private val cola = mutableListOf<Pista>()
    private var indice = -1

    /** Nueva pista cargada (o null si se paró) */
    var enPista: ((Pista?) -> Unit)? = null

    /** true = reproduciendo, false = en pausa */
    var enEstado: ((Boolean) -> Unit)? = null

    /** Cambió la posición dentro de la cola (índice, total) */
    var enCola: ((Int, Int) -> Unit)? = null

    /** Se llama cada vez que cambia la pista o se reproduce (para refrescar UI) */
    var refresca: (() -> Unit)? = null

    fun actual(): Pista? = actual
    fun cola(): List<Pista> = cola.toList()
    fun posicionEnCola(): Int = indice

    /**
     * Reproduce una pista sola (sin cambiar la cola actual),
     * por ejemplo una recomendación suelta.
     */
    fun reproduce(pista: Pista) = reproduceLista(listOf(pista), 0)

    /**
     * Reproduce una lista completa a partir de [inicio].
     * Es lo que se usa desde la biblioteca y desde las playlists.
     */
    fun reproduceLista(lista: List<Pista>, inicio: Int) {
        if (lista.isEmpty()) return
        cola.clear()
        cola.addAll(lista)
        indice = inicio.coerceIn(0, cola.size - 1)
        reproduciendoIndice(indice)
    }

    private fun reproduciendoIndice(i: Int) {
        if (i < 0 || i >= cola.size) return
        detente(avisar = false)

        val pista = cola[i]
        indice = i
        try {
            val mp = MediaPlayer()
            mp.setDataSource(pista.filePath)
            mp.setOnPreparedListener {
                it.start()
                actual = pista
                enPista?.invoke(pista)
                enEstado?.invoke(true)
                enCola?.invoke(indice, cola.size)
            }
            mp.setOnCompletionListener { siguiente(automatico = true) }
            mp.setOnErrorListener { _, _, _ ->
                actual = null
                enEstado?.invoke(false)
                true
            }
            mp.prepareAsync()
            player = mp
        } catch (_: Exception) {
            actual = null
            enPista?.invoke(null)
            enEstado?.invoke(false)
        }
    }

    /** Pasa a la siguiente pista de la cola. */
    fun siguiente(automatico: Boolean = false) {
        if (cola.isEmpty()) return
        if (indice + 1 < cola.size) {
            reproduciendoIndice(indice + 1)
        } else {
            // fin de la cola
            if (automatico) {
                detente()
            } else {
                indice = 0
                reproduciendoIndice(0)
            }
        }
    }

    /** Vuelve a la pista anterior (o reinicia la actual si ya lleva rato). */
    fun anterior() {
        if (cola.isEmpty()) return
        if (player != null && (player?.currentPosition ?: 0) > 3000) {
            player?.seekTo(0)
            return
        }
        if (indice > 0) reproduciendoIndice(indice - 1) else reproduciendoIndice(0)
    }

    fun pausaOReanuda() {
        val p = player ?: return
        try {
            if (p.isPlaying) {
                p.pause(); enEstado?.invoke(false)
            } else {
                p.start(); enEstado?.invoke(true)
            }
        } catch (_: Exception) { }
    }

    fun detente(avisar: Boolean = true) {
        player?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: Exception) { }
            try { it.release() } catch (_: Exception) { }
        }
        player = null
        if (avisar) {
            actual = null
            enEstado?.invoke(false)
        }
    }

    fun posicion(): Int = try { player?.currentPosition ?: 0 } catch (_: Exception) { 0 }
    fun duracion(): Int = try { player?.duration ?: 0 } catch (_: Exception) { 0 }
    fun estaReproduciendo(): Boolean = try { player?.isPlaying == true } catch (_: Exception) { false }

    fun avanza(ms: Int) {
        try { player?.seekTo(ms) } catch (_: Exception) { }
    }

    /** Cambia la velocidad de reproducción (0.5x - 2.0x). */
    fun velocidad(factor: Float) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                player?.playbackParams = player?.playbackParams?.setSpeed(factor)
                    ?: android.media.PlaybackParams().setSpeed(factor)
            }
        } catch (_: Exception) { }
    }

    /** Devuelve mm:ss o m:ss */
    fun formatear(segundos: Double): String {
        val s = segundos.toInt()
        val m = s / 60
        val r = s % 60
        return String.format("%d:%02d", m, r)
    }

    /** Convierte milisegundos a mm:ss */
    fun formatearMs(ms: Int): String = formatear(ms / 1000.0)
}
