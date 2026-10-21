package com.axchisan.desliza.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import com.axchisan.desliza.datos.Medio
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun PantallaSesion(modelo: ModeloApp) {
    val sesion = modelo.sesion ?: return
    var enVisor by remember { mutableStateOf<Medio?>(null) }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = modelo::volver) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
            Column(Modifier.weight(1f)) {
                Text(sesion.fuente.titulo, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val posicion = (sesion.posicion + 1).coerceAtMost(sesion.elementos.size)
                Text(
                    "${numero(posicion)} de ${numero(sesion.elementos.size)}",
                    color = Colores.textoSuave,
                    fontSize = 12.sp,
                )
            }
            IconButton(onClick = { sesion.enCuadricula = !sesion.enCuadricula }) {
                Icon(
                    if (sesion.enCuadricula) Icons.Rounded.ViewCarousel else Icons.Rounded.GridView,
                    if (sesion.enCuadricula) "Ver como tarjetas" else "Ver en cuadrícula",
                )
            }
            BotonPapelera(modelo)
        }
        if (sesion.enCuadricula) {
            Cuadricula(modelo, sesion) { enVisor = it }
        } else {
            Deslizar(modelo, sesion) { enVisor = it }
        }
    }
    enVisor?.let { Visor(it) { enVisor = null } }
}

@Composable
private fun BotonPapelera(modelo: ModeloApp) {
    val cantidad = modelo.pendientes.size
    Box(
        Modifier.padding(end = 8.dp).clip(CircleShape)
            .background(if (cantidad > 0) Colores.borrar.copy(alpha = 0.16f) else Color.Transparent)
            .clickable { modelo.ir(Pantalla.Papelera) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Delete, "Papelera", tint = if (cantidad > 0) Colores.borrar else Colores.textoSuave, modifier = Modifier.size(20.dp))
            if (cantidad > 0) {
                Spacer(Modifier.width(6.dp))
                Text(numero(cantidad), color = Colores.borrar, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ------------------------------------------------------------------------------------ tarjetas

private enum class Accion { BORRAR, CONSERVAR, SALTAR }

private class EstadoTarjeta {
    val x = Animatable(0f)
    val y = Animatable(0f)
    var ocupada = false

    suspend fun mover(dx: Float, dy: Float) = coroutineScope {
        launch { x.animateTo(dx, tween(200)) }
        launch { y.animateTo(dy, tween(200)) }
    }

    suspend fun volver() = coroutineScope {
        launch { x.animateTo(0f, spring(dampingRatio = 0.65f)) }
        launch { y.animateTo(0f, spring(dampingRatio = 0.65f)) }
    }
}

@Composable
private fun ColumnScope.Deslizar(modelo: ModeloApp, sesion: Sesion, alVer: (Medio) -> Unit) {
    val actual = sesion.actual
    if (actual == null) {
        FinSesion(modelo, sesion)
        return
    }
    val siguiente = remember(sesion.posicion, sesion.decididos) {
        (sesion.posicion + 1 until sesion.elementos.size).asSequence()
            .map { sesion.elementos[it] }
            .firstOrNull { it.id !in sesion.decididos && it.id !in modelo.pendientes }
    }
    val estado = remember(actual.id) { EstadoTarjeta() }
    val alcance = rememberCoroutineScope()
    val haptica = LocalHapticFeedback.current
    val reproductor = rememberReproductor()
    var conSonido by remember { mutableStateOf(false) }

    LaunchedEffect(actual.id) {
        if (actual.esVideo) {
            reproductor.setMediaItem(MediaItem.fromUri(actual.uri))
            reproductor.prepare()
            reproductor.playWhenReady = true
        } else {
            reproductor.stop()
            reproductor.clearMediaItems()
        }
    }
    LaunchedEffect(conSonido) { reproductor.volume = if (conSonido) 1f else 0f }

    LinearProgressIndicator(
        progress = { sesion.posicion.toFloat() / sesion.elementos.size },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(3.dp).clip(CircleShape),
        color = Colores.ambar,
        trackColor = Colores.superficie,
        drawStopIndicator = {},
    )

    var area by remember { mutableStateOf(IntSize(1080, 1600)) }
    val ancho = area.width.toFloat()
    val alto = area.height.toFloat()
    val umbral = ancho * 0.28f

    fun ejecutar(accion: Accion) {
        if (estado.ocupada) return
        estado.ocupada = true
        alcance.launch {
            when (accion) {
                Accion.BORRAR -> estado.mover(-ancho * 1.4f, estado.y.value + alto * 0.1f)
                Accion.CONSERVAR -> estado.mover(ancho * 1.4f, estado.y.value + alto * 0.1f)
                Accion.SALTAR -> estado.mover(estado.x.value, -alto * 1.2f)
            }
            when (accion) {
                Accion.BORRAR -> modelo.decidir(borrar = true)
                Accion.CONSERVAR -> modelo.decidir(borrar = false)
                Accion.SALTAR -> modelo.saltar()
            }
        }
    }

    Box(Modifier.fillMaxWidth().weight(1f).padding(16.dp).onSizeChanged { area = it }) {
        val progreso = (abs(estado.x.value) / umbral).coerceIn(0f, 1f)
        if (siguiente != null) {
            key(siguiente.id) {
                ContenidoTarjeta(
                    siguiente,
                    reproductor = null,
                    conSonido = false,
                    alAlternarSonido = {},
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        val escala = 0.92f + 0.08f * progreso
                        scaleX = escala
                        scaleY = escala
                        alpha = 0.5f + 0.5f * progreso
                    },
                )
            }
        }

        key(actual.id) {
            var arrastre by remember { mutableStateOf(Offset.Zero) }
            var pasoUmbral by remember { mutableStateOf(false) }
            val rastreador = remember { VelocityTracker() }
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(area) {
                        detectDragGestures(
                            onDragStart = {
                                arrastre = Offset.Zero
                                rastreador.resetTracking()
                            },
                            onDragEnd = {
                                val v = rastreador.calculateVelocity()
                                val x = arrastre.x
                                val y = arrastre.y
                                when {
                                    x > umbral || (v.x > 1600f && x > 40f) -> ejecutar(Accion.CONSERVAR)
                                    x < -umbral || (v.x < -1600f && x < -40f) -> ejecutar(Accion.BORRAR)
                                    y < -alto * 0.22f && abs(x) < umbral -> ejecutar(Accion.SALTAR)
                                    else -> alcance.launch { estado.volver() }
                                }
                            },
                            onDragCancel = { alcance.launch { estado.volver() } },
                        ) { cambio, delta ->
                            if (estado.ocupada) return@detectDragGestures
                            cambio.consume()
                            arrastre += delta
                            rastreador.addPosition(cambio.uptimeMillis, arrastre)
                            val pasa = abs(arrastre.x) > umbral
                            if (pasa != pasoUmbral) {
                                pasoUmbral = pasa
                                if (pasa) haptica.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            }
                            alcance.launch {
                                estado.x.snapTo(arrastre.x)
                                estado.y.snapTo(arrastre.y)
                            }
                        }
                    }
                    .pointerInput(Unit) { detectTapGestures(onTap = { alVer(actual) }) }
                    .graphicsLayer {
                        translationX = estado.x.value
                        translationY = estado.y.value
                        rotationZ = estado.x.value / ancho * 18f
                    },
            ) {
                ContenidoTarjeta(
                    actual,
                    reproductor = reproductor,
                    conSonido = conSonido,
                    alAlternarSonido = { conSonido = !conSonido },
                    modifier = Modifier.fillMaxSize(),
                )
                val x = estado.x.value
                Sello(
                    "CONSERVAR", Colores.conservar, -12f,
                    Modifier.align(Alignment.TopStart).padding(24.dp)
                        .graphicsLayer { alpha = (x / umbral).coerceIn(0f, 1f) },
                )
                Sello(
                    "BORRAR", Colores.borrar, 12f,
                    Modifier.align(Alignment.TopEnd).padding(24.dp)
                        .graphicsLayer { alpha = (-x / umbral).coerceIn(0f, 1f) },
                )
            }
        }

        if (!modelo.guiaVista) Guia { modelo.marcarGuiaVista() }
    }

    Row(
        Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotonRedondo(Icons.AutoMirrored.Rounded.Undo, "Deshacer", Colores.textoSuave, 52.dp, sesion.historial.isNotEmpty()) {
            modelo.deshacer()
        }
        BotonRedondo(Icons.Rounded.Close, "Borrar", Colores.borrar, 72.dp) { ejecutar(Accion.BORRAR) }
        BotonRedondo(Icons.Rounded.KeyboardArrowUp, "Saltar", Colores.ambar, 52.dp) { ejecutar(Accion.SALTAR) }
        BotonRedondo(Icons.Rounded.Favorite, "Conservar", Colores.conservar, 72.dp) { ejecutar(Accion.CONSERVAR) }
    }
}

@Composable
private fun ContenidoTarjeta(
    medio: Medio,
    reproductor: ExoPlayer?,
    conSonido: Boolean,
    alAlternarSonido: () -> Unit,
    modifier: Modifier,
) {
    // El marco es oscuro en ambos temas: las fotos se aprecian mejor sobre negro.
    Box(modifier.clip(RoundedCornerShape(28.dp)).background(Color(0xFF18191E))) {
        if (medio.esVideo && reproductor != null) {
            VideoSuperpuesto(medio, reproductor, Modifier.fillMaxSize())
        } else {
            AsyncImage(medio.uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                .padding(start = 20.dp, end = 12.dp, top = 40.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(fecha(medio.fecha), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    val detalles = buildList {
                        add(bytes(medio.tamano))
                        if (medio.esVideo) add(duracion(medio.duracion))
                        if (medio.ancho > 0) add("${medio.ancho}×${medio.alto}")
                        add(medio.album)
                    }
                    Text(
                        detalles.joinToString(" · "),
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (medio.esVideo && reproductor != null) {
                    IconButton(onClick = alAlternarSonido) {
                        Icon(
                            if (conSonido) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                            if (conSonido) "Silenciar" else "Activar sonido",
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Sello(texto: String, color: Color, giro: Float, modifier: Modifier) {
    Text(
        texto,
        color = color,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        letterSpacing = 2.sp,
        modifier = modifier.graphicsLayer { rotationZ = giro }
            .border(4.dp, color, RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
    )
}

@Composable
private fun BotonRedondo(
    icono: ImageVector,
    descripcion: String,
    color: Color,
    tamano: Dp,
    habilitado: Boolean = true,
    alTocar: () -> Unit,
) {
    Box(
        Modifier.size(tamano).clip(CircleShape)
            .background(Colores.superficie)
            .border(2.dp, if (habilitado) color.copy(alpha = 0.6f) else Colores.borde, CircleShape)
            .clickable(enabled = habilitado, onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icono,
            descripcion,
            tint = if (habilitado) color else Colores.borde,
            modifier = Modifier.size(tamano * 0.45f),
        )
    }
}

@Composable
private fun Guia(alCerrar: () -> Unit) {
    Column(
        Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(onClick = alCerrar)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        Text("Cómo se usa", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
        LineaGuia("←", "Desliza a la izquierda para mandarla a la papelera", Color(0xFFFF5A4E))
        LineaGuia("→", "Desliza a la derecha para conservarla", Color(0xFF3DD68C))
        LineaGuia("↑", "Hacia arriba para saltarla y decidir otro día", Color(0xFFFFB547))
        LineaGuia("◉", "Tócala para verla en grande o hacer zoom", Color.White)
        Text(
            "Nada se borra hasta que vacíes la papelera, y siempre puedes deshacer.",
            color = Color.White.copy(alpha = 0.7f),
        )
        Text("Toca para empezar", color = Color(0xFFFFB547), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LineaGuia(simbolo: String, texto: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(simbolo, color = color, fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(44.dp))
        Text(texto, fontSize = 16.sp, color = Color.White)
    }
}

@Composable
private fun FinSesion(modelo: ModeloApp, sesion: Sesion) {
    val papelera = modelo.enPapelera
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.Check, null, Modifier.size(72.dp), tint = Colores.conservar)
        Spacer(Modifier.height(16.dp))
        Text("¡Terminaste esta tanda!", fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(
            if (sesion.borradas > 0) {
                "Marcaste ${numero(sesion.borradas)} para borrar · ${bytes(sesion.bytesBorrados)}"
            } else {
                "No marcaste nada para borrar"
            },
            color = Colores.textoSuave,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        if (papelera.isNotEmpty()) {
            Button(
                onClick = { modelo.borrarDefinitivamente() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Colores.borrar, contentColor = Color.White),
            ) {
                Text(
                    "Vaciar papelera (${numero(papelera.size)} · ${bytes(papelera.sumOf { it.tamano })})",
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { modelo.ir(Pantalla.Papelera) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Revisar la papelera primero")
            }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedButton(onClick = modelo::volver, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Volver al inicio")
        }
    }
}

// ------------------------------------------------------------------------------------ cuadrícula

@Composable
private fun Cuadricula(modelo: ModeloApp, sesion: Sesion, alVer: (Medio) -> Unit) {
    var seleccion by remember(sesion) { mutableStateOf<Set<Long>>(emptySet()) }
    val existentes = remember(sesion, modelo.medios) {
        val ids = modelo.medios.mapTo(HashSet()) { it.id }
        sesion.elementos.filter { it.id in ids }
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (seleccion.isEmpty()) "Toca para elegir · mantén pulsado para ver"
                else "${numero(seleccion.size)} elegidas · ${bytes(existentes.filter { it.id in seleccion }.sumOf { it.tamano })}",
                color = Colores.textoSuave,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (seleccion.size == existentes.size) "Ninguna" else "Todas",
                color = Colores.ambar,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).clickable {
                    seleccion = if (seleccion.size == existentes.size) emptySet() else existentes.mapTo(HashSet()) { it.id }
                }.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        LazyVerticalGrid(
            GridCells.Adaptive(104.dp),
            Modifier.weight(1f),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            items(existentes, key = { it.id }) { medio ->
                Celda(
                    medio,
                    elegida = medio.id in seleccion,
                    pendiente = medio.id in modelo.pendientes,
                    conservada = medio.id in modelo.conservados,
                    alTocar = { seleccion = if (medio.id in seleccion) seleccion - medio.id else seleccion + medio.id },
                    alMantener = { alVer(medio) },
                )
            }
        }
        if (seleccion.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { modelo.marcar(seleccion, borrar = false); seleccion = emptySet() },
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Colores.superficieAlta, contentColor = Colores.conservar),
                ) { Text("Conservar", fontWeight = FontWeight.Bold) }
                Button(
                    onClick = { modelo.marcar(seleccion, borrar = true); seleccion = emptySet() },
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Colores.borrar, contentColor = Color.White),
                ) { Text("A la papelera", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun Celda(
    medio: Medio,
    elegida: Boolean,
    pendiente: Boolean,
    conservada: Boolean,
    alTocar: () -> Unit,
    alMantener: () -> Unit,
) {
    Box(
        Modifier.aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(Colores.superficie)
            .combinedClickable(onClick = alTocar, onLongClick = alMantener),
    ) {
        AsyncImage(medio.uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (pendiente) {
            Box(Modifier.fillMaxSize().background(Colores.borrar.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Delete, "En la papelera", tint = Color.White)
            }
        }
        if (medio.esVideo) {
            Row(
                Modifier.align(Alignment.BottomStart).padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(12.dp), tint = Color.White)
                Text(duracion(medio.duracion), fontSize = 10.sp, color = Color.White)
            }
        }
        Text(
            bytes(medio.tamano),
            fontSize = 10.sp,
            color = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
        if (conservada && !pendiente) {
            Icon(
                Icons.Rounded.Favorite, "Conservada",
                Modifier.align(Alignment.TopEnd).padding(5.dp).size(16.dp),
                tint = Colores.conservar,
            )
        }
        if (elegida) {
            Box(Modifier.fillMaxSize().border(3.dp, Colores.ambar, RoundedCornerShape(6.dp)).background(Colores.ambar.copy(alpha = 0.18f)))
            Box(
                Modifier.align(Alignment.TopStart).padding(5.dp).size(22.dp).clip(CircleShape).background(Colores.ambar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = Colores.sobreAmbar)
            }
        }
    }
}
