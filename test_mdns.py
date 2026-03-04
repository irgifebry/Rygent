import socket
from zeroconf import ServiceInfo, Zeroconf, IPVersion
import time

def test_broadcast():
    hostname = socket.gethostname()
    local_ip = "192.168.101.11" # Force the IP we saw in logs
    port = 5000
    
    desc = {'version': '1.0.2'}
    
    info = ServiceInfo(
        "_rygent._tcp.local.",
        f"{hostname}-test._rygent._tcp.local.",
        addresses=[socket.inet_aton(local_ip)],
        port=port,
        properties=desc,
        server=f"{hostname}.local.",
    )

    print(f"Testing mDNS broadcast for {hostname} at {local_ip}:{port}...")
    try:
        zeroconf = Zeroconf(ip_version=IPVersion.V4Only)
        zeroconf.register_service(info)
        print("Service registered! Press Ctrl+C to stop.")
        while True:
            time.sleep(1)
    except Exception as e:
        print(f"Error: {e}")
    finally:
        print("Stopping...")
        zeroconf.unregister_service(info)
        zeroconf.close()

if __name__ == "__main__":
    test_broadcast()
