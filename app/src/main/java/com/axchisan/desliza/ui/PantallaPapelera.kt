package com.axchisan.desliza.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axchisan.desliza.datos.Medio

@Composable
fun PantallaPapelera(modelo: ModeloApp) {
    val contexto = LocalContext.current
    val papelera = modelo.enPapelera
    var seleccion by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var enVisor by remember { mutableStateOf<Medio?>(null) }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Encabezado(
            "Papelera",
            if (papelera.isEmpty()) null else "${numero(papelera.size)} archivos · ${bytes(papelera.sumOf { it.tamano })}",
            modelo::volver,
        ) {
            if (seleccion.isNotEmpty()) {
                TextButton(onClick = { modelo.rescatar(seleccion); seleccion = emptySet() }) {
                    Text("Rescatar ${numero(seleccion.size)}", color = Colores.conservar, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (papelera.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(64.dp), tint = Colores.textoSuave)
                Spacer(Modifier.height(12.dp))
                Text("La papelera está vacía", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Lo que deslices a la izquierda aparece aquí antes de borrarse.",
                    color = Colores.textoSuave,
                    textAlign = TextAlign.Center,
                )
            }
            return@Column
        }
        Text(
            "Toca las que quieras salvar · mantén pulsado para verlas en grande",
            color = Colores.textoSuave,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyVerticalGrid(
            GridCells.Adaptive(104.dp),
            Modifier.weight(1f),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            items(papelera, key = { it.id }) { medio ->
                Celda(
                    medio,
                    elegida = medio.id in seleccion,
                    pendiente = false,
                    conservada = false,
                    alTocar = { seleccion = if (medio.id in seleccion) seleccion - medio.id else seleccion + medio.id },
                    alMantener = { enVisor = medio },
                )
            }
        }
        Column(Modifier.padding(16.dp)) {
            Text(
                when {
                    modelo.usarPapeleraSistema ->
                        "Irán a la papelera del sistema: se recuperan durante 30 días y luego Android las borra."
                    modelo.gestionMultimedia -> "Se borrarán para siempre sin más preguntas."
                    else -> "Android te pedirá confirmar una vez. Activa «Gestión de multimedia» en Ajustes para saltarte ese paso."
                },
                color = Colores.textoSuave,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { modelo.borrarDefinitivamente() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Colores.borrar, contentColor = Color.White),
            ) {
                Text(
                    "${if (modelo.usarPapeleraSistema) "Enviar" else "Borrar"} ${numero(papelera.size)} · ${bytes(papelera.sumOf { it.tamano })}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            if (!modelo.gestionMultimedia && !modelo.usarPapeleraSistema) {
                TextButton(onClick = { abrirGestionMultimedia(contexto) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Activar «Gestión de multimedia»", color = Colores.ambar)
                }
            }
        }
    }
    enVisor?.let { Visor(it) { enVisor = null } }
}

