package com.sonora.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sonora.app.databinding.FragmentAjustesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ajustes: URL del servidor (local o nube) con prueba de conexión
 * en tiempo real, estado de los binarios y preferencias del reproductor.
 */
class AjustesFragment : Fragment() {

    private var _b: FragmentAjustesBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentAjustesBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        b.editServidor.setText(Store.servidor())
        b.estadoServidor.text = "Sin comprobar todavía"

        b.btnGuardarServidor.setOnClickListener {
            val url = b.editServidor.text?.toString()?.trim().orEmpty()
            if (url.isEmpty() || !url.startsWith("http")) {
                Toast.makeText(requireContext(), "Escribe una URL que empiece por http", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Store.guardarServidor(url)
            ApiClient.base = url
            Toast.makeText(requireContext().applicationContext, "Servidor guardado", Toast.LENGTH_SHORT).show()
            comprueba()
        }

        b.btnProbar?.setOnClickListener { comprueba() }

        // Atajos: la app rellena sola la URL según cómo quieras usarla
        b.btnServidorMac.setOnClickListener {
            b.editServidor.setText(Store.SERVIDOR_LOCAL)
            Toast.makeText(requireContext().applicationContext, "URL de tu Mac rellenada. Pulsa Guardar.", Toast.LENGTH_SHORT).show()
        }
        b.btnServidorNube.setOnClickListener {
            b.editServidor.setText("")
            b.editServidor.hint = "https://tu-servicio.onrender.com"
            b.editServidor.requestFocus()
            Toast.makeText(requireContext().applicationContext, "Pega aquí la URL de Render", Toast.LENGTH_SHORT).show()
        }
    }

    /** Comprueba que el servidor responde y muestra el estado real. */
    private fun comprueba() {
        val url = b.editServidor.text?.toString()?.trim().orEmpty()
        if (url.isEmpty()) return
        b.estadoServidor.text = "Comprobando $url …"

        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                try {
                    ApiClient.base = url
                    val est = ApiClient.ping()
                    "✅ Conectado\n" +
                        "Servidor: ${est.nombre ?: "Sonora"} v${est.version ?: "?"}\n" +
                        "yt-dlp: ${est.ytDlp ?: "no encontrado"}\n" +
                        "ffmpeg: ${est.ffmpeg ?: "no encontrado"}"
                } catch (e: Exception) {
                    "❌ Sin conexión\n" + (e.message ?: "Comprueba que la URL sea correcta")
                }
            }
            if (_b != null) b.estadoServidor.text = resultado
        }
    }

    override fun onResume() {
        super.onResume()
        if (b.editServidor.text.isNullOrBlank()) b.editServidor.setText(Store.servidor())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
