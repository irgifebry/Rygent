from functools import wraps
from flask import request, jsonify
from config import Config

def require_token(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        auth_header = request.headers.get('Authorization')
        if auth_header:
            try:
                parts = auth_header.split(None, 1)
                if len(parts) == 2 and parts[0].lower() == 'bearer':
                    if parts[1].strip() == Config.AUTH_TOKEN.strip():
                        return f(*args, **kwargs)
            except Exception:
                pass

        custom_token = request.headers.get('X-Token')
        if custom_token and custom_token.strip() == Config.AUTH_TOKEN.strip():
            return f(*args, **kwargs)

        return jsonify({"error": "Unauthorized"}), 401
    return decorated_function
