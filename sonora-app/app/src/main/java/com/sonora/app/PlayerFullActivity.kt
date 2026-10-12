package com.sonora.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.sonora.app.databinding.ActivityPlayerFullBinding

class PlayerFullActivity : AppCompatActivity() {

    private lateinit var b: ActivityPlayerFullBinding
    private val handler = Handler(Looper.getMainLooper())
    private var mostrarOndas = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPlayerFullBinding.inflate(layoutInflater)
        setContentView(b.root)

        val pista = PlayerManager.actual()
        if (pista == null) {
            finish()
            return
        }

        setupToolbar()
        llenarDatos(pista)
        setupControles()
        setupLetras(pista)
        iniciarActualizacion()
    }

    private fun setupToolbar() {
        setSupportActionBar(b.toolbarPlayer)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        b.toolbarPlayer.setNavigationOnClickListener { onBackPressed() }
        b.toolbarPlayer.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_ondas -> {
                    mostrarOndas = !mostrarOndas
                    b.waveView.visibility = if (mostrarOndas) View.VISIBLE else View.GONE
                    b.layoutDisco.visibility = View.VISIBLE
                    b.layoutLetras.visibility = View.GONE
                    b.layoutLetrasEdit.visibility = View.GONE
                    true
                }
                else -> false
            }
        }
    }

    private fun llenarDatos(p: Pista) {
        b.txtTituloFull.text = p.title
        b.txtArtistaFull.text = p.artist
        b.imgCaratulaFull.load(p.thumbnail) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
        }
    }

    private fun setupControles() {
        b.btnPlayPause.setOnClickListener { PlayerManager.pausaOReanuda() }
        b.btnAtras.setOnClickListener { Toast.makeText(this, "Anterior", Toast.LENGTH_SHORT).show() }
        b.btnSiguiente.setOnClickListener { Toast.makeText(this, "Siguiente", Toast.LENGTH_SHORT).show() }
        b.btnVelocidad.setOnClickListener {
            Toast.makeText(this, "Velocidad x1.0 (pendiente)", Toast.LENGTH_SHORT).show()
        }
        b.btnAhorro.setOnClickListener {
            Toast.makeText(this, "Modo ahorro batería (básico)", Toast.LENGTH_SHORT).show()
        }

        b.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.progress?.let { PlayerManager.avanza(it) }
            }
        })
    }

    private fun setupLetras(p: Pista) {
        val id = p.id
        val txt = LyricsManager.loadLyrics(this, id)
        b.btnLetras.setOnClickListener {
            b.layoutLetras.visibility = View.VISIBLE
            b.layoutDisco.visibility = View.GONE
            b.layoutLetrasEdit.visibility = View.GONE
            b.recyclerLetras.adapter = SimpleLyricsAdapter(txt.lines())
            b.recyclerLetras.layoutManager = LinearLayoutManager(this)
        }
        b.btnLetrasEdit.setOnClickListener {
            b.layoutLetrasEdit.visibility = View.VISIBLE
            b.layoutLetras.visibility = View.GONE
            b.layoutDisco.visibility = View.GONE
            b.editLetras.setText(txt)
        }
        b.btnGuardarLetras.setOnClickListener {
            LyricsManager.saveLyrics(this, id, b.editLetras.text.toString())
            Toast.makeText(this, "Letras guardadas", Toast.LENGTH_SHORT).show()
        }
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            val d = PlayerManager.duracion()
            val pos = PlayerManager.posicion()
            if (d > 0) b.seekBar.max = d
            b.seekBar.progress = pos
            b.txtDuracion.text = PlayerManager.formatear(d / 1000.0)
            b.txtPosicion.text = PlayerManager.formatear(pos / 1000.0)
            if (PlayerManager.estaReproduciendo()) {
                b.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                b.waveView.update(1f)
            } else {
                b.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            }
            handler.postDelayed(this, 500)
        }
    }

    private fun iniciarActualizacion() {
        handler.post(updateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }
}

class SimpleLyricsAdapter(private val lines: List<String>) :
    RecyclerView.Adapter<SimpleLyricsAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val t: android.widget.TextView = v.findViewById(android.R.id.text1)
    }
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val v = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return VH(v)
    }
    override fun getItemCount() = lines.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.t.text = lines[position]
        holder.t.setTextColor(holder.itemView.context.resources.getColor(R.color.texto))
    }
}
