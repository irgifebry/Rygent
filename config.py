import os
from dotenv import load_dotenv

# Load environment variables from .env file
# Check multiple locations in priority order
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
    load_dotenv()  # Try CWD as last resort

class Config:
    PORT = int(os.getenv("PORT", 5000))
    HOST = os.getenv("HOST", "0.0.0.0")
    PUBLIC_ADDRESS = os.getenv("PUBLIC_ADDRESS", "")
    AUTH_TOKEN = os.getenv("AUTH_TOKEN", "debug_token_123")
    CACHE_TTL = int(os.getenv("CACHE_TTL", 1))
    TOP_PROCESS_COUNT = int(os.getenv("TOP_PROCESS_COUNT", 10))
    TUNNEL_TOKEN = os.getenv("TUNNEL_TOKEN", "")
    REMOTE_HOSTNAME = os.getenv("REMOTE_HOSTNAME", "")
