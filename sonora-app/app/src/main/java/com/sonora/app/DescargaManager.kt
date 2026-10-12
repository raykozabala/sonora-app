package com.sonora.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Gestiona el ciclo completo de una descarga:
 * 1) pide al servidor que empiece,
 * 2) consulta el progreso cada pocos segundos,
 * 3) descarga el archivo final al móvil,
 * 4) lo registra en la biblioteca.
 */
object DescargaManager {

    fun interface Listener {
        fun onCambio(trabajo: Trabajo, progreso: Int)
    }

    fun interface FinListener {
        fun onFin(pista: Pista?, error: String?)
    }

    private val cliente = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    /**
     * Descarga una pista. Devuelve la pista guardada o lanza una excepción.
     */
    suspend fun descargar(
        context: Context,
        enlace: String,
        formato: String,          // "mp3" | "mp4"
        fuente: String,
        listener: Listener?,
        fin: FinListener?
    ): Pista = withContext(Dispatchers.IO) {
        try {
            // 1) Iniciar el trabajo en el servidor
            val id = ApiClient.iniciarDescarga(enlace, formato)

            // 2) Seguir el progreso
            var trabajo: Trabajo
            while (true) {
                delay(1500)
                trabajo = ApiClient.estado(id)

                when (trabajo.state) {
                    "error" -> {
                        val msg = trabajo.error ?: "Error desconocido"
                        withContext(Dispatchers.Main) { fin?.onFin(null, msg) }
                        throw Exception(msg)
                    }
                    "done" -> break
                    else -> {
                        val pct = trabajo.progress ?: 0
                        withContext(Dispatchers.Main) { listener?.onCambio(trabajo, pct) }
                    }
                }
            }

            // 3) Descargar el archivo resultante
            val dir = File(context.getExternalFilesDir(null), "musica")
            if (!dir.exists()) dir.mkdirs()

            val nombreSeguro = (trabajo.filename ?: ("sonora_" + id))
                .replace(Regex("[^A-Za-z0-9._ -]"), "_")
            val destino = File(dir, nombreSeguro)

            val req = Request.Builder().url(ApiClient.base.trimEnd('/') + "/api/file/$id").build()
            cliente.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw Exception("No se pudo descargar el archivo (${resp.code})")
                val entrada = resp.body?.byteStream() ?: throw Exception("Respuesta vacía del servidor")
                destino.outputStream().use { salida -> entrada.copyTo(salida) }
            }

            val info = trabajo.info
            val pista = Pista(
                id = (info?.id ?: id),
                title = (info?.title ?: trabajo.filename ?: "Sin título"),
                artist = (info?.uploader ?: "Desconocido"),
                duration = (info?.duration ?: 0.0),
                thumbnail = (info?.thumbnail ?: ""),
                source = fuente,
                filePath = destino.absolutePath,
                format = formato
            )

            withContext(Dispatchers.Main) {
                listener?.onCambio(trabajo.copy(progress = 100, state = "done"), 100)
                fin?.onFin(pista, null)
            }
            pista
        } catch (e: Exception) {
            val msg = e.message ?: "Fallo inesperado"
            withContext(Dispatchers.Main) { fin?.onFin(null, msg) }
            throw e
        }
    }

    /** Lanza la descarga en segundo plano (no bloquea la interfaz). */
    fun descargarEnSegundoPlano(
        context: Context,
        enlace: String,
        formato: String,
        fuente: String,
        listener: Listener?,
        fin: FinListener?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            descargar(context, enlace, formato, fuente, listener, fin)
        }
    }
}
