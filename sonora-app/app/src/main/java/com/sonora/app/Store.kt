package com.sonora.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistencia local de Sonora: biblioteca, playlists e historial.
 * Usa SharedPreferences con JSON, sin dependencias externas.
 */
object Store {

    const val SERVIDOR_POR_DEFECTO = "http://192.168.1.132:8080"

    private const val PREFS = "sonora_prefs"
    private const val K_BIBLIOTECA = "biblioteca"
    private const val K_PLAYLISTS = "playlists"
    private const val K_HISTORIAL = "historial"
    private const val K_SERVIDOR = "servidor"

    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ApiBase.leer()
    }

    /* ---------- Servidor ---------- */
    fun servidor(): String = prefs.getString(K_SERVIDOR, SERVIDOR_POR_DEFECTO) ?: SERVIDOR_POR_DEFECTO

    fun guardarServidor(url: String) {
        prefs.edit().putString(K_SERVIDOR, url).apply()
        ApiBase.leer()
    }

    /* ---------- Biblioteca ---------- */

    fun biblioteca(): List<Pista> = leerLista(K_BIBLIOTECA)

    fun añadirABiblioteca(p: Pista): List<Pista> {
        val actual = biblioteca().toMutableList()
        if (actual.none { it.id == p.id }) actual.add(0, p)
        escribirLista(K_BIBLIOTECA, actual)
        return actual
    }

    fun quitarDeBiblioteca(id: String): List<Pista> {
        val restante = biblioteca().filterNot { it.id == id }
        escribirLista(K_BIBLIOTECA, restante)
        return restante
    }

    /* ---------- Historial de búsquedas ---------- */

    fun historial(): List<String> {
        val arr = JSONArray(prefs.getString(K_HISTORIAL, "[]") ?: "[]")
        return (0 until arr.length()).map { arr.getString(it) }
    }

    fun añadirHistorial(consulta: String) {
        val limpio = consulta.trim()
        if (limpio.isEmpty()) return
        val lista = historial().toMutableList()
        lista.remove(limpio)
        lista.add(0, limpio)
        escribirLista(K_HISTORIAL, lista.take(12).map { Pista("h:" + it, it, "", 0.0, "", "", "", "mp3", addedAt = System.currentTimeMillis()) })
    }

    /* ---------- Playlists ---------- */

    fun playlists(): List<Playlist> {
        val arr = JSONArray(prefs.getString(K_PLAYLISTS, "[]") ?: "[]")
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val pistas = o.optJSONArray("pistas") ?: JSONArray()
            Playlist(
                id = o.optString("id"),
                nombre = o.optString("nombre"),
                pistas = (0 until pistas.length()).mapNotNull { j -> pistas.optJSONObject(j)?.let { leerPista(it) } }
            )
        }
    }

    fun crearPlaylist(nombre: String): List<Playlist> {
        val lista = playlists().toMutableList()
        lista.add(Playlist(id = "pl" + System.currentTimeMillis(), nombre = nombre.trim(), pistas = emptyList()))
        escribirPlaylists(lista)
        return lista
    }

    fun añadirAPlaylist(playlistId: String, pista: Pista): List<Playlist> {
        val lista = playlists().toMutableList()
        val idx = lista.indexOfFirst { it.id == playlistId }
        if (idx >= 0) {
            val pl = lista[idx]
            if (pl.pistas.none { it.id == pista.id }) {
                lista[idx] = pl.copy(pistas = pl.pistas + pista)
                escribirPlaylists(lista)
            }
        }
        return lista
    }

    fun borrarPlaylist(id: String): List<Playlist> {
        val restante = playlists().filterNot { it.id == id }
        escribirPlaylists(restante)
        return restante
    }

    /* ---------- Helpers de serialización ---------- */

    private fun leerLista(clave: String): List<Pista> {
        val arr = JSONArray(prefs.getString(clave, "[]") ?: "[]")
        return (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { leerPista(it) } }
    }

    private fun escribirLista(clave: String, lista: List<Pista>) {
        val arr = JSONArray()
        lista.forEach { arr.put(escribirPista(it)) }
        prefs.edit().putString(clave, arr.toString()).apply()
    }

    private fun escribirPlaylists(lista: List<Playlist>) {
        val arr = JSONArray()
        lista.forEach { pl ->
            val o = JSONObject().put("id", pl.id).put("nombre", pl.nombre)
            val pistas = JSONArray()
            pl.pistas.forEach { pistas.put(escribirPista(it)) }
            o.put("pistas", pistas)
            arr.put(o)
        }
        prefs.edit().putString(K_PLAYLISTS, arr.toString()).apply()
    }

    private fun leerPista(o: JSONObject): Pista? {
        val id = o.optString("id")
        if (id.isBlank()) return null
        return Pista(
            id = id,
            title = o.optString("title"),
            artist = o.optString("artist"),
            duration = o.optDouble("duration").takeIf { !it.isNaN() } ?: 0.0,
            thumbnail = o.optString("thumbnail"),
            source = o.optString("source"),
            filePath = o.optString("filePath"),
            format = o.optString("format"),
            addedAt = o.optLong("addedAt").takeIf { it > 0 } ?: System.currentTimeMillis()
        )
    }

    private fun escribirPista(p: Pista): JSONObject = JSONObject()
        .put("id", p.id)
        .put("title", p.title)
        .put("artist", p.artist)
        .put("duration", p.duration)
        .put("thumbnail", p.thumbnail)
        .put("source", p.source)
        .put("filePath", p.filePath)
        .put("format", p.format)
        .put("addedAt", p.addedAt)
}

/** Playlist guardada localmente. */
data class Playlist(val id: String, val nombre: String, val pistas: List<Pista>)

/** Atajo para leer la URL del servidor desde cualquier punto. */
private object ApiBase {
    fun leer() { ApiClient.base = Store.servidor() }
}
