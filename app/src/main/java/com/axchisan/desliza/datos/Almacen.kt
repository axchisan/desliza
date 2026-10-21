package com.axchisan.desliza.datos

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lo que la aplicación recuerda entre usos: los ajustes, qué se decidió conservar, qué está en la
 * papelera pendiente de borrar y cuánto se ha liberado. Todo queda en el teléfono.
 */
class Almacen(contexto: Context) {

    private val prefs = contexto.getSharedPreferences("desliza", Context.MODE_PRIVATE)
    private val archivoConservados = File(contexto.filesDir, "conservados.txt")
    private val archivoPendientes = File(contexto.filesDir, "pendientes.txt")

    var orden: Orden
        get() = Orden.entries.getOrElse(prefs.getInt("orden", 0)) { Orden.ANTIGUAS }
        set(valor) = prefs.edit().putInt("orden", valor.ordinal).apply()

    var tipo: Tipo
        get() = Tipo.entries.getOrElse(prefs.getInt("tipo", 0)) { Tipo.TODO }
        set(valor) = prefs.edit().putInt("tipo", valor.ordinal).apply()

    var repasarConservadas: Boolean
        get() = prefs.getBoolean("repasarConservadas", false)
        set(valor) = prefs.edit().putBoolean("repasarConservadas", valor).apply()

    var usarPapeleraSistema: Boolean
        get() = prefs.getBoolean("usarPapeleraSistema", false)
        set(valor) = prefs.edit().putBoolean("usarPapeleraSistema", valor).apply()

    var bytesLiberados: Long
        get() = prefs.getLong("bytesLiberados", 0)
        set(valor) = prefs.edit().putLong("bytesLiberados", valor).apply()

    var archivosBorrados: Int
        get() = prefs.getInt("archivosBorrados", 0)
        set(valor) = prefs.edit().putInt("archivosBorrados", valor).apply()

    var tema: Int
        get() = prefs.getInt("tema", 0)
        set(valor) = prefs.edit().putInt("tema", valor).apply()

    var guiaVista: Boolean
        get() = prefs.getBoolean("guiaVista", false)
        set(valor) = prefs.edit().putBoolean("guiaVista", valor).apply()

    suspend fun leerConservados() = leer(archivoConservados)
    suspend fun leerPendientes() = leer(archivoPendientes)
    suspend fun guardarConservados(ids: Set<Long>) = guardar(archivoConservados, ids)
    suspend fun guardarPendientes(ids: Set<Long>) = guardar(archivoPendientes, ids)

    private suspend fun leer(archivo: File): Set<Long> = withContext(Dispatchers.IO) {
        if (!archivo.exists()) return@withContext emptySet()
        archivo.readLines().mapNotNullTo(HashSet()) { it.toLongOrNull() }
    }

    /** Escribe en un temporal y lo renombra, para no dejar el archivo a medias si se cierra la app. */
    private suspend fun guardar(archivo: File, ids: Set<Long>) = withContext(Dispatchers.IO) {
        val temporal = File(archivo.parentFile, archivo.name + ".tmp")
        temporal.writeText(ids.joinToString("\n"))
        temporal.renameTo(archivo)
    }
}
