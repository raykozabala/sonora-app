package com.sonora.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sonora.app.databinding.FragmentRecomendadosBinding
import kotlinx.coroutines.launch

/**
 * Recomendaciones: usa las pistas de la biblioteca como base y
 * pide al servidor contenido relacionado (YouTube).
 */
class RecomendadosFragment : Fragment() {

    private var _b: FragmentRecomendadosBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: ResultadoAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentRecomendadosBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        adaptador = ResultadoAdapter(
            alPulsar = { muestraFormatos(it) },
            alDescargar = { muestraFormatos(it) }
        )

        b.lista.layoutManager = LinearLayoutManager(requireContext())
        b.lista.adapter = adaptador

        carga()
    }

    /** Usa las pistas guardadas para pedir recomendaciones. */
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
        val enlace = semilla.thumbnail
            .replace("https://i.ytimg.com/vi_webp/", "https://www.youtube.com/watch?v=")
            .replace("https://i.ytimg.com/vi/", "https://www.youtube.com/watch?v=")
            .replace(Regex("\\.(jpg|webp|png).*"), "")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val relacionados = ApiClient.relacionados(enlace)
                b.cargando.visibility = View.GONE

                if (relacionados.isEmpty()) {
                    b.textoVacio.visibility = View.VISIBLE
                    b.textoVacio.text = "No se pudieron cargar recomendaciones."
                } else {
                    adaptador.actualizar(relacionados)
                    b.subtitulo.text = "Basado en «${semilla.title}»"
                }
            } catch (e: Exception) {
                b.cargando.visibility = View.GONE
                b.textoVacio.visibility = View.VISIBLE
                b.textoVacio.text = "No se pudo cargar. ¿Está el servidor encendido?"
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
        }
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
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun descarga(enlace: String, formato: String, fuente: String) {
        Toast.makeText(requireContext(), R.string.descargando, Toast.LENGTH_SHORT).show()
        DescargaManager.descargarEnSegundoPlano(
            requireContext().applicationContext,
            enlace, formato, fuente,
            listener = null,
            fin = { pista, error ->
                if (pista != null) {
                    Store.añadirABiblioteca(pista)
                    Toast.makeText(requireContext(), R.string.descarga_lista, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), error ?: "Fallo", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
