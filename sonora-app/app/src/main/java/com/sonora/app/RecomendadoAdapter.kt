package com.sonora.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

/**
 * Tarjetas grandes tipo Spotify/Apple Music para "Para ti".
 */
class RecomendadoAdapter(
    private val alPulsar: (Resultado) -> Unit,
    private val alDescargar: (Resultado) -> Unit
) : RecyclerView.Adapter<RecomendadoAdapter.VH>() {

    private val items = mutableListOf<Resultado>()

    fun actualizar(nuevos: List<Resultado>) {
        items.clear()
        items.addAll(nuevos)
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val imagen: ImageView = v.findViewById(R.id.imagen)
        val titulo: TextView = v.findViewById(R.id.textoTitulo)
        val artista: TextView = v.findViewById(R.id.textoArtista)
        val descargar: ImageView = v.findViewById(R.id.botonDescargar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recomendado, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.titulo.text = r.title ?: "Sin título"
        holder.artista.text = r.uploader ?: "Desconocido"

        holder.imagen.load(r.thumbnail) {
            crossfade(300)
            placeholder(android.R.color.darker_gray)
            error(android.R.color.darker_gray)
        }

        holder.itemView.setOnClickListener { alPulsar(r) }
        holder.descargar.setOnClickListener { alDescargar(r) }
    }
}
