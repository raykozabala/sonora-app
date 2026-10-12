package com.sonora.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sonora.app.databinding.FragmentPlaylistsBinding

/**
 * Pantalla de playlists: crear, ver y borrar.
 */
class PlaylistsFragment : Fragment() {

    private var _b: FragmentPlaylistsBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: AdaptadorPlaylists

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentPlaylistsBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        adaptador = AdaptadorPlaylists(
            alAbrir = { pl ->
                // Reproduce toda la playlist en orden
                if (pl.pistas.isEmpty()) {
                    Toast.makeText(requireContext(), "Esta playlist está vacía", Toast.LENGTH_SHORT).show()
                } else {
                    PlayerManager.reproduceLista(pl.pistas, 0)
                    Toast.makeText(
                        requireContext().applicationContext,
                        "Reproduciendo «${pl.nombre}»",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            alBorrar = { pl ->
                val lista = Store.borrarPlaylist(pl.id)
                refresca(lista)
                Toast.makeText(requireContext().applicationContext, "Playlist borrada", Toast.LENGTH_SHORT).show()
            }
        )

        b.lista.layoutManager = LinearLayoutManager(requireContext())
        b.lista.adapter = adaptador

        b.botonCrear.setOnClickListener { pideNombre() }

        refresca(Store.playlists())
    }

    private fun pideNombre() {
        val campo = EditText(requireContext()).apply { hint = "Nombre de la playlist" }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Crear nueva playlist")
            .setMessage("Dale un nombre bonito a tu playlist")
            .setView(campo)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = campo.text?.toString()?.trim().orEmpty()
                if (nombre.isEmpty()) {
                    Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                } else {
                    val lista = Store.crearPlaylist(nombre)
                    refresca(lista)
                    Toast.makeText(requireContext(), "Playlist creada", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun refresca(lista: List<Playlist>) {
        adaptador.actualizar(lista)
        b.textoVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}

/** Adaptador interno de playlists. */
class AdaptadorPlaylists(
    private val alAbrir: (Playlist) -> Unit,
    private val alBorrar: (Playlist) -> Unit
) : RecyclerView.Adapter<AdaptadorPlaylists.VH>() {

    private val items = mutableListOf<Playlist>()

    fun actualizar(nuevos: List<Playlist>) {
        items.clear()
        items.addAll(nuevos)
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: android.widget.TextView = v.findViewById(R.id.textoNombre)
        val cantidad: android.widget.TextView = v.findViewById(R.id.textoCantidad)
        val borrar: android.widget.ImageButton = v.findViewById(R.id.botonBorrar)
        val icono: android.widget.ImageView = v.findViewById(R.id.iconoPlaylist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pl = items[position]
        holder.nombre.text = pl.nombre
        holder.cantidad.text = if (pl.pistas.size == 1) "1 canción" else "${pl.pistas.size} canciones"
        holder.itemView.setOnClickListener { alAbrir(pl) }
        holder.borrar.setOnClickListener { alBorrar(pl) }

        // Colores vivos que rotan para que la lista no se vea plana
        val paleta = intArrayOf(
            android.graphics.Color.parseColor("#8B5CF6"),
            android.graphics.Color.parseColor("#22D3EE"),
            android.graphics.Color.parseColor("#34D399"),
            android.graphics.Color.parseColor("#F59E0B"),
            android.graphics.Color.parseColor("#F87171"),
            android.graphics.Color.parseColor("#60A5FA")
        )
        val color = paleta[position % paleta.size]
        holder.icono.setColorFilter(color)
        android.graphics.drawable.GradientDrawable().apply {
            setColor((color and 0x00FFFFFF) or 0x33000000)
            cornerRadius = 14f * holder.itemView.resources.displayMetrics.density
            holder.icono.background = this
        }
    }
}
