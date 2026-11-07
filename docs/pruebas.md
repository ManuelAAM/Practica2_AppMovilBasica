# Matriz de Pruebas de Calidad (QA) - Práctica 5

## Ficha de Información de la Práctica
- **Institución:** Instituto Politécnico Nacional (IPN)
- **Unidad Académica:** Escuela Superior de Cómputo (ESCOM)
- **Materia:** Desarrollo de Aplicaciones Móviles Nativas
- **Profesor:** M. en C. Gabriel Hurtado Avilés
- **Grupo:** 7CV4 | **Ciclo Escolar:** 2027-1
- **Alumno:** **Aragón Martínez Manuel Alejandro** (Boleta: **2023630411**)
- **Tecnología Implementada:** **Kotlin Multiplatform (KMP)** con soporte para Android (API 24+) e iOS (iOS 15+), **Ktor Client**, persistencia local y caché offline con cola de acciones pendientes, consumo de **TVMaze API**, motor de recomendaciones determinista por frecuencias de género, autenticación OAuth con Google y control de acceso basado en roles (**RBAC** con decorador `@admin_required`).

---

## Matriz de 6 Casos de Prueba Estructurados (QA Gates)

| # | Categoría de Prueba | Módulo Evaluado | Precondición | Pasos de Ejecución | Resultado Esperado | Resultado Obtenido | Evidencia Visual | Estado |
|---|---|---|---|---|---|---|---|---|
| **CP-01** | **Ruta Feliz (Happy Path)** | Búsqueda Pública y Favoritos (TVMaze API) | Dispositivo con conexión a internet y sesión de usuario activa | 1. Ir a "Buscar Series".<br>2. Ingresar término de búsqueda (`Family Guy`).<br>3. Abrir la ficha técnica de la serie y pulsar "Guardar en Favoritos". | La API de TVMaze devuelve resultados reales (imagen, rating, géneros y sinopsis). Al marcar la estrella, la serie se guarda en la base de datos remota y en la base local con persistencia reactiva. | Los resultados se renderizan inmediatamente con chips de género y calificación; la serie queda registrada en la pantalla "Mis Favoritos" con estrella dorada activa. | `capturas/02_busqueda_publica_detalle.jpg`<br>`capturas/05_favoritos_marcadores.jpg` | **EXITOSO** |
| **CP-02** | **Condición Límite / Alterna (Edge Case & Resiliencia)** | Búsqueda y Manejo de Errores | App conectada a internet | 1. Ingresar búsqueda sin coincidencias (`lolol777`).<br>2. Probar forzar desconexión o respuesta de error HTTP.<br>3. Observar la reacción de la interfaz. | La aplicación no debe cerrarse de forma inesperada (*Zero Crashes*); debe desplegar una vista de estado vacío amigable con icono y sugerencia de búsqueda alternativa. | La vista muestra *"Sin resultados para lolol777"* y ofrece el catálogo en memoria local sin cierres inesperados. | `capturas/03_estado_vacio_y_error.jpg` | **EXITOSO** |
| **CP-03** | **Regresión** | CRUD de Tareas de Práctica 2 | Usuario autenticado contra el backend REST propio (Flask) | 1. Navegar a "Tareas CRUD".<br>2. Crear una nueva tarea.<br>3. Editar su título y marcarla como completada.<br>4. Eliminar la tarea confirmando el diálogo modal. | Las 4 operaciones CRUD (GET, POST, PUT, DELETE) de la Práctica 2 deben operar íntegramente a través del nuevo módulo compartido KMP. | CRUD 100% funcional con actualización reactiva en `LazyColumn`, chips de estado y sincronización con el servidor Flask. | `capturas/01_consulta_rest_crud.jpg` | **EXITOSO** |
| **CP-04** | **Navegación y Ciclo de Vida** | Persistencia de Sesión y Reinicio | Usuario autenticado con búsquedas previas en historial y favoritos guardados | 1. Guardar series en favoritos e interactuar con el buscador.<br>2. Forzar el cierre de la aplicación desde la multitarea del sistema operativo.<br>3. Reabrir la app (incluso sin conexión). | La sesión del usuario se restaura automáticamente mediante el token JWT seguro; los favoritos, el historial y las recomendaciones cacheadas se cargan de inmediato sin solicitar login nuevamente. | Se comprueba la persistencia local de la sesión activa, los favoritos y las sugerencias previas tras el reinicio en frío del proceso. | `capturas/07_video_reinicio_app_persistencia.mp4` | **EXITOSO** |
| **CP-05** | **Sincronización Offline y Cola Pendiente** | Motor de Sincronización Local / Remoto | Dispositivo en **Modo Avión** (sin conectividad de red) | 1. Activar Modo Avión.<br>2. Realizar búsquedas locales y agregar favoritos a la cola.<br>3. Desactivar Modo Avión y pulsar "Sincronizar". | En modo offline la acción se almacena en la cola local (`offline_queue`). Al recuperar conexión, se envía en bloque a `POST /sync/favorites`, actualizando la estampa de última sincronización y evitando registros duplicados (idempotencia). | La barra superior notifica los cambios en cola y confirma *"Sincronizado con éxito"* actualizando la estampa temporal sin duplicar favoritos en el servidor. | `capturas/08_offline_reconexion_sync.jpg`<br>`capturas/video_funcionamiento_offline_cache.mp4` | **EXITOSO** |
| **CP-06** | **Seguridad y Control de Acceso (RBAC)** | Auditoría Administrativa y Google OAuth | Cuenta de usuario estándar (`role: user`) vs Administrador (`role: admin`) | 1. Iniciar sesión como `admin` y consultar el historial y favoritos de otro usuario.<br>2. Ejecutar la prueba de verificación de seguridad 403 Forbidden (o invocar endpoint sin rol admin).<br>3. Verificar el bloqueo por el decorador `@admin_required`. | El backend debe conceder acceso 200 OK al Administrador; mientras que peticiones no autorizadas deben ser bloqueadas con código HTTP **403 Forbidden** por el decorador `@admin_required`. | El administrador inspecciona los datos de cualquier cuenta; las peticiones no autorizadas son rechazadas con 403 Forbidden por la API y la app confirma el bloqueo con una insignia de seguridad verde. | `capturas/04_google_auth_sesion.jpg`<br>`capturas/09_admin_auditoria_rbac.jpg`<br>`capturas/Captura_Terminal_POST_GET_adminyusuario.png` | **EXITOSO** |

---

## Análisis Profundo de Seguridad: ¿Qué Ocurre Detrás de la Prueba 403 Forbidden?

1. **Emisión de Solicitud de Prueba:** Al presionar el botón `[Ejecutar Prueba 403 Forbidden]` en la pantalla de Auditoría Administrativa, el cliente KMP realiza deliberadamente una llamada HTTP al endpoint de auditoría privada `/admin/users/1/history` desprovisto del token con rol `admin` (o simulando un rol `user`).
2. **Intercepción Temprana por Decorador:** El framework Flask enruta la petición hacia `admin_get_user_history`, pero antes de que comience su ejecución, el decorador `@admin_required` intercepta la llamada.
3. **Decodificación Criptográfica del JWT:** Se extrae el encabezado `Authorization: Bearer <token>` y se valida la firma criptográfica HMAC-SHA256 con el `SECRET_KEY` del backend.
4. **Verificación de Rol (RBAC):** El decorador evalúa `current_user.role`. Al detectar que el usuario no tiene rol `'admin'`, interrumpe de inmediato el procesamiento sin tocar la base de datos SQLite.
5. **Respuesta HTTP 403 Forbidden:** El backend emite una respuesta formal con código de estado HTTP `403 Forbidden` y el cuerpo JSON:
   ```json
   {
     "status": "error",
     "message": "Acceso denegado: se requieren privilegios de Administrador para esta operación"
   }
   ```
6. **Manejo en la Interfaz (Zero Crashes):** El cliente de red KMP captura el código 403 sin fallar ni cerrarse, confirmando que la política de control de acceso basada en roles opera correctamente a nivel de servidor, y presenta al usuario la confirmación visual de protección exitosa.

---

## Verificación de Quality Gates Locales
1. **Compilación KMP:** Módulo `:shared` compila con éxito para Android (AAR) e iOS (Framework Darwin).
2. **Offline 100%:** Todas las pantallas de consulta (Tareas, Favoritos, Historial, Recomendaciones) operan sin conexión mediante caché local.
3. **Seguridad y Criptografía:** Sin contraseñas en texto plano (hashing `Flask-Bcrypt` con sal aleatoria) y tokens JWT válidos por 24 horas con RBAC en backend.
4. **Cero Secretos Expuestos:** Sin API keys quemadas en el código fuente.
