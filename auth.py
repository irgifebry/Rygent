from functools import wraps
from flask import request, jsonify
from config import Config

def require_token(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        # 1. Check Authorization (Bearer) header
        auth_header = request.headers.get('Authorization')
        if auth_header:
            try:
                parts = auth_header.split(None, 1)
                if len(parts) == 2 and parts[0].lower() == 'bearer':
                    if parts[1].strip() == Config.AUTH_TOKEN.strip():
                        return f(*args, **kwargs)
            except: pass
        
        # 2. Check X-Token header (custom)
        custom_token = request.headers.get('X-Token')
        if custom_token and custom_token.strip() == Config.AUTH_TOKEN.strip():
            return f(*args, **kwargs)
            
        # 3. Check 'token' query parameter (for easy pairing/checks)
        query_token = request.args.get('token')
        if query_token and query_token.strip() == Config.AUTH_TOKEN.strip():
            return f(*args, **kwargs)
            
        # If all failed, log and return 401
        print(f"Auth Failed: No valid token in headers or query params.", flush=True)
        return jsonify({"error": "Unauthorized"}), 401
    return decorated_function
