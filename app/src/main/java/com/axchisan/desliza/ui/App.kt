package com.axchisan.desliza.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

@Composable
fun App(modelo: ModeloApp) {
    val oscuro = esOscuro(modelo.tema)
    val actividad = LocalActivity.current as? ComponentActivity
    LaunchedEffect(oscuro) {
        val estilo = if (oscuro) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        }
        actividad?.enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
    }
    TemaDesliza(oscuro) {
        val contexto = LocalContext.current
        var acceso by remember { mutableStateOf(estadoAcceso(contexto)) }
        LifecycleResumeEffect(Unit) {
            acceso = estadoAcceso(contexto)
            onPauseOrDispose { }
        }
        LaunchedEffect(acceso) {
            if (acceso == Acceso.TOTAL) modelo.iniciar()
        }

        val avisos = remember { SnackbarHostState() }
        LaunchedEffect(modelo.mensaje) {
            val texto = modelo.mensaje ?: return@LaunchedEffect
            modelo.mensaje = null
            avisos.showSnackbar(texto)
        }

        val lanzadorBorrado = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { resultado -> modelo.resultadoBorrado(resultado.resultCode == Activity.RESULT_OK) }
        LaunchedEffect(modelo.peticionBorrado) {
            val peticion = modelo.peticionBorrado ?: return@LaunchedEffect
            modelo.peticionLanzada()
            lanzadorBorrado.launch(IntentSenderRequest.Builder(peticion).build())
        }

        BackHandler(enabled = modelo.puedeVolver) { modelo.volver() }

        Box(Modifier.fillMaxSize().background(Colores.fondo)) {
            if (acceso != Acceso.TOTAL) {
                PantallaPermiso(acceso) { acceso = estadoAcceso(contexto) }
            } else {
                when (val p = modelo.pantalla) {
                    Pantalla.Inicio -> PantallaInicio(modelo)
                    Pantalla.Deslizar -> PantallaSesion(modelo)
                    Pantalla.Papelera -> PantallaPapelera(modelo)
                    Pantalla.Ajustes -> PantallaAjustes(modelo)
                    is Pantalla.Grupos -> PantallaGrupos(modelo, p.tipo)
                }
            }
            SnackbarHost(
                avisos,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp),
            )
        }
    }
}

enum class Acceso { NINGUNO, PARCIAL, TOTAL }

private fun concedido(contexto: Context, permiso: String) =
    ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED

fun estadoAcceso(contexto: Context): Acceso = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> when {
        concedido(contexto, Manifest.permission.READ_MEDIA_IMAGES) &&
            concedido(contexto, Manifest.permission.READ_MEDIA_VIDEO) -> Acceso.TOTAL
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            concedido(contexto, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> Acceso.PARCIAL
        else -> Acceso.NINGUNO
    }
    concedido(contexto, Manifest.permission.READ_EXTERNAL_STORAGE) -> Acceso.TOTAL
    else -> Acceso.NINGUNO
}

private fun permisosNecesarios(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
    )
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

fun abrirAjustesDeLaApp(contexto: Context) {
    contexto.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${contexto.packageName}")),
    )
}

@Composable
private fun PantallaPermiso(acceso: Acceso, alCambiar: () -> Unit) {
    val contexto = LocalContext.current
    var intentos by remember { mutableStateOf(0) }
    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        intentos++
        alCambiar()
    }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(64.dp), tint = Colores.ambar)
        Spacer(Modifier.height(24.dp))
        Text("Desliza necesita ver tu galería", fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            if (acceso == Acceso.PARCIAL) {
                "Le diste acceso solo a algunas fotos. Para limpiar la galería completa, entra a " +
                    "Permisos › Fotos y videos y elige «Permitir siempre» o «Permitir todo»."
            } else {
                "Para mostrarte tus fotos y videos y poder borrarlos. Nada sale del teléfono: la app " +
                    "no tiene permiso de internet ni anuncios."
            },
            color = Colores.textoSuave,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (acceso == Acceso.PARCIAL || intentos >= 2) abrirAjustesDeLaApp(contexto)
                else lanzador.launch(permisosNecesarios())
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Colores.ambar, contentColor = Colores.sobreAmbar),
        ) {
            Text(
                if (acceso == Acceso.PARCIAL || intentos >= 2) "Abrir ajustes de la app" else "Dar acceso a fotos y videos",
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Abre el ajuste «Gestión de multimedia», que deja borrar sin confirmar cada lote. */
fun abrirGestionMultimedia(contexto: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA, Uri.parse("package:${contexto.packageName}"))
    } else {
        null
    }
    try {
        contexto.startActivity(intent ?: return abrirAjustesDeLaApp(contexto))
    } catch (_: Exception) {
        abrirAjustesDeLaApp(contexto)
    }
}
