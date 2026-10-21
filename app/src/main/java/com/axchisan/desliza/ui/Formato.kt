package com.axchisan.desliza.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val espanol = Locale.forLanguageTag("es-CO")
private val formatoFecha = SimpleDateFormat("d 'de' MMM 'de' yyyy · HH:mm", espanol)

fun numero(n: Int): String = String.format(espanol, "%,d", n).replace(',', '.')

fun bytes(b: Long): String = when {
    b >= 1L shl 30 -> String.format(espanol, "%.1f GB", b / (1L shl 30).toDouble())
    b >= 1L shl 20 -> String.format(espanol, "%.1f MB", b / (1L shl 20).toDouble())
    b >= 1L shl 10 -> "${b shr 10} KB"
    else -> "$b B"
}.replace('.', ',')

fun fecha(ms: Long): String = synchronized(formatoFecha) { formatoFecha.format(Date(ms)) }

fun duracion(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
    else "%d:%02d".format(s / 60, s % 60)
}
