package com.axchisan.desliza.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axchisan.desliza.datos.Medio
import com.axchisan.desliza.datos.TipoGrupo

/**
 * Duplicados y ráfagas: cada grupo viene con una propuesta de qué borrar (en rojo). Tocar una
 * miniatura cambia la propuesta; el botón de abajo manda lo marcado a la papelera.
 */
@Composable
fun PantallaGrupos(modelo: ModeloApp, tipo: TipoGrupo) {
    val grupos = modelo.grupos
    val marcados = modelo.marcadosGrupos
    var enVisor by remember { mutableStateOf<Medio?>(null) }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Encabezado(
            tipo.titulo,
            grupos?.let { "${numero(it.size)} grupos encontrados" },
            modelo::volver,
        )
        when {
            grupos == null -> Buscando(tipo, modelo.progresoGrupos)
            grupos.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No hay nada que limpiar aquí 🎉", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            else -> {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Text(
                            tipo.explicacion + " Toca una foto para cambiar la propuesta; mantén pulsado para verla.",
                            color = Colores.textoSuave,
                            fontSize = 13.sp,
                        )
                    }
                    items(grupos, key = { g -> g.medios.first().id }) { grupo ->
                        Column {
                            Text(
                                "${fecha(grupo.medios.first().fecha)} · ${grupo.medios.size} archivos",
                                color = Colores.textoSuave,
                                fontSize = 12.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(grupo.medios, key = { it.id }) { medio ->
                                    Box(Modifier.width(120.dp)) {
                                        Celda(
                                            medio,
                                            elegida = false,
                                            pendiente = medio.id in marcados,
                                            conservada = medio.id !in marcados,
                                            alTocar = {
                                                modelo.marcadosGrupos =
                                                    if (medio.id in marcados) marcados - medio.id else marcados + medio.id
                                            },
                                            alMantener = { enVisor = medio },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                val porBorrar = grupos.flatMap { it.medios }.filter { it.id in marcados }
                Button(
                    onClick = modelo::enviarMarcadosGrupos,
                    enabled = porBorrar.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Colores.borrar, contentColor = Color.White),
                ) {
                    Text(
                        "Enviar ${numero(porBorrar.size)} a la papelera · ${bytes(porBorrar.sumOf { it.tamano })}",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
    enVisor?.let { Visor(it) { enVisor = null } }
}

@Composable
private fun Buscando(tipo: TipoGrupo, progreso: Float) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (tipo == TipoGrupo.DUPLICADOS && progreso > 0f) {
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = Colores.ambar,
                trackColor = Colores.superficie,
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(12.dp))
            Text("Comparando archivos… ${(progreso * 100).toInt()} %", color = Colores.textoSuave)
        } else {
            CircularProgressIndicator(color = Colores.ambar)
            Spacer(Modifier.height(12.dp))
            Text("Buscando…", color = Colores.textoSuave, textAlign = TextAlign.Center)
        }
    }
}

