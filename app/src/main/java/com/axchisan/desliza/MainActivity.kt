package com.axchisan.desliza

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.axchisan.desliza.ui.App
import com.axchisan.desliza.ui.ModeloApp

class MainActivity : ComponentActivity() {

    private val modelo: ModeloApp by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // El color de los iconos de las barras lo ajusta App según el tema elegido.
        enableEdgeToEdge()
        setContent { App(modelo) }
    }

    override fun onResume() {
        super.onResume()
        modelo.alVolverAlFrente()
    }

    override fun onStop() {
        super.onStop()
        modelo.guardarYa()
    }
}
