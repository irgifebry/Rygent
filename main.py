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

# PyInstaller bundle support
if getattr(sys, 'frozen', False):
    base_path = sys._MEIPASS
    template_dir = os.path.join(base_path, 'templates')
    static_dir = os.path.join(base_path, 'static')
    app = Flask(__name__, template_folder=template_dir, static_folder=static_dir)
else:
    app = Flask(__name__, static_folder='static', template_folder='templates')

CORS(app)

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
        data['diskIo'] = monitor.get_disk_io()
        return jsonify(data)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

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
        path = request.args.get('path', 'C:\\')
        limit = int(request.args.get('limit', 20))
        min_size = int(request.args.get('min_size_mb', 50))
        return jsonify(monitor.get_large_files(path, limit, min_size))
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/network/speedtest', methods=['POST'])
@require_token
def run_speedtest():
    try:
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
        subprocess.run(['shutdown', '/s', '/t', '1'], check=False)
    threading.Thread(target=do_shutdown, daemon=True).start()
    return jsonify({"status": "Success", "message": "System is shutting down..."})

@app.route('/api/power/reboot', methods=['POST'])
@require_token
def reboot():
    def do_reboot():
        time.sleep(1)
        subprocess.run(['shutdown', '/r', '/t', '1'], check=False)
    threading.Thread(target=do_reboot, daemon=True).start()
    return jsonify({"status": "Success", "message": "System is rebooting..."})

@app.route('/api/power/suspend', methods=['POST'])
@require_token
def suspend():
    def do_suspend():
        time.sleep(1)
        subprocess.run(['rundll32.exe', 'powrprof.dll,SetSuspendState', '0,1,0'], check=False)
    threading.Thread(target=do_suspend, daemon=True).start()
    return jsonify({"status": "Success", "message": "System is suspending..."})

PROTECTED_PIDS = {0, 4} # Windows Idle/System

@app.route('/api/process/kill/<int:pid>', methods=['POST'])
@require_token
def kill_process(pid):
    try:
        import psutil
        if pid in PROTECTED_PIDS:
            return jsonify({"error": f"PID {pid} is a protected system process"}), 403
        
        p = psutil.Process(pid)
        proc_name = p.name()
        if p.pid == os.getpid():
            return jsonify({"error": "Cannot kill the monitoring agent"}), 403
        p.terminate()
        try:
            p.wait(timeout=3)
        except psutil.TimeoutExpired:
            p.kill()
        return jsonify({"status": "Success", "message": f"Process terminated"})
    except psutil.NoSuchProcess:
        return jsonify({"error": f"Process not found"}), 404
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/agent/stop', methods=['POST'])
@require_token
def stop_agent():
    def suicide():
        time.sleep(0.5)
        try:
            tunnel_manager.force_stop_tunnel()
        except: pass
        os._exit(0) 
    threading.Thread(target=suicide).start()
    return jsonify({"status": "Success", "message": "Agent is stopping..."})

def run_server():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        if s.connect_ex((Config.HOST, Config.PORT)) == 0:
            print(f"Port {Config.PORT} is already in use.")
            return

    # No Discovery on Windows version as per request
    
    import shutil
    cf_path = shutil.which("cloudflared") or shutil.which("cloudflared.exe")
    if cf_path:
        tunnel_manager.port = Config.PORT
        tunnel_manager.start_tunnel(token=Config.TUNNEL_TOKEN, remote_hostname=Config.REMOTE_HOSTNAME)
    
    print(f"[Agent] Starting Rygent Agent on {Config.HOST}:{Config.PORT}")
    app.run(host=Config.HOST, port=Config.PORT, debug=False)

if __name__ == '__main__':
    run_server()
