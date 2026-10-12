package com.sonora.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sonora.app.databinding.FragmentBibliotecaBinding

/**
 * Biblioteca: música ya descargada en el dispositivo.
 * Muestra el progreso de las descargas en curso y se actualiza sola
 * cuando termina una descarga.
 */
class BibliotecaFragment : Fragment() {

    private var _b: FragmentBibliotecaBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: PistaAdapter

    private val oyenteDescarga: () -> Unit = {
        // Se ejecuta en el hilo principal desde Descargas
        if (_b != null && isAdded) pintaProgreso()
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentBibliotecaBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        adaptador = PistaAdapter(
            alPulsar = { pista ->
                val lista = Store.biblioteca()
                val i = lista.indexOfFirst { it.id == pista.id }
                if (i >= 0) PlayerManager.reproduceLista(lista, i)
                else PlayerManager.reproduce(pista)
            },
            alAccion = { pista -> muestraOpciones(pista) }
        )

        b.lista.layoutManager = LinearLayoutManager(requireContext())
        b.lista.adapter = adaptador
        aplicaAnimacion(b.lista)

        refresca()
        pintaProgreso()
        Descargas.observe(oyenteDescarga)
    }

    /** Menú de la pista: añadir a playlist, quitar de la biblioteca. */
    private fun muestraOpciones(pista: Pista) {
        val opciones = arrayOf("Reproducir", "Añadir a playlist", "Quitar de la biblioteca")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(pista.title)
            .setItems(opciones) { _, cual ->
                when (cual) {
                    0 -> {
                        val lista = Store.biblioteca()
                        val i = lista.indexOfFirst { it.id == pista.id }
                        if (i >= 0) PlayerManager.reproduceLista(lista, i)
                        else PlayerManager.reproduce(pista)
                    }
                    1 -> eligePlaylist(pista)
                    2 -> {
                        Store.quitarDeBiblioteca(pista.id)
                        refresca()
                    }
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    /** Elige en qué playlist meter la pista (crea una si no tienes). */
    private fun eligePlaylist(pista: Pista) {
        val actuales = Store.playlists()
        val etiquetas = (actuales.map { it.nombre } + "+ Crear playlist nueva").toTypedArray()

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Añadir a playlist")
            .setItems(etiquetas) { _, cual ->
                if (cual == actuales.size) {
                    creaPlaylistCon(pista)
                } else {
                    Store.añadirAPlaylist(actuales[cual].id, pista)
                    android.widget.Toast.makeText(
                        requireContext().applicationContext,
                        "Añadida a «${actuales[cual].nombre}»",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun creaPlaylistCon(pista: Pista) {
        val campo = android.widget.EditText(requireContext()).apply {
            hint = "Nombre de la playlist"
            setSingleLine()
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva playlist")
            .setView(campo)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = campo.text?.toString()?.trim().orEmpty()
                if (nombre.isNotEmpty()) {
                    val nueva = Store.crearPlaylistNueva(nombre)
                    Store.añadirAPlaylist(nueva.id, pista)
                    android.widget.Toast.makeText(
                        requireContext().applicationContext,
                        "Playlist creada",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun pintaProgreso() {
        if (_b == null) return
        if (Descargas.activa) {
            b.barraDescarga.visibility = View.VISIBLE
            b.indDescarga.progress = Descargas.progreso
            b.txtProgreso.text = "${Descargas.progreso}%"
            b.txtDescargando.text = "Descargando y guardando metadatos…"
        } else {
            b.barraDescarga.visibility = View.GONE
        }
    }

    private fun refresca() {
        if (_b == null) return
        val pistas = Store.biblioteca()
        adaptador.actualizar(pistas)

        val hayAlgo = pistas.isNotEmpty()
        b.textoVacio.visibility = if (hayAlgo) View.GONE else View.VISIBLE
        b.contador.text = if (hayAlgo) "${pistas.size} canciones" else ""
        if (hayAlgo) aplicaAnimacion(b.lista)
    }

    /** Animación de entrada de los elementos (transición suave). */
    private fun aplicaAnimacion(lista: RecyclerView) {
        val anim = android.view.animation.AnimationUtils.loadLayoutAnimation(
            requireContext(), R.anim.layout_fade_in
        )
        lista.layoutAnimation = anim
        lista.adapter?.notifyDataSetChanged()
        lista.scheduleLayoutAnimation()
    }

    override fun onResume() {
        super.onResume()
        refresca()
        pintaProgreso()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Descargas.desobserve(oyenteDescarga)
        _b = null
    }
}
