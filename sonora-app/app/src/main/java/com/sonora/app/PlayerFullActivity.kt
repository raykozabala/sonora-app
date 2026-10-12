package com.sonora.app

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.sonora.app.databinding.ActivityPlayerFullBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reproductor a pantalla completa.
 * - Carátula grande con fondo dinámico (colores de la portada).
 * - Letras sincronizadas tipo Apple Music (con resaltado y autoscroll).
 * - Editor de letras con marcado de tiempos en vivo.
 * - Ondas animadas, velocidad, ahorro de batería y cola de reproducción.
 */
class PlayerFullActivity : AppCompatActivity() {

    private lateinit var b: ActivityPlayerFullBinding
    private val handler = Handler(Looper.getMainLooper())

    private var lineas: List<LineaLetra> = emptyList()
    private var indiceLetra = -1
    private var ahorroActivo = false
    private var girar: ObjectAnimator? = null

    private var ondaActivada = false
    private var idUltimo: String? = null

    data class LineaLetra(val ms: Long, val texto: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPlayerFullBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Transición de entrada: sube desde abajo
        overridePendingTransition(R.anim.slide_up_bottom, android.R.anim.fade_out)

        if (PlayerManager.actual() == null) { finish(); return }

        configuraToolbar()
        configuraControles()
        cargaPista()
        cargaLetras()
        iniciaBucle()
    }

    /* ---------------- Datos ---------------- */

    private fun cargaPista() {
        val p = PlayerManager.actual() ?: return

        b.txtTituloFull.text = p.title
        b.txtArtistaFull.text = p.artist
        b.txtAlbumFull.text = if (p.album.isBlank()) p.source else p.album

        b.imgCaratulaFull.load(p.thumbnail) {
            crossfade(400)
            placeholder(android.R.color.darker_gray)
            listener(onSuccess = { _, _ -> pintaFondoDinamico() })
        }

        pintaCola()
        pintaBoton(PlayerManager.estaReproduciendo())
        iniciaGiro()
    }

    /** Fondo con los colores dominantes de la carátula (tipo Apple Music). */
    private fun pintaFondoDinamico() {
        val bmp = (b.imgCaratulaFull.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            ?: return
        Palette.from(bmp).generate { paleta ->
            val oscuro = paleta?.getDominantColor(
                resources.getColor(R.color.fondo)
            ) ?: resources.getColor(R.color.fondo)
            val claro = paleta?.getVibrantColor(
                resources.getColor(R.color.primario)
            ) ?: resources.getColor(R.color.primario)

            val fondo = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(oscuro, ColorUtils.blendARGB(oscuro, Color.BLACK, 0.55f))
            )
            b.fondoDinamico.background = fondo
            b.txtTituloFull.setTextColor(Color.WHITE)
            b.caratulaCard.setCardBackgroundColor(claro)
        }
    }

    private fun pintaCola() {
        val idx = PlayerManager.posicionEnCola()
        val total = PlayerManager.cola().size
        b.txtCola.text = if (total > 1) "${idx + 1} / $total en cola" else ""
    }

    private fun pintaBoton(reproduciendo: Boolean) {
        b.btnPlayPause.setImageResource(
            if (reproduciendo) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
        if (reproduciendo) iniciaGiro() else paraGiro()
    }

    /* ---------------- Disco giratorio ---------------- */

    private fun iniciaGiro() {
        if (ahorroActivo) return
        if (girar?.isRunning == true) return
        girar = ObjectAnimator.ofFloat(b.caratulaCard, "rotation", 0f, 360f).apply {
            duration = 22000
            interpolator = LinearInterpolator()
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun paraGiro() {
        girar?.cancel()
        girar = null
    }

    /* ---------------- Toolbar ---------------- */

    private fun configuraToolbar() {
        b.toolbarPlayer.setNavigationOnClickListener { cierra() }
        b.toolbarPlayer.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_ondas -> muestraVista("ondas")
                R.id.action_disco -> muestraVista("disco")
                else -> false
            }
        }
    }

    private fun muestraVista(cual: String): Boolean {
        when (cual) {
            "disco" -> {
                ondaActivada = false
                b.layoutDisco.visibility = View.VISIBLE
                b.waveView.visibility = View.GONE
                b.recyclerLetras.visibility = View.GONE
                b.layoutLetrasEdit.visibility = View.GONE
                b.txtSinLetras.visibility = View.GONE
            }
            "ondas" -> {
                ondaActivada = true
                b.layoutDisco.visibility = View.VISIBLE
                b.waveView.visibility = View.VISIBLE
                b.recyclerLetras.visibility = View.GONE
                b.layoutLetrasEdit.visibility = View.GONE
                b.txtSinLetras.visibility = View.GONE
            }
            "letras" -> {
                ondaActivada = false
                b.layoutDisco.visibility = View.GONE
                b.layoutLetrasEdit.visibility = View.GONE
                b.waveView.visibility = View.GONE
                b.recyclerLetras.visibility =
                    if (lineas.isEmpty()) View.GONE else View.VISIBLE
                b.txtSinLetras.visibility =
                    if (lineas.isEmpty()) View.VISIBLE else View.GONE
            }
            "editor" -> {
                ondaActivada = false
                b.layoutDisco.visibility = View.GONE
                b.recyclerLetras.visibility = View.GONE
                b.txtSinLetras.visibility = View.GONE
                b.layoutLetrasEdit.visibility = View.VISIBLE
            }
        }
        return true
    }

    /* ---------------- Controles ---------------- */

    private fun configuraControles() {
        b.btnPlayPause.setOnClickListener { PlayerManager.pausaOReanuda() }
        b.btnSiguiente.setOnClickListener { PlayerManager.siguiente() }
        b.btnAtras.setOnClickListener { PlayerManager.anterior() }

        b.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, pr: Int, user: Boolean) {
                if (user) b.txtPosicion.text = PlayerManager.formatearMs(pr)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                sb?.progress?.let { PlayerManager.avanza(it) }
            }
        })

        b.btnVelocidad.setOnClickListener { cambiaVelocidad() }
        b.btnAhorro.setOnClickListener { alternaAhorro() }
        b.btnLetras.setOnClickListener { muestraVista("letras") }
        b.btnEditarLetras.setOnClickListener { muestraVista("editor") }

        b.btnGuardarLetras.setOnClickListener {
            val p = PlayerManager.actual() ?: return@setOnClickListener
            LyricsManager.saveLyrics(this, p.id, b.editLetras.text.toString())
            Toast.makeText(this, "Letras guardadas", Toast.LENGTH_SHORT).show()
            cargaLetras()
            muestraVista("letras")
        }

        // Marcado de tiempos: añade [mm:ss.xx] a la línea del cursor
        b.btnMarcarLetra.setOnClickListener {
            val pos = b.editLetras.selectionStart
            if (pos < 0) return@setOnClickListener
            val texto = b.editLetras.text
            val tiempo = PlayerManager.posicion()
            val marca = "[%02d:%02d.%02d]".format(
                tiempo / 60000, (tiempo / 1000) % 60, (tiempo % 1000) / 10
            )
            // inicio de la línea del cursor
            val inicioLinea = texto.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let {
                if (pos == 0) 0 else it + 1
            }
            // ¿ya tiene marca?
            val yaTiene = texto.substring(inicioLinea).startsWith("[")
            if (!yaTiene) {
                texto.insert(inicioLinea, marca + " ")
                b.editLetras.setSelection((inicioLinea + marca.length + 1).coerceAtMost(texto.length))
            }
            // salta a la siguiente línea
            val salto = texto.indexOf('\n', b.editLetras.selectionStart)
            if (salto > 0) b.editLetras.setSelection(salto + 1)
            Toast.makeText(this, "Marcado en " + PlayerManager.formatearMs(tiempo), Toast.LENGTH_SHORT).show()
        }
    }

    private fun cambiaVelocidad() {
        val opciones = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val etiquetas = arrayOf("0,5x", "0,75x", "1,0x", "1,25x", "1,5x", "2,0x")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Velocidad")
            .setItems(etiquetas) { _, i ->
                PlayerManager.velocidad(opciones[i])
                b.btnVelocidad.text = etiquetas[i]
                Toast.makeText(this, "Velocidad ${etiquetas[i]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun alternaAhorro() {
        ahorroActivo = !ahorroActivo
        if (ahorroActivo) {
            paraGiro()
            b.btnAhorro.setImageResource(R.drawable.ic_battery)
            b.btnAhorro.alpha = 1f
            Toast.makeText(this, "Ahorro de batería activado", Toast.LENGTH_SHORT).show()
        } else {
            if (PlayerManager.estaReproduciendo()) iniciaGiro()
            b.btnAhorro.alpha = 0.5f
            Toast.makeText(this, "Ahorro de batería desactivado", Toast.LENGTH_SHORT).show()
        }
        Store.guardarAhorroBateria(this, ahorroActivo)
    }

    /* ---------------- Letras ---------------- */

    private fun cargaLetras() {
        val p = PlayerManager.actual() ?: return
        b.editLetras.setText(LyricsManager.loadLyrics(this, p.id))

        // 1) si tenemos guardadas, las usamos ya
        val local = LyricsManager.loadLyrics(this, p.id)
        if (local.isNotBlank()) {
            pintaLetras(local)
            return
        }

        // 2) si no, pedimos al servidor (letras sincronizadas reales)
        lifecycleScope.launch {
            val r = withContext(Dispatchers.IO) {
                try {
                    ApiClient.letras(p.title, p.artist, p.duration)
                } catch (_: Exception) { null }
            }
            val texto = r?.sync ?: r?.plain
            if (!texto.isNullOrBlank()) {
                LyricsManager.saveLyrics(this@PlayerFullActivity, p.id, texto)
                pintaLetras(texto)
            } else if (lineas.isEmpty()) {
                b.txtSinLetras.visibility = View.VISIBLE
            }
        }
    }

    private fun pintaLetras(texto: String) {
        lineas = parseaLRC(texto)
        val adaptador = LetrasAdapter(lineas) { ms -> PlayerManager.avanza(ms.toInt()) }
        b.recyclerLetras.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = false
        }
        b.recyclerLetras.adapter = adaptador
        indiceLetra = -1
    }

    private fun parseaLRC(texto: String): List<LineaLetra> {
        val regex = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?](.*)""")
        val marcadas = texto.lines().mapNotNull { l ->
            regex.find(l.trim())?.let { m ->
                val min = m.groupValues[1].toLong()
                val seg = m.groupValues[2].toLong()
                val mils = (m.groupValues[3].ifBlank { "0" }).padEnd(3, '0').take(3).toLong()
                LineaLetra(min * 60000 + seg * 1000 + mils, m.groupValues[4].trim())
            }
        }
        // si no hay marcas de tiempo, usamos las líneas simples
        return marcadas.ifEmpty {
            texto.lines().filter { it.isNotBlank() }.map { LineaLetra(0, it) }
        }
    }

    /* ---------------- Bucle de refresco ---------------- */

    private val bucle = object : Runnable {
        override fun run() {
            val pos = PlayerManager.posicion()
            val dur = PlayerManager.duracion()
            if (dur > 0) b.seekBar.max = dur
            b.seekBar.progress = pos
            b.txtPosicion.text = PlayerManager.formatearMs(pos)
            b.txtDuracion.text = PlayerManager.formatearMs(dur)
            pintaBoton(PlayerManager.estaReproduciendo())

            // Si cambió la canción dentro de la cola, recargamos la vista
            val idAhora = PlayerManager.actual()?.id
            if (idAhora != idUltimo) {
                idUltimo = idAhora
                indiceLetra = -1
                cargaPista()
                cargaLetras()
            }

            // ondas
            if (ondaActivada && !ahorroActivo && PlayerManager.estaReproduciendo()) {
                b.waveView.update(1f)
            }

            // letra actual
            if (lineas.isNotEmpty()) sincronizaLetra(pos)

            handler.postDelayed(this, 250)
        }
    }

    private fun sincronizaLetra(pos: Int) {
        var nuevo = -1
        for (i in lineas.indices) {
            if (lineas[i].ms <= pos) nuevo = i else break
        }
        if (nuevo != indiceLetra && nuevo >= 0) {
            val previo = indiceLetra
            indiceLetra = nuevo
            val adapt = b.recyclerLetras.adapter as? LetrasAdapter ?: return
            adapt.actual = nuevo
            if (previo >= 0) adapt.notifyItemChanged(previo)
            adapt.notifyItemChanged(nuevo)
            (b.recyclerLetras.layoutManager as? LinearLayoutManager)?.let { lm ->
                val scroller = object : LinearSmoothScroller(this) {
                    override fun getVerticalSnapPreference() = SNAP_TO_START
                    override fun calculateSpeedPerPixel(metrics: android.util.DisplayMetrics) =
                        55f / metrics.densityDpi
                }
                scroller.targetPosition = nuevo
                lm.startSmoothScroll(scroller)
            }
        }
    }

    private fun iniciaBucle() {
        handler.post(bucle)
    }

    private fun cierra() {
        onBackPressed()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, R.anim.slide_down_out)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(bucle)
        paraGiro()
    }
}

/**
 * Adaptador de letras con resaltado de la línea actual
 * y salto al tiempo correspondiente al tocar.
 */
class LetrasAdapter(
    private val items: List<PlayerFullActivity.LineaLetra>,
    private val alPulsar: (Long) -> Unit
) : RecyclerView.Adapter<LetrasAdapter.VH>() {

    var actual = -1

    class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val t: android.widget.TextView =
            v.findViewById(R.id.textoLetra)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val v = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_letra, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val l = items[position]
        holder.t.text = l.texto
        val activa = position == actual
        holder.t.alpha = if (activa) 1f else 0.45f
        holder.t.textSize = if (activa) 19f else 15f
        holder.t.setTextColor(
            if (activa) android.graphics.Color.WHITE
            else holder.itemView.context.resources.getColor(R.color.texto_suave)
        )
        holder.t.setOnClickListener { alPulsar(l.ms) }
    }
}
