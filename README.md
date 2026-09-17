# INSTITUTO POLITÉCNICO NACIONAL
## Escuela Superior de Cómputo (ESCOM)
### Ingeniería en Sistemas Computacionales

---

| **Materia** | Desarrollo de aplicaciones móviles nativas |
|:---|:---|
| **Práctica** | **Práctica 2:** Aplicación móvil básica para operaciones CRUD con un servicio REST |
| **Alumno(a)** | *Aragón Martínez Manuel Alejandro* |
| **Boleta** | *2023630411* |
| **Grupo** | 7CV4 |
| **Profesor** | *Gabriel Hurtado Avilés* |
| **Fecha de Entrega** | 18 de septiembre de 2026 |

---

## 1. Introducción

El desarrollo contemporáneo de aplicaciones móviles demanda una arquitectura desacoplada, donde la interfaz de usuario nativa (frontend) interactúa de forma eficiente, reactiva y segura con servicios del lado del servidor (backend) a través de interfaces de programación de aplicaciones bajo el estándar REST (*Representational State Transfer*).

El presente proyecto implementa un ecosistema cliente-servidor compuesto por:
1. **Un Backend REST Dockerizado**: Desarrollado en Python con el microframework **Flask**, persistencia relacional con **Flask-SQLAlchemy** sobre SQLite, seguridad criptográfica mediante **Flask-Bcrypt** para el hashing de contraseñas con sal y un sistema de sesiones seguras mediante tokens firmados **JWT (JSON Web Tokens)** con expiración temporal.
2. **Una Aplicación Móvil Nativa Android**: Desarrollada en **Kotlin** utilizando el toolkit declarativo moderno **Jetpack Compose** bajo directrices de diseño **Material 3**. La aplicación integra un menú de navegación tipo cajón lateral (*Navigation Drawer*) para transicionar fluidamente entre pantallas de Inicio de Sesión, Registro de Usuario y Gestión CRUD de Tareas (*Tasks*), empleando **Retrofit** y **OkHttp** con interceptores automáticos para el consumo de la API REST.

### 1.1. Origen del Proyecto y Aportaciones Propias

> [!NOTE]
> **Declaración de Clonación y Base de Trabajo:**
> Este proyecto toma como punto de partida el repositorio de ejemplo proporcionado por la cátedra de Desarrollo de Aplicaciones Móviles: [gabrielhuav/Flask-Compose-Login-API](https://github.com/gabrielhuav/Flask-Compose-Login-API).
> 
> Sobre dicha base inicial —la cual contenía una lógica deliberadamente incompleta y un modelo básico— se implementó el sistema de sesiones seguras por tokens JWT, las cuatro operaciones CRUD sobre el recurso Tareas, la persistencia híbrida, la suite de pruebas unitarias, la arquitectura cliente completa en Jetpack Compose (Material 3) y la documentación requerida.

#### Archivos Modificados respecto al Repositorio Base
| Archivo | Ruta | Modificaciones Realizadas |
|:---|:---|:---|
| `app.py` | `Docker-Flask/ORM/` | Se implementó el modelo relacional `Task` (Tareas), los endpoints CRUD completos (`POST`, `GET`, `PUT`, `DELETE`), el decorador de seguridad `@token_required` con JWT, expiración de 24 horas, y la arquitectura híbrida con fallback tolerante a fallos entre PostgreSQL 17 y SQLite. |
| `docker-compose.yml` | `Docker-Flask/ORM/` | Se configuró una arquitectura multicontenedor añadiendo el servicio de base de datos `postgres:17` (`postgres17_7CV4_sep2026`) con volumen persistente en `./data`, puerto 5432, inyección de variables de entorno y enlace de red con el servicio `web`. |
| `requirements.txt` | `Docker-Flask/ORM/` | Se agregaron las librerías `PyJWT` (manejo de tokens firmados), `python-dotenv` (variables de entorno) y `psycopg2-binary` (controlador de conexión para PostgreSQL). |
| `build.gradle.kts` / `libs.versions.toml` | `Android/FlaskLogin/` | Se agregaron dependencias de `Retrofit 2`, `OkHttp 3`, `converter-gson`, `material-icons-extended` y `lifecycle-viewmodel-compose`. |
| `AndroidManifest.xml` | `Android/FlaskLogin/` | Se declaró el permiso de red `<uses-permission android:name="android.permission.INTERNET" />` y se habilitó `android:usesCleartextTraffic="true"` para permitir peticiones HTTP en desarrollo local. |
| `MainActivity.kt` | `Android/FlaskLogin/` | Se rediseñó el contenedor principal de la aplicación, implementando el `ModalNavigationDrawer`, la `TopAppBar` con menú desplegable, control de estado reactivo de sesión y navegación hacia las pantallas de Login, Registro y CRUD. |
| `README.md` | Raíz | Reestructuración total con portada formal, marco teórico en palabras propias, catálogo de endpoints con ejemplos JSON, instrucciones paso a paso de Docker y evidencias. |

#### Archivos Nuevos Creados
| Archivo | Ruta | Propósito en el Proyecto |
|:---|:---|:---|
| `.gitignore` | Raíz | Exclusión de secretos (`.env`), base de datos local SQLite, directorio de almacenamiento de Postgres (`data/`) y archivos generados por Gradle. |
| `.env.example` / `.env` | `Docker-Flask/ORM/` | Plantilla de configuración segura y archivo de variables locales para llaves criptográficas y cadenas de conexión a base de datos. |
| `test_api.py` | `Docker-Flask/ORM/` | Suite automatizada de pruebas unitarias y de integración que valida los 12 escenarios de la API REST (códigos 200, 201, 400, 401, 404). |
| `User.kt` & `Task.kt` | `Android/.../data/model/` | Clases de datos Kotlin (`Data Classes`) para serialización y mapeo de peticiones y respuestas JSON de autenticación y CRUD. |
| `ApiService.kt` | `Android/.../data/api/` | Interfaz de Retrofit que define las funciones suspendidas (`suspend fun`) para cada endpoint HTTP. |
| `AuthInterceptor.kt` | `Android/.../data/api/` | Interceptor de red de OkHttp que adjunta automáticamente el encabezado `Authorization: Bearer <token>` en cada petición. |
| `RetrofitClient.kt` | `Android/.../data/api/` | Singleton cliente de Retrofit con soporte para la IP especial de emulador (`10.0.2.2:5000`) e IP de dispositivo físico. |
| `SessionManager.kt` | `Android/.../data/session/` | Clase para la persistencia local del token JWT y nombre de usuario autenticado en `SharedPreferences`. |
| `LoginScreen.kt` | `Android/.../ui/screens/` | Pantalla de inicio de sesión con formulario, validación, feedback de carga y consumo de la API. |
| `RegisterScreen.kt` | `Android/.../ui/screens/` | Pantalla de registro de nuevos usuarios con confirmación de contraseñas y redirección automática. |
| `TaskCrudScreen.kt` | `Android/.../ui/screens/` | Pantalla principal de gestión de tareas con `LazyColumn`, chips de estado y botón flotante de acción. |
| `AppDrawer.kt` | `Android/.../ui/components/` | Componente de menú desplegable lateral (Drawer) con cabecera de usuario y opciones de navegación. |
| `TaskDialogs.kt` | `Android/.../ui/components/` | Diálogos modales para Crear, Editar y Confirmar Eliminación de tareas. |
| `docs/img/` | Raíz | Directorio para alojar las capturas de pantalla de evidencia. |

---

### 1.2. Marco Tecnológico y Seguridad de Contraseñas

#### ¿Qué es Flask y cómo se implementa en esta práctica?
**Flask** es un microframework web para Python basado en el estándar **WSGI (Web Server Gateway Interface)** y las librerías Werkzeug y Jinja2. A diferencia de frameworks monolíticos como Django, Flask se caracteriza por ser liviano, minimalista y no imponer una estructura rígida de base de datos o autenticación, otorgando control total al desarrollador.

En este proyecto, Flask se implementa como el motor central de la API REST:
1. **Enrutamiento Declarativo mediante Decoradores**: Rutas como `@app.route('/register', methods=['POST'])` o `@app.route('/tasks', methods=['GET'])` interceptan las solicitudes HTTP entrantes según el método y URL solicitados.
2. **Serialización JSON Nativa**: A través de `request.get_json()` y la función `jsonify()`, se procesan y devuelven estructuras de datos JSON estrictamente estructuradas con sus respectivos códigos de estado HTTP (200, 201, 400, 401, 404).
3. **Manejo del Contexto de Aplicación**: Mediante bloques `with app.app_context():`, se gestiona la conexión con el ORM SQLAlchemy y se crea automáticamente el esquema de tablas en la base de datos al iniciar el servicio.

#### Seguridad y Encriptación de Contraseñas con Flask-Bcrypt
Almacenar contraseñas en texto plano o utilizando funciones hash desactualizadas (como MD5 o SHA-1) representa una vulnerabilidad de seguridad inaceptable. Para esta práctica se implementó **`Flask-Bcrypt`**, una biblioteca que utiliza el algoritmo de cifrado por bloques Blowfish adaptado al hashing seguro:

- **Generación Automática de Sal Criptográfica (*Salting*)**: Cada vez que un usuario se registra (`/register`), Bcrypt genera automáticamente una sal pseudoaleatoria única de 128 bits que se concatena con la contraseña antes de aplicar el algoritmo:
  ```python
  hashed_password = bcrypt.generate_password_hash(password).decode('utf-8')
  ```
  Esto garantiza que dos usuarios con exactamente la misma contraseña generen hashes completamente distintos, neutralizando por completo los ataques basados en tablas precalculadas (*Rainbow Tables*).
- **Factor de Coste Adaptativo (*Work Factor / Rounds*)**: Bcrypt es deliberadamente intensivo en cálculo (por defecto 12 rondas iterativas, equivalentes a $2^{12} = 4096$ iteraciones). Este retardo programado es imperceptible para un usuario legítimo al iniciar sesión, pero vuelve matemáticamente inviable cualquier ataque de fuerza bruta masivo o ataque de diccionario offline.
- **Verificación en Tiempo Constante**: Durante el login (`/login`), se verifica la contraseña suministrada contra el hash almacenado utilizando:
  ```python
  bcrypt.check_password_hash(user.password, password)
  ```
  Esta función realiza una comparación en tiempo constante, evitando ataques de canal lateral basados en el análisis de tiempos de respuesta (*Timing Attacks*).

---

### 1.3. Justificación del Stack Tecnológico


- **Flask (Python 3.9+)**: Microframework altamente eficiente, ligero y modular. Permite construir endpoints RESTful con mínima sobrecarga y control total sobre el pipeline HTTP y la gestión de cabeceras de autorización.
- **SQLite + SQLAlchemy ORM**: Abstrae las operaciones de base de datos a objetos de dominio, permitiendo portabilidad absoluta sin dependencias de motores de bases de datos externos pesados. El esquema relacional se genera automáticamente en el arranque.
- **Flask-Bcrypt**: Implementa el algoritmo de hashing adaptativo Blowfish con sal generada de forma aleatoria, mitigando ataques de fuerza bruta, ataques de tablas arcoíris (*rainbow tables*) y asegurando que ninguna contraseña se almacene o manipule en texto plano.
- **PyJWT (JSON Web Tokens)**: Mecanismo de autenticación sin estado (*stateless*). Permite proteger endpoints críticos verificando la firma criptográfica HS256 y la validez temporal del token antes de conceder acceso a operaciones de base de datos.
- **Docker & Docker Compose**: Garantiza la reproducibilidad del entorno de ejecución con paridad absoluta entre desarrollo y producción, encapsulando dependencias, configuraciones de red y variables de entorno.
- **Kotlin + Jetpack Compose (Material 3)**: Paradigma declarativo nativo para Android que minimiza el código repetitivo, simplifica el manejo de estados visuales (*Loading, Success, Error*) y produce interfaces responsivas, accesibles e intuitivas.
- **Retrofit 2 + OkHttp 3**: Estándar de la industria en Android para networking. Facilita la serialización/deserialización JSON automática con Gson y el uso de interceptores para inyectar dinámicamente el token `Authorization: Bearer <token>` en peticiones autenticadas.

---

## 2. Desarrollo

### 2.1. Fundamentos Teóricos (Ejercicio 2 con palabras propias)

- **Docker**: Es una tecnología de virtualización a nivel de sistema operativo basada en contenedores. A diferencia de las máquinas virtuales tradicionales —que emulan un hardware completo y requieren un sistema operativo invitado completo—, Docker aprovecha las primitivas del núcleo (*kernel*) del sistema operativo anfitrión (como *cgroups* y *namespaces*) para ejecutar procesos en entornos completamente aislados, arrancando en cuestión de milisegundos y consumiendo una fracción de memoria.
- **Imagen vs. Contenedor**:
  - *Imagen*: Es una plantilla inmutable de solo lectura que contiene el sistema de archivos base, el código de la aplicación, las bibliotecas y las instrucciones necesarias para su ejecución.
  - *Contenedor*: Es una instancia viva, ejecutable y efímera de una imagen Docker. Los cambios producidos durante la ejecución se descartan al destruirse el contenedor, a menos que se sincronicen con volúmenes persistentes.
- **Dockerfile**: Archivo declarativo que enumera paso a paso cómo ensamblar la imagen: imagen base (`FROM`), directorio de trabajo (`WORKDIR`), copia de archivos (`COPY`), instalación de dependencias (`RUN`), puertos expuestos (`EXPOSE`) y el comando de arranque del servicio (`CMD`).
- **docker-compose.yml**: Archivo de orquestación en formato YAML que permite definir, interconectar y levantar múltiples contenedores, redes virtuales, mapeos de puertos, variables de entorno y volúmenes con un único comando unificado (`docker compose up --build`).
- **Backend o Servicio REST**: Arquitectura de software que expone capacidades de negocio a través del protocolo HTTP. Emplea verbos estándar (`GET` para consultar, `POST` para crear, `PUT` para actualizar y `DELETE` para eliminar) y comunica los resultados mediante códigos de estado semánticos (200, 201, 400, 401, 404) y cuerpos de datos en formato JSON.
- **ORM (Object-Relational Mapping) y Base de Datos**: Capa de abstracción que traduce tablas relacionales a clases y registros a instancias de objetos en el lenguaje de programación (en este caso SQLAlchemy). Facilita consultas seguras parametrizadas contra inyección SQL y automatiza las transacciones de base de datos.

---

### 2.2. Decisiones Técnicas y de Seguridad

1. **Recurso para Operaciones CRUD (Tareas / Tasks)**:
   Se seleccionó el recurso **Tareas (`Task`)**, el cual modela una unidad de trabajo que contiene identificador (`id`), título (`title`), descripción opcional (`description`), estado de completitud (`status`: `'pending'` o `'completed'`), marca temporal de creación (`created_at`) y vinculación relacional obligatoria con el usuario propietario (`user_id`).
2. **Sesiones Seguras con Tokens Firmados (JWT)**:
   Al autenticarse exitosamente en `/login`, el servidor firma un token JWT mediante el algoritmo simétrico **HS256** utilizando una clave secreta (`SECRET_KEY`). El token tiene un tiempo de expiración programado a **24 horas**. Cada petición hacia endpoints CRUD protegidos requiere el encabezado HTTP:
   ```http
   Authorization: Bearer <token_jwt>
   ```
   Si el token no se suministra, ha expirado o está alterado, el decorador `@token_required` rechaza la petición inmediatamente con código **401 Unauthorized**.
3. **Protección Criptográfica de Contraseñas**:
   Las contraseñas se encriptan al momento del registro mediante una función hash robusta con sal generada aleatoriamente por `Flask-Bcrypt`. La base de datos nunca almacena contraseñas en texto plano.
4. **Almacenamiento Seguro de Secretos**:
   Ninguna credencial o secreto sensible reside en duro en el código fuente. Se utiliza el estándar `.env`, cargado mediante `python-dotenv`. Se distribuye en el repositorio el archivo plantilla [.env.example](file:///c:/Users/osito/OneDrive/Documentos/Desarrollo%20Apps%20Moviles/Flask-Compose-Login-API/Docker-Flask/ORM/.env.example) y se incluye `.env` en el [.gitignore](file:///c:/Users/osito/OneDrive/Documentos/Desarrollo%20Apps%20Moviles/Flask-Compose-Login-API/.gitignore).
5. **Persistencia de Sesión en la App Móvil**:
   La app Android administra el ciclo de vida de la sesión mediante `SessionManager`, persistiendo el token y datos no sensibles del usuario en `SharedPreferences`. Un interceptor de OkHttp (`AuthInterceptor`) adjunta transparentemente el token en cada petición HTTP hacia la API REST.

---

### 2.3. Catálogo Detallado de Endpoints REST

| Método | Ruta | Requiere Token | Código Éxito | Descripción |
|:---|:---|:---:|:---:|:---|
| `GET` | `/` | No | `200 OK` | Verificación de salud y estado de la API REST |
| `POST` | `/register` | No | `201 Created` | Registro de nuevos usuarios con contraseña hasheada |
| `POST` | `/login` | No | `200 OK` | Autenticación de usuarios y entrega de Token JWT |
| `GET` | `/tasks` | **Sí (Bearer)** | `200 OK` | Lectura de todas las tareas del usuario autenticado |
| `GET` | `/tasks/<id>` | **Sí (Bearer)** | `200 OK` | Lectura de una tarea específica por su identificador |
| `POST` | `/tasks` | **Sí (Bearer)** | `201 Created` | Creación de una nueva tarea |
| `PUT` | `/tasks/<id>` | **Sí (Bearer)** | `200 OK` | Actualización de título, descripción o estado de tarea |
| `DELETE` | `/tasks/<id>` | **Sí (Bearer)** | `200 OK` | Eliminación de una tarea de la base de datos |

#### Ejemplos de Peticiones y Respuestas

##### 1. Registro (`POST /register`)
```json
// Petición (Body JSON)
{
  "username": "alumno_escom",
  "password": "PasswordSeguro2026*"
}

// Respuesta Exitosa (201 Created)
{
  "status": "success",
  "message": "Usuario creado exitosamente",
  "user_id": 1,
  "username": "alumno_escom"
}

// Error: Usuario Duplicado (400 Bad Request)
{
  "status": "error",
  "message": "El nombre de usuario ya está registrado"
}
```

##### 2. Inicio de Sesión (`POST /login`)
```json
// Petición (Body JSON)
{
  "username": "alumno_escom",
  "password": "PasswordSeguro2026*"
}

// Respuesta Exitosa (200 OK)
{
  "status": "success",
  "message": "Inicio de sesión exitoso",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user_id": 1,
  "username": "alumno_escom"
}

// Error: Credenciales Incorrectas (401 Unauthorized)
{
  "status": "error",
  "message": "Credenciales inválidas"
}
```

##### 3. Crear Tarea (`POST /tasks`)
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```
```json
// Petición
{
  "title": "Estudiar para examen de Aplicaciones Móviles",
  "description": "Repasar Jetpack Compose y Retrofit",
  "status": "pending"
}

// Respuesta Exitosa (201 Created)
{
  "status": "success",
  "message": "Tarea creada exitosamente",
  "task": {
    "id": 1,
    "title": "Estudiar para examen de Aplicaciones Móviles",
    "description": "Repasar Jetpack Compose y Retrofit",
    "status": "pending",
    "created_at": "2026-09-15 10:05:00",
    "user_id": 1
  }
}
```

##### 4. Listar Tareas (`GET /tasks`)
```json
// Respuesta Exitosa (200 OK)
{
  "status": "success",
  "tasks": [
    {
      "id": 1,
      "title": "Estudiar para examen de Aplicaciones Móviles",
      "description": "Repasar Jetpack Compose y Retrofit",
      "status": "pending",
      "created_at": "2026-09-15 10:05:00",
      "user_id": 1
    }
  ]
}
```

##### 5. Actualizar Tarea (`PUT /tasks/1`)
```json
// Petición
{
  "status": "completed"
}

// Respuesta Exitosa (200 OK)
{
  "status": "success",
  "message": "Tarea actualizada exitosamente",
  "task": {
    "id": 1,
    "title": "Estudiar para examen de Aplicaciones Móviles",
    "description": "Repasar Jetpack Compose y Retrofit",
    "status": "completed",
    "created_at": "2026-09-15 10:05:00",
    "user_id": 1
  }
}
```

##### 6. Eliminar Tarea (`DELETE /tasks/1`)
```json
// Respuesta Exitosa (200 OK)
{
  "status": "success",
  "message": "Tarea eliminada exitosamente"
}
```

---

### 2.4. Guía de Despliegue y Ejecución del Backend

#### Requisitos Previos
- Tener instalado **Docker Desktop** (con motor Linux activo) o **Python 3.9+** si se desea ejecución directa.

#### Paso 1: Configuración de Variables de Entorno
Ubicarse en el directorio del backend:
```bash
cd "Docker-Flask/ORM"
```
Copiar el archivo de plantilla `.env.example` a `.env`:
- En Windows (PowerShell):
  ```powershell
  Copy-Item .env.example .env
  ```
- En Linux / macOS:
  ```bash
  cp .env.example .env
  ```

#### Pasos Previos para el Despliegue
1. **Clonar el Repositorio de GitHub**:
   ```bash
   git clone https://github.com/gabrielhuav/Flask-Compose-Login-API.git
   cd Flask-Compose-Login-API
   ```
2. **Navegar a la Carpeta del Backend**:
   ```bash
   cd Docker-Flask/ORM
   ```
3. **Configurar el Archivo de Variables de Entorno (`.env`)**:
   Copiar el archivo de plantilla `.env.example` a `.env` para habilitar la clave secreta y la cadena de conexión:
   - En Windows (PowerShell):
     ```powershell
     Copy-Item .env.example .env
     ```
   - En Linux / macOS:
     ```bash
     cp .env.example .env
     ```

#### Explicación Detallada Instrucción por Instrucción del `Dockerfile`

El archivo [`Dockerfile`](file:///c:/Users/osito/OneDrive/Documentos/Desarrollo%20Apps%20Moviles/Flask-Compose-Login-API/Docker-Flask/ORM/Dockerfile) automatiza la creación de la imagen para el microservicio Flask. Cada directiva cumple una función crítica:

| Instrucción | Parámetro | Explicación Técnica Detallada |
|:---|:---|:---|
| `FROM` | `python:3.9-slim` | Establece la imagen base oficial. Se elige la variante *slim* (basada en Debian reducida) porque contiene únicamente los binarios indispensables para ejecutar Python 3.9, reduciendo drásticamente el tamaño de la imagen final y minimizando la superficie de posibles vulnerabilidades. |
| `WORKDIR` | `/app` | Define el directorio de trabajo predeterminado dentro del contenedor. Todas las acciones subsecuentes (`COPY`, `RUN`, `CMD`) se ejecutarán en esta ruta absoluta `/app`. Si no existe, Docker la crea automáticamente. |
| `COPY` | `requirements.txt .` | Copia **únicamente** el archivo de dependencias antes que el resto del código. Esta es una optimización fundamental del sistema de capas (*Docker Cache*): mientras `requirements.txt` no cambie, Docker reutiliza la capa previa en compilaciones posteriores sin reinstalar librerías. |
| `RUN` | `pip install --no-cache-dir -r requirements.txt` | Ejecuta la instalación de las dependencias de Python. El parámetro `--no-cache-dir` evita almacenar los paquetes descargados en el caché temporal de pip, manteniendo la imagen compacta y liviana. |
| `COPY` | `. .` | Copia la totalidad de los archivos del directorio local del backend hacia el directorio de trabajo `/app` del contenedor. |
| `EXPOSE` | `5000` | Documenta e indica a nivel de metadatos que el proceso que se ejecutará dentro del contenedor escuchará peticiones de red en el puerto TCP 5000. |
| `CMD` | `["python", "app.py"]` | Define el comando por defecto en formato *exec* que se ejecutará en el momento exacto en que se inicie un contenedor derivado de esta imagen, arrancando el servidor web de Flask. |

#### Explicación Detallada Instrucción por Instrucción del `docker-compose.yml`

El archivo [`docker-compose.yml`](file:///c:/Users/osito/OneDrive/Documentos/Desarrollo%20Apps%20Moviles/Flask-Compose-Login-API/Docker-Flask/ORM/docker-compose.yml) orquesta la arquitectura multicontenedor:

| Instrucción / Clave | Valor / Ejemplo | Explicación Técnica Detallada |
|:---|:---|:---|
| `version:` | `'3.8'` | Especifica la versión del esquema de Docker Compose que define la sintaxis admitida y la compatibilidad con las características modernas del motor Docker. |
| `services:` | *(Bloque raíz)* | Sección que declara el conjunto de contenedores interconectados que conforman la solución. |
| **Servicio: `postgres:`** | *(Nombre del servicio)* | Define el servicio encargado de la base de datos relacional PostgreSQL. |
| `image:` | `postgres:17` | Instruye a Docker descargar directamente la imagen oficial de PostgreSQL versión 17 desde Docker Hub sin requerir una compilación local. |
| `container_name:` | `postgres17_7CV4_sep2026` | Asigna un identificador amigable al contenedor en ejecución, combinando la versión del motor, el grupo académico (**7CV4**) y la fecha de entrega, facilitando su monitoreo con `docker ps`. |
| `environment:` | `POSTGRES_DB: postgres`<br>`POSTGRES_USER: postgres`<br>`POSTGRES_PASSWORD: postgres` | Inyecta las variables de entorno obligatorias que el entrypoint oficial de PostgreSQL necesita para inicializar la base de datos, el superusuario y sus credenciales de acceso iniciales. |
| `ports:` | `-"5432:5432"` | Mapeo de puertos `Host:Contenedor`. Permite que aplicaciones o clientes externos alcancen el motor Postgres mediante el puerto 5432 de la máquina host. |
| `volumes:` | `- ./data:/var/lib/postgresql/data` | Crea un volumen enlazado (*bind mount*) entre la carpeta local `./data` y el directorio de almacenamiento interno de PostgreSQL. Garantiza que la información y las tablas **no se destruyan** al apagar o recrear el contenedor. |
| `restart:` | `unless-stopped` | Política de resiliencia que indica al demonio de Docker reiniciar el contenedor automáticamente si falla o tras un reinicio del sistema, a menos que se detenga manualmente. |
| **Servicio: `web:`** | *(Nombre del servicio)* | Define el contenedor que ejecuta el backend REST en Flask. |
| `build:` | `.` | Ordena a Compose construir la imagen en tiempo de ejecución utilizando el `Dockerfile` ubicado en el directorio actual (`.`). |
| `container_name:` | `flask_login_backend` | Nombre identificador explícito para el contenedor de la API. |
| `ports:` | `-"5000:5000"` | Mapea el puerto 5000 de la máquina física al puerto 5000 del contenedor, permitiendo que la aplicación Android y herramientas como curl consuman los endpoints. |
| `env_file:` | `- .env` | Lee e inyecta las variables y secretos almacenados en el archivo local `.env`, evitando exponer credenciales en el código versionado. |
| `environment:` | `FLASK_ENV`, `FLASK_DEBUG`, `PORT`, `DATABASE_URL` | Configura el modo de depuración para desarrollo y establece la cadena de conexión interna `postgresql://postgres:postgres@postgres:5432/postgres` usando el nombre del servicio `postgres` como host DNS. |
| `depends_on:` | `- postgres` | Establece una dependencia de arranque: Compose garantiza que el contenedor de PostgreSQL inicie antes de intentar levantar el servicio Flask. |
| `volumes:` | `- .:/app` | Vincula el directorio de código fuente del host con `/app` dentro del contenedor, lo que permite la recarga en caliente (*Hot-Reloading*) inmediata cuando se modifica código en Python sin tener que reconstruir la imagen. |

#### Paso para Levantar el Entorno con Docker Compose
Ejecutar el comando de construcción y puesta en marcha:
```bash
docker compose up --build
```
*(El flag `--build` fuerza la construcción de la imagen actualizada antes de arrancar los contenedores).*


#### Opción Alternativa (Ejecución Directa con Python)
Si Docker Desktop se encuentra pausado o se desea probar localmente:
```bash
cd "Docker-Flask/ORM"
pip install -r requirements.txt
python app.py
```

#### Paso 3: Verificación Automatizada con Test Suite
Se incluye un script de validación que prueba todos los endpoints, códigos de estado y la expiración de tokens:
```bash
python test_api.py
```

---

### 2.5. Configuración y Ejecución de la App Android

#### Consideraciones de Red para Conexión al Backend
1. **Emulador Oficial de Android (AVD)**:
   - El emulador tiene su propio espacio de red aislado donde `localhost` o `127.0.0.1` apunta a sí mismo.
   - Para comunicarse con el backend REST corriendo en la computadora anfitriona (en el puerto 5000), se debe utilizar la dirección especial **`http://10.0.2.2:5000/`**. Ésta se encuentra configurada por defecto en [`RetrofitClient.kt`](file:///c:/Users/osito/OneDrive/Documentos/Desarrollo%20Apps%20Moviles/Flask-Compose-Login-API/Android/FlaskLogin/app/src/main/java/ovh/gabrielhuav/flasklogin/data/api/RetrofitClient.kt).
2. **Dispositivo Físico**:
   - Conectar el teléfono y la computadora a la misma red Wi-Fi.
   - Obtener la IP local de la computadora (ej. `ipconfig` en Windows -> `192.168.1.76`).
   - Configurar la URL en `RetrofitClient.kt`: `http://192.168.1.76:5000/`.
3. **Permisos y Tráfico HTTP en Texto Claro**:
   - En [`AndroidManifest.xml`](file:///c:/Users/osito/OneDrive/Documentos/Desarrollo%20Apps%20Moviles/Flask-Compose-Login-API/Android/FlaskLogin/app/src/main/AndroidManifest.xml) se habilitó el permiso de internet:
     ```xml
     <uses-permission android:name="android.permission.INTERNET" />
     ```
   - Y se permitió tráfico HTTP no cifrado para entornos de desarrollo:
     ```xml
     android:usesCleartextTraffic="true"
     ```

#### Ejecución en Android Studio
1. Abrir **Android Studio**.
2. Seleccionar **Open** y abrir la carpeta `Android/FlaskLogin`.
3. Permitir que Gradle sincronice el proyecto (*Sync Project with Gradle Files*).
4. Iniciar un Emulador Android (AVD) con API 24 o superior.
5. Presionar el botón **Run 'app'** (Shift + F10).

---

### 2.6. Evidencias de Ejecución (Capturas de Pantalla)

Las capturas deben colocarse en la carpeta [`docs/img/`](file:///c:/Users/osito/OneDrive/Documentos/Desarrollo%20Apps%20Moviles/Flask-Compose-Login-API/docs/img/). 

> [!TIP]
> **¿Cómo incrustar y visualizar las capturas en GitHub?**
> - **Método Automático vía Git:** Guarda tus capturas dentro de la carpeta local `docs/img/` con los nombres exactos de la tabla (ej. `01_registro_exitoso.png`). Haz `git add docs/img/`, `git commit` y `git push origin main`. GitHub renderizará automáticamente las imágenes dentro de esta tabla en el README.
> - **Método Web en GitHub:** Entra a tu repositorio en el navegador, ve a la carpeta `docs/img`, pulsa **Add file > Upload files**, arrastra las fotos y haz commit.


| Acción Evaluada | Evidencia Incrustada | Descripción |
|:---|:---:|:---|
| **Contenedor Docker Activo** | ![Docker](docs/img/10_docker_running.png) | Servicio REST corriendo en puerto 5000 vía Docker Compose |
| **Registro de Usuario** | ![Registro](docs/img/01_registro_exitoso.png) | Creación exitosa de una nueva cuenta |
| **Control de Duplicados (400)** | ![Duplicado](docs/img/02_registro_duplicado.png) | Validación y rechazo si el usuario ya existe |
| **Credenciales Inválidas (401)**| ![Login Error](docs/img/03_login_invalido.png) | Manejo de contraseña incorrecta |
| **Inicio de Sesión Exitoso** | ![Login OK](docs/img/04_login_exitoso.png) | Obtención de sesión con token JWT |
| **Menú de Navegación (Drawer)** | ![Drawer](docs/img/05_drawer_navegacion.png) | Drawer lateral con opciones y estado de sesión |
| **CRUD: Crear Tarea (POST)** | ![Crear](docs/img/06_crud_crear.png) | Diálogo modal y registro de tarea (201) |
| **CRUD: Listar Tareas (GET)** | ![Listar](docs/img/07_crud_listar.png) | Visualización reactiva de tareas en `LazyColumn` (200) |
| **CRUD: Actualizar Tarea (PUT)**| ![Actualizar](docs/img/08_crud_actualizar.png)| Cambio de estado o edición de texto (200) |
| **CRUD: Borrar Tarea (DELETE)**| ![Borrar](docs/img/09_crud_eliminar.png) | Diálogo de confirmación y eliminación (200) |

---

## 3. Conclusiones

El desarrollo de esta práctica permitió comprender en profundidad los principios fundamentales de la integración cliente-servidor nativa y la containerización de servicios:

1. **Aislamiento y Reproducibilidad con Docker**: Docker Compose elimina los problemas clásicos de "en mi máquina sí funciona". Al encapsular la versión exacta de Python, las dependencias y la configuración de red, cualquier colaborador puede poner en marcha el backend inmediatamente con un único comando.
2. **Seguridad en la Autenticación y Manejo de Sesiones**: Se comprobó que almacenar contraseñas en texto plano es una vulnerabilidad crítica. El uso de `Flask-Bcrypt` con sal aleatoria garantiza la resistencia ante ataques de diccionario. Adicionalmente, el patrón de tokens JWT permitió proteger de manera desacoplada los endpoints CRUD, logrando que el servidor no necesite almacenar estado en memoria para verificar la autenticidad de cada petición.
3. **Desarrollo Declarativo con Jetpack Compose**: La transición de layouts tradicionales en XML hacia Jetpack Compose agiliza significativamente la construcción de interfaces. El desacoplamiento del estado visual (*Loading*, *Error*, *Empty*, *Success*) garantiza una experiencia de usuario clara, evitando bloqueos en la interfaz y manteniendo informado al usuario en todo momento.
4. **Resolución de Red en Emuladores**: Uno de los retos más comunes en el desarrollo móvil es comprender el direccionamiento de red. Comprender que `localhost` dentro de un emulador apunta al dispositivo virtual y que la IP virtual de puente hacia el host es `10.0.2.2` resultó clave para lograr la comunicación HTTP fluida con el contenedor Docker.

---

## 4. Bibliografía (Formato APA 7.ª Edición)

- Android Developers. (2024). *Jetpack Compose basics*. Google Developers. https://developer.android.com/develop/ui/compose/tutorial
- Docker Inc. (2024). *Docker Compose overview*. Docker Documentation. https://docs.docker.com/compose/
- Fielding, R. T. (2000). *Architectural Styles and the Design of Network-based Software Architectures* (Doctoral dissertation). University of California, Irvine.
- Grinberg, M. (2018). *Flask Web Development: Developing Web Applications with Python* (2nd ed.). O'Reilly Media.
- Jones, M., Bradley, J., & Sakimura, N. (2015). *JSON Web Token (JWT)* (RFC 7519). Internet Engineering Task Force (IETF). https://doi.org/10.17487/RFC7519
- Square Inc. (2024). *Retrofit: A type-safe HTTP client for Android and Java*. Square Open Source. https://square.github.io/retrofit/
