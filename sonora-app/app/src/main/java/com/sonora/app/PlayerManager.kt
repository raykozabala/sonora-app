package com.sonora.app

import android.media.MediaPlayer

/**
 * Reproductor simple basado en MediaPlayer.
 * Un único objeto compartido para toda la app.
 */
object PlayerManager {

    private var player: MediaPlayer? = null
    private var actual: Pista? = null

    var enPista: ((Pista?) -> Unit)? = null
    var enEstado: ((Boolean) -> Unit)? = null   // true = reproduciendo

    fun actual(): Pista? = actual

    fun reproduce(pista: Pista) {
        detente()
        try {
            player = MediaPlayer().apply {
                setDataSource(pista.filePath)
                prepare()
                setOnCompletionListener { enEstado?.invoke(false) }
                start()
            }
            actual = pista
            enPista?.invoke(pista)
            enEstado?.invoke(true)
        } catch (_: Exception) {
            actual = null
            enPista?.invoke(null)
            enEstado?.invoke(false)
        }
    }

    fun pausaOReanuda() {
        val p = player ?: return
        if (p.isPlaying) { p.pause(); enEstado?.invoke(false) }
        else { p.start(); enEstado?.invoke(true) }
    }

    fun detente() {
        player?.let {
            try { if (it.isPlaying) it.stop() } catch (_: Exception) {}
            it.release()
        }
        player = null
        enEstado?.invoke(false)
    }

    fun posicion(): Int = player?.currentPosition ?: 0
    fun duracion(): Int = player?.duration ?: 0
    fun estaReproduciendo(): Boolean = player?.isPlaying == true

    fun avanza(ms: Int) {
        player?.seekTo(ms)
    }

    /** Devuelve mm:ss o m:ss */
    fun formatear(segundos: Double): String {
        val s = segundos.toInt()
        val m = s / 60
        val r = s % 60
        return String.format("%d:%02d", m, r)
    }
}
