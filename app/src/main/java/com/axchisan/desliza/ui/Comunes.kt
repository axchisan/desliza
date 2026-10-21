package com.axchisan.desliza.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Encabezado(titulo: String, subtitulo: String?, alVolver: () -> Unit, acciones: @Composable () -> Unit = {}) {
    Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (subtitulo != null) Text(subtitulo, color = Colores.textoSuave, fontSize = 12.sp)
        }
        acciones()
    }
}
