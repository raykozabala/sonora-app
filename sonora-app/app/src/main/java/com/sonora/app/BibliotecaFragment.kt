package com.sonora.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.sonora.app.databinding.FragmentBibliotecaBinding

/**
 * Biblioteca: música ya descargada en el dispositivo.
 */
class BibliotecaFragment : Fragment() {

    private var _b: FragmentBibliotecaBinding? = null
    private val b get() = _b!!
    private lateinit var adaptador: PistaAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentBibliotecaBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        adaptador = PistaAdapter(alPulsar = { PlayerManager.reproduce(it) })

        b.lista.layoutManager = LinearLayoutManager(requireContext())
        b.lista.adapter = adaptador

        refresca()
    }

    private fun refresca() {
        val pistas = Store.biblioteca()
        adaptador.actualizar(pistas)

        val hayAlgo = pistas.isNotEmpty()
        b.textoVacio.visibility = if (hayAlgo) View.GONE else View.VISIBLE
        b.contador.text = if (hayAlgo) "${pistas.size} canciones" else ""
    }

    override fun onResume() {
        super.onResume()
        refresca()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
