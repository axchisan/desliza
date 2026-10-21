package com.axchisan.desliza.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.axchisan.desliza.datos.Medio

/** Un reproductor que se pausa al salir de la app y se libera al dejar la pantalla. */
@Composable
fun rememberReproductor(): ExoPlayer {
    val contexto = LocalContext.current
    val reproductor = remember {
        ExoPlayer.Builder(contexto).build().apply { repeatMode = Player.REPEAT_MODE_ONE }
    }
    DisposableEffect(reproductor) { onDispose { reproductor.release() } }
    LifecycleResumeEffect(reproductor) {
        if (reproductor.mediaItemCount > 0) reproductor.play()
        onPauseOrDispose { reproductor.pause() }
    }
    return reproductor
}

/**
 * Video sobre su miniatura: la miniatura se ve hasta que llega el primer cuadro, así la tarjeta
 * nunca aparece en negro. Se usa TextureView porque la tarjeta gira y se desplaza.
 */
@Composable
fun VideoSuperpuesto(medio: Medio, reproductor: ExoPlayer, modifier: Modifier) {
    val presentacion = rememberPresentationState(reproductor)
    Box(modifier, contentAlignment = Alignment.Center) {
        PlayerSurface(
            player = reproductor,
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            modifier = Modifier.resizeWithContentScale(ContentScale.Fit, presentacion.videoSizeDp),
        )
        if (presentacion.coverSurface) {
            AsyncImage(medio.uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

/** Pantalla completa: fotos con zoom (pellizcar o doble toque) y videos con sonido. */
@Composable
fun Visor(medio: Medio, alCerrar: () -> Unit) {
    BackHandler(onBack = alCerrar)
    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { detectTapGestures { } },
    ) {
        if (medio.esVideo) {
            VideoVisor(medio)
        } else {
            FotoConZoom(medio)
        }
        IconButton(
            onClick = alCerrar,
            modifier = Modifier.statusBarsPadding().padding(12.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
        ) {
            Icon(Icons.Rounded.Close, "Cerrar", tint = Color.White)
        }
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .navigationBarsPadding().padding(16.dp),
        ) {
            Text(medio.nombre, color = Color.White, fontSize = 14.sp)
            Text(
                "${fecha(medio.fecha)} · ${bytes(medio.tamano)} · ${medio.ruta}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun FotoConZoom(medio: Medio) {
    val contexto = LocalContext.current
    var escala by remember { mutableFloatStateOf(1f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }
    AsyncImage(
        // Más resolución que la tarjeta para que el zoom no se vea borroso, con un tope para no
        // reventar la memoria con fotos de 50 MP.
        model = ImageRequest.Builder(contexto).data(medio.uri).size(2560).build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, paneo, zoom, _ ->
                    escala = (escala * zoom).coerceIn(1f, 6f)
                    desplazamiento = if (escala == 1f) Offset.Zero else desplazamiento + paneo
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (escala > 1f) {
                        escala = 1f
                        desplazamiento = Offset.Zero
                    } else {
                        escala = 2.5f
                    }
                })
            }
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
                translationX = desplazamiento.x
                translationY = desplazamiento.y
            },
    )
}

@Composable
private fun VideoVisor(medio: Medio) {
    val reproductor = rememberReproductor()
    var reproduciendo by remember { mutableStateOf(true) }
    LaunchedEffect(medio.id) {
        reproductor.setMediaItem(MediaItem.fromUri(medio.uri))
        reproductor.prepare()
        reproductor.playWhenReady = true
    }
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures {
                reproduciendo = !reproduciendo
                reproductor.playWhenReady = reproduciendo
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        VideoSuperpuesto(medio, reproductor, Modifier.fillMaxSize())
        if (!reproduciendo) {
            Icon(
                Icons.Rounded.PlayArrow, "Reproducir",
                Modifier.size(72.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).padding(12.dp),
                tint = Color.White,
            )
        }
    }
}

