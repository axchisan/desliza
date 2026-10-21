package com.axchisan.desliza.ui

import android.app.Application
import android.content.IntentSender
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.axchisan.desliza.datos.Agrupador
import com.axchisan.desliza.datos.Almacen
import com.axchisan.desliza.datos.Fuente
import com.axchisan.desliza.datos.Grupo
import com.axchisan.desliza.datos.Medio
import com.axchisan.desliza.datos.Orden
import com.axchisan.desliza.datos.RepositorioMedios
import com.axchisan.desliza.datos.Tipo
import com.axchisan.desliza.datos.TipoGrupo
import com.axchisan.desliza.datos.claveMes
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Pantalla {
    data object Inicio : Pantalla
    data object Deslizar : Pantalla
    data object Papelera : Pantalla
    data object Ajustes : Pantalla
    data class Grupos(val tipo: TipoGrupo) : Pantalla
}

/** Un paso de la sesión, para poder deshacerlo. `borrar` es null si se saltó. */
data class Paso(val indice: Int, val medio: Medio, val borrar: Boolean?, val yaConservado: Boolean)

/** Una pasada por una colección. La lista se fija al empezar para que no cambie bajo el dedo. */
class Sesion(val fuente: Fuente, val elementos: List<Medio>) {
    var posicion by mutableIntStateOf(0)
    var enCuadricula by mutableStateOf(false)
    val historial = mutableStateListOf<Paso>()
    /** Lo decidido en esta sesión, para saltarlo al avanzar aunque se esté repasando. */
    var decididos by mutableStateOf<Set<Long>>(emptySet())
    var borradas by mutableIntStateOf(0)
    var bytesBorrados by mutableStateOf(0L)

    val actual: Medio? get() = elementos.getOrNull(posicion)
}

data class Bloque(val fuente: Fuente, val total: Int, val restantes: Int, val bytesRestantes: Long)

data class Resumen(
    val fotos: Int,
    val videos: Int,
    val bytes: Long,
    val restantes: Int,
    val rapidos: List<Bloque>,
    val meses: List<Bloque>,
    val albumes: List<Bloque>,
)

class ModeloApp(app: Application) : AndroidViewModel(app) {

    private val repositorio = RepositorioMedios(app)
    private val almacen = Almacen(app)

    var cargando by mutableStateOf(true)
        private set
    var medios by mutableStateOf<List<Medio>>(emptyList())
        private set
    var conservados by mutableStateOf<Set<Long>>(emptySet())
        private set
    var pendientes by mutableStateOf<Set<Long>>(emptySet())
        private set

    var orden by mutableStateOf(almacen.orden)
        private set
    var tipo by mutableStateOf(almacen.tipo)
        private set
    var repasarConservadas by mutableStateOf(almacen.repasarConservadas)
        private set
    var usarPapeleraSistema by mutableStateOf(almacen.usarPapeleraSistema)
        private set
    var bytesLiberados by mutableStateOf(almacen.bytesLiberados)
        private set
    var archivosBorrados by mutableIntStateOf(almacen.archivosBorrados)
        private set
    var tema by mutableStateOf(Tema.entries.getOrElse(almacen.tema) { Tema.SISTEMA })
        private set
    var guiaVista by mutableStateOf(almacen.guiaVista)
        private set
    var gestionMultimedia by mutableStateOf(false)
        private set

    private val pila = mutableStateListOf<Pantalla>(Pantalla.Inicio)
    val pantalla: Pantalla get() = pila.last()
    val puedeVolver: Boolean get() = pila.size > 1

    var sesion by mutableStateOf<Sesion?>(null)
        private set

    var mensaje by mutableStateOf<String?>(null)

    private var iniciado = false
    /** Hasta leer lo guardado no se escribe nada, para no pisar los archivos con listas vacías. */
    private var almacenLeido = false

    // ---------------------------------------------------------------- carga

    fun iniciar() {
        if (iniciado) return
        iniciado = true
        viewModelScope.launch {
            conservados = almacen.leerConservados()
            pendientes = almacen.leerPendientes()
            almacenLeido = true
            recargar()
        }
    }

    fun recargar() {
        gestionMultimedia = repositorio.puedeGestionarMultimedia()
        viewModelScope.launch {
            cargando = true
            val lista = repositorio.cargar()
            medios = lista
            // Lo que se borró desde otra aplicación ya no tiene sentido tenerlo en la papelera.
            val existentes = lista.mapTo(HashSet()) { it.id }
            if (pendientes.any { it !in existentes }) {
                pendientes = pendientes.filterTo(HashSet()) { it in existentes }
                programarGuardado()
            }
            cargando = false
        }
    }

    fun alVolverAlFrente() {
        gestionMultimedia = repositorio.puedeGestionarMultimedia()
    }

    val resumen by derivedStateOf { resumir() }

    val enPapelera by derivedStateOf {
        val ids = pendientes
        medios.filter { it.id in ids }.sortedByDescending { it.tamano }
    }

    private fun quedaPorRevisar(medio: Medio) =
        medio.id !in pendientes && (repasarConservadas || medio.id !in conservados)

    private fun resumir(): Resumen {
        val rapidas = buildList {
            add(Fuente.Capturas)
            add(Fuente.WhatsApp)
            if (tipo != Tipo.FOTOS) add(Fuente.VideosPesados)
            add(Fuente.Pesadas)
            add(Fuente.Descargas)
        }
        val totales = HashMap<Fuente, IntArray>()
        val bytes = HashMap<Fuente, Long>()
        var fotos = 0
        var videos = 0
        var bytesTotal = 0L
        var restantes = 0
        fun contar(fuente: Fuente, medio: Medio, queda: Boolean) {
            val t = totales.getOrPut(fuente) { IntArray(2) }
            t[0]++
            if (queda) {
                t[1]++
                bytes[fuente] = (bytes[fuente] ?: 0L) + medio.tamano
            }
        }
        for (medio in medios) {
            if (!tipo.admite(medio)) continue
            if (medio.esVideo) videos++ else fotos++
            bytesTotal += medio.tamano
            val queda = quedaPorRevisar(medio)
            if (queda) restantes++
            val clave = claveMes(medio.fecha)
            contar(Fuente.Mes(clave / 100, clave % 100), medio, queda)
            contar(Fuente.Album(medio.album), medio, queda)
            for (fuente in rapidas) if (fuente.incluye(medio)) contar(fuente, medio, queda)
        }
        fun bloque(fuente: Fuente) = totales[fuente]?.let { Bloque(fuente, it[0], it[1], bytes[fuente] ?: 0L) }
        return Resumen(
            fotos = fotos,
            videos = videos,
            bytes = bytesTotal,
            restantes = restantes,
            rapidos = rapidas.mapNotNull { bloque(it) }.filter { it.restantes > 0 },
            meses = totales.keys.filterIsInstance<Fuente.Mes>()
                .sortedByDescending { it.anio * 100 + it.mes }
                .mapNotNull { bloque(it) },
            albumes = totales.keys.filterIsInstance<Fuente.Album>()
                .mapNotNull { bloque(it) }
                .sortedByDescending { it.total },
        )
    }

    // ---------------------------------------------------------------- navegación

    fun ir(destino: Pantalla) {
        pila.add(destino)
    }

    fun volver() {
        if (pila.size > 1) pila.removeAt(pila.lastIndex)
        if (pantalla == Pantalla.Inicio) sesion = null
    }

    fun abrirSesion(fuente: Fuente, enCuadricula: Boolean = false, orden: Orden? = null) {
        val ordenEfectivo = orden ?: fuente.ordenFijo ?: this.orden
        val lista = medios.filter { fuente.incluye(it) && tipo.admite(it) && quedaPorRevisar(it) }
        val ordenada = when (ordenEfectivo) {
            Orden.ANTIGUAS -> lista.sortedBy { it.fecha }
            Orden.RECIENTES -> lista.sortedByDescending { it.fecha }
            Orden.PESADAS -> lista.sortedByDescending { it.tamano }
            Orden.ALEATORIO -> lista.shuffled()
        }
        if (ordenada.isEmpty()) {
            mensaje = "No queda nada por revisar en «${fuente.titulo}»"
            return
        }
        sesion = Sesion(fuente, ordenada).also { it.enCuadricula = enCuadricula }
        ir(Pantalla.Deslizar)
    }

    // ---------------------------------------------------------------- decisiones

    /** Decide sobre la tarjeta actual: `true` la manda a la papelera, `false` la conserva. */
    fun decidir(borrar: Boolean) {
        val s = sesion ?: return
        val medio = s.actual ?: return
        val yaConservado = medio.id in conservados
        if (borrar) {
            pendientes = pendientes + medio.id
            conservados = conservados - medio.id
            s.borradas++
            s.bytesBorrados += medio.tamano
        } else {
            conservados = conservados + medio.id
        }
        s.historial.add(Paso(s.posicion, medio, borrar, yaConservado))
        s.decididos = s.decididos + medio.id
        avanzar(s)
        programarGuardado()
    }

    fun saltar() {
        val s = sesion ?: return
        val medio = s.actual ?: return
        s.historial.add(Paso(s.posicion, medio, null, medio.id in conservados))
        avanzar(s)
    }

    fun deshacer() {
        val s = sesion ?: return
        val paso = s.historial.removeLastOrNull() ?: return
        val id = paso.medio.id
        when (paso.borrar) {
            true -> {
                pendientes = pendientes - id
                if (paso.yaConservado) conservados = conservados + id
                s.borradas--
                s.bytesBorrados -= paso.medio.tamano
            }
            false -> if (!paso.yaConservado) conservados = conservados - id
            null -> Unit
        }
        s.decididos = s.decididos - id
        s.posicion = paso.indice
        programarGuardado()
    }

    private fun avanzar(s: Sesion) {
        var i = s.posicion + 1
        while (i < s.elementos.size) {
            val id = s.elementos[i].id
            if (id !in s.decididos && id !in pendientes) break
            i++
        }
        s.posicion = i
    }

    /** Marca varios a la vez, desde la cuadrícula o los grupos. */
    fun marcar(ids: Set<Long>, borrar: Boolean) {
        if (ids.isEmpty()) return
        if (borrar) {
            pendientes = pendientes + ids
            conservados = conservados - ids
        } else {
            conservados = conservados + ids
            pendientes = pendientes - ids
        }
        sesion?.let { s ->
            s.decididos = s.decididos + ids
            if (s.actual?.id in ids) avanzar(s)
        }
        programarGuardado()
    }

    /** Saca de la papelera de la app; lo rescatado cuenta como conservado. */
    fun rescatar(ids: Set<Long>) {
        pendientes = pendientes - ids
        conservados = conservados + ids
        programarGuardado()
    }

    fun vaciarPapeleraApp() {
        pendientes = emptySet()
        programarGuardado()
    }

    // ---------------------------------------------------------------- borrado real

    var peticionBorrado by mutableStateOf<IntentSender?>(null)
        private set
    private val colaBorrado = ArrayDeque<List<Medio>>()
    private var borradosEnTanda = 0
    private var bytesEnTanda = 0L

    /**
     * Borra de verdad. Se parte en lotes porque Android pasa la lista de archivos en una sola
     * transacción y con miles de elementos podría pasarse del límite.
     */
    fun borrarDefinitivamente(lista: List<Medio> = enPapelera) {
        if (lista.isEmpty() || colaBorrado.isNotEmpty()) return
        lista.chunked(LOTE).forEach(colaBorrado::add)
        borradosEnTanda = 0
        bytesEnTanda = 0
        siguienteLote()
    }

    private fun siguienteLote() {
        val lote = colaBorrado.firstOrNull() ?: return terminarTanda()
        peticionBorrado = try {
            repositorio.peticionBorrado(lote, usarPapeleraSistema).intentSender
        } catch (e: Exception) {
            colaBorrado.clear()
            mensaje = "Android no permitió el borrado: ${e.message}"
            null
        }
    }

    fun peticionLanzada() {
        peticionBorrado = null
    }

    fun resultadoBorrado(aprobado: Boolean) {
        val lote = colaBorrado.removeFirstOrNull() ?: return
        if (!aprobado) {
            colaBorrado.clear()
            if (borradosEnTanda == 0) mensaje = "No se borró nada" else terminarTanda()
            return
        }
        val ids = lote.mapTo(HashSet()) { it.id }
        medios = medios.filter { it.id !in ids }
        pendientes = pendientes - ids
        conservados = conservados - ids
        borradosEnTanda += lote.size
        bytesEnTanda += lote.sumOf { it.tamano }
        programarGuardado()
        siguienteLote()
    }

    private fun terminarTanda() {
        if (borradosEnTanda == 0) return
        bytesLiberados += bytesEnTanda
        archivosBorrados += borradosEnTanda
        almacen.bytesLiberados = bytesLiberados
        almacen.archivosBorrados = archivosBorrados
        val verbo = if (usarPapeleraSistema) "Enviados a la papelera" else "Borrados"
        mensaje = "$verbo ${numero(borradosEnTanda)} archivos · ${bytes(bytesEnTanda)}"
        borradosEnTanda = 0
        bytesEnTanda = 0
    }

    // ---------------------------------------------------------------- grupos

    var grupos by mutableStateOf<List<Grupo>?>(null)
        private set
    var progresoGrupos by mutableStateOf(0f)
        private set
    var marcadosGrupos by mutableStateOf<Set<Long>>(emptySet())
    private var trabajoGrupos: Job? = null

    fun abrirGrupos(tipo: TipoGrupo) {
        ir(Pantalla.Grupos(tipo))
        grupos = null
        progresoGrupos = 0f
        trabajoGrupos?.cancel()
        trabajoGrupos = viewModelScope.launch {
            val candidatos = medios.filter { it.id !in pendientes }
            val resultado = when (tipo) {
                TipoGrupo.DUPLICADOS -> Agrupador.duplicados(
                    getApplication<Application>().contentResolver, candidatos,
                ) { progresoGrupos = it }
                TipoGrupo.RAFAGAS -> withContext(kotlinx.coroutines.Dispatchers.Default) {
                    Agrupador.rafagas(candidatos)
                }
            }
            marcadosGrupos = resultado.flatMapTo(HashSet()) { it.sugeridos }
            grupos = resultado
        }
    }

    fun enviarMarcadosGrupos() {
        val ids = marcadosGrupos
        marcar(ids, borrar = true)
        grupos = grupos?.mapNotNull { g ->
            val quedan = g.medios.filter { it.id !in ids }
            if (quedan.size > 1) Grupo(quedan, g.sugeridos - ids) else null
        }
        marcadosGrupos = emptySet()
    }

    // ---------------------------------------------------------------- ajustes

    fun cambiarOrden(valor: Orden) { orden = valor; almacen.orden = valor }
    fun cambiarTipo(valor: Tipo) { tipo = valor; almacen.tipo = valor }
    fun cambiarRepasar(valor: Boolean) { repasarConservadas = valor; almacen.repasarConservadas = valor }
    fun cambiarPapeleraSistema(valor: Boolean) { usarPapeleraSistema = valor; almacen.usarPapeleraSistema = valor }
    fun cambiarTema(valor: Tema) { tema = valor; almacen.tema = valor.ordinal }
    fun marcarGuiaVista() { guiaVista = true; almacen.guiaVista = true }

    fun olvidarConservadas() {
        conservados = emptySet()
        programarGuardado()
        mensaje = "Todas las fotos volverán a aparecer para revisar"
    }

    // ---------------------------------------------------------------- guardado

    private var trabajoGuardado: Job? = null

    private fun programarGuardado() {
        trabajoGuardado?.cancel()
        trabajoGuardado = viewModelScope.launch {
            delay(700)
            guardar()
        }
    }

    fun guardarYa() {
        trabajoGuardado?.cancel()
        viewModelScope.launch { withContext(NonCancellable) { guardar() } }
    }

    private suspend fun guardar() {
        if (!almacenLeido) return
        almacen.guardarConservados(conservados)
        almacen.guardarPendientes(pendientes)
    }

    private companion object {
        const val LOTE = 500
    }
}
