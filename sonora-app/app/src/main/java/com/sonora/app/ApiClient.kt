package com.sonora.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente de la API del servidor local.
 * Toda la red ocurre fuera del hilo principal mediante corrutinas.
 */
object ApiClient {

    private val cliente = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** URL base del servidor. Se puede cambiar desde Ajustes. */
    var base: String = Store.SERVIDOR_POR_DEFECTO

    private fun url(ruta: String, params: Map<String, String>): String {
        val builder = (base.trimEnd('/') + ruta).toHttpUrlOrNull()?.newBuilder() ?: throw IllegalStateException("URL del servidor no válida")
        params.forEach { (k, v) -> builder.addQueryParameter(k, v) }
        return builder.build().toString()
    }

    /** GET que devuelve el cuerpo como texto. */
    private suspend fun get(ruta: String, params: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            val req = Request.Builder().url(url(ruta, params)).build()
            cliente.newCall(req).execute().use { resp ->
                val cuerpo = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw Exception(extraerError(cuerpo) ?: "Error HTTP ${resp.code}")
                cuerpo
            }
        }

    /** POST JSON que devuelve el cuerpo como texto. */
    private suspend fun postJson(ruta: String, json: JSONObject): String =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url(url(ruta, emptyMap()))
                .post(okhttp3.RequestBody.create("application/json".toMediaType(), json.toString()))
                .build()
            cliente.newCall(req).execute().use { resp ->
                val cuerpo = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw Exception(extraerError(cuerpo) ?: "Error HTTP ${resp.code}")
                cuerpo
            }
        }

    private fun extraerError(cuerpo: String): String? = try {
        JSONObject(cuerpo).optString("error").takeIf { it.isNotBlank() }
    } catch (_: Exception) { null }

    /* ---------- Endpoints ---------- */

    suspend fun buscar(consulta: String, limite: Int = 10): List<Resultado> {
        val cuerpo = get("/api/search", mapOf("q" to consulta, "limit" to limite.toString()))
        val arr = JSONObject(cuerpo).optJSONArray("results") ?: JSONArray()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            Resultado(
                source = o.optString("source").takeIf { it.isNotBlank() },
                id = o.optString("id").takeIf { it.isNotBlank() },
                title = o.optString("title").takeIf { it.isNotBlank() },
                uploader = o.optString("uploader").takeIf { it.isNotBlank() },
                duration = o.optDouble("duration").takeIf { !it.isNaN() },
                thumbnail = o.optString("thumbnail").takeIf { it.isNotBlank() },
                url = o.optString("url").takeIf { it.isNotBlank() }
            )
        }
    }

    suspend fun ficha(enlace: String): Ficha {
        val cuerpo = get("/api/formats", mapOf("url" to enlace))
        val o = JSONObject(cuerpo)
        return Ficha(
            title = o.optString("title").takeIf { it.isNotBlank() },
            uploader = o.optString("uploader").takeIf { it.isNotBlank() },
            duration = o.optDouble("duration").takeIf { !it.isNaN() },
            thumbnail = o.optString("thumbnail").takeIf { it.isNotBlank() },
            webpage_url = o.optString("webpage_url").takeIf { it.isNotBlank() },
            audio = leerAudio(o.optJSONArray("audio")),
            video = leerVideo(o.optJSONArray("video"))
        )
    }

    private fun leerAudio(arr: JSONArray?): List<FormatoAudio>? = arr?.let {
        (0 until it.length()).mapNotNull { i ->
            val o = it.optJSONObject(i) ?: return@mapNotNull null
            FormatoAudio(
                ext = o.optString("ext").takeIf { s -> s.isNotBlank() },
                abr = o.optDouble("abr").takeIf { !it.isNaN() },
                asr = o.optDouble("asr").takeIf { !it.isNaN() },
                note = o.optString("note").takeIf { s -> s.isNotBlank() },
                filesize = o.optDouble("filesize").takeIf { !it.isNaN() }
            )
        }
    }

    private fun leerVideo(arr: JSONArray?): List<FormatoVideo>? = arr?.let {
        (0 until it.length()).mapNotNull { i ->
            val o = it.optJSONObject(i) ?: return@mapNotNull null
            FormatoVideo(
                ext = o.optString("ext").takeIf { s -> s.isNotBlank() },
                height = o.optInt("height").takeIf { it > 0 },
                width = o.optInt("width").takeIf { it > 0 },
                fps = o.optDouble("fps").takeIf { !it.isNaN() },
                vcodec = o.optString("vcodec").takeIf { s -> s.isNotBlank() },
                note = o.optString("note").takeIf { s -> s.isNotBlank() },
                filesize = o.optDouble("filesize").takeIf { !it.isNaN() }
            )
        }
    }

    suspend fun relacionados(enlace: String): List<Resultado> {
        val cuerpo = get("/api/related", mapOf("url" to enlace))
        val arr = JSONObject(cuerpo).optJSONArray("results") ?: JSONArray()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            Resultado(
                source = o.optString("source").takeIf { it.isNotBlank() },
                id = o.optString("id").takeIf { it.isNotBlank() },
                title = o.optString("title").takeIf { it.isNotBlank() },
                uploader = o.optString("uploader").takeIf { it.isNotBlank() },
                duration = o.optDouble("duration").takeIf { !it.isNaN() },
                thumbnail = o.optString("thumbnail").takeIf { it.isNotBlank() },
                url = o.optString("url").takeIf { it.isNotBlank() }
            )
        }
    }

    /** Comprueba que el servidor está vivo y devuelve su información. */
    suspend fun ping(): EstadoServidor {
        val cuerpo = get("/api/ping")
        val o = JSONObject(cuerpo)
        return EstadoServidor(
            ok = o.optBoolean("ok", false),
            nombre = o.optString("nombre").takeIf { it.isNotBlank() },
            version = o.optString("version").takeIf { it.isNotBlank() },
            ytDlp = o.optString("yt_dlp").takeIf { it.isNotBlank() },
            ffmpeg = o.optString("ffmpeg").takeIf { it.isNotBlank() },
            ip = o.optString("ip").takeIf { it.isNotBlank() }
        )
    }

    /**
     * Letras sincronizadas (LRC) para una canción.
     * Devuelve null si no hay.
     */
    suspend fun letras(titulo: String, artista: String, duracion: Double): Letras? =
        try {
            val cuerpo = get(
                "/api/lyrics",
                mapOf(
                    "title" to titulo,
                    "artist" to artista,
                    "duration" to (duracion.toInt().toString())
                )
            )
            val o = JSONObject(cuerpo)
            Letras(
                sync = o.optString("sync").takeIf { it.isNotBlank() },
                plain = o.optString("plain").takeIf { it.isNotBlank() },
                album = o.optString("album").takeIf { it.isNotBlank() },
                fuente = o.optString("fuente").takeIf { it.isNotBlank() }
            )
        } catch (_: Exception) {
            null
        }

    suspend fun iniciarDescarga(enlace: String, formato: String, calidad: String = "0"): String {
        val json = JSONObject()
            .put("url", enlace)
            .put("format", formato)
            .put("quality", calidad)
        val cuerpo = postJson("/api/start", json)
        val id = JSONObject(cuerpo).optString("id")
        if (id.isBlank()) throw Exception("El servidor no devolvió un identificador.")
        return id
    }

    suspend fun estado(id: String): Trabajo {
        val cuerpo = get("/api/status/$id")
        val o = JSONObject(cuerpo)
        val info = o.optJSONObject("info")?.let {
            InfoPista(
                id = it.optString("id").takeIf { s -> s.isNotBlank() },
                title = it.optString("title").takeIf { s -> s.isNotBlank() },
                uploader = it.optString("uploader").takeIf { s -> s.isNotBlank() },
                duration = it.optDouble("duration").takeIf { d -> !d.isNaN() },
                thumbnail = it.optString("thumbnail").takeIf { s -> s.isNotBlank() },
                webpage_url = it.optString("webpage_url").takeIf { s -> s.isNotBlank() },
                view_count = it.optDouble("view_count").takeIf { d -> !d.isNaN() },
                album = it.optString("album").takeIf { s -> s.isNotBlank() }
            )
        }
        return Trabajo(
            id = o.optString("id").takeIf { it.isNotBlank() },
            state = o.optString("state").takeIf { it.isNotBlank() },
            progress = o.optInt("progress").takeIf { it >= 0 },
            error = o.optString("error").takeIf { it.isNotBlank() },
            format = o.optString("format").takeIf { it.isNotBlank() },
            info = info,
            filename = o.optString("filename").takeIf { it.isNotBlank() }
        )
    }
}
