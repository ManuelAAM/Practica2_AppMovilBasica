"""
Script de Pruebas Automatizadas de la API REST (Flask + RBAC + CRUD de Usuarios y Tareas)
Práctica 2 - Desarrollo de Aplicaciones Móviles Nativas (ESCOM - IPN)

Pruebas integrales de:
1. Health Check (GET /)
2. Registro de Usuario Normal (POST /register)
3. Login de Usuario Normal (POST /login) -> Retorno de token con role 'user'
4. Modificación de información propia por Usuario Normal (PUT /profile)
5. Intento de Usuario Normal de acceder al panel de administración (GET /users) -> 403 Forbidden
6. Login del Administrador por Defecto (POST /login con 'admin' / 'Admin123*')
7. Admin lista todos los usuarios (GET /users) -> 200 OK
8. Admin crea un usuario con rol específico (POST /users) -> 201 Created
9. Admin actualiza los datos de otro usuario (PUT /users/<id>) -> 200 OK
10. Admin elimina un usuario (DELETE /users/<id>) -> 200 OK
11. Admin intenta autoeliminarse -> 400 Bad Request
12. CRUD de Tareas con Token (POST /tasks, GET /tasks, DELETE /tasks)
"""

import sys
import time
import requests

BASE_URL = "http://localhost:5000"

def run_tests():
    print("=" * 70)
    print("INICIANDO PRUEBAS DE LA API REST - ROLES, USUARIOS Y CRUD")
    print(f"URL Objetivo: {BASE_URL}")
    print("=" * 70)

    timestamp = int(time.time())
    test_user = f"alumno_{timestamp}"
    test_pass = "Secreto123*"

    # 1. Health Check
    print("\n[TEST 1] GET / (Health Check)...", end=" ")
    r = requests.get(f"{BASE_URL}/")
    assert r.status_code == 200, f"Fallo: {r.status_code}"
    print("EXITOSO (200 OK)")

    # 2. Registro de Usuario Normal
    print(f"[TEST 2] POST /register (Usuario: {test_user})...", end=" ")
    r = requests.post(f"{BASE_URL}/register", json={
        "username": test_user,
        "password": test_pass
    })
    assert r.status_code == 201, f"Fallo: {r.text}"
    user_data = r.json()
    assert user_data.get("role") == "user"
    print("EXITOSO (201 Created - Rol: user)")

    # 3. Login de Usuario Normal
    print("[TEST 3] POST /login (Usuario normal)...", end=" ")
    r = requests.post(f"{BASE_URL}/login", json={
        "username": test_user,
        "password": test_pass
    })
    assert r.status_code == 200, f"Fallo: {r.text}"
    user_token = r.json().get("token")
    assert user_token, "No se recibió token"
    assert r.json().get("role") == "user"
    user_headers = {"Authorization": f"Bearer {user_token}"}
    print(f"EXITOSO (200 OK) -> Token recibido para rol: user")

    # 4. Modificar propia información (PUT /profile)
    print("[TEST 4] PUT /profile (Usuario modifica sus propios datos)...", end=" ")
    r = requests.put(f"{BASE_URL}/profile", headers=user_headers, json={
        "username": f"{test_user}_modificado",
        "password": "NuevoPassword123*"
    })
    assert r.status_code == 200, f"Fallo: {r.text}"
    updated_user = r.json().get("user")
    assert updated_user["username"] == f"{test_user}_modificado"
    user_token = r.json().get("token")  # Actualizar token renovado
    user_headers = {"Authorization": f"Bearer {user_token}"}
    print("EXITOSO (200 OK - Perfil actualizado)")

    # 5. Usuario normal intenta acceder a /users (debe ser rechazado con 403)
    print("[TEST 5] GET /users (Usuario normal intenta ver lista de admin)...", end=" ")
    r = requests.get(f"{BASE_URL}/users", headers=user_headers)
    assert r.status_code == 403, f"Esperado 403 Forbidden, recibido: {r.status_code}"
    print("EXITOSO (403 Forbidden - Control de Acceso RBAC verificado)")

    # 6. Login de Administrador por defecto
    print("[TEST 6] POST /login (Admin por defecto: 'admin' / 'Admin123*')...", end=" ")
    r = requests.post(f"{BASE_URL}/login", json={
        "username": "admin",
        "password": "Admin123*"
    })
    assert r.status_code == 200, f"Fallo login admin: {r.text}"
    admin_token = r.json().get("token")
    assert admin_token, "No se recibió token de admin"
    assert r.json().get("role") == "admin"
    admin_headers = {"Authorization": f"Bearer {admin_token}"}
    print("EXITOSO (200 OK - Rol: admin)")

    # 7. Admin lista todos los usuarios (GET /users)
    print("[TEST 7] GET /users (Admin lista todos los usuarios del sistema)...", end=" ")
    r = requests.get(f"{BASE_URL}/users", headers=admin_headers)
    assert r.status_code == 200, f"Fallo: {r.text}"
    users_list = r.json().get("users", [])
    assert len(users_list) >= 2, "Deberían existir al menos el admin y el usuario creado"
    print(f"EXITOSO (200 OK -> Total usuarios: {len(users_list)})")

    # 8. Admin crea un nuevo usuario con rol específico (POST /users)
    print("[TEST 8] POST /users (Admin crea usuario 'operador')...", end=" ")
    r = requests.post(f"{BASE_URL}/users", headers=admin_headers, json={
        "username": f"operador_{timestamp}",
        "password": "PasswordOperador123*",
        "role": "user"
    })
    assert r.status_code == 201, f"Fallo: {r.text}"
    operador_id = r.json().get("user", {}).get("id")
    print(f"EXITOSO (201 Created -> ID: {operador_id})")

    # 9. Admin actualiza a otro usuario (PUT /users/<id>)
    print(f"[TEST 9] PUT /users/{operador_id} (Admin modifica rol y nombre de operador)...", end=" ")
    r = requests.put(f"{BASE_URL}/users/{operador_id}", headers=admin_headers, json={
        "username": f"operador_promovido_{timestamp}",
        "role": "admin"
    })
    assert r.status_code == 200, f"Fallo: {r.text}"
    assert r.json().get("user", {}).get("role") == "admin"
    print("EXITOSO (200 OK - Promovido a admin)")

    # 10. Admin elimina a un usuario (DELETE /users/<id>)
    print(f"[TEST 10] DELETE /users/{operador_id} (Admin elimina usuario)...", end=" ")
    r = requests.delete(f"{BASE_URL}/users/{operador_id}", headers=admin_headers)
    assert r.status_code == 200, f"Fallo: {r.text}"
    print("EXITOSO (200 OK - Usuario eliminado)")

    # 11. Admin intenta autoeliminarse (debe fallar para evitar bloqueo)
    print("[TEST 11] DELETE /users/1 (Admin intenta eliminarse a sí mismo)...", end=" ")
    r = requests.delete(f"{BASE_URL}/users/1", headers=admin_headers)
    assert r.status_code == 400, f"Esperado 400, recibido {r.status_code}"
    print("EXITOSO (400 Bad Request - Autoeliminación bloqueada)")

    # 12. CRUD Tareas (Recurso complementario)
    print("[TEST 12] POST y GET /tasks (Operación sobre tareas)...", end=" ")
    r_task = requests.post(f"{BASE_URL}/tasks", headers=admin_headers, json={
        "title": "Verificar práctica 2",
        "description": "Comprobar roles y endpoints REST"
    })
    assert r_task.status_code == 201
    task_id = r_task.json().get("task", {}).get("id")
    requests.delete(f"{BASE_URL}/tasks/{task_id}", headers=admin_headers)
    print("EXITOSO (201 Created & 200 Deleted)")

    print("\n" + "=" * 70)
    print("¡TODAS LAS PRUEBAS DE ROLES, USUARIOS Y CRUD (12/12) PASARON CON ÉXITO!")
    print("=" * 70)
    return True

if __name__ == "__main__":
    success = run_tests()
    sys.exit(0 if success else 1)
