import subprocess
import threading
import time
import os
import json
import re
import signal

RYGENT_DIR = os.path.expanduser("~\\.rygent")
TUNNEL_JSON = os.path.join(RYGENT_DIR, "tunnel.json")
PID_FILE = os.path.join(RYGENT_DIR, "cloudflared.pid")

class TunnelManager:
    def __init__(self, port=5000):
        self.port = port
        self.process = None
        self.tunnel_url = None
        self._stop_event = threading.Event()
        self._owned_process = False 

    def _is_cloudflared_running(self):
        try:
            if os.path.exists(PID_FILE):
                with open(PID_FILE, "r") as f:
                    pid = int(f.read().strip())
                # On Windows, signal 0 doesn't check existence the same way as Linux
                # We use tasklist to check
                res = subprocess.run(["tasklist", "/FI", f"PID eq {pid}", "/NH"], capture_output=True, text=True)
                if "cloudflared" in res.stdout.lower():
                    return pid
        except:
            pass
        return None

    def _load_existing_tunnel(self):
        try:
            if os.path.exists(TUNNEL_JSON):
                with open(TUNNEL_JSON, "r") as f:
                    data = json.load(f)
                    return data.get("url")
        except: pass
        return None

    def start_tunnel(self, token=None, remote_hostname=None):
        existing_pid = self._is_cloudflared_running()
        if existing_pid:
            existing_url = self._load_existing_tunnel()
            if existing_url:
                self.tunnel_url = existing_url
                self._owned_process = False
                return
        
        self._start_new_tunnel(token, remote_hostname)

    def _start_new_tunnel(self, token=None, remote_hostname=None):
        def run():
            import shutil
            cf_path = shutil.which("cloudflared") or shutil.which("cloudflared.exe")
            if not cf_path:
                bundled = os.path.join(os.path.dirname(os.path.abspath(__file__)), "cloudflared.exe")
                if os.path.exists(bundled):
                    cf_path = bundled
                else:
                    print("[Tunnel] Error: cloudflared binary not found. Remote connectivity disabled.")
                    return

            try:
                if token:
                    cmd = [cf_path, "tunnel", "run", "--token", token]
                    if remote_hostname:
                        self.tunnel_url = f"https://{remote_hostname.replace('https://', '').replace('http://', '')}"
                else:
                    cmd = [cf_path, "tunnel", "--url", f"http://localhost:{self.port}"]
                
                # Windows specific process flags
                self.process = subprocess.Popen(
                    cmd,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    text=True,
                    bufsize=1,
                    creationflags=subprocess.CREATE_NEW_PROCESS_GROUP
                )
                self._owned_process = True
                
                os.makedirs(RYGENT_DIR, exist_ok=True)
                with open(PID_FILE, "w") as f:
                    f.write(str(self.process.pid))

                url_pattern = re.compile(r"https://[a-zA-Z0-9-]+\.trycloudflare\.com")

                for line in iter(self.process.stdout.readline, ''):
                    if self._stop_event.is_set(): break
                    match = url_pattern.search(line)
                    if match:
                        self.tunnel_url = match.group(0)
                        self._save_tunnel_info()
                    if "Connected" in line and token:
                         self._save_tunnel_info()
                self.process.wait()
            except Exception as e:
                print(f"[Tunnel] Error: {e}")

        threading.Thread(target=run, daemon=True).start()

    def stop_tunnel(self):
        self._stop_event.set()
        if self._owned_process and self.process:
            self.process.terminate()
            try: os.remove(PID_FILE)
            except: pass

    def force_stop_tunnel(self):
        self._stop_event.set()
        if self.process:
            self.process.kill()
        try:
            if os.path.exists(PID_FILE):
                with open(PID_FILE, "r") as f:
                    pid = int(f.read().strip())
                subprocess.run(["taskkill", "/F", "/PID", str(pid)], capture_output=True)
                os.remove(PID_FILE)
        except: pass

    def _save_tunnel_info(self):
        try:
            os.makedirs(RYGENT_DIR, exist_ok=True)
            with open(TUNNEL_JSON, "w") as f:
                json.dump({"url": self.tunnel_url, "timestamp": time.time(), "pid": self.process.pid if self.process else None}, f)
        except: pass

tunnel_manager = TunnelManager()
