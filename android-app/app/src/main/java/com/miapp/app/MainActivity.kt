package com.miapp.app

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Actividad principal.
 * Pantalla de validación: comprueba que el APK compila, se instala
 * y responde a los toques del usuario.
 */
class MainActivity : AppCompatActivity() {

    private var contador = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val padding = (24 * resources.displayMetrics.density).toInt()

        val titulo = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 26f
            gravity = Gravity.CENTER
            setPadding(padding, padding, padding, 8)
        }

        val estado = TextView(this).apply {
            text = getString(R.string.listo)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(padding, 8, padding, padding)
        }

        val boton = Button(this).apply {
            text = getString(R.string.boton)
            setOnClickListener {
                contador++
                estado.text = getString(R.string.contador, contador)
            }
        }

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(padding, padding, padding, padding)
            addView(titulo)
            addView(estado)
            addView(boton)
        }

        setContentView(raiz)
    }
}
