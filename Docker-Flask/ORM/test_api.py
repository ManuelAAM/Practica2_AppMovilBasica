"""
Suite de Pruebas Automatizadas de la API REST - Práctica 5 (Continuación Práctica 2)
Desarrollo de Aplicaciones Móviles Nativas - ESCOM IPN
Alumno: Manuel Alejandro Aragón Martínez - Boleta: 2023630411 - Grupo: 7CV4

Valida:
1. Health Check (GET /)
2. Registro de Usuario Normal (POST /register)
3. Login de Usuario Normal (POST /login) -> Rol 'user'
4. Actualización de perfil propio (PUT /profile)
5. Control de Acceso RBAC: Usuario normal no puede acceder a /users (403 Forbidden)
6. Login de Administrador por defecto ('admin' / 'Admin123*')
7. Admin lista y administra usuarios (GET /users)
8. CRUD de Tareas con Token (POST, GET, PUT, DELETE /tasks)
9. Autenticación con Google (POST /auth/google) -> Rol 'user', JWT y proveedor registrado
10. Registro y consulta de Historial de Búsquedas (POST, GET, DELETE /history)
11. Registro y consulta de Favoritos (POST, GET, DELETE /favorites)
12. Sincronización en lote de cola offline (POST /sync/favorites) -> Idempotencia y sin duplicados
13. Estado de sincronización del servidor (GET /sync/status)
14. Auditoría Administrativa (Solo Admin):
    - Admin consulta historial de otro usuario (GET /admin/users/<id>/history) -> 200 OK
    - Admin consulta favoritos de otro usuario (GET /admin/users/<id>/favorites) -> 200 OK
15. Verificación de Seguridad RBAC en Auditoría:
    - Usuario normal intenta consultar historial de otro usuario -> 403 Forbidden
    - Usuario normal intenta consultar favoritos de otro usuario -> 403 Forbidden
"""

import sys
import os
import json
import time

# Intentar usar requests contra servidor en vivo; si no, usar Flask test_client directamente
try:
    import requests
    LIVE_SERVER_AVAILABLE = True
except ImportError:
    LIVE_SERVER_AVAILABLE = False

from app import app, db, init_database, User, Task, SearchHistory, Favorite

class TestClientWrapper:
    """Wrapper unificado para ejecutar peticiones tanto contra un servidor live como contra Flask test_client."""
    def __init__(self, base_url="http://localhost:5000"):
        self.base_url = base_url
        self.use_live = False
        if LIVE_SERVER_AVAILABLE:
            try:
                r = requests.get(f"{self.base_url}/", timeout=1)
                if r.status_code == 200:
                    self.use_live = True
            except Exception:
                self.use_live = False
        self.flask_client = app.test_client()

    def get(self, path, headers=None):
        if self.use_live:
            return requests.get(f"{self.base_url}{path}", headers=headers)
        return self._wrap_flask(self.flask_client.get(path, headers=headers))

    def post(self, path, json_data=None, headers=None):
        if self.use_live:
            return requests.post(f"{self.base_url}{path}", json=json_data, headers=headers)
        return self._wrap_flask(self.flask_client.post(path, json=json_data, headers=headers))

    def put(self, path, json_data=None, headers=None):
        if self.use_live:
            return requests.put(f"{self.base_url}{path}", json=json_data, headers=headers)
        return self._wrap_flask(self.flask_client.put(path, json=json_data, headers=headers))

    def delete(self, path, headers=None):
        if self.use_live:
            return requests.delete(f"{self.base_url}{path}", headers=headers)
        return self._wrap_flask(self.flask_client.delete(path, headers=headers))

    def _wrap_flask(self, resp):
        class ResponseAdapter:
            def __init__(self, r):
                self.status_code = r.status_code
                self.text = r.get_data(as_text=True)
                self._json = None
                try:
                    self._json = r.get_json()
                except Exception:
                    pass
            def json(self):
                return self._json or {}
        return ResponseAdapter(resp)

def run_tests():
    print("=" * 80)
    print("SUITE DE PRUEBAS AUTOMATIZADAS - PRÁCTICA 5 (KMP + API REST + RBAC + SYNC)")
    print("=" * 80)

    with app.app_context():
        init_database()

    client = TestClientWrapper()
    mode_str = "Servidor Live (http://localhost:5000)" if client.use_live else "Flask Test Client (In-Process)"
    print(f"Modo de Ejecución de Pruebas: {mode_str}\n")

    timestamp = int(time.time())
    test_user = f"alumno_p5_{timestamp}"
    test_pass = "Secreto123*"

    # 1. Health Check
    print("[TEST 01] GET / (Health Check)...", end=" ")
    r = client.get("/")
    assert r.status_code == 200, f"Fallo: {r.status_code}"
    print("EXITOSO (200 OK)")

    # 2. Registro de Usuario Normal
    print(f"[TEST 02] POST /register (Usuario: {test_user})...", end=" ")
    r = client.post("/register", json_data={"username": test_user, "password": test_pass})
    assert r.status_code == 201, f"Fallo: {r.text}"
    user_id = r.json().get("user_id")
    print(f"EXITOSO (201 Created - ID: {user_id}, Rol: user)")

    # 3. Login de Usuario Normal
    print("[TEST 03] POST /login (Usuario normal)...", end=" ")
    r = client.post("/login", json_data={"username": test_user, "password": test_pass})
    assert r.status_code == 200, f"Fallo: {r.text}"
    user_token = r.json().get("token")
    assert user_token, "No se recibió token"
    user_headers = {"Authorization": f"Bearer {user_token}"}
    print("EXITOSO (200 OK - Token JWT emitido)")

    # 4. Modificar perfil propio
    print("[TEST 04] PUT /profile (Usuario modifica sus propios datos)...", end=" ")
    r = client.put("/profile", json_data={"username": f"{test_user}_mod", "password": "NuevaPassword123*"}, headers=user_headers)
    assert r.status_code == 200, f"Fallo: {r.text}"
    user_token = r.json().get("token")
    user_headers = {"Authorization": f"Bearer {user_token}"}
    print("EXITOSO (200 OK - Perfil actualizado y token renovado)")

    # 5. Usuario normal intenta acceder a /users (debe retornar 403 Forbidden)
    print("[TEST 05] GET /users (Usuario normal sin permisos -> 403 Forbidden)...", end=" ")
    r = client.get("/users", headers=user_headers)
    assert r.status_code == 403, f"Esperado 403, recibido: {r.status_code}"
    print("EXITOSO (403 Forbidden - Control RBAC verificado)")

    # 6. Login de Administrador por defecto
    print("[TEST 06] POST /login (Admin: 'admin' / 'Admin123*')...", end=" ")
    r = client.post("/login", json_data={"username": "admin", "password": "Admin123*"})
    assert r.status_code == 200, f"Fallo: {r.text}"
    admin_token = r.json().get("token")
    admin_headers = {"Authorization": f"Bearer {admin_token}"}
    print("EXITOSO (200 OK - Sesión de Administrador activa)")

    # 7. Admin lista usuarios
    print("[TEST 07] GET /users (Admin lista todos los usuarios)...", end=" ")
    r = client.get("/users", headers=admin_headers)
    assert r.status_code == 200, f"Fallo: {r.text}"
    assert len(r.json().get("users", [])) >= 2
    print("EXITOSO (200 OK)")

    # 8. CRUD de Tareas (Conservación P2)
    print("[TEST 08] CRUD Tareas (POST, GET, PUT, DELETE /tasks)...", end=" ")
    r = client.post("/tasks", json_data={"title": "Tarea de Prueba P5", "description": "Comprobar persistencia"}, headers=user_headers)
    assert r.status_code == 201, f"Fallo creación: {r.text}"
    task_id = r.json().get("task")["id"]

    r = client.get("/tasks", headers=user_headers)
    assert r.status_code == 200 and len(r.json().get("tasks")) >= 1

    r = client.put(f"/tasks/{task_id}", json_data={"status": "completed"}, headers=user_headers)
    assert r.status_code == 200

    r = client.delete(f"/tasks/{task_id}", headers=user_headers)
    assert r.status_code == 200
    print("EXITOSO (CRUD de Tareas 100% operativo)")

    # 9. Autenticación con Google (Práctica 5)
    print("[TEST 09] POST /auth/google (Simulación de ingreso OAuth con Google)...", end=" ")
    r = client.post("/auth/google", json_data={
        "email": f"estudiante_{timestamp}@alumno.ipn.mx",
        "name": "Estudiante ESCOM",
        "google_id": f"g_{timestamp}"
    })
    assert r.status_code == 200, f"Fallo: {r.text}"
    google_token = r.json().get("token")
    assert google_token and r.json().get("auth_provider") == "google"
    print("EXITOSO (200 OK - Cuenta vinculada y JWT emitido)")

    # 10. Historial de Búsquedas (Práctica 5 - Ejercicio 2.1 y 3.1)
    print("[TEST 10] Historial de Búsquedas (POST, GET, DELETE /history)...", end=" ")
    r = client.post("/history", json_data={"query": "Breaking Bad", "category": "tv_shows"}, headers=user_headers)
    assert r.status_code == 201, f"Fallo: {r.text}"
    r = client.post("/history", json_data={"query": "Stranger Things", "category": "tv_shows"}, headers=user_headers)
    assert r.status_code == 201

    r = client.get("/history", headers=user_headers)
    assert r.status_code == 200
    history_items = r.json().get("history", [])
    assert len(history_items) >= 2
    assert history_items[0]["query"] == "Stranger Things"  # Orden descendente

    r = client.delete("/history", headers=user_headers)
    assert r.status_code == 200
    r = client.get("/history", headers=user_headers)
    assert len(r.json().get("history", [])) == 0
    print("EXITOSO (Historial registrado, listado y limpiado)")

    # 11. Gestión de Favoritos (Práctica 5 - Ejercicio 2 y 3)
    print("[TEST 11] Favoritos (POST, GET, DELETE /favorites)...", end=" ")
    r = client.post("/favorites", json_data={
        "item_id": "139",
        "title": "Girls",
        "genres": "Drama, Romance, Comedy",
        "rating": "6.6",
        "image_url": "https://static.tvmaze.com/uploads/images/medium_portrait/31/78286.jpg",
        "summary": "Six seasons about four twenty-somethings in NYC."
    }, headers=user_headers)
    assert r.status_code == 201, f"Fallo: {r.text}"

    # Idempotencia: añadir el mismo show no debe duplicar
    r_dup = client.post("/favorites", json_data={"item_id": "139", "title": "Girls"}, headers=user_headers)
    assert r_dup.status_code == 200

    r = client.get("/favorites", headers=user_headers)
    assert r.status_code == 200
    favs = r.json().get("favorites", [])
    assert len(favs) == 1
    assert favs[0]["item_id"] == "139"

    r = client.delete("/favorites/139", headers=user_headers)
    assert r.status_code == 200
    print("EXITOSO (Favoritos creados, idempotentes y eliminados)")

    # 12. Sincronización en lote de cola offline (Práctica 5 - Ejercicio 1.2)
    print("[TEST 12] POST /sync/favorites (Resolución de cola offline sin duplicados)...", end=" ")
    sync_payload = {
        "actions": [
            {"action": "add", "item_id": "169", "title": "Breaking Bad", "genres": "Drama, Crime, Thriller", "rating": "9.2"},
            {"action": "add", "item_id": "2993", "title": "Stranger Things", "genres": "Drama, Fantasy, Horror, Sci-Fi", "rating": "8.6"},
            {"action": "add", "item_id": "169", "title": "Breaking Bad Duplicado"},  # Acción repetida
            {"action": "remove", "item_id": "99999"}  # Eliminación inexistente
        ]
    }
    r = client.post("/sync/favorites", json_data=sync_payload, headers=user_headers)
    assert r.status_code == 200, f"Fallo: {r.text}"
    res = r.json()
    assert res.get("processed_count") == 4
    # Debe haber exactamente 2 favoritos (Breaking Bad y Stranger Things)
    assert len(res.get("favorites", [])) == 2
    print("EXITOSO (Cola offline procesada idempotentemente sin duplicados)")

    # 13. Estado de Sincronización
    print("[TEST 13] GET /sync/status (Métricas y estampa de sincronización)...", end=" ")
    r = client.get("/sync/status", headers=user_headers)
    assert r.status_code == 200
    assert "server_time" in r.json() and r.json()["counts"]["favorites"] == 2
    print("EXITOSO (200 OK - Estampa y conteos devueltos)")

    # 14. Auditoría Administrativa (Solo Admin)
    print("[TEST 14] Auditoría Admin (GET /admin/users/<id>/history y /favorites)...", end=" ")
    # Añadir una búsqueda previa para el usuario
    client.post("/history", json_data={"query": "Auditoría Show", "category": "tv_shows"}, headers=user_headers)

    r_hist = client.get(f"/admin/users/{user_id}/history", headers=admin_headers)
    assert r_hist.status_code == 200, f"Fallo admin historial: {r_hist.text}"
    assert len(r_hist.json().get("history", [])) >= 1

    r_fav = client.get(f"/admin/users/{user_id}/favorites", headers=admin_headers)
    assert r_fav.status_code == 200, f"Fallo admin favoritos: {r_fav.text}"
    assert len(r_fav.json().get("favorites", [])) == 2
    print("EXITOSO (200 OK - Admin visualiza datos de otros usuarios)")

    # 15. Verificación de Seguridad RBAC en Auditoría (Usuario común rechazado con 403)
    print("[TEST 15] RBAC Auditoría (Usuario normal intenta ver historial de otro -> 403)...", end=" ")
    r_unauth = client.get("/admin/users/1/history", headers=user_headers)
    assert r_unauth.status_code == 403, f"Esperado 403, recibido: {r_unauth.status_code}"
    r_unauth_fav = client.get("/admin/users/1/favorites", headers=user_headers)
    assert r_unauth_fav.status_code == 403, f"Esperado 403, recibido: {r_unauth_fav.status_code}"
    print("EXITOSO (403 Forbidden - Backend impide acceso no autorizado)")

    print("\n" + "=" * 80)
    print("¡TODAS LAS PRUEBAS DE LA API REST (15 DE 15) PASARON EXITOSAMENTE (100%)!")
    print("=" * 80)

if __name__ == '__main__':
    run_tests()
