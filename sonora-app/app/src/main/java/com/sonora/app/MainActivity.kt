package com.sonora.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.sonora.app.databinding.ActivityMainBinding

/**
 * Actividad principal de Sonora.
 * Contiene la navegación inferior y el mini-reproductor.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Arranca en la pestaña de búsqueda
        if (savedInstanceState == null) cambia(BuscarFragment())

        b.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_buscar -> cambia(BuscarFragment())
                R.id.nav_biblioteca -> cambia(BibliotecaFragment())
                R.id.nav_playlists -> cambia(PlaylistsFragment())
                R.id.nav_recomendados -> cambia(RecomendadosFragment())
            }
            true
        }

        configuraMiniReproductor()
    }

    private fun cambia(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor, f)
            .commit()
    }

    private fun configuraMiniReproductor() {
        PlayerManager.enPista = { pista ->
            if (pista != null) {
                b.mini.root.visibility = android.view.View.VISIBLE
                b.mini.textoTitulo.text = pista.title
                b.mini.textoArtista.text = pista.artist
                b.mini.botonPlay.setImageResource(R.drawable.ic_library)
            } else {
                b.mini.root.visibility = android.view.View.GONE
            }
        }

        PlayerManager.enEstado = { reproduciendo ->
            b.mini.botonPlay.setImageResource(
                if (reproduciendo) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
        }

        b.mini.botonPlay.setOnClickListener {
            if (PlayerManager.actual() == null) {
                Toast.makeText(this, "Elige una canción de tu biblioteca", Toast.LENGTH_SHORT).show()
            } else {
                PlayerManager.pausaOReanuda()
            }
        }

        b.mini.root.setOnClickListener { PlayerManager.pausaOReanuda() }
    }

    override fun onDestroy() {
        super.onDestroy()
        PlayerManager.detente()
    }
}
