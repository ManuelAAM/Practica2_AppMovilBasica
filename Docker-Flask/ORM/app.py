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
    created_at = db.Column(db.DateTime, default=lambda: datetime.now(timezone.utc))

    tasks = db.relationship('Task', backref='owner', lazy=True, cascade='all, delete-orphan')

    def to_dict(self):
        return {
            'id': self.id,
            'username': self.username,
            'role': self.role,
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
        "message": "API REST de Autenticación, Roles y CRUD en funcionamiento",
        "version": "2.0.0"
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
    # Nuevos usuarios registrados desde la app son por defecto 'user'
    new_user = User(username=username, password=hashed_password, role='user')
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

    # Generar token renovado con la información actualizada
    now = datetime.now(timezone.utc)
    token = jwt.encode({
        'user_id': current_user.id,
        'username': current_user.username,
        'role': current_user.role,
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
    new_user = User(username=username, password=hashed_password, role=role)
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
# Rutas CRUD de Tareas (Recurso complementario)
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

    # Sembrar usuario administrador por defecto si no existe
    admin_user = User.query.filter_by(username='admin').first()
    if not admin_user:
        admin_pass_hash = bcrypt.generate_password_hash('Admin123*').decode('utf-8')
        default_admin = User(username='admin', password=admin_pass_hash, role='admin')
        db.session.add(default_admin)
        db.session.commit()
        print("[BD INFO] Administrador creado por defecto: 'admin' (Clave: 'Admin123*').")
    else:
        # Asegurar que tenga rol admin
        if admin_user.role != 'admin':
            admin_user.role = 'admin'
            db.session.commit()

if __name__ == '__main__':
    with app.app_context():
        init_database()

    port = int(os.getenv('PORT', 5000))
    debug = os.getenv('FLASK_DEBUG', 'True').lower() in ('true', '1')
    app.run(host='0.0.0.0', port=port, debug=debug)