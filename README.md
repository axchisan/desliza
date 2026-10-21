# Desliza

Limpia la galería del celular deslizando fotos y videos como en Tinder: **izquierda para borrar,
derecha para conservar**. Sin anuncios, sin cuentas y sin permiso de internet: nada sale del
teléfono.

Hecha para Android 11 o superior (probada con Android 15, la versión del Honor 400 Smart).

## Descargar

En [Releases](../../releases/latest) está el `Desliza-x.y.z.apk`. Al abrirlo en el teléfono, Android
pide permitir la instalación desde el navegador o el gestor de archivos; es normal en apps que no
vienen de Play Store.

## Cómo funciona

| Gesto | Qué hace |
|---|---|
| ← deslizar a la izquierda | La manda a la papelera de la app |
| → deslizar a la derecha | La conserva y no vuelve a salir |
| ↑ deslizar hacia arriba | La salta para decidir otro día |
| Tocar | La abre en grande (zoom con pellizco o doble toque; los videos suenan) |
| ↶ Deshacer | Vuelve a la anterior, sin límite |

Nada se borra mientras deslizas. Lo marcado espera en la **papelera de la app**, donde puedes
rescatar lo que quieras, y se borra todo de una vez al vaciarla.

### Formas de limpiar

- **Toda la galería**, en el orden que elijas: más antiguas, más recientes, más pesadas o al azar.
- **Por mes** o **por álbum**, con el progreso de cada uno.
- **Limpieza rápida**: capturas de pantalla, WhatsApp, descargas, videos más pesados y lo que más
  ocupa.
- **Duplicados exactos**: encuentra copias idénticas y propone conservar el original.
- **Ráfagas**: fotos tomadas con menos de 3 segundos de diferencia; propone quedarse con la de más
  detalle.
- **Cuadrícula**: en cualquier mes o álbum, elegir muchas a la vez (o «Todas») y mandarlas a la
  papelera o conservarlas de un golpe.

### Tema claro u oscuro

En **Ajustes › Apariencia**: como el teléfono, claro u oscuro.

### Borrar sin confirmar cada vez

Android pide confirmación cada vez que una app borra fotos. En **Ajustes › Borrar sin
confirmaciones** se abre el permiso especial **«Gestión de multimedia»**; con él activado, vaciar la
papelera no pregunta nada.

También se puede elegir **usar la papelera del sistema**: Android guarda los archivos 30 días antes
de borrarlos (el espacio no se libera hasta entonces).

## Compilar

Requiere JDK 17 y el SDK de Android (API 37).

```sh
./gradlew assembleRelease
```

El APK queda en `app/build/outputs/apk/release/`. La firma de publicación se lee de
`firma.properties` (fuera del repositorio):

```properties
storeFile=/ruta/a/desliza.jks
storePassword=…
keyAlias=desliza
keyPassword=…
```

Sin ese archivo se firma con la clave de depuración.

## Estructura

```
app/src/main/java/com/axchisan/desliza/
├── datos/
│   ├── Medio.kt              Foto o video, y las colecciones (mes, álbum, capturas…)
│   ├── RepositorioMedios.kt  Lectura de MediaStore y peticiones de borrado
│   ├── Agrupador.kt          Duplicados (tamaño + huella) y ráfagas
│   └── Almacen.kt            Ajustes, conservadas y papelera, guardados en el teléfono
└── ui/
    ├── ModeloApp.kt          Estado, sesiones, deshacer y borrado por lotes
    ├── PantallaSesion.kt     Tarjetas deslizables y cuadrícula
    ├── PantallaInicio.kt     Resumen y formas de limpiar
    ├── PantallaPapelera.kt   Revisión final antes de borrar
    ├── PantallaGrupos.kt     Duplicados y ráfagas
    ├── PantallaAjustes.kt
    └── Multimedia.kt         Reproductor de video y visor con zoom
```

Kotlin, Jetpack Compose, Coil 3 para las miniaturas y Media3 para los videos.
