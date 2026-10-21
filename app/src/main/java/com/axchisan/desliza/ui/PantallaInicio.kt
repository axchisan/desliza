package com.axchisan.desliza.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BurstMode
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axchisan.desliza.datos.Fuente
import com.axchisan.desliza.datos.Orden
import com.axchisan.desliza.datos.TipoGrupo

@Composable
fun PantallaInicio(modelo: ModeloApp) {
    if (modelo.cargando && modelo.medios.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Colores.ambar)
        }
        return
    }
    val resumen = modelo.resumen
    var verAlbumes by rememberSaveable { mutableStateOf(false) }
    var verTodos by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    Modifier.statusBarsPadding().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Desliza", fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    IconButton(onClick = modelo::recargar) { Icon(Icons.Rounded.Refresh, "Recargar galería") }
                    IconButton(onClick = { modelo.ir(Pantalla.Ajustes) }) { Icon(Icons.Rounded.Settings, "Ajustes") }
                }
            }
            item { TarjetaResumen(modelo, resumen) }
            item {
                TarjetaEmpezar(
                    restantes = resumen.restantes,
                    orden = modelo.orden,
                    alEmpezar = { modelo.abrirSesion(Fuente.Todo) },
                    alAleatorio = { modelo.abrirSesion(Fuente.Todo, orden = Orden.ALEATORIO) },
                )
            }
            item { Titulo("Limpieza rápida") }
            item {
                val bloques = resumen.rapidos
                val atajos = bloques.map { b ->
                    Atajo(iconoDe(b.fuente), b.fuente.titulo, "${numero(b.restantes)} · ${bytes(b.bytesRestantes)}") {
                        modelo.abrirSesion(b.fuente)
                    }
                } + listOf(
                    Atajo(Icons.Rounded.ContentCopy, "Duplicados", "Copias idénticas") {
                        modelo.abrirGrupos(TipoGrupo.DUPLICADOS)
                    },
                    Atajo(Icons.Rounded.BurstMode, "Ráfagas", "Fotos casi iguales") {
                        modelo.abrirGrupos(TipoGrupo.RAFAGAS)
                    },
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    atajos.chunked(2).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            fila.forEach { TarjetaAtajo(it, Modifier.weight(1f)) }
                            if (fila.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pestana("Por mes", !verAlbumes) { verAlbumes = false; verTodos = false }
                    Spacer(Modifier.width(8.dp))
                    Pestana("Por álbum", verAlbumes) { verAlbumes = true; verTodos = false }
                }
            }
            val lista = if (verAlbumes) resumen.albumes else resumen.meses
            val visibles = if (verTodos) lista else lista.take(12)
            items(visibles, key = { it.fuente.toString() }) { bloque ->
                FilaBloque(
                    bloque,
                    alAbrir = { modelo.abrirSesion(bloque.fuente) },
                    alCuadricula = { modelo.abrirSesion(bloque.fuente, enCuadricula = true) },
                )
            }
            if (lista.size > visibles.size) {
                item {
                    Text(
                        "Ver los ${lista.size} ${if (verAlbumes) "álbumes" else "meses"}",
                        color = Colores.ambar,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().clickable { verTodos = true }.padding(12.dp),
                    )
                }
            }
        }
        BarraPapelera(modelo, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun TarjetaResumen(modelo: ModeloApp, resumen: Resumen) {
    val total = resumen.fotos + resumen.videos
    val revisadas = total - resumen.restantes
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Colores.superficie).padding(20.dp),
    ) {
        Text(
            "${numero(resumen.fotos)} fotos · ${numero(resumen.videos)} videos",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text("${bytes(resumen.bytes)} en total", color = Colores.textoSuave)
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else revisadas.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = Colores.conservar,
            trackColor = Colores.superficieAlta,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Text("${numero(revisadas)} revisadas", color = Colores.textoSuave, fontSize = 13.sp, modifier = Modifier.weight(1f))
            if (modelo.bytesLiberados > 0) {
                Text("${bytes(modelo.bytesLiberados)} liberados", color = Colores.conservar, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TarjetaEmpezar(restantes: Int, orden: Orden, alEmpezar: () -> Unit, alAleatorio: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(alturaEmpezar), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Colores.ambar)
                .clickable(onClick = alEmpezar).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Empezar a deslizar", color = Colores.sobreAmbar, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text(
                    "${numero(restantes)} por revisar · ${orden.etiqueta.lowercase()}",
                    color = Colores.sobreAmbar.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Colores.sobreAmbar)
        }
        Box(
            Modifier.size(alturaEmpezar).clip(RoundedCornerShape(20.dp)).background(Colores.superficie)
                .clickable(onClick = alAleatorio),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Shuffle, "Al azar", tint = Colores.ambar)
        }
    }
}

private val alturaEmpezar = 84.dp

private data class Atajo(val icono: ImageVector, val titulo: String, val detalle: String, val alTocar: () -> Unit)

private fun iconoDe(fuente: Fuente): ImageVector = when (fuente) {
    Fuente.Capturas -> Icons.Rounded.Screenshot
    Fuente.WhatsApp -> Icons.AutoMirrored.Rounded.Chat
    Fuente.VideosPesados -> Icons.Rounded.VideoLibrary
    Fuente.Descargas -> Icons.Rounded.Download
    else -> Icons.Rounded.Storage
}

@Composable
private fun TarjetaAtajo(atajo: Atajo, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Colores.superficie)
            .clickable(onClick = atajo.alTocar).padding(16.dp),
    ) {
        Icon(atajo.icono, null, tint = Colores.ambar)
        Spacer(Modifier.height(10.dp))
        Text(atajo.titulo, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(atajo.detalle, color = Colores.textoSuave, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Colores.textoSuave, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun Pestana(texto: String, activa: Boolean, alTocar: () -> Unit) {
    Text(
        texto,
        fontWeight = FontWeight.SemiBold,
        color = if (activa) Colores.fondo else Colores.texto,
        modifier = Modifier.clip(CircleShape)
            .background(if (activa) Colores.texto else Colores.superficie)
            .clickable(onClick = alTocar)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun FilaBloque(bloque: Bloque, alAbrir: () -> Unit, alCuadricula: () -> Unit) {
    val hecho = bloque.restantes == 0
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Colores.superficie)
            .clickable(enabled = !hecho, onClick = alAbrir).padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                bloque.fuente.titulo,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (hecho) Colores.textoSuave else Colores.texto,
            )
            Text(
                if (hecho) "Revisado ✓" else "${numero(bloque.restantes)} de ${numero(bloque.total)} por revisar · ${bytes(bloque.bytesRestantes)}",
                color = if (hecho) Colores.conservar else Colores.textoSuave,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { 1f - bloque.restantes.toFloat() / bloque.total },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = Colores.conservar,
                trackColor = Colores.superficieAlta,
                drawStopIndicator = {},
            )
        }
        IconButton(onClick = alCuadricula, enabled = !hecho) {
            Icon(Icons.Rounded.GridView, "Ver en cuadrícula", tint = if (hecho) Colores.borde else Colores.textoSuave)
        }
    }
}

/** Siempre visible mientras haya algo en la papelera, para no olvidar el último paso. */
@Composable
fun BarraPapelera(modelo: ModeloApp, modifier: Modifier = Modifier) {
    val papelera = modelo.enPapelera
    if (papelera.isEmpty()) return
    Row(
        modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)).background(Colores.superficieAlta)
            .border(1.dp, Colores.borrar.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clickable { modelo.ir(Pantalla.Papelera) }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Delete, null, tint = Colores.borrar)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${numero(papelera.size)} en la papelera", fontWeight = FontWeight.SemiBold)
            Text("${bytes(papelera.sumOf { it.tamano })} por liberar", color = Colores.textoSuave, fontSize = 12.sp)
        }
        Text("Revisar", color = Colores.borrar, fontWeight = FontWeight.Bold)
    }
}
