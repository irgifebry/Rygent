import socket
import time
import logging
import os
import json
import threading

DISCOVERY_LOG = "/tmp/gi-connect-discovery.log"

def log_debug(msg):
    with open(DISCOVERY_LOG, "a") as f:
        f.write(f"{time.ctime()}: {msg}\n")

def get_local_ip():
    """Get local IP with retry logic for boot-time startup."""
    for attempt in range(30):  # Retry up to 30 times (150 seconds)
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.settimeout(2)
            s.connect(("8.8.8.8", 80))
            local_ip = s.getsockname()[0]
            s.close()
            if local_ip and local_ip != "0.0.0.0":
                return local_ip
        except Exception as e:
            log_debug(f"IP detection attempt {attempt+1}/30 failed: {e}")
        time.sleep(5)
    
    # Fallback: try hostname resolution
    try:
        return socket.gethostbyname(socket.gethostname())
    except:
        return None

def get_broadcast_address():
    """Calculate the subnet broadcast address."""
    try:
        import subprocess
        result = subprocess.run(['ip', '-4', 'addr', 'show'], capture_output=True, text=True)
        for line in result.stdout.split('\n'):
            if 'brd' in line and '127.' not in line:
                parts = line.strip().split()
                brd_idx = parts.index('brd')
                return parts[brd_idx + 1]
    except:
        pass
    return '255.255.255.255'

def register_service(port=5000):
    """Register mDNS service with retry for network readiness."""
    log_debug(f"Starting mDNS registration on port {port}")
    
    local_ip = get_local_ip()
    if not local_ip:
        log_debug("FATAL: Could not detect local IP after all retries")
        return None, None
    
    log_debug(f"Detected IP: {local_ip}")
    hostname = socket.gethostname()
    
    # Check for tunnel URL to support remote auto-reconnect
    tunnel_url = None
    try:
        persistent_path = os.path.expanduser("~/.rygent/tunnel.json")
        if os.path.exists(persistent_path):
            with open(persistent_path, "r") as f:
                tunnel_url = json.load(f).get("url")
    except: pass

    desc = {
        'version': '1.0.0', 
        'path': '/api/system',
        'tunnel_url': tunnel_url if tunnel_url else ""
    }

    try:
        from zeroconf import ServiceInfo, Zeroconf
        
        info = ServiceInfo(
            "_rygent._tcp.local.",
            f"{hostname}._rygent._tcp.local.",
            addresses=[socket.inet_aton(local_ip)],
            port=port,
            properties=desc,
            server=f"{hostname}.local.",
        )

        log_debug("Creating Zeroconf instance...")
        zeroconf = Zeroconf()
        log_debug(f"Registering mDNS service: {info.name}")
        zeroconf.register_service(info, allow_name_change=True)
        log_debug(f"mDNS registration completed. Final name: {info.name}")
        return zeroconf, info
    except Exception as e:
        import traceback
        log_debug(f"Zeroconf error: {e}")
        log_debug(traceback.format_exc())
        return None, None

def start_udp_broadcast(port=5000):
    """UDP broadcast with retry for network readiness."""
    local_ip = get_local_ip()
    if not local_ip:
        log_debug("UDP: Could not detect local IP, aborting")
        return
    
    hostname = socket.gethostname()
    broadcast_addr = get_broadcast_address()
    
    # Check for tunnel URL
    tunnel_url = None
    try:
        persistent_path = os.path.expanduser("~/.rygent/tunnel.json")
        if os.path.exists(persistent_path):
            with open(persistent_path, "r") as f:
                tunnel_url = json.load(f).get("url")
    except: pass

    discovery_data = {
        "service": "rygent",
        "ip": local_ip,
        "port": port,
        "name": hostname,
        "tunnel_url": tunnel_url
    }
    message = json.dumps(discovery_data).encode('utf-8')
    
    log_debug(f"UDP broadcast starting: IP={local_ip}, broadcast={broadcast_addr}, port=30001")
    
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        
        while True:
            # Re-read tunnel URL to support dynamic updates
            tunnel_url = None
            try:
                persistent_path = os.path.expanduser("~/.rygent/tunnel.json")
                if os.path.exists(persistent_path):
                    with open(persistent_path, "r") as f:
                        tunnel_url = json.load(f).get("url")
            except: pass
            
            discovery_data["tunnel_url"] = tunnel_url
            message = json.dumps(discovery_data).encode('utf-8')

            for dest in (broadcast_addr, '255.255.255.255', '<broadcast>'):
                try:
                    sock.sendto(message, (dest, 30001))
                except Exception as e:
                    log_debug(f"UDP send to {dest} failed: {e}")
            time.sleep(3)
    except Exception as e:
        log_debug(f"UDP broadcast fatal error: {e}")
    finally:
        try:
            sock.close()
        except:
            pass

if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    zc, info = register_service()
    if zc:
        try:
            while True:
                time.sleep(0.1)
        except KeyboardInterrupt:
            pass
        finally:
            print("Unregistering...")
            zc.unregister_service(info)
            zc.close()
    else:
        print("Failed to register mDNS service")
