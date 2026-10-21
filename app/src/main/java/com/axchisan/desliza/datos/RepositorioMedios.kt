package com.axchisan.desliza.datos

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lee la galería del teléfono y prepara las peticiones de borrado. */
class RepositorioMedios(private val contexto: Context) {

    private val resolver = contexto.contentResolver
    private val uriImagenes = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    private val uriVideos = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    suspend fun cargar(): List<Medio> = withContext(Dispatchers.IO) {
        val columnas = arrayOf(
            FileColumns._ID,
            FileColumns.MEDIA_TYPE,
            FileColumns.DATE_TAKEN,
            FileColumns.DATE_MODIFIED,
            FileColumns.SIZE,
            FileColumns.WIDTH,
            FileColumns.HEIGHT,
            FileColumns.DURATION,
            FileColumns.BUCKET_DISPLAY_NAME,
            FileColumns.RELATIVE_PATH,
            FileColumns.DISPLAY_NAME,
        )
        val seleccion = "${FileColumns.MEDIA_TYPE} IN (?, ?) AND ${FileColumns.SIZE} > 0"
        val argumentos = arrayOf(
            FileColumns.MEDIA_TYPE_IMAGE.toString(),
            FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )
        val medios = ArrayList<Medio>()
        resolver.query(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
            columnas, seleccion, argumentos, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val esVideo = c.getInt(1) == FileColumns.MEDIA_TYPE_VIDEO
                val tomada = c.getLong(2)
                medios += Medio(
                    id = id,
                    uri = ContentUris.withAppendedId(if (esVideo) uriVideos else uriImagenes, id),
                    esVideo = esVideo,
                    fecha = if (tomada > 0) tomada else c.getLong(3) * 1000,
                    tamano = c.getLong(4),
                    ancho = c.getInt(5),
                    alto = c.getInt(6),
                    duracion = c.getLong(7),
                    album = c.getString(8) ?: "Sin álbum",
                    ruta = c.getString(9) ?: "",
                    nombre = c.getString(10) ?: "",
                )
            }
        }
        medios
    }

    /**
     * Petición que Android muestra al usuario para confirmar el borrado. Si la aplicación tiene el
     * acceso «Gestión de multimedia», Android la aprueba sin mostrar nada.
     */
    fun peticionBorrado(medios: List<Medio>, aPapelera: Boolean): PendingIntent {
        val uris = medios.map { it.uri }
        return if (aPapelera) {
            MediaStore.createTrashRequest(resolver, uris, true)
        } else {
            MediaStore.createDeleteRequest(resolver, uris)
        }
    }

    fun puedeGestionarMultimedia(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && MediaStore.canManageMedia(contexto)
}
