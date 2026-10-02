# Nexo
App Android (Kotlin + Compose) para manga, manhwa, anime y películas. Local-first: biblioteca, historial y backups viven solo en el dispositivo.
- Pestañas: Anime, Manga, Actualizaciones, Explorar, Más.
- Fuentes = manifiestos JSON (ver app/src/main/assets/ejemplos).
- Idiomas: app/src/main/res/values-XX/strings.xml (copia values/strings.xml para añadir uno).
- APK: sube a GitHub > Actions > Build APK.
Pendiente: ejecutar extensiones Mihon/Aniyomi (source-api).

## Interfaz
Estilo "cristal": barra de navegación flotante con desenfoque real del fondo (Android 12+; en versiones menores solo tinte translúcido), títulos grandes, tarjetas redondeadas y fondo ambiental con el color de acento. Ver `ui/Cristal.kt`.

## Idiomas
Incluidos: inglés (por defecto) y español. Las demás traducciones quedan para la comunidad.
Para añadir un idioma: copia `res/values/strings.xml` a `res/values-XX/` (XX = código, p. ej. `pt`, `fr`), traduce, y añade el código en `res/xml/locales_config.xml` y en la lista `idiomas` de `ui/Mas.kt`. Lo que falte cae al inglés.
Para recibir muchas traducciones: sube `values/strings.xml` a Weblate o Crowdin (gratis para código abierto).
