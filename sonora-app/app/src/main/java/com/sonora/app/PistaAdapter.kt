package com.sonora.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

/**
 * Adaptador de pistas guardadas (biblioteca y playlists).
 * Al pulsar una pista, se reproduce.
 */
class PistaAdapter(
    private val alPulsar: (Pista) -> Unit,
    private val alAccion: ((Pista) -> Unit)? = null
) : RecyclerView.Adapter<PistaAdapter.VH>() {

    private val items = mutableListOf<Pista>()

    fun actualizar(nuevos: List<Pista>) {
        items.clear()
        items.addAll(nuevos)
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val imagen: ImageView = v.findViewById(R.id.imagen)
        val titulo: TextView = v.findViewById(R.id.textoTitulo)
        val artista: TextView = v.findViewById(R.id.textoArtista)
        val detalle: TextView = v.findViewById(R.id.textoDetalle)
        val accion: ImageButton = v.findViewById(R.id.botonAccion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_pista, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.titulo.text = p.title
        holder.artista.text = p.artist

        val dur = PlayerManager.formatear(p.duration)
        val fuente = if (p.source.isNotBlank()) " · ${p.source}" else ""
        holder.detalle.text = "$dur$fuente"

        holder.imagen.load(p.thumbnail) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
            error(android.R.color.darker_gray)
        }

        holder.itemView.setOnClickListener { alPulsar(p) }
        holder.accion.setOnClickListener {
            if (alAccion != null) alAccion(p) else alPulsar(p)
        }
    }
}
