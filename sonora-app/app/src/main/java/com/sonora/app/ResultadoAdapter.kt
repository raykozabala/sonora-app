package com.sonora.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

/**
 * Adaptador de la lista de resultados de búsqueda.
 * Muestra carátula, título, artista, fuente y duración.
 */
class ResultadoAdapter(
    private val alPulsar: (Resultado) -> Unit,
    private val alDescargar: (Resultado) -> Unit
) : RecyclerView.Adapter<ResultadoAdapter.VH>() {

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
        val fuente: TextView = v.findViewById(R.id.insigniaFuente)
        val formato: TextView = v.findViewById(R.id.insigniaFormato)
        val duracion: TextView = v.findViewById(R.id.textoDuracion)
        val descargar: ImageView = v.findViewById(R.id.botonDescargar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_resultado, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.titulo.text = r.title ?: "Sin título"
        holder.artista.text = r.uploader ?: "Desconocido"
        holder.fuente.text = r.source ?: "Web"

        // Color del badge según la fuente
        val ctx = holder.itemView.context
        val (bg, txt) = when ((r.source ?: "").lowercase()) {
            "youtube" -> android.graphics.Color.parseColor("#FF0033") to android.graphics.Color.WHITE
            "youtube music" -> android.graphics.Color.parseColor("#FF0033") to android.graphics.Color.WHITE
            "soundcloud" -> android.graphics.Color.parseColor("#FF5500") to android.graphics.Color.WHITE
            "enlace directo" -> ctx.resources.getColor(R.color.acento) to android.graphics.Color.BLACK
            else -> ctx.resources.getColor(R.color.primario) to android.graphics.Color.WHITE
        }
        holder.fuente.background.setTint(bg)
        holder.fuente.setTextColor(txt)
        holder.formato.text = "MP3 · MP4"

        val seg = r.duration?.toInt() ?: 0
        holder.duracion.text = PlayerManager.formatear(seg.toDouble())

        holder.imagen.load(r.thumbnail) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
            error(android.R.color.darker_gray)
        }

        holder.itemView.setOnClickListener { alPulsar(r) }
        holder.descargar.setOnClickListener { alDescargar(r) }
    }
}
