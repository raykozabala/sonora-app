package com.sonora.app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sonora.app.databinding.FragmentBuscarBinding
import kotlinx.coroutines.launch

/**
 * Pantalla de búsqueda: busca en YouTube y SoundCloud, muestra los resultados
 * y permite elegir formato/calidad antes de descargar.
 */
class BuscarFragment : Fragment() {

    private var _b: FragmentBuscarBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: ResultadoAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentBuscarBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        adaptador = ResultadoAdapter(
            alPulsar = { muestraFormatos(it) },
            alDescargar = { muestraFormatos(it) }
        )

        b.listaResultados.layoutManager = LinearLayoutManager(requireContext())
        b.listaResultados.adapter = adaptador

        b.campoBusqueda.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_SEARCH) {
                val q = b.campoBusqueda.text?.toString()?.trim().orEmpty()
                if (q.length >= 2) busca(q)
                true
            } else false
        }

        // Rellena con la última búsqueda guardada
        Store.historial().firstOrNull()?.let { b.campoBusqueda.setText(it) }
    }

    private fun busca(consulta: String) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(b.campoBusqueda.windowToken, 0)

        b.cargando.visibility = View.VISIBLE
        b.textoVacio.visibility = View.GONE
        b.listaResultados.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resultados = ApiClient.buscar(consulta, 10)
                Store.añadirHistorial(consulta)

                b.cargando.visibility = View.GONE
                if (resultados.isEmpty()) {
                    b.textoVacio.text = getString(R.string.sin_resultados)
                    b.textoVacio.visibility = View.VISIBLE
                } else {
                    adaptador.actualizar(resultados)
                    b.listaResultados.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                b.cargando.visibility = View.GONE
                b.textoVacio.text = "No se pudo conectar con el servidor.\n" +
                    "URL actual: " + ApiClient.base + "\n" +
                    "Revisa la pestaña Ajustes (guarda y pulsa Probar)."
                b.textoVacio.visibility = View.VISIBLE
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Consulta los formatos disponibles y muestra el selector. */
    private fun muestraFormatos(r: Resultado) {
        val enlace = r.url ?: return
        Toast.makeText(requireContext(), "Consultando formatos…", Toast.LENGTH_SHORT).show()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val ficha = ApiClient.ficha(enlace)
                val opciones = construyeOpciones(ficha, r.source ?: "Web", enlace)
                if (opciones.isEmpty()) {
                    Toast.makeText(requireContext(), "No hay formatos disponibles", Toast.LENGTH_SHORT).show()
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.titulo_formatos)
                        .setItems(opciones.map { it.etiqueta }.toTypedArray()) { _, i ->
                            descarga(enlace, opciones[i].formato, r.source ?: "Web")
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private data class Opcion(val etiqueta: String, val formato: String)

    private fun construyeOpciones(ficha: Ficha, fuente: String, enlace: String): List<Opcion> {
        val lista = mutableListOf<Opcion>()

        ficha.audio?.distinctBy { it.abr }?.sortedByDescending { it.abr ?: 0.0 }?.forEach { a ->
            val kbps = a.abr?.let { "${it.toInt()} kbps" } ?: "calidad variable"
            val mb = a.filesize?.let { " · ${"%.1f".format(it / 1048576)} MB" } ?: ""
            lista.add(Opcion("🎵 MP3 · $kbps$mb", "mp3"))
        }

        ficha.video?.distinctBy { it.height }?.forEach { v ->
            val p = v.height?.let { "${it}p" } ?: "video"
            val fps = v.fps?.let { " ${it.toInt()}fps" } ?: ""
            val mb = v.filesize?.let { " · ${"%.1f".format(it / 1048576)} MB" } ?: ""
            lista.add(Opcion("🎬 Vídeo · $p$fps$mb", "mp4"))
        }

        // Añadimos siempre la opción por defecto por si el servidor no lista calidades
        if (lista.none { it.formato == "mp3" }) lista.add(0, Opcion("🎵 MP3 · calidad máxima", "mp3"))
        return lista
    }

    private fun descarga(enlace: String, formato: String, fuente: String) {
        // Contexto de aplicación: evita que la app se cierre si el usuario
        // cambia de pestaña antes de que termine la descarga.
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
