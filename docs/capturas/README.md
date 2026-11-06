# Guía de Capturas de Pantalla y Evidencias de Evaluación - Práctica 5

Esta guía detalla con precisión **qué pantalla abrir y qué elementos específicos deben verse en cada una de las 8 capturas obligatorias** (más la evidencia complementaria de Rol Administrador y RBAC), para cumplir con la rúbrica oficial de la Práctica 5.

Guarda las capturas en esta misma carpeta (`docs/capturas/`) con los nombres exactos listados a continuación:

---

### Catálogo de las 8 Evidencias Obligatorias

| # | Archivo | Escenario Evaluado | ¿Qué debe mostrar la captura exactamente? |
|:---:|:---|:---|:---|
| **01** | `01_consulta_rest_crud.png` | **1. Consulta REST (CRUD Backend)** | Pantalla de **"Gestión de Tareas"** con tareas reales devueltas por el backend Flask propio, mostrando el botón flotante `+` y opciones de editar o eliminar. |
| **02** | `02_busqueda_publica_detalle.png` | **2. Búsqueda Pública (TVMaze)** | Pantalla de **"Buscar Series"** tras realizar una búsqueda (ej. *"Breaking Bad"* o *"Stranger Things"*), mostrando la tarjeta con imagen, calificación ⭐, chips de género y el diálogo de detalle abierto. |
| **03** | `03_estado_vacio_y_error.png` | **3. Estado Vacío y Manejo de Error** | La pantalla de búsqueda mostrando el estado de **"Sin resultados"** ante una búsqueda sin coincidencias (ej. `xyzqwe999`) o el mensaje amigable de error de red sin cierre inesperado (*Zero Crashes*). |
| **04** | `04_google_auth_sesion.png` | **4. Google Sign-In y Sesión** | Pantalla de Login mostrando el botón **"Continuar con Google"**, o el diálogo modal de confirmación OAuth de Google con el correo y nombre del alumno. |
| **05** | `05_favoritos_marcadores.png` | **5. Favoritos y Persistencia** | Pantalla de **"Mis Favoritos"** listando las series que el usuario guardó (con la estrella dorada activa), mostrando el conteo total de series guardadas en la cuenta. |
| **06** | `06_recomendaciones_regla.png` | **6. Recomendaciones Verificables** | Pantalla de **"Recomendaciones"** mostrando al menos 3 sugerencias con la justificación explicable de la regla: *"💡 Coincide con tus géneros preferidos: Drama, Crime, Sci-Fi"*. |
| **07** | `07_reinicio_app_persistencia.png` | **7. Reinicio de App (Persistencia)** | La aplicación recién abierta (tras forzar cierre en el Administrador de Tareas de Android), demostrando que la sesión continúa activa y que el historial y datos locales se restauraron de inmediato. |
| **08** | `08_offline_reconexion_sync.png` | **8. Offline, Reconexión y Sincronización** | La barra superior de sincronización (`SyncBanner`): mostrando el estado en **Modo Avión** con *"X cambios pendientes en cola"*, y al reconectar a internet mostrando *"🟢 Conectado | Sincronizado con éxito"* con su estampa de fecha y hora. |
| **09** | `09_admin_auditoria_rbac.png` | **Auditoría Admin y Bloqueo 403** | Pantalla de **"Auditoría de Usuarios (Admin)"** donde el administrador inspecciona el historial de otro usuario, junto con el resultado del botón rojo que comprueba el rechazo con código **403 Forbidden** para cuentas sin permisos. |

---

### ¿Cómo tomar las capturas desde Android Studio?

1. **Captura directa del Emulador:**
   * En la barra lateral derecha del Emulador Android, haz clic en el icono de **Cámara** (*Take Screenshot*).
   * Guarda el archivo PNG en tu disco y cópialo a `docs/capturas/` con el nombre correspondiente (ej. `01_consulta_rest_crud.png`).
2. **Atajo rápido en Windows:**
   * Pulsa `Windows + Shift + S`, recorta la ventana del emulador y guárdala.
