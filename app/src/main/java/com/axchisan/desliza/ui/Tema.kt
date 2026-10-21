package com.axchisan.desliza.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class Tema(val etiqueta: String) {
    SISTEMA("Como el teléfono"),
    CLARO("Claro"),
    OSCURO("Oscuro"),
}

data class Paleta(
    val fondo: Color,
    val superficie: Color,
    val superficieAlta: Color,
    val borde: Color,
    val texto: Color,
    val textoSuave: Color,
    val borrar: Color,
    val conservar: Color,
    val ambar: Color,
    /** Texto sobre el botón ámbar. */
    val sobreAmbar: Color,
)

private val oscura = Paleta(
    fondo = Color(0xFF0F1013),
    superficie = Color(0xFF18191E),
    superficieAlta = Color(0xFF22242A),
    borde = Color(0xFF2C2F36),
    texto = Color(0xFFF4EFE6),
    textoSuave = Color(0xFF9A9CA3),
    borrar = Color(0xFFFF5A4E),
    conservar = Color(0xFF3DD68C),
    ambar = Color(0xFFFFB547),
    sobreAmbar = Color(0xFF0F1013),
)

// En claro los acentos se oscurecen un poco para que el texto de color se lea sobre blanco.
private val clara = Paleta(
    fondo = Color(0xFFF6F3EE),
    superficie = Color(0xFFFFFFFF),
    superficieAlta = Color(0xFFECE7DF),
    borde = Color(0xFFD9D3C9),
    texto = Color(0xFF17181C),
    textoSuave = Color(0xFF6B6D74),
    borrar = Color(0xFFE5392D),
    conservar = Color(0xFF14A363),
    ambar = Color(0xFFF2A02C),
    sobreAmbar = Color(0xFF17181C),
)

private val LocalPaleta = staticCompositionLocalOf { oscura }

/** Los colores del tema activo. */
object Colores {
    val fondo: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.fondo
    val superficie: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.superficie
    val superficieAlta: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.superficieAlta
    val borde: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.borde
    val texto: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.texto
    val textoSuave: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.textoSuave
    val borrar: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.borrar
    val conservar: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.conservar
    val ambar: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.ambar
    val sobreAmbar: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.sobreAmbar
}

@Composable
fun esOscuro(tema: Tema): Boolean = when (tema) {
    Tema.SISTEMA -> isSystemInDarkTheme()
    Tema.CLARO -> false
    Tema.OSCURO -> true
}

@Composable
fun TemaDesliza(oscuro: Boolean, contenido: @Composable () -> Unit) {
    val p = if (oscuro) oscura else clara
    val base = if (oscuro) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = base.copy(
            primary = p.ambar,
            onPrimary = p.sobreAmbar,
            secondary = p.conservar,
            error = p.borrar,
            background = p.fondo,
            onBackground = p.texto,
            surface = p.superficie,
            onSurface = p.texto,
            surfaceVariant = p.superficieAlta,
            onSurfaceVariant = p.textoSuave,
            surfaceContainer = p.superficie,
            surfaceContainerHigh = p.superficieAlta,
            surfaceContainerHighest = p.superficieAlta,
            outline = p.borde,
            outlineVariant = p.borde,
            inverseSurface = p.texto,
            inverseOnSurface = p.fondo,
        ),
    ) {
        // El texto suelto (fuera de una Surface) hereda este color en vez del negro por defecto.
        CompositionLocalProvider(LocalPaleta provides p, LocalContentColor provides p.texto, content = contenido)
    }
}
