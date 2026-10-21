package com.axchisan.desliza.datos

import android.net.Uri

/** Una foto o un video de la galería, tal como lo describe MediaStore. */
data class Medio(
    val id: Long,
    val uri: Uri,
    val esVideo: Boolean,
    /** Fecha de captura en milisegundos; si el archivo no la trae, la de modificación. */
    val fecha: Long,
    val tamano: Long,
    val ancho: Int,
    val alto: Int,
    /** Duración en milisegundos; cero en las fotos. */
    val duracion: Long,
    val album: String,
    /** Carpeta relativa al almacenamiento, p. ej. `DCIM/Camera/`. */
    val ruta: String,
    val nombre: String,
) {
    val esCaptura: Boolean
        get() = ruta.contains("screenshot", ignoreCase = true) ||
            ruta.contains("captura", ignoreCase = true) ||
            ruta.contains("screenrecord", ignoreCase = true) ||
            album.contains("screenshot", ignoreCase = true) ||
            album.contains("captura", ignoreCase = true)

    val esWhatsApp: Boolean
        get() = ruta.contains("whatsapp", ignoreCase = true)

    val esDescarga: Boolean
        get() = ruta.startsWith("Download", ignoreCase = true)
}

/** Las colecciones de las que puede salir una sesión de limpieza. */
sealed interface Fuente {
    val titulo: String

    data object Todo : Fuente {
        override val titulo = "Toda la galería"
    }

    data object Capturas : Fuente {
        override val titulo = "Capturas"
    }

    data object WhatsApp : Fuente {
        override val titulo = "WhatsApp"
    }

    data object Descargas : Fuente {
        override val titulo = "Descargas"
    }

    data object VideosPesados : Fuente {
        override val titulo = "Videos pesados"
    }

    data object Pesadas : Fuente {
        override val titulo = "Lo que más ocupa"
    }

    data class Mes(val anio: Int, val mes: Int) : Fuente {
        override val titulo get() = "${nombreMes(mes).replaceFirstChar { it.uppercase() }} $anio"
    }

    data class Album(val nombre: String) : Fuente {
        override val titulo get() = nombre
    }

    fun incluye(medio: Medio): Boolean = when (this) {
        Todo, Pesadas -> true
        Capturas -> medio.esCaptura
        WhatsApp -> medio.esWhatsApp
        Descargas -> medio.esDescarga
        VideosPesados -> medio.esVideo
        is Mes -> claveMes(medio.fecha) == anio * 100 + mes
        is Album -> medio.album == nombre
    }

    /** Las fuentes pensadas para liberar espacio se recorren siempre de mayor a menor tamaño. */
    val ordenFijo: Orden?
        get() = if (this == VideosPesados || this == Pesadas) Orden.PESADAS else null
}

enum class Orden(val etiqueta: String) {
    ANTIGUAS("Más antiguas"),
    RECIENTES("Más recientes"),
    PESADAS("Más pesadas"),
    ALEATORIO("Aleatorio"),
}

enum class Tipo(val etiqueta: String) {
    TODO("Fotos y videos"),
    FOTOS("Solo fotos"),
    VIDEOS("Solo videos");

    fun admite(medio: Medio) = when (this) {
        TODO -> true
        FOTOS -> !medio.esVideo
        VIDEOS -> medio.esVideo
    }
}

private val calendario = java.util.Calendar.getInstance()

/** `aaaamm` de una fecha en milisegundos, en la zona horaria del teléfono. */
fun claveMes(fecha: Long): Int = synchronized(calendario) {
    calendario.timeInMillis = fecha
    calendario.get(java.util.Calendar.YEAR) * 100 + calendario.get(java.util.Calendar.MONTH) + 1
}

fun nombreMes(mes: Int): String = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)[mes - 1]
