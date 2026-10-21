package com.axchisan.desliza.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axchisan.desliza.datos.Orden
import com.axchisan.desliza.datos.Tipo

@Composable
fun PantallaAjustes(modelo: ModeloApp) {
    val contexto = LocalContext.current
    var confirmarOlvido by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Encabezado("Ajustes", null, modelo::volver)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Seccion("Apariencia") {
                Opciones(Tema.entries, modelo.tema, { it.etiqueta }, modelo::cambiarTema)
            }
            Seccion("Orden de las tarjetas") {
                Opciones(Orden.entries, modelo.orden, { it.etiqueta }, modelo::cambiarOrden)
            }
            Seccion("Qué mostrar") {
                Opciones(Tipo.entries, modelo.tipo, { it.etiqueta }, modelo::cambiarTipo)
            }
            Seccion("Comportamiento") {
                Interruptor(
                    "Repasar las que ya conservé",
                    "Normalmente lo que deslizaste a la derecha no vuelve a salir.",
                    modelo.repasarConservadas,
                    modelo::cambiarRepasar,
                )
                Interruptor(
                    "Usar la papelera del sistema",
                    "En vez de borrar para siempre, Android guarda los archivos 30 días. Ojo: el espacio no se libera hasta entonces.",
                    modelo.usarPapeleraSistema,
                    modelo::cambiarPapeleraSistema,
                )
            }
            Seccion("Borrar sin confirmaciones") {
                Text(
                    if (modelo.gestionMultimedia) {
                        "✓ «Gestión de multimedia» activada: Android no pregunta al vaciar la papelera."
                    } else {
                        "Sin este permiso especial, Android muestra un cuadro de confirmación cada vez que vacías la papelera."
                    },
                    color = if (modelo.gestionMultimedia) Colores.conservar else Colores.textoSuave,
                    fontSize = 13.sp,
                )
                if (!modelo.gestionMultimedia) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Activar «Gestión de multimedia»",
                        color = Colores.ambar,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(CircleShape).clickable { abrirGestionMultimedia(contexto) }
                            .padding(vertical = 6.dp),
                    )
                }
            }
            Seccion("Tu progreso") {
                Text("${numero(modelo.archivosBorrados)} archivos borrados · ${bytes(modelo.bytesLiberados)} liberados")
                Text("${numero(modelo.conservados.size)} marcadas para conservar", color = Colores.textoSuave, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Olvidar las conservadas y empezar de cero",
                    color = Colores.borrar,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { confirmarOlvido = true }.padding(vertical = 6.dp),
                )
            }
            Text(
                "Desliza no tiene anuncios ni permiso de internet: tus fotos no salen del teléfono.\nVersión 1.0.0",
                color = Colores.textoSuave,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
    if (confirmarOlvido) {
        AlertDialog(
            onDismissRequest = { confirmarOlvido = false },
            title = { Text("¿Empezar de cero?") },
            text = { Text("Todas las fotos que conservaste volverán a aparecer para revisar. No se borra nada.") },
            confirmButton = {
                TextButton(onClick = { modelo.olvidarConservadas(); confirmarOlvido = false }) { Text("Sí, olvidar") }
            },
            dismissButton = { TextButton(onClick = { confirmarOlvido = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Colores.superficie).padding(16.dp),
    ) {
        Text(titulo, fontWeight = FontWeight.Bold, color = Colores.textoSuave, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        contenido()
    }
}

@Composable
private fun <T> Opciones(opciones: List<T>, actual: T, etiqueta: (T) -> String, alElegir: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEach { opcion ->
            val activa = opcion == actual
            Text(
                etiqueta(opcion),
                color = if (activa) Colores.sobreAmbar else Colores.texto,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.clip(CircleShape)
                    .background(if (activa) Colores.ambar else Colores.superficieAlta)
                    .clickable { alElegir(opcion) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun Interruptor(titulo: String, detalle: String, valor: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { alCambiar(!valor) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.SemiBold)
            Text(detalle, color = Colores.textoSuave, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = valor,
            onCheckedChange = alCambiar,
            colors = SwitchDefaults.colors(checkedTrackColor = Colores.ambar, checkedThumbColor = Colores.fondo),
        )
    }
}
