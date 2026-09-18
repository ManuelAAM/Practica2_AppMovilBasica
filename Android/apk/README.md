# APK Precompilado de la Aplicación Android

En este directorio se aloja el archivo binario precompilado de la aplicación móvil (`app-debug.apk`), listo para  instalación y ejecución directa sin requerir compilación previa ni llaves de API externas.

---

## 📦 Archivo Binario
- **Nombre:** `app-debug.apk`
- **Ubicación:** `Android/apk/app-debug.apk`
- **Tipo:** Depuración / Debug APK firmado con llave de prueba de Android SDK.

---

## 🚀 Instalación y Ejecución Inmediata

### Opción 1: Mediante ADB (Línea de Comandos)
Con el emulador encendido o un dispositivo físico conectado con depuración USB activa:

```bash
adb install Android/apk/app-debug.apk
```

*(Si la aplicación ya estuviese instalada y deseas reinstalarla/actualizarla:)*
```bash
adb install -r Android/apk/app-debug.apk
```

### Opción 2: Arrastrar y Soltar (Drag & Drop en Emulador)
1. Abre tu emulador de Android (AVD) en Android Studio.
2. Arrastra el archivo `app-debug.apk` desde el explorador de archivos y suéltalo sobre la ventana del emulador.
3. El sistema operativo Android instalará la app automáticamente en cuestión de segundos.

---

## ⚙️ Configuración y Requisitos de Red
- **Sin Llaves Externas**: La aplicación no requiere API Keys de terceros (como Firebase, Google Maps, etc.). Funciona de forma 100% autónoma.
- **Conexión al Backend**: Por defecto, la app está preconfigurada para conectarse a `http://10.0.2.2:5000/` (la IP especial del emulador Android para acceder al `localhost` de la computadora anfitriona donde corre el contenedor Docker de Flask).
- Para pruebas con dispositivo físico en la misma red Wi-Fi, asegurarse de que el backend Flask esté escuchando en `0.0.0.0:5000` y cambiar la URL base en el código si fuera necesario.
