package com.axchisan.desliza.datos

import android.content.ContentResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.security.MessageDigest

/** Un conjunto de archivos parecidos; `sugeridos` son los que se proponen para borrar. */
data class Grupo(val medios: List<Medio>, val sugeridos: Set<Long>)

enum class TipoGrupo(val titulo: String, val explicacion: String) {
    DUPLICADOS(
        "Duplicados exactos",
        "Archivos idénticos (mismo tamaño y mismo contenido). Se propone conservar el más antiguo y borrar las copias.",
    ),
    RAFAGAS(
        "Ráfagas y fotos seguidas",
        "Fotos del mismo álbum tomadas con menos de 3 segundos de diferencia. " +
            "Se propone conservar la de más detalle (la que más pesa).",
    ),
}

object Agrupador {

    private const val BLOQUE = 64 * 1024

    /**
     * Busca archivos idénticos. Primero agrupa por tamaño, que es gratis, y solo a los que coinciden
     * les calcula una huella con el principio, el medio y el final del archivo.
     */
    suspend fun duplicados(
        resolver: ContentResolver,
        medios: List<Medio>,
        progreso: (Float) -> Unit,
    ): List<Grupo> = withContext(Dispatchers.IO) {
        val candidatos = medios.groupBy { it.tamano to it.esVideo }.values.filter { it.size > 1 }
        val total = candidatos.sumOf { it.size }.coerceAtLeast(1)
        var hechos = 0
        val grupos = ArrayList<Grupo>()
        for (mismoTamano in candidatos) {
            val porHuella = mismoTamano.groupBy { medio ->
                ensureActive()
                hechos++
                progreso(hechos.toFloat() / total)
                huella(resolver, medio)
            }
            for ((huella, iguales) in porHuella) {
                if (huella == null || iguales.size < 2) continue
                // Se conserva el original: preferiblemente fuera de WhatsApp y el más antiguo.
                val ordenados = iguales.sortedWith(compareBy({ it.esWhatsApp }, { it.fecha }))
                grupos += Grupo(ordenados, ordenados.drop(1).mapTo(HashSet()) { it.id })
            }
        }
        grupos.sortedByDescending { g -> g.medios.drop(1).sumOf { it.tamano } }
    }

    /** Fotos tomadas casi a la vez en el mismo álbum, típicamente ráfagas o repeticiones. */
    fun rafagas(medios: List<Medio>): List<Grupo> {
        val grupos = ArrayList<Grupo>()
        val fotos = medios.filter { !it.esVideo && !it.esCaptura }
            .sortedWith(compareBy({ it.album }, { it.fecha }))
        var actual = ArrayList<Medio>()
        fun cerrar() {
            if (actual.size > 1) {
                val mejor = actual.maxBy { it.tamano }
                grupos += Grupo(actual, actual.filter { it !== mejor }.mapTo(HashSet()) { it.id })
            }
            actual = ArrayList()
        }
        for (foto in fotos) {
            val anterior = actual.lastOrNull()
            if (anterior != null && (anterior.album != foto.album || foto.fecha - anterior.fecha > 3000)) {
                cerrar()
            }
            actual += foto
        }
        cerrar()
        return grupos.sortedByDescending { it.medios.first().fecha }
    }

    private fun huella(resolver: ContentResolver, medio: Medio): String? = try {
        resolver.openFileDescriptor(medio.uri, "r")?.use { pfd ->
            FileInputStream(pfd.fileDescriptor).channel.use { canal ->
                val digest = MessageDigest.getInstance("MD5")
                val tamano = canal.size()
                val buffer = ByteBuffer.allocate(BLOQUE)
                for (posicion in listOf(0L, tamano / 2, (tamano - BLOQUE).coerceAtLeast(0))) {
                    buffer.clear()
                    canal.read(buffer, posicion)
                    buffer.flip()
                    digest.update(buffer)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        }
    } catch (_: Exception) {
        null
    }
}
