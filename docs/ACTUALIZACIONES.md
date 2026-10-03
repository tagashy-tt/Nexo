# Actualizaciones automáticas de Nexo

Nexo busca el último *release* de GitHub al abrirse (máx. cada 6 h) y desde Ajustes → Acerca de.
Si hay una versión más nueva muestra un aviso; al aceptar descarga el APK y abre el instalador de Android.
(Android no permite actualizar en silencio fuera de Google Play: siempre pide confirmación.)

## 1. Indica tu repositorio
Edita `app/src/main/java/com/nexo/app/core/Config.kt` → `REPO_ACTUALIZACIONES = "usuario/repositorio"`,
o escríbelo en la app: Ajustes → Avanzado → "Repositorio de actualizaciones".

## 2. Firma estable (obligatorio para que el APK nuevo se instale sobre el anterior)
Sin esto, cada build de GitHub se firma con una clave distinta y Android rechaza la actualización.

    keytool -genkey -v -keystore nexo.jks -alias nexo -keyalg RSA -keysize 2048 -validity 36500
    base64 -w0 nexo.jks        # copia el resultado

En GitHub → Settings → Secrets and variables → Actions, crea:
`KEYSTORE_B64` (el base64), `KEYSTORE_PASS`, `KEY_ALIAS` (nexo), `KEY_PASS`.
Guarda `nexo.jks` en un lugar seguro: si lo pierdes, no podrás publicar actualizaciones.

## 3. Publicar una versión
1. Sube `versionCode` y `versionName` en `app/build.gradle.kts` (p. ej. 5 y "0.5.0").
2. `git tag v0.5.0 && git push origin v0.5.0`
3. El workflow compila y crea el release con `nexo.apk`. Los usuarios verán el aviso.
