import os
import functools
from datetime import datetime, timedelta, timezone
from flask import Flask, jsonify, request
from flask_sqlalchemy import SQLAlchemy
from flask_bcrypt import Bcrypt
import jwt
from dotenv import load_dotenv

# Cargar variables de entorno desde .env
load_dotenv()

app = Flask(__name__)

# Configuración desde variables de entorno con soporte híbrido (PostgreSQL 17 / SQLite)
app.config['SECRET_KEY'] = os.getenv('SECRET_KEY', 'super_secret_jwt_key_escom_practica_2_2026')

raw_db_uri = os.getenv('DATABASE_URL', 'sqlite:///site.db')
if raw_db_uri.startswith('postgres://'):
    raw_db_uri = raw_db_uri.replace('postgres://', 'postgresql://', 1)

app.config['SQLALCHEMY_DATABASE_URI'] = raw_db_uri
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

db = SQLAlchemy(app)
bcrypt = Bcrypt(app)

# ----------------------------------------------------------------------
# Modelos de Base de Datos
# ----------------------------------------------------------------------
class User(db.Model):
    __tablename__ = 'users'
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(50), unique=True, nullable=False)
    password = db.Column(db.String(128), nullable=False)
    role = db.Column(db.String(20), default='user', nullable=False)  # 'user' | 'admin'
    auth_provider = db.Column(db.String(20), default='native', nullable=False)  # 'native' | 'google'
    created_at = db.Column(db.DateTime, default=lambda: datetime.now(timezone.utc))

    tasks = db.relationship('Task', backref='owner', lazy=True, cascade='all, delete-orphan')
    search_history = db.relationship('SearchHistory', backref='owner', lazy=True, cascade='all, delete-orphan')
    favorites = db.relationship('Favorite', backref='owner', lazy=True, cascade='all, delete-orphan')

    def to_dict(self):
        return {
            'id': self.id,
            'username': self.username,
            'role': self.role,
            'auth_provider': self.auth_provider,
            'created_at': self.created_at.strftime('%Y-%m-%d %H:%M:%S') if self.created_at else None
        }

    def __repr__(self):
        return f"<User {self.username} ({self.role})>"

class Task(db.Model):
    __tablename__ = 'tasks'
    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(100), nullable=False)
    description = db.Column(db.String(255), nullable=True)
    status = db.Column(db.String(20), default='pending')  # 'pending' | 'completed'
    created_at = db.Column(db.DateTime, default=lambda: datetime.now(timezone.utc))
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)

    def to_dict(self):
        return {
            'id': self.id,
            'title': self.title,
            'description': self.description or '',
            'status': self.status,
            'created_at': self.created_at.strftime('%Y-%m-%d %H:%M:%S') if self.created_at else None,
            'user_id': self.user_id
        }

class SearchHistory(db.Model):
    """Historial de búsquedas realizadas por el usuario en la API pública (TVMaze)."""
    __tablename__ = 'search_history'
    id = db.Column(db.Integer, primary_key=True)
    search_term = db.Column(db.String(100), nullable=False)
    category = db.Column(db.String(50), default='tv_shows')
    timestamp = db.Column(db.DateTime, default=lambda: datetime.now(timezone.utc))
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)

    def to_dict(self):
        return {
            'id': self.id,
            'query': self.search_term,
            'category': self.category,
            'timestamp': self.timestamp.strftime('%Y-%m-%d %H:%M:%S') if self.timestamp else None,
            'user_id': self.user_id
        }

class Favorite(db.Model):
    """Favoritos de series guardados por el usuario desde la API pública."""
    __tablename__ = 'favorites'
    id = db.Column(db.Integer, primary_key=True)
    item_id = db.Column(db.String(100), nullable=False)  # ID TVMaze (ej. "139")
    title = db.Column(db.String(150), nullable=False)
    genres = db.Column(db.String(150), default='')
    rating = db.Column(db.String(20), default='')
    image_url = db.Column(db.String(255), default='')
    summary = db.Column(db.Text, default='')
    created_at = db.Column(db.DateTime, default=lambda: datetime.now(timezone.utc))
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)

    __table_args__ = (
        db.UniqueConstraint('user_id', 'item_id', name='uq_user_favorite_item'),
    )

    def to_dict(self):
        return {
            'id': self.id,
            'item_id': self.item_id,
            'title': self.title,
            'genres': self.genres or '',
            'rating': self.rating or '',
            'image_url': self.image_url or '',
            'summary': self.summary or '',
            'created_at': self.created_at.strftime('%Y-%m-%d %H:%M:%S') if self.created_at else None,
            'user_id': self.user_id
        }

# ----------------------------------------------------------------------
# Decoradores de Autenticación y Control de Roles (RBAC)
# ----------------------------------------------------------------------
def token_required(f):
    @functools.wraps(f)
    def decorated(*args, **kwargs):
        auth_header = request.headers.get('Authorization')
        if not auth_header:
            return jsonify({"status": "error", "message": "Token de autorización requerido"}), 401

        parts = auth_header.split()
        if len(parts) != 2 or parts[0].lower() != 'bearer':
            return jsonify({"status": "error", "message": "Formato de autorización inválido. Debe ser: Bearer <token>"}), 401

        token = parts[1]
        try:
            payload = jwt.decode(token, app.config['SECRET_KEY'], algorithms=['HS256'])
            current_user = User.query.get(payload.get('user_id'))
            if not current_user:
                return jsonify({"status": "error", "message": "Usuario inexistente o inválido"}), 401
        except jwt.ExpiredSignatureError:
            return jsonify({"status": "error", "message": "El token ha expirado. Por favor, inicie sesión nuevamente"}), 401
        except jwt.InvalidTokenError:
            return jsonify({"status": "error", "message": "Token inválido o corrupto"}), 401

        return f(current_user, *args, **kwargs)
    return decorated

def admin_required(f):
    @functools.wraps(f)
    def decorated(current_user, *args, **kwargs):
        if current_user.role != 'admin':
            return jsonify({
                "status": "error",
                "message": "Acceso denegado: se requieren privilegios de Administrador para esta operación"
            }), 403
        return f(current_user, *args, **kwargs)
    return decorated

# ----------------------------------------------------------------------
# Rutas de Estado y Autenticación
# ----------------------------------------------------------------------
@app.route('/', methods=['GET'])
def health_check():
    return jsonify({
        "status": "success",
        "message": "API REST Multiplataforma KMP (Práctica 5 - Continuación P2) en funcionamiento",
        "version": "5.0.0"
    }), 200

@app.route('/register', methods=['POST'])
def register():
    data = request.get_json(silent=True) or {}
    username = (data.get('username') or '').strip()
    password = data.get('password') or ''

    if not username or not password:
        return jsonify({"status": "error", "message": "Usuario y contraseña requeridos"}), 400

    if len(username) < 3:
        return jsonify({"status": "error", "message": "El nombre de usuario debe tener al menos 3 caracteres"}), 400

    if len(password) < 4:
        return jsonify({"status": "error", "message": "La contraseña debe tener al menos 4 caracteres"}), 400

    if User.query.filter_by(username=username).first():
        return jsonify({"status": "error", "message": "El nombre de usuario ya está registrado"}), 400

    hashed_password = bcrypt.generate_password_hash(password).decode('utf-8')
    new_user = User(username=username, password=hashed_password, role='user', auth_provider='native')
    db.session.add(new_user)
    db.session.commit()

    return jsonify({
        "status": "success",
        "message": "Usuario creado exitosamente",
        "user_id": new_user.id,
        "username": new_user.username,
        "role": new_user.role
    }), 201

@app.route('/login', methods=['POST'])
def login():
    data = request.get_json(silent=True) or {}
    username = (data.get('username') or '').strip()
    password = data.get('password') or ''

    if not username or not password:
        return jsonify({"status": "error", "message": "Usuario y contraseña requeridos"}), 400

    user = User.query.filter_by(username=username).first()

    if user and bcrypt.check_password_hash(user.password, password):
        now = datetime.now(timezone.utc)
        payload = {
            'user_id': user.id,
            'username': user.username,
            'role': user.role,
            'auth_provider': user.auth_provider,
            'iat': now,
            'exp': now + timedelta(hours=24)
        }
        token = jwt.encode(payload, app.config['SECRET_KEY'], algorithm='HS256')

        return jsonify({
            "status": "success",
            "message": "Inicio de sesión exitoso",
            "token": token,
            "user_id": user.id,
            "username": user.username,
            "role": user.role
        }), 200
    else:
        return jsonify({"status": "error", "message": "Credenciales inválidas"}), 401

@app.route('/auth/google', methods=['POST'])
def google_auth():
    """
    Endpoint de integración OAuth con Google (Práctica 5).
    Recibe los datos del usuario autenticado vía Google Sign-In,
    lo crea si no existe (con rol user) y emite un JWT firmado válido por 24 horas.
    """
    data = request.get_json(silent=True) or {}
    email = (data.get('email') or '').strip().lower()
    name = (data.get('name') or '').strip()
    google_id = (data.get('google_id') or '').strip()
    id_token = data.get('id_token') or ''

    if not email and not google_id and not id_token:
        return jsonify({"status": "error", "message": "Credenciales o token de Google requeridos"}), 400

    base_name = email.split('@')[0] if email else (name if name else f"google_{google_id[:6]}")
    clean_username = "".join(c for c in base_name if c.isalnum() or c in ('_', '-'))
    if len(clean_username) < 3:
        clean_username = f"usr_{clean_username}_{google_id[:4] if google_id else 'g'}"

    user = User.query.filter_by(username=clean_username).first()
    if not user:
        random_pwd = os.urandom(16).hex()
        hashed_pwd = bcrypt.generate_password_hash(random_pwd).decode('utf-8')
        user = User(username=clean_username, password=hashed_pwd, role='user', auth_provider='google')
        db.session.add(user)
        db.session.commit()

    now = datetime.now(timezone.utc)
    payload = {
        'user_id': user.id,
        'username': user.username,
        'role': user.role,
        'auth_provider': 'google',
        'iat': now,
        'exp': now + timedelta(hours=24)
    }
    token = jwt.encode(payload, app.config['SECRET_KEY'], algorithm='HS256')

    return jsonify({
        "status": "success",
        "message": "Autenticación con Google exitosa",
        "token": token,
        "user_id": user.id,
        "username": user.username,
        "role": user.role,
        "auth_provider": "google"
    }), 200

# ----------------------------------------------------------------------
# Rutas de Perfil (Para Usuario Normal y Admin sobre sus propios datos)
# ----------------------------------------------------------------------
@app.route('/profile', methods=['GET'])
@token_required
def get_profile(current_user):
    return jsonify({
        "status": "success",
        "user": current_user.to_dict()
    }), 200

@app.route('/profile', methods=['PUT'])
@token_required
def update_profile(current_user):
    data = request.get_json(silent=True) or {}
    new_username = (data.get('username') or '').strip()
    new_password = data.get('password') or ''

    if new_username and new_username != current_user.username:
        if len(new_username) < 3:
            return jsonify({"status": "error", "message": "El nombre de usuario debe tener al menos 3 caracteres"}), 400
        existing = User.query.filter_by(username=new_username).first()
        if existing and existing.id != current_user.id:
            return jsonify({"status": "error", "message": "El nombre de usuario ya está registrado"}), 400
        current_user.username = new_username

    if new_password:
        if len(new_password) < 4:
            return jsonify({"status": "error", "message": "La contraseña debe tener al menos 4 caracteres"}), 400
        current_user.password = bcrypt.generate_password_hash(new_password).decode('utf-8')

    db.session.commit()

    now = datetime.now(timezone.utc)
    token = jwt.encode({
        'user_id': current_user.id,
        'username': current_user.username,
        'role': current_user.role,
        'auth_provider': current_user.auth_provider,
        'iat': now,
        'exp': now + timedelta(hours=24)
    }, app.config['SECRET_KEY'], algorithm='HS256')

    return jsonify({
        "status": "success",
        "message": "Tu información ha sido actualizada exitosamente",
        "token": token,
        "user": current_user.to_dict()
    }), 200

# ----------------------------------------------------------------------
# Rutas CRUD de Administración de Usuarios (Solo Administrador)
# ----------------------------------------------------------------------
@app.route('/users', methods=['GET'])
@token_required
@admin_required
def admin_get_all_users(current_user):
    users = User.query.order_by(User.id.asc()).all()
    return jsonify({
        "status": "success",
        "users": [u.to_dict() for u in users]
    }), 200

@app.route('/users/<int:user_id>', methods=['GET'])
@token_required
@admin_required
def admin_get_user_by_id(current_user, user_id):
    user = User.query.get(user_id)
    if not user:
        return jsonify({"status": "error", "message": "Usuario no encontrado"}), 404

    return jsonify({
        "status": "success",
        "user": user.to_dict()
    }), 200

@app.route('/users', methods=['POST'])
@token_required
@admin_required
def admin_create_user(current_user):
    data = request.get_json(silent=True) or {}
    username = (data.get('username') or '').strip()
    password = data.get('password') or ''
    role = data.get('role', 'user')

    if not username or not password:
        return jsonify({"status": "error", "message": "Usuario y contraseña requeridos"}), 400

    if len(username) < 3:
        return jsonify({"status": "error", "message": "El nombre de usuario debe tener al menos 3 caracteres"}), 400

    if len(password) < 4:
        return jsonify({"status": "error", "message": "La contraseña debe tener al menos 4 caracteres"}), 400

    if role not in ('user', 'admin'):
        return jsonify({"status": "error", "message": "El rol debe ser 'user' o 'admin'"}), 400

    if User.query.filter_by(username=username).first():
        return jsonify({"status": "error", "message": "El nombre de usuario ya está registrado"}), 400

    hashed_password = bcrypt.generate_password_hash(password).decode('utf-8')
    new_user = User(username=username, password=hashed_password, role=role, auth_provider='native')
    db.session.add(new_user)
    db.session.commit()

    return jsonify({
        "status": "success",
        "message": f"Usuario '{new_user.username}' ({role}) creado exitosamente",
        "user": new_user.to_dict()
    }), 201

@app.route('/users/<int:user_id>', methods=['PUT'])
@token_required
@admin_required
def admin_update_user(current_user, user_id):
    user = User.query.get(user_id)
    if not user:
        return jsonify({"status": "error", "message": "Usuario no encontrado"}), 404

    data = request.get_json(silent=True) or {}

    if 'username' in data:
        new_username = data['username'].strip()
        if len(new_username) < 3:
            return jsonify({"status": "error", "message": "El nombre de usuario debe tener al menos 3 caracteres"}), 400
        existing = User.query.filter_by(username=new_username).first()
        if existing and existing.id != user.id:
            return jsonify({"status": "error", "message": "El nombre de usuario ya está registrado"}), 400
        user.username = new_username

    if 'password' in data and data['password']:
        if len(data['password']) < 4:
            return jsonify({"status": "error", "message": "La contraseña debe tener al menos 4 caracteres"}), 400
        user.password = bcrypt.generate_password_hash(data['password']).decode('utf-8')

    if 'role' in data:
        role = data['role']
        if role not in ('user', 'admin'):
            return jsonify({"status": "error", "message": "El rol debe ser 'user' o 'admin'"}), 400
        user.role = role

    db.session.commit()

    return jsonify({
        "status": "success",
        "message": f"Usuario '{user.username}' actualizado exitosamente",
        "user": user.to_dict()
    }), 200

@app.route('/users/<int:user_id>', methods=['DELETE'])
@token_required
@admin_required
def admin_delete_user(current_user, user_id):
    if current_user.id == user_id:
        return jsonify({"status": "error", "message": "No puedes eliminar tu propia cuenta de administrador"}), 400

    user = User.query.get(user_id)
    if not user:
        return jsonify({"status": "error", "message": "Usuario no encontrado"}), 404

    db.session.delete(user)
    db.session.commit()

    return jsonify({
        "status": "success",
        "message": f"Usuario '{user.username}' eliminado exitosamente"
    }), 200

# ----------------------------------------------------------------------
# Rutas CRUD de Tareas (Recurso complementario de Práctica 2 conservado)
# ----------------------------------------------------------------------
@app.route('/tasks', methods=['GET'])
@token_required
def get_tasks(current_user):
    tasks = Task.query.filter_by(user_id=current_user.id).order_by(Task.created_at.desc()).all()
    return jsonify({
        "status": "success",
        "tasks": [t.to_dict() for t in tasks]
    }), 200

@app.route('/tasks/<int:task_id>', methods=['GET'])
@token_required
def get_task(current_user, task_id):
    task = Task.query.filter_by(id=task_id, user_id=current_user.id).first()
    if not task:
        return jsonify({"status": "error", "message": "Tarea no encontrada"}), 404

    return jsonify({
        "status": "success",
        "task": task.to_dict()
    }), 200

@app.route('/tasks', methods=['POST'])
@token_required
def create_task(current_user):
    data = request.get_json(silent=True) or {}
    title = (data.get('title') or '').strip()
    description = (data.get('description') or '').strip()
    status = data.get('status', 'pending')

    if not title:
        return jsonify({"status": "error", "message": "El título de la tarea es obligatorio"}), 400

    new_task = Task(
        title=title,
        description=description,
        status=status,
        user_id=current_user.id
    )
    db.session.add(new_task)
    db.session.commit()

    return jsonify({
        "status": "success",
        "message": "Tarea creada exitosamente",
        "task": new_task.to_dict()
    }), 201

@app.route('/tasks/<int:task_id>', methods=['PUT'])
@token_required
def update_task(current_user, task_id):
    task = Task.query.filter_by(id=task_id, user_id=current_user.id).first()
    if not task:
        return jsonify({"status": "error", "message": "Tarea no encontrada"}), 404

    data = request.get_json(silent=True) or {}
    if 'title' in data:
        title = data['title'].strip()
        if not title:
            return jsonify({"status": "error", "message": "El título no puede estar vacío"}), 400
        task.title = title

    if 'description' in data:
        task.description = data['description'].strip()

    if 'status' in data:
        task.status = data['status']

    db.session.commit()

    return jsonify({
        "status": "success",
        "message": "Tarea actualizada exitosamente",
        "task": task.to_dict()
    }), 200

@app.route('/tasks/<int:task_id>', methods=['DELETE'])
@token_required
def delete_task(current_user, task_id):
    task = Task.query.filter_by(id=task_id, user_id=current_user.id).first()
    if not task:
        return jsonify({"status": "error", "message": "Tarea no encontrada"}), 404

    db.session.delete(task)
    db.session.commit()

    return jsonify({
        "status": "success",
        "message": "Tarea eliminada exitosamente"
    }), 200

# ----------------------------------------------------------------------
# Rutas de Historial de Búsquedas (Práctica 5 - Ejercicio 3.1)
# ----------------------------------------------------------------------
@app.route('/history', methods=['GET'])
@token_required
def get_user_search_history(current_user):
    """Consulta del historial de búsquedas del usuario autenticado."""
    history = SearchHistory.query.filter_by(user_id=current_user.id).order_by(SearchHistory.timestamp.desc()).limit(50).all()
    return jsonify({
        "status": "success",
        "history": [h.to_dict() for h in history]
    }), 200

@app.route('/history', methods=['POST'])
@token_required
def add_user_search_history(current_user):
    """Registra una búsqueda realizada por el usuario en la API pública."""
    data = request.get_json(silent=True) or {}
    query = (data.get('query') or '').strip()
    category = (data.get('category') or 'tv_shows').strip()

    if not query:
        return jsonify({"status": "error", "message": "El término de búsqueda es requerido"}), 400

    entry = SearchHistory(search_term=query, category=category, user_id=current_user.id)
    db.session.add(entry)
    db.session.commit()

    return jsonify({
        "status": "success",
        "message": "Búsqueda registrada en el historial",
        "entry": entry.to_dict()
    }), 201

@app.route('/history', methods=['DELETE'])
@token_required
def clear_user_search_history(current_user):
    """Limpia el historial de búsquedas del usuario actual."""
    SearchHistory.query.filter_by(user_id=current_user.id).delete()
    db.session.commit()
    return jsonify({
        "status": "success",
        "message": "Historial de búsquedas eliminado exitosamente"
    }), 200

# ----------------------------------------------------------------------
# Rutas de Favoritos (Práctica 5 - Ejercicio 2 y 3)
# ----------------------------------------------------------------------
@app.route('/favorites', methods=['GET'])
@token_required
def get_user_favorites(current_user):
    """Consulta de la lista de series favoritas del usuario autenticado."""
    favs = Favorite.query.filter_by(user_id=current_user.id).order_by(Favorite.created_at.desc()).all()
    return jsonify({
        "status": "success",
        "favorites": [f.to_dict() for f in favs]
    }), 200

@app.route('/favorites', methods=['POST'])
@token_required
def add_user_favorite(current_user):
    """Añade un show a favoritos (operación idempotente)."""
    data = request.get_json(silent=True) or {}
    item_id = str(data.get('item_id') or '').strip()
    title = (data.get('title') or '').strip()

    if not item_id or not title:
        return jsonify({"status": "error", "message": "item_id y title son obligatorios"}), 400

    existing = Favorite.query.filter_by(user_id=current_user.id, item_id=item_id).first()
    if existing:
        return jsonify({
            "status": "success",
            "message": "El elemento ya se encuentra en favoritos",
            "favorite": existing.to_dict()
        }), 200

    new_fav = Favorite(
        item_id=item_id,
        title=title,
        genres=data.get('genres', ''),
        rating=str(data.get('rating', '')),
        image_url=data.get('image_url', ''),
        summary=data.get('summary', ''),
        user_id=current_user.id
    )
    db.session.add(new_fav)
    db.session.commit()

    return jsonify({
        "status": "success",
        "message": "Añadido a favoritos exitosamente",
        "favorite": new_fav.to_dict()
    }), 201

@app.route('/favorites/<string:item_id>', methods=['DELETE'])
@token_required
def remove_user_favorite(current_user, item_id):
    """Elimina un show de favoritos."""
    fav = Favorite.query.filter_by(user_id=current_user.id, item_id=str(item_id)).first()
    if not fav:
        return jsonify({"status": "error", "message": "Elemento no encontrado en favoritos"}), 404

    db.session.delete(fav)
    db.session.commit()
    return jsonify({
        "status": "success",
        "message": "Eliminado de favoritos exitosamente"
    }), 200

# ----------------------------------------------------------------------
# Sincronización en Lote para Cola Offline (Práctica 5 - Ejercicio 1.2)
# ----------------------------------------------------------------------
@app.route('/sync/favorites', methods=['POST'])
@token_required
def sync_offline_favorites(current_user):
    """
    Endpoint para resolución de la cola de acciones pendientes generadas en modo offline.
    Recibe un arreglo con las acciones 'add' y 'remove' acumuladas sin conexión.
    Aplica las operaciones de forma idempotente, evitando duplicados.
    """
    data = request.get_json(silent=True) or {}
    actions = data.get('actions', [])
    processed_count = 0

    for act in actions:
        action_type = act.get('action')
        item_id = str(act.get('item_id') or '').strip()
        if not item_id:
            continue

        if action_type == 'add':
            existing = Favorite.query.filter_by(user_id=current_user.id, item_id=item_id).first()
            if not existing:
                title = (act.get('title') or f"Show {item_id}").strip()
                new_fav = Favorite(
                    item_id=item_id,
                    title=title,
                    genres=act.get('genres', ''),
                    rating=str(act.get('rating', '')),
                    image_url=act.get('image_url', ''),
                    summary=act.get('summary', ''),
                    user_id=current_user.id
                )
                db.session.add(new_fav)
            processed_count += 1
        elif action_type == 'remove':
            Favorite.query.filter_by(user_id=current_user.id, item_id=item_id).delete()
            processed_count += 1

    db.session.commit()

    all_favs = Favorite.query.filter_by(user_id=current_user.id).order_by(Favorite.created_at.desc()).all()
    server_time = datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S UTC')

    return jsonify({
        "status": "success",
        "message": f"Sincronización completada. {processed_count} operaciones resueltas.",
        "processed_count": processed_count,
        "last_sync_timestamp": server_time,
        "favorites": [f.to_dict() for f in all_favs]
    }), 200

@app.route('/sync/status', methods=['GET'])
@token_required
def get_sync_status(current_user):
    """Devuelve la estampa temporal del servidor y métricas para verificación de sincronización."""
    server_time = datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S UTC')
    task_count = Task.query.filter_by(user_id=current_user.id).count()
    fav_count = Favorite.query.filter_by(user_id=current_user.id).count()
    history_count = SearchHistory.query.filter_by(user_id=current_user.id).count()
    return jsonify({
        "status": "success",
        "server_time": server_time,
        "counts": {
            "tasks": task_count,
            "favorites": fav_count,
            "history": history_count
        }
    }), 200

# ----------------------------------------------------------------------
# Rutas de Auditoría Administrativa (Solo Administrador con verificación RBAC)
# ----------------------------------------------------------------------
@app.route('/admin/users/<int:user_id>/history', methods=['GET'])
@token_required
@admin_required
def admin_get_user_history(current_user, user_id):
    """Permite al Administrador inspeccionar el historial de búsquedas de cualquier usuario."""
    target_user = User.query.get(user_id)
    if not target_user:
        return jsonify({"status": "error", "message": "Usuario no encontrado"}), 404

    history = SearchHistory.query.filter_by(user_id=user_id).order_by(SearchHistory.timestamp.desc()).all()
    return jsonify({
        "status": "success",
        "target_user": target_user.to_dict(),
        "history": [h.to_dict() for h in history]
    }), 200

@app.route('/admin/users/<int:user_id>/favorites', methods=['GET'])
@token_required
@admin_required
def admin_get_user_favorites(current_user, user_id):
    """Permite al Administrador inspeccionar los favoritos de cualquier usuario."""
    target_user = User.query.get(user_id)
    if not target_user:
        return jsonify({"status": "error", "message": "Usuario no encontrado"}), 404

    favorites = Favorite.query.filter_by(user_id=user_id).order_by(Favorite.created_at.desc()).all()
    return jsonify({
        "status": "success",
        "target_user": target_user.to_dict(),
        "favorites": [f.to_dict() for f in favorites]
    }), 200

# ----------------------------------------------------------------------
# Inicialización y Sembrado de Administrador por Defecto
# ----------------------------------------------------------------------
def init_database():
    try:
        db.create_all()
    except Exception as err:
        print(f"[BD INFO] No se pudo conectar a {app.config['SQLALCHEMY_DATABASE_URI']}: {err}")
        print("[BD INFO] Activando fallback transparente a SQLite local ('sqlite:///site.db')...")
        app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///site.db'
        try:
            db.engine.dispose()
        except Exception:
            pass
        db.init_app(app)
        db.create_all()

    # Migración transparente para agregar columnas nuevas si la BD ya existía
    try:
        with db.engine.connect() as conn:
            conn.execute(db.text("ALTER TABLE users ADD COLUMN auth_provider VARCHAR(20) DEFAULT 'native'"))
            conn.commit()
    except Exception:
        pass  # La columna ya existe

    # Sembrar usuario administrador por defecto si no existe
    admin_user = User.query.filter_by(username='admin').first()
    if not admin_user:
        admin_pass_hash = bcrypt.generate_password_hash('Admin123*').decode('utf-8')
        default_admin = User(username='admin', password=admin_pass_hash, role='admin', auth_provider='native')
        db.session.add(default_admin)
        db.session.commit()
        print("[BD INFO] Administrador creado por defecto: 'admin' (Clave: 'Admin123*').")
    else:
        if admin_user.role != 'admin':
            admin_user.role = 'admin'
            db.session.commit()

if __name__ == '__main__':
    with app.app_context():
        init_database()

    port = int(os.getenv('PORT', 5000))
    debug = os.getenv('FLASK_DEBUG', 'True').lower() in ('true', '1')
    app.run(host='0.0.0.0', port=port, debug=debug)
