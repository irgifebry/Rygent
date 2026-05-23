import os
import uuid
from dotenv import load_dotenv

_env_loaded = False
_env_locations = [
    "/etc/system-monitor/.env",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env"),
    os.path.expanduser("~/.config/rygent/.env"),
    os.path.expanduser("~/.rygent/.env"),
]

for _env_path in _env_locations:
    if os.path.exists(_env_path):
        load_dotenv(_env_path)
        _env_loaded = True
        break

if not _env_loaded:
    load_dotenv()

def _get_or_generate_token():
    existing = os.getenv("AUTH_TOKEN", "")
    if existing.strip():
        return existing.strip()
    generated = str(uuid.uuid4())
    env_path = os.path.expanduser("~/.rygent/.env")
    try:
        os.makedirs(os.path.dirname(env_path), exist_ok=True)
        with open(env_path, "a") as f:
            f.write(f"\nAUTH_TOKEN={generated}\n")
        print(f"[Config] No AUTH_TOKEN found. Generated and saved to {env_path}", flush=True)
    except Exception as e:
        print(f"[Config] Warning: Could not save generated AUTH_TOKEN: {e}", flush=True)
    return generated

class Config:
    PORT = int(os.getenv("PORT", 5000))
    HOST = os.getenv("HOST", "0.0.0.0")
    PUBLIC_ADDRESS = os.getenv("PUBLIC_ADDRESS", "")
    AUTH_TOKEN = _get_or_generate_token()
    CACHE_TTL = int(os.getenv("CACHE_TTL", 1))
    TOP_PROCESS_COUNT = int(os.getenv("TOP_PROCESS_COUNT", 10))
    TUNNEL_TOKEN = os.getenv("TUNNEL_TOKEN", "")
    REMOTE_HOSTNAME = os.getenv("REMOTE_HOSTNAME", "")
