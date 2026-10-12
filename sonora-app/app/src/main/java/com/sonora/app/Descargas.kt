package com.sonora.app

/**
 * Estado global de las descargas en curso.
 * Permite que cualquier pantalla muestre el progreso en tiempo real
 * y que la biblioteca se actualice sola cuando termina una descarga.
 */
object Descargas {

    /** true mientras hay una descarga lanzada */
    @Volatile var activa = false

    /** progreso 0..100 de la última descarga lanzada */
    @Volatile var progreso = 0

    private val oyentes = mutableListOf<() -> Unit>()

    fun observe(f: () -> Unit) {
        oyentes.add(f)
    }

    fun desobserve(f: () -> Unit) {
        oyentes.remove(f)
    }

    fun notifica(p: Int) {
        activa = true
        progreso = p
        oyentes.toList().forEach { it() }
    }

    fun fin() {
        activa = false
        progreso = 0
        oyentes.toList().forEach { it() }
    }
}
