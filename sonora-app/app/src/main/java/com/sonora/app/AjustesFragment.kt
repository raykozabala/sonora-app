package com.sonora.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.sonora.app.databinding.FragmentAjustesBinding

class AjustesFragment : Fragment() {

    private var _b: FragmentAjustesBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentAjustesBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        b.editServidor.setText(Store.servidor())
        b.btnGuardarServidor.setOnClickListener {
            val url = b.editServidor.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(requireContext(), "URL no válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Store.guardarServidor(url)
            ApiClient.base = url
            Toast.makeText(requireContext(), "Servidor guardado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
