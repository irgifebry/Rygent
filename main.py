from flask import Flask, jsonify, render_template, send_from_directory
from flask_cors import CORS
from config import Config
from monitor import monitor
from auth import require_token
from tunnel_manager import tunnel_manager
import subprocess
import time
import os
import socket
import signal
import sys
import threading

app = Flask(__name__, static_folder='static', template_folder='templates')
CORS(app) # Enable CORS for all routes

@app.route('/')
def dashboard_view():
    local_ip = monitor.get_local_ip()
    return render_template('index.html', local_ip=local_ip)

@app.route('/health', methods=['GET'])
@require_token
def health():
    return jsonify({
        "status": "ok",
        "timestamp": int(time.time() * 1000),
        "tunnel_url": tunnel_manager.tunnel_url
    })

@app.route('/api/system', methods=['GET'])
@require_token
def get_system():
    try:
        data = monitor.get_system_info()
        # Include live disk I/O in main system response
        data['diskIo'] = monitor.get_disk_io()
        return jsonify(data)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# ── Phase 3: Storage Intelligence Endpoints ────────────────────

@app.route('/api/storage/smart', methods=['GET'])
@require_token
def get_smart():
    try:
        return jsonify(monitor.get_smart_info())
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/storage/io', methods=['GET'])
@require_token
def get_disk_io():
    try:
        return jsonify(monitor.get_disk_io())
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/storage/large-files', methods=['GET'])
@require_token
def get_large_files():
    from flask import request
    try:
        path = request.args.get('path', '/')
        limit = int(request.args.get('limit', 20))
        min_size = int(request.args.get('min_size_mb', 50))
        return jsonify(monitor.get_large_files(path, limit, min_size))
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# ── Phase 4: Advanced Analytics & Connectivity ────────────────────

@app.route('/api/network/speedtest', methods=['POST'])
@require_token
def run_speedtest():
    try:
        # Note: Depending on the host this might take 10-30s.
        return jsonify(monitor.run_speedtest())
    except Exception as e:
         return jsonify({"error": str(e)}), 500

@app.route('/api/analytics/history', methods=['GET'])
@require_token
def get_process_history():
    return jsonify(monitor.get_process_history())

@app.route('/api/analytics/alerts', methods=['GET'])
@require_token
def get_alerts():
    return jsonify(monitor.get_alerts())

@app.route('/api/power/shutdown', methods=['POST'])
@require_token
def shutdown():
    def do_shutdown():
        time.sleep(1)
        subprocess.run(['systemctl', 'poweroff'], check=False)
    threading.Thread(target=do_shutdown, daemon=True).start()
    return jsonify({"status": "Success", "message": "System is shutting down..."})

@app.route('/api/power/reboot', methods=['POST'])
@require_token
def reboot():
    def do_reboot():
        time.sleep(1)
        subprocess.run(['systemctl', 'reboot'], check=False)
    threading.Thread(target=do_reboot, daemon=True).start()
    return jsonify({"status": "Success", "message": "System is rebooting..."})

@app.route('/api/power/suspend', methods=['POST'])
@require_token
def suspend():
    def do_suspend():
        time.sleep(1)
        subprocess.run(['systemctl', 'suspend'], check=False)
    threading.Thread(target=do_suspend, daemon=True).start()
    return jsonify({"status": "Success", "message": "System is suspending..."})

# Discovery Logic
discovery_data = []

def start_discovery():
    from discovery import register_service
    
    # 1. Start UDP Broadcast Fallback independently (more reliable)
    def run_udp():
        try:
            from discovery import start_udp_broadcast
            start_udp_broadcast(Config.PORT)
        except Exception as e:
            print(f"UDP discovery failed: {e}")

    threading.Thread(target=run_udp, daemon=True).start()

    # 2. Start mDNS (Zeroconf) independently
    def run_mdns():
        try:
            zc, info = register_service(Config.PORT)
            discovery_data.append((zc, info))
            # Keep thread alive
            while True:
                time.sleep(1)
        except Exception as e:
            print(f"mDNS discovery failed: {e}")

    threading.Thread(target=run_mdns, daemon=True).start()

# Protected system PIDs that should never be killed
PROTECTED_PIDS = {1}  # PID 1 = init/systemd

@app.route('/api/process/kill/<int:pid>', methods=['POST'])
@require_token
def kill_process(pid):
    try:
        import psutil
        # F7: Validate PID before killing
        if pid in PROTECTED_PIDS:
            return jsonify({"error": f"PID {pid} is a protected system process"}), 403
        if pid <= 0:
            return jsonify({"error": "Invalid PID"}), 400
        
        p = psutil.Process(pid)
        proc_name = p.name()
        
        # Don't allow killing the agent itself
        if p.pid == os.getpid():
            return jsonify({"error": "Cannot kill the monitoring agent"}), 403
            
        p.terminate()  # Graceful first
        try:
            p.wait(timeout=3)
        except psutil.TimeoutExpired:
            p.kill()  # Force if graceful fails
            
        return jsonify({"status": "Success", "message": f"Process '{proc_name}' (PID {pid}) terminated"})
    except psutil.NoSuchProcess:
        return jsonify({"error": f"Process {pid} not found"}), 404
    except psutil.AccessDenied:
        return jsonify({"error": f"Access denied for PID {pid}"}), 403
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/agent/stop', methods=['POST'])
@require_token
def stop_agent():
    def suicide():
        time.sleep(0.5)
        
        # 0. Force-stop the tunnel (full shutdown requested remotely)
        try:
            tunnel_manager.force_stop_tunnel()
        except: pass

        # 1. Try to unregister from discovery first
        try:
            for zc, info in discovery_data:
                zc.unregister_service(info)
                zc.close()
        except:
            pass
        # 2. Hard exit with success code to prevent systemd restart
        os._exit(0) 
    
    threading.Thread(target=suicide).start()
    return jsonify({"status": "Success", "message": "Agent is stopping..."})

def run_server():
    # Check if port is already in use (e.g. by systemd service)
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        if s.connect_ex((Config.HOST, Config.PORT)) == 0:
            print(f"Port {Config.PORT} is already in use. Assuming server is already running.")
            return

    # Start discovery
    threading.Thread(target=start_discovery, daemon=True).start()
    
    # Start Tunnel if cloudflared is installed
    import shutil
    cf_path = shutil.which("cloudflared")
    # Also check bundled cloudflared in /opt/rygent-agent
    if not cf_path:
        alt_path = "/opt/rygent-agent/cloudflared"
        if os.path.exists(alt_path):
            cf_path = alt_path
            # Add to PATH for child processes
            os.environ["PATH"] = f"/opt/rygent-agent:{os.environ.get('PATH', '')}"
    
    if cf_path:
        print(f"[Agent] Cloudflare Tunnel available ({cf_path})")
        tunnel_manager.port = Config.PORT
        tunnel_manager.start_tunnel(token=Config.TUNNEL_TOKEN, remote_hostname=Config.REMOTE_HOSTNAME)
    else:
        print("[Agent] cloudflared not found. Remote connectivity disabled.")
    
    # Signal handling - graceful stop keeps tunnel alive
    def handle_signal(signum, frame):
        print(f"\n[Agent] Received signal {signum}, shutting down server...")
        # Don't kill the tunnel on signal - it should persist
        sys.exit(0)

    if threading.current_thread() is threading.main_thread():
        signal.signal(signal.SIGINT, handle_signal)
        signal.signal(signal.SIGTERM, handle_signal)

    print(f"[Agent] Starting Rygent Agent on {Config.HOST}:{Config.PORT}")
    app.run(host=Config.HOST, port=Config.PORT, debug=False)

if __name__ == '__main__':
    run_server()
