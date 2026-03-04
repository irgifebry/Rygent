import subprocess
import threading
import time
import os
import json
import re
import signal

RYGENT_DIR = os.path.expanduser("~/.rygent")
TUNNEL_JSON = os.path.join(RYGENT_DIR, "tunnel.json")
PID_FILE = os.path.join(RYGENT_DIR, "cloudflared.pid")

class TunnelManager:
    def __init__(self, port=5000):
        self.port = port
        self.process = None
        self.tunnel_url = None
        self._stop_event = threading.Event()
        self._owned_process = False  # True if WE started the cloudflared process

    def _is_cloudflared_running(self):
        """Check if a cloudflared process from a previous session is still alive."""
        try:
            if os.path.exists(PID_FILE):
                with open(PID_FILE, "r") as f:
                    pid = int(f.read().strip())
                # Check if the process is still running
                os.kill(pid, 0)  # Signal 0 = just check, don't kill
                # Also verify it's actually cloudflared
                try:
                    result = subprocess.run(
                        ["ps", "-p", str(pid), "-o", "comm="],
                        capture_output=True, text=True, timeout=2
                    )
                    if "cloudflared" in result.stdout.strip():
                        print(f"[Tunnel] Existing cloudflared process found (PID {pid})")
                        return pid
                except Exception:
                    pass
        except (ProcessLookupError, ValueError, FileNotFoundError, PermissionError):
            pass
        return None

    def _load_existing_tunnel(self):
        """Load tunnel URL from previous session."""
        try:
            if os.path.exists(TUNNEL_JSON):
                with open(TUNNEL_JSON, "r") as f:
                    data = json.load(f)
                    url = data.get("url")
                    if url:
                        return url
        except Exception:
            pass
        return None

    def _verify_tunnel_alive(self, url):
        """Quick check if the tunnel URL is still responding."""
        try:
            import urllib.request
            req = urllib.request.Request(
                f"{url}/health",
                headers={"Authorization": f"Bearer debug_token_123"},
                method="GET"
            )
            resp = urllib.request.urlopen(req, timeout=5)
            if resp.status == 200:
                return True
        except Exception as e:
            print(f"[Tunnel] URL verification failed: {e}")
        return False

    def start_tunnel(self, token=None, remote_hostname=None):
        """Starts a Cloudflare Tunnel. Reuses existing process if still running."""
        
        # ── Step 1: Check if cloudflared is already running from previous session
        existing_pid = self._is_cloudflared_running()
        if existing_pid:
            existing_url = self._load_existing_tunnel()
            if existing_url:
                print(f"[Tunnel] Reusing existing tunnel: {existing_url} (PID {existing_pid})")
                self.tunnel_url = existing_url
                self._owned_process = False
                
                # Verify the tunnel is actually working
                def verify_in_background():
                    time.sleep(3)  # Give the Flask server time to start
                    if self._verify_tunnel_alive(existing_url):
                        print(f"[Tunnel] ✓ Existing tunnel is alive and responding")
                    else:
                        print(f"[Tunnel] ✗ Existing tunnel not responding. Starting new one...")
                        self._kill_old_process(existing_pid)
                        self._start_new_tunnel(token, remote_hostname)
                
                threading.Thread(target=verify_in_background, daemon=True).start()
                return
        
        # ── Step 2: No existing process, start a new one
        self._start_new_tunnel(token, remote_hostname)

    def _kill_old_process(self, pid):
        """Kill an old cloudflared process."""
        try:
            os.kill(pid, signal.SIGTERM)
            time.sleep(1)
            try:
                os.kill(pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
        except Exception:
            pass
        try:
            os.remove(PID_FILE)
        except Exception:
            pass

    def _start_new_tunnel(self, token=None, remote_hostname=None):
        """Start a fresh cloudflared process as a DETACHED subprocess."""
        def run():
            import shutil
            cf_path = shutil.which("cloudflared")
            if not cf_path:
                # Also check /opt/rygent-agent/cloudflared
                alt_path = "/opt/rygent-agent/cloudflared"
                if os.path.exists(alt_path):
                    cf_path = alt_path
                else:
                    print("[Tunnel] Error: cloudflared binary not found")
                    return

            try:
                if token:
                    print(f"[Tunnel] Starting Named Tunnel with token...")
                    cmd = [cf_path, "tunnel", "run", "--token", token]
                    if remote_hostname:
                        self.tunnel_url = f"https://{remote_hostname.replace('https://', '').replace('http://', '')}"
                else:
                    # Quick tunnel (trycloudflare)
                    cmd = [cf_path, "tunnel", "--url", f"http://localhost:{self.port}"]
                
                print(f"[Tunnel] Executing: {' '.join(cmd)}")
                
                # Start as a DETACHED process that survives GUI exit
                self.process = subprocess.Popen(
                    cmd,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    text=True,
                    bufsize=1,
                    preexec_fn=os.setpgrp  # Detach from parent process group
                )
                self._owned_process = True
                
                # Save PID immediately so future sessions can find it
                os.makedirs(RYGENT_DIR, exist_ok=True)
                with open(PID_FILE, "w") as f:
                    f.write(str(self.process.pid))
                print(f"[Tunnel] cloudflared started with PID {self.process.pid}")

                # Parse output for the tunnel URL
                url_pattern = re.compile(r"https://[a-zA-Z0-9-]+\.trycloudflare\.com")

                for line in iter(self.process.stdout.readline, ''):
                    if self._stop_event.is_set():
                        break
                    
                    line_strip = line.strip()
                    if line_strip:
                        print(f"[Tunnel] {line_strip}")
                    
                    match = url_pattern.search(line)
                    if match:
                        found_url = match.group(0)
                        if self.tunnel_url != found_url:
                            self.tunnel_url = found_url
                            print(f"[Tunnel] ★ Captured URL: {self.tunnel_url}")
                            self._save_tunnel_info()
                    
                    if "Connected" in line and token:
                         self._save_tunnel_info()

                self.process.wait()
            except Exception as e:
                print(f"[Tunnel] Error: {e}")

        self.thread = threading.Thread(target=run, daemon=True)
        self.thread.start()

    def stop_tunnel(self):
        """Stop the tunnel ONLY if we started it AND the user explicitly wants to shut down."""
        self._stop_event.set()
        if self._owned_process and self.process:
            print("[Tunnel] Stopping cloudflared process...")
            try:
                self.process.terminate()
                self.process.wait(timeout=3)
            except Exception:
                try:
                    self.process.kill()
                    self.process.wait()
                except Exception:
                    pass
            # Clean up PID file
            try:
                os.remove(PID_FILE)
            except Exception:
                pass
        elif not self._owned_process:
            print("[Tunnel] Keeping existing cloudflared process running (not owned by this session)")

    def force_stop_tunnel(self):
        """Force stop cloudflared regardless of ownership. Used for full shutdown."""
        self._stop_event.set()
        
        # Kill our own process if we have one
        if self.process:
            try:
                self.process.kill()
                self.process.wait()
            except Exception:
                pass
        
        # Also kill any process tracked by PID file
        try:
            if os.path.exists(PID_FILE):
                with open(PID_FILE, "r") as f:
                    pid = int(f.read().strip())
                os.kill(pid, signal.SIGTERM)
                time.sleep(1)
                try:
                    os.kill(pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
                os.remove(PID_FILE)
                print(f"[Tunnel] Force-killed cloudflared (PID {pid})")
        except Exception:
            pass

    def _save_tunnel_info(self):
        """Save tunnel URL to disk for persistence."""
        try:
            os.makedirs(RYGENT_DIR, exist_ok=True)
            with open(TUNNEL_JSON, "w") as f:
                json.dump({
                    "url": self.tunnel_url,
                    "timestamp": time.time(),
                    "pid": self.process.pid if self.process else None
                }, f)
        except Exception as e:
            print(f"[Tunnel] Error saving tunnel info: {e}")

    def _cleanup_tunnel_info(self):
        pass

tunnel_manager = TunnelManager()
