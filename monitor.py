import psutil
import time
import socket
import platform
import os
import re
import threading
import subprocess
from collections import deque
from config import Config

class SystemMonitor:
    def __init__(self):
        self.last_cache_time = 0
        self.cached_data = None
        self.cache_ttl = Config.CACHE_TTL
        # B5: Initialize CPU percent baseline so first call doesn't return 0
        psutil.cpu_percent(interval=None)
        psutil.cpu_percent(interval=None, percpu=True)
        self.static_info = self._get_static_info()

        # Phase 3 Disk IO state
        self._prev_disk_io = psutil.disk_io_counters(perdisk=True)
        self._prev_disk_io_time = time.time()
        
        # Phase 4 Network IO baseline
        self._prev_net_io = psutil.net_io_counters()
        self._prev_net_io_time = time.time()

        # Phase 4 Process History & Runaway Tracking
        self.process_history = deque(maxlen=1440) 
        self.runaway_processes = {} # pid -> consecutive high CPU count
        self.alerts = [] # List of active runaway alerts
        self._start_background_tracking()

    def _start_background_tracking(self):
        def tracker():
            while True:
                self._record_process_history()
                time.sleep(60) # Run every minute
        t = threading.Thread(target=tracker, daemon=True)
        t.start()

    def _record_process_history(self):
        try:
            current_time_ms = int(time.time() * 1000)
            processes = []
            for proc in psutil.process_iter(['pid', 'name', 'cpu_percent', 'memory_info']):
                try:
                    pinfo = proc.info
                    cpu_p = pinfo['cpu_percent'] or 0.0
                    if cpu_p > 0.1 or (pinfo['memory_info'] and pinfo['memory_info'].rss > 50*1024*1024): 
                        processes.append({
                            "pid": pinfo['pid'],
                            "name": pinfo['name'] or "Unknown",
                            "cpuPercent": cpu_p,
                            "memoryBytes": pinfo['memory_info'].rss if pinfo['memory_info'] else 0
                        })
                except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                    pass
            
            processes.sort(key=lambda x: x['cpuPercent'], reverse=True)
            top50 = processes[:50]
            
            self.process_history.append({
                "timestamp": current_time_ms,
                "processes": top50
            })

            # Runaway Detection
            current_pids = set()
            new_alerts = []
            existing_alert_pids = {a['pid'] for a in self.alerts}
            for p in top50:
                pid = p['pid']
                current_pids.add(pid)
                if p['cpuPercent'] > 80.0:
                    self.runaway_processes[pid] = self.runaway_processes.get(pid, 0) + 1
                    if self.runaway_processes[pid] >= 5 and pid not in existing_alert_pids:
                        new_alerts.append({
                            "pid": pid,
                            "name": p['name'],
                            "cpuPercent": p['cpuPercent'],
                            "durationMin": self.runaway_processes[pid],
                            "message": f"Process {p['name']} is using {p['cpuPercent']}% CPU for >5 minutes!"
                        })
                else:
                    if pid in self.runaway_processes:
                        del self.runaway_processes[pid]
            
            for pid in list(self.runaway_processes.keys()):
                if pid not in current_pids:
                    del self.runaway_processes[pid]
                    
            seen_pids = set()
            updated_alerts = []
            for alert in self.alerts + new_alerts:
                pid = alert['pid']
                if pid in self.runaway_processes and self.runaway_processes[pid] >= 5 and pid not in seen_pids:
                    seen_pids.add(pid)
                    updated_alerts.append(alert)
            self.alerts = updated_alerts
        except Exception as e:
            print("Error in background tracking:", e)

    def get_alerts(self):
        return self.alerts

    def _get_static_info(self):
        # OS Info
        os_name = f"Windows {platform.release()} ({platform.version()})"
        
        # Hardware Info (using wmic for Windows)
        cpu_model = platform.processor()
        manufacturer = "Unknown"
        device_model = "Unknown"
        board_name = "Unknown"
        board_vendor = "Unknown"
        bios_version = "Unknown"
        bios_date = "Unknown"

        try:
            res = subprocess.check_output(["wmic", "cpu", "get", "name"], text=True)
            lines = [l.strip() for l in res.split("\n") if l.strip()]
            if len(lines) > 1:
                cpu_model = lines[1]

            res = subprocess.check_output(["wmic", "computersystem", "get", "manufacturer,model"], text=True)
            lines = [l.strip() for l in res.split("\n") if l.strip()]
            if len(lines) > 1:
                parts = lines[1].split(None, 1)
                if len(parts) >= 2:
                    manufacturer = parts[0]
                    device_model = parts[1]
                elif len(parts) == 1:
                    manufacturer = parts[0]

            res = subprocess.check_output(["wmic", "baseboard", "get", "manufacturer,product"], text=True)
            lines = [l.strip() for l in res.split("\n") if l.strip()]
            if len(lines) > 1:
                parts = lines[1].split(None, 1)
                if len(parts) >= 2:
                    board_vendor = parts[0]
                    board_name = parts[1]

            res = subprocess.check_output(["wmic", "bios", "get", "version,releasedate"], text=True)
            lines = [l.strip() for l in res.split("\n") if l.strip()]
            if len(lines) > 1:
                parts = lines[1].split()
                if len(parts) >= 2:
                    bios_date = parts[0]
                    bios_version = parts[1]
        except Exception:
            pass

        return {
            "osName": os_name,
            "kernel": platform.version(),
            "architecture": platform.machine(),
            "cpuModel": cpu_model,
            "cores": psutil.cpu_count(logical=False),
            "threads": psutil.cpu_count(logical=True),
            "deviceModel": device_model,
            "manufacturer": manufacturer,
            "boardName": board_name,
            "boardVendor": board_vendor,
            "biosVersion": bios_version,
            "biosDate": bios_date,
            "rootAccess": self.is_admin(),
            "hostname": socket.gethostname(),
            "ip": self.get_local_ip(),
            "apps": self.get_apps()
        }

    def is_admin(self):
        try:
            import ctypes
            return ctypes.windll.shell32.IsUserAnAdmin() != 0
        except: return False

    def get_apps(self):
        apps = []
        try:
            import winreg
            def search_registry(root, path):
                try:
                    with winreg.OpenKey(root, path) as key:
                        for i in range(winreg.QueryInfoKey(key)[0]):
                            try:
                                subkey_name = winreg.EnumKey(key, i)
                                with winreg.OpenKey(key, subkey_name) as subkey:
                                    try: name = winreg.QueryValueEx(subkey, "DisplayName")[0]
                                    except: continue
                                    apps.append({"name": name, "packageName": subkey_name})
                            except: continue
                except: pass

            search_registry(winreg.HKEY_LOCAL_MACHINE, r"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall")
            search_registry(winreg.HKEY_LOCAL_MACHINE, r"SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall")
            search_registry(winreg.HKEY_CURRENT_USER, r"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall")
        except: pass
        # Deduplicate and sort
        unique = {a["name"]: a for a in apps}.values()
        return sorted(list(unique), key=lambda x: x["name"])[:50]

    def get_local_ip(self):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            try:
                s.connect(("8.8.8.8", 80))
                return s.getsockname()[0]
            finally:
                s.close()
        except Exception:
            try:
                return socket.gethostbyname(socket.gethostname())
            except Exception:
                return "127.0.0.1"

    def get_system_info(self):
        current_time = time.time()
        if self.cached_data and (current_time - self.last_cache_time) < self.cache_ttl:
            return self.cached_data

        cpu_percent = psutil.cpu_percent(interval=None)
        cpu_percent_per_core = psutil.cpu_percent(interval=None, percpu=True)
        cpu_freq = psutil.cpu_freq()
        
        memory = psutil.virtual_memory()
        
        disk_partitions = []
        for part in psutil.disk_partitions():
            if 'fixed' not in part.opts: continue # Only fixed drives
            try:
                usage = psutil.disk_usage(part.mountpoint)
                disk_partitions.append({
                    "device": part.device,
                    "mountpoint": part.mountpoint,
                    "total": usage.total,
                    "used": usage.used,
                    "free": usage.free,
                    "percent": usage.percent
                })
            except (PermissionError, OSError):
                continue

        net_io = psutil.net_io_counters()
        interfaces = []
        try:
            addrs = psutil.net_if_addrs()
            stats = psutil.net_if_stats()
            for nic, addrs_list in addrs.items():
                nic_info = {"name": nic, "ip": "Unknown", "mac": "Unknown", "speed": 0, "up": False}
                if nic in stats:
                    nic_info["speed"] = stats[nic].speed
                    nic_info["up"] = stats[nic].isup
                for addr in addrs_list:
                    if addr.family == socket.AF_INET: nic_info["ip"] = addr.address
                    elif addr.family == psutil.AF_LINK: nic_info["mac"] = addr.address
                interfaces.append(nic_info)
        except: pass
            
        net_info = {"bytesSent": net_io.bytes_sent, "bytesRecv": net_io.bytes_recv, "interfaces": interfaces}

        battery_info = None
        if hasattr(psutil, "sensors_battery"):
            try:
                bat = psutil.sensors_battery()
                if bat:
                    battery_info = {
                        "percent": bat.percent,
                        "powerPlugged": bat.power_plugged,
                        "secsLeft": bat.secsleft if bat.secsleft != psutil.POWER_TIME_UNLIMITED else -1,
                        "temp": 0.0
                    }
            except: pass

        processes = []
        try:
            for proc in psutil.process_iter(['pid', 'name', 'cpu_percent', 'memory_info']):
                try:
                    pinfo = proc.info
                    processes.append({
                        "pid": pinfo['pid'],
                        "name": pinfo['name'] or "Unknown",
                        "cpuPercent": pinfo['cpu_percent'] or 0.0,
                        "memoryBytes": pinfo['memory_info'].rss if pinfo['memory_info'] else 0
                    })
                except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                    pass
        except: pass
        
        processes.sort(key=lambda x: x['cpuPercent'], reverse=True)
        top_processes = processes[:Config.TOP_PROCESS_COUNT]

        boot_time = psutil.boot_time()
        uptime_seconds = int(current_time - boot_time)

        sensors = self._get_sensors()
        
        self.cached_data = {
            "hostname": self.static_info["hostname"],
            "ip": self.static_info["ip"],
            "os": self.static_info["osName"], 
            "uptime": uptime_seconds,
            "hardware": {
                "manufacturer": self.static_info["manufacturer"],
                "model": self.static_info["deviceModel"],
                "cpuModel": self.static_info["cpuModel"],
                "cores": self.static_info["cores"],
                "threads": self.static_info["threads"],
                "architecture": self.static_info["architecture"],
                "boardName": self.static_info["boardName"],
                "boardVendor": self.static_info["boardVendor"],
                "biosVersion": self.static_info["biosVersion"],
                "biosDate": self.static_info["biosDate"]
            },
            "system": {
                "os": self.static_info["osName"],
                "kernel": self.static_info["kernel"],
                "rootAccess": self.static_info["rootAccess"]
            },
            "cpu": {
                "usage": int(cpu_percent),
                "usagePerCore": [int(x) for x in cpu_percent_per_core] if cpu_percent_per_core else [],
                "frequency": cpu_freq.current if cpu_freq else 0,
                "freqPerCore": []
            },
            "memory": {
                "total": memory.total,
                "available": memory.available,
                "used": memory.used,
                "percent": int(memory.percent)
            },
            "disk": disk_partitions,
            "diskIo": self.get_disk_io(),
            "network": net_info,
            "battery": battery_info,
            "processes": top_processes,
            "apps": self.static_info.get("apps", []),
            "sensors": sensors,
            "timestamp": int(current_time * 1000)
        }
        
        self.last_cache_time = current_time
        return self.cached_data

    def _get_sensors(self):
        sensors = {
            'cpuTemp': 0.0, 'coreTemps': [], 'gpu': None, 'gpus': [],
            'boardTemp': 0.0, 'batteryTemp': 0.0, 'storageTemp': 0.0, 'fans': []
        }
        try:
            res = subprocess.check_output(
                ['wmic', '/namespace:\\\\root\\wmi', 'PATH', 'MSAcpi_ThermalZoneTemperature', 'get', 'CurrentTemperature'],
                text=True, stderr=subprocess.DEVNULL
            )
            lines = [l.strip() for l in res.split('\n') if l.strip()]
            if len(lines) > 1:
                temp_k = float(lines[1]) / 10.0
                sensors['cpuTemp'] = round(temp_k - 273.15, 1)
        except Exception:
            pass

        # Basic GPU info via tasklist/powershell would be slow, psutil gives memory bits
        return sensors

    def get_smart_info(self):
        drives = []
        try:
            res = subprocess.check_output(['wmic', 'diskdrive', 'get', 'model,status,size,serialnumber'], text=True)
            lines = [l.strip() for l in res.split('\n') if l.strip()]
            for line in lines[1:]:
                parts = line.split()
                if len(parts) >= 3:
                    status = parts[-1]
                    size_str = parts[-2]
                    model = " ".join(parts[:-2])
                    drives.append({
                        "device": "Disk",
                        "model": model,
                        "health": status,
                        "capacityBytes": int(size_str) if size_str.isdigit() else 0,
                        "status": status
                    })
        except Exception:
            pass
        return drives

    def get_disk_io(self):
        result = []
        try:
            now = time.time()
            counters = psutil.disk_io_counters(perdisk=True)
            elapsed = now - self._prev_disk_io_time if self._prev_disk_io_time > 0 else 0
            for disk_name, stats in counters.items():
                entry = {
                    "name": disk_name,
                    "readBytes": stats.read_bytes, "writeBytes": stats.write_bytes,
                    "readCount": stats.read_count, "writeCount": stats.write_count,
                    "readIOPS": 0.0, "writeIOPS": 0.0,
                    "readBytesPerSec": 0.0, "writeBytesPerSec": 0.0
                }
                if elapsed > 0 and disk_name in self._prev_disk_io:
                    prev = self._prev_disk_io[disk_name]
                    entry["readIOPS"] = round((stats.read_count - prev.read_count) / elapsed, 1)
                    entry["writeIOPS"] = round((stats.write_count - prev.write_count) / elapsed, 1)
                    entry["readBytesPerSec"] = round((stats.read_bytes - prev.read_bytes) / elapsed)
                    entry["writeBytesPerSec"] = round((stats.write_bytes - prev.write_bytes) / elapsed)
                result.append(entry)
            self._prev_disk_io = counters
            self._prev_disk_io_time = now
        except: pass
        return result

    def get_large_files(self, path="C:\\", limit=20, min_size_mb=10):
        large_files = []
        min_bytes = min_size_mb * 1024 * 1024
        start_time = time.time()
        timeout = 30
        try:
            for dirpath, dirnames, filenames in os.walk(path):
                if time.time() - start_time > timeout: break
                if any(x in dirpath for x in ["$Recycle.Bin", "System Volume Information", "Windows\\WinSxS"]):
                    dirnames[:] = [] 
                    continue
                for fname in filenames:
                    fpath = os.path.join(dirpath, fname)
                    try:
                        size = os.path.getsize(fpath)
                        if size >= min_bytes:
                            large_files.append({"path": fpath, "sizeBytes": size, "modified": int(os.path.getmtime(fpath) * 1000)})
                    except: continue
        except: pass
        large_files.sort(key=lambda x: x["sizeBytes"], reverse=True)
        return large_files[:limit]

    def run_speedtest(self):
        try:
            import subprocess, json as _json
            # speedtest-cli is expected for binary parity
            cmd = ['speedtest-cli', '--json']
            res = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
            if res.returncode == 0:
                data = _json.loads(res.stdout)
                return {
                    "ping": data.get("ping", 0.0),
                    "downloadBits": data.get("download", 0.0),
                    "uploadBits": data.get("upload", 0.0),
                    "server": data.get("server", {}).get("name", "Unknown")
                }
            return {"error": "Speedtest failed", "details": res.stderr}
        except Exception as e:
            return {"error": str(e)}

monitor = SystemMonitor()
