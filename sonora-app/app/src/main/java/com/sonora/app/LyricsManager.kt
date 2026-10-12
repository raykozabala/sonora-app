package com.sonora.app

import android.content.Context
import org.json.JSONObject
import java.io.File

object LyricsManager {
    private const val DIR = "letras"

    private fun getFile(context: Context, songId: String): File {
        val dir = File(context.filesDir, DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${songId}.json")
    }

    fun saveLyrics(context: Context, songId: String, lyrics: String) {
        val f = getFile(context, songId)
        val obj = JSONObject().put("lyrics", lyrics).put("timestamp", System.currentTimeMillis())
        f.writeText(obj.toString())
    }

    fun loadLyrics(context: Context, songId: String): String {
        val f = getFile(context, songId)
        if (!f.exists()) return ""
        return try {
            JSONObject(f.readText()).optString("lyrics", "")
        } catch (_: Exception) { "" }
    }
}
