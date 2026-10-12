package com.sonora.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.sonora.app.databinding.FragmentRecomendadosBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "Para ti": recomendaciones basadas en el contenido relacionado
 * de las canciones que ya tienes en la biblioteca.
 */
class RecomendadosFragment : Fragment() {

    private var _b: FragmentRecomendadosBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: RecomendadoAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentRecomendadosBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        adaptador = RecomendadoAdapter(
            alPulsar = { muestraFormatos(it) },
            alDescargar = { muestraFormatos(it) }
        )

        // Rejilla de tarjetas grandes, tipo Spotify
        b.lista.layoutManager = GridLayoutManager(requireContext(), 2)
        b.lista.adapter = adaptador

        carga()
    }

    private fun carga() {
        val biblioteca = Store.biblioteca()

        if (biblioteca.isEmpty()) {
            b.textoVacio.visibility = View.VISIBLE
            b.subtitulo.text = "Descarga algo y aquí te sugiero más"
            return
        }

        b.textoVacio.visibility = View.GONE
        b.cargando.visibility = View.VISIBLE

        val semilla = biblioteca.first()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val relacionados = withContext(Dispatchers.IO) {
                    pedirRelacionados(semilla)
                }

                if (_b == null) return@launch
                b.cargando.visibility = View.GONE

                if (relacionados.isEmpty()) {
                    b.textoVacio.visibility = View.VISIBLE
                    b.textoVacio.text = "No se pudieron cargar recomendaciones.\n¿Está el servidor encendido?"
                } else {
                    adaptador.actualizar(relacionados)
                    b.subtitulo.text = "Basado en «${semilla.title}»"
                }
            } catch (e: Exception) {
                if (_b == null) return@launch
                b.cargando.visibility = View.GONE
                b.textoVacio.visibility = View.VISIBLE
                b.textoVacio.text = "No se pudo cargar. ¿Está el servidor encendido?"
                Toast.makeText(requireContext().applicationContext, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Pide contenido relacionado a la pista guardada.
     * Usa el enlace original guardado al descargar; si no lo hay,
     * busca la canción en el servidor.
     */
    private suspend fun pedirRelacionados(semilla: Pista): List<Resultado> {
        if (semilla.enlace.isNotBlank()) {
            val r = ApiClient.relacionados(semilla.enlace)
            if (r.isNotEmpty()) return r
        }
        // Respaldo: buscar la canción y usar el primer resultado
        val q = (semilla.artist + " " + semilla.title).trim()
        if (q.isBlank()) return emptyList()
        val encontrados = ApiClient.buscar(q, 5)
        val primero = encontrados.firstOrNull { it.url?.isNotBlank() == true } ?: return emptyList()
        return if (primero.url != null) ApiClient.relacionados(primero.url) else emptyList()
    }

    private fun muestraFormatos(r: Resultado) {
        val enlace = r.url ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val ficha = ApiClient.ficha(enlace)
                val opciones = mutableListOf<Pair<String, String>>()
                ficha.audio?.distinctBy { it.abr }?.sortedByDescending { it.abr ?: 0.0 }?.forEach { a ->
                    val kbps = a.abr?.let { "${it.toInt()} kbps" } ?: "calidad variable"
                    opciones.add("🎵 MP3 · $kbps" to "mp3")
                }
                ficha.video?.distinctBy { it.height }?.forEach { v ->
                    val p = v.height?.let { "${it}p" } ?: "video"
                    opciones.add("🎬 Vídeo · $p" to "mp4")
                }
                if (opciones.none { it.second == "mp3" }) opciones.add(0, "🎵 MP3 · calidad máxima" to "mp3")

                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.titulo_formatos)
                    .setItems(opciones.map { it.first }.toTypedArray()) { _, i ->
                        descarga(enlace, opciones[i].second, r.source ?: "Web")
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext().applicationContext, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun descarga(enlace: String, formato: String, fuente: String) {
        val app = requireContext().applicationContext
        Toast.makeText(app, R.string.descargando, Toast.LENGTH_SHORT).show()
        DescargaManager.descargarEnSegundoPlano(
            app,
            enlace, formato, fuente,
            listener = { _, progreso -> Descargas.notifica(progreso) },
            fin = { pista, error ->
                if (pista != null) {
                    Store.añadirABiblioteca(pista)
                    Toast.makeText(app, R.string.descarga_lista, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(app, error ?: "Fallo", Toast.LENGTH_LONG).show()
                }
                Descargas.fin()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
