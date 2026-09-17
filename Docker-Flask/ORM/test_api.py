"""
Script de Pruebas Automatizadas de la API REST (Flask + JWT + CRUD)
Práctica 2 - Desarrollo de Aplicaciones Móviles Nativas (ESCOM - IPN)

Este script realiza pruebas exhaustivas sobre los endpoints de la API:
1. Health check (GET /)
2. Registro de nuevo usuario (POST /register) -> 201 Created
3. Intento de registro duplicado (POST /register) -> 400 Bad Request
4. Login con credenciales inválidas (POST /login) -> 401 Unauthorized
5. Login exitoso (POST /login) -> 200 OK + JWT Token
6. Acceso a ruta protegida sin token (GET /tasks) -> 401 Unauthorized
7. Crear tarea (POST /tasks) -> 201 Created
8. Listar tareas (GET /tasks) -> 200 OK
9. Obtener tarea por ID (GET /tasks/<id>) -> 200 OK
10. Actualizar tarea (PUT /tasks/<id>) -> 200 OK
11. Eliminar tarea (DELETE /tasks/<id>) -> 200 OK
12. Consultar tarea eliminada (GET /tasks/<id>) -> 404 Not Found
"""

import sys
import time
import requests

BASE_URL = "http://localhost:5000"

def run_tests():
    print("=" * 65)
    print("INICIANDO PRUEBAS DE LA API REST - PRACTICA 2")
    print(f"URL Objetivo: {BASE_URL}")
    print("=" * 65)

    timestamp = int(time.time())
    test_user = f"alumno_{timestamp}"
    test_pass = "Secreto123*"

    # 1. Health check
    print("\n[TEST 1] GET / (Health Check)...", end=" ")
    try:
        r = requests.get(f"{BASE_URL}/")
        assert r.status_code == 200, f"Esperado 200, recibido {r.status_code}"
        print("EXITOSO (200 OK)")
    except Exception as e:
        print(f"FALLO: {e}")
        return False

    # 2. Registro de Usuario
    print(f"[TEST 2] POST /register (Usuario: {test_user})...", end=" ")
    r = requests.post(f"{BASE_URL}/register", json={
        "username": test_user,
        "password": test_pass
    })
    assert r.status_code == 201, f"Esperado 201, recibido {r.status_code}: {r.text}"
    print("EXITOSO (201 Created)")

    # 3. Registro Duplicado
    print("[TEST 3] POST /register (Usuario duplicado debe fallar)...", end=" ")
    r = requests.post(f"{BASE_URL}/register", json={
        "username": test_user,
        "password": test_pass
    })
    assert r.status_code == 400, f"Esperado 400, recibido {r.status_code}"
    print("EXITOSO (400 Bad Request)")

    # 4. Login con contraseña incorrecta
    print("[TEST 4] POST /login (Credenciales incorrectas)...", end=" ")
    r = requests.post(f"{BASE_URL}/login", json={
        "username": test_user,
        "password": "wrongpassword"
    })
    assert r.status_code == 401, f"Esperado 401, recibido {r.status_code}"
    print("EXITOSO (401 Unauthorized)")

    # 5. Login Exitoso y obtención de JWT
    print("[TEST 5] POST /login (Credenciales correctas)...", end=" ")
    r = requests.post(f"{BASE_URL}/login", json={
        "username": test_user,
        "password": test_pass
    })
    assert r.status_code == 200, f"Esperado 200, recibido {r.status_code}"
    data = r.json()
    token = data.get("token")
    assert token, "Token no presente en la respuesta"
    print(f"EXITOSO (200 OK) -> Token recibido: {token[:20]}...")

    headers = {"Authorization": f"Bearer {token}"}

    # 6. Intentar acceder a /tasks sin token
    print("[TEST 6] GET /tasks (Sin header Authorization)...", end=" ")
    r = requests.get(f"{BASE_URL}/tasks")
    assert r.status_code == 401, f"Esperado 401, recibido {r.status_code}"
    print("EXITOSO (401 Unauthorized)")

    # 7. Crear Tarea (POST /tasks)
    print("[TEST 7] POST /tasks (Crear nueva tarea)...", end=" ")
    r = requests.post(f"{BASE_URL}/tasks", headers=headers, json={
        "title": "Preparar reporte Práctica 2",
        "description": "Completar README y capturas de pantalla de la app móvil",
        "status": "pending"
    })
    assert r.status_code == 201, f"Esperado 201, recibido {r.status_code}: {r.text}"
    task_created = r.json().get("task")
    task_id = task_created["id"]
    print(f"EXITOSO (201 Created) -> Tarea ID {task_id}")

    # 8. Listar Tareas (GET /tasks)
    print("[TEST 8] GET /tasks (Listar todas las tareas del usuario)...", end=" ")
    r = requests.get(f"{BASE_URL}/tasks", headers=headers)
    assert r.status_code == 200, f"Esperado 200, recibido {r.status_code}"
    tasks = r.json().get("tasks", [])
    assert len(tasks) >= 1, "Debería haber al menos 1 tarea"
    print(f"EXITOSO (200 OK) -> Total tareas: {len(tasks)}")

    # 9. Obtener Tarea por ID (GET /tasks/<id>)
    print(f"[TEST 9] GET /tasks/{task_id} (Detalle de tarea)...", end=" ")
    r = requests.get(f"{BASE_URL}/tasks/{task_id}", headers=headers)
    assert r.status_code == 200, f"Esperado 200, recibido {r.status_code}"
    assert r.json().get("task", {}).get("id") == task_id
    print("EXITOSO (200 OK)")

    # 10. Actualizar Tarea (PUT /tasks/<id>)
    print(f"[TEST 10] PUT /tasks/{task_id} (Marcar como completada)...", end=" ")
    r = requests.put(f"{BASE_URL}/tasks/{task_id}", headers=headers, json={
        "title": "Preparar reporte Práctica 2 (FINALIZADO)",
        "status": "completed"
    })
    assert r.status_code == 200, f"Esperado 200, recibido {r.status_code}"
    assert r.json().get("task", {}).get("status") == "completed"
    print("EXITOSO (200 OK)")

    # 11. Eliminar Tarea (DELETE /tasks/<id>)
    print(f"[TEST 11] DELETE /tasks/{task_id} (Eliminar tarea)...", end=" ")
    r = requests.delete(f"{BASE_URL}/tasks/{task_id}", headers=headers)
    assert r.status_code == 200, f"Esperado 200, recibido {r.status_code}"
    print("EXITOSO (200 OK)")

    # 12. Verificar que ya no existe (404 Not Found)
    print(f"[TEST 12] GET /tasks/{task_id} (Verificar que retorna 404)...", end=" ")
    r = requests.get(f"{BASE_URL}/tasks/{task_id}", headers=headers)
    assert r.status_code == 404, f"Esperado 404, recibido {r.status_code}"
    print("EXITOSO (404 Not Found)")

    print("\n" + "=" * 65)
    print("¡TODAS LAS PRUEBAS (12/12) PASARON SATISFACTORIAMENTE!")
    print("=" * 65)
    return True

if __name__ == "__main__":
    success = run_tests()
    sys.exit(0 if success else 1)
