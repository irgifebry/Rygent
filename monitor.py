import psutil
import time
import socket
import platform
import os
import distro
import re
import glob
import threading
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
        # Store last 1440 data points (24 hours at 1 point/min)
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
            # We don't want to use process_iter in a way that blocks forever
            processes = []
            for proc in psutil.process_iter(['pid', 'name', 'cpu_percent', 'memory_info']):
                try:
                    pinfo = proc.info
                    # For bg tracker we can use a small interval to get actual CPU of the moment
                    # But calling cpu_percent(0.1) on hundreds of procs might be slow
                    # the proc.info dict already has cpu_percent cached since last psutil read
                    cpu_p = pinfo['cpu_percent'] or 0.0
                    if cpu_p > 0.1 or pinfo['memory_info'].rss > 50*1024*1024: # Only track notable processes
                        processes.append({
                            "pid": pinfo['pid'],
                            "name": pinfo['name'] or "Unknown",
                            "cpuPercent": cpu_p,
                            "memoryBytes": pinfo['memory_info'].rss
                        })
                except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                    pass
            
            # Sort and only keep top 50 to save memory in history
            processes.sort(key=lambda x: x['cpuPercent'], reverse=True)
            top50 = processes[:50]
            
            self.process_history.append({
                "timestamp": current_time_ms,
                "processes": top50
            })

            # Runaway Detection (>80% CPU for >5 consecutive min)
            current_pids = set()
            new_alerts = []
            for p in top50:
                pid = p['pid']
                current_pids.add(pid)
                if p['cpuPercent'] > 80.0:
                    self.runaway_processes[pid] = self.runaway_processes.get(pid, 0) + 1
                    if self.runaway_processes[pid] >= 5: # 5 minutes
                        is_new = True
                        for a in self.alerts:
                            if a['pid'] == pid: is_new = False
                        if is_new:
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
            
            # Cleanup old runaways
            for pid in list(self.runaway_processes.keys()):
                if pid not in current_pids:
                    del self.runaway_processes[pid]
                    
            # Update alerts list (keep existing, add new, remove those that cooled down)
            updated_alerts = []
            for alert in self.alerts + new_alerts:
                if alert['pid'] in self.runaway_processes and self.runaway_processes[alert['pid']] >= 5:
                    if alert not in updated_alerts:
                        updated_alerts.append(alert)
            self.alerts = updated_alerts

        except Exception as e:
            print("Error in background tracking:", e)

    def get_process_history(self):
        return list(self.process_history)
        
    def get_alerts(self):
        return self.alerts

    def _get_static_info(self):
        # OS Info
        try:
            os_name = f"{distro.name()} {distro.version()}"
        except:
            os_name = f"{platform.system()} {platform.release()}"
            
        # Hardware Info (CPU Model)
        cpu_model = platform.processor()
        try:
            with open("/proc/cpuinfo", "r") as f:
                for line in f:
                    if "model name" in line:
                        cpu_model = line.split(":")[1].strip()
                        break
        except:
            pass
            
        # Device & Motherboard Info (via DMI/sysfs)
        device_model = "Unknown"
        manufacturer = "Unknown"
        board_name = "Unknown"
        board_vendor = "Unknown"
        bios_version = "Unknown"
        bios_date = "Unknown"
        
        try:
            # Try reading from /sys/class/dmi/id/
            paths = {
                "product_name": ("deviceModel", "/sys/class/dmi/id/product_name"),
                "sys_vendor": ("manufacturer", "/sys/class/dmi/id/sys_vendor"),
                "board_name": ("boardName", "/sys/class/dmi/id/board_name"),
                "board_vendor": ("boardVendor", "/sys/class/dmi/id/board_vendor"),
                "bios_version": ("biosVersion", "/sys/class/dmi/id/bios_version"),
                "bios_date": ("biosDate", "/sys/class/dmi/id/bios_date")
            }
            results = {}
            for key, (attr, path) in paths.items():
                if os.path.exists(path):
                    with open(path, "r") as f:
                        results[key] = f.read().strip()
            
            device_model = results.get("product_name", "Unknown")
            manufacturer = results.get("sys_vendor", "Unknown")
            board_name = results.get("board_name", "Unknown")
            board_vendor = results.get("board_vendor", "Unknown")
            bios_version = results.get("bios_version", "Unknown")
            bios_date = results.get("bios_date", "Unknown")
        except:
            pass

        return {
            "osName": os_name,
            "kernel": platform.release(),
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
            "rootAccess": os.geteuid() == 0 if hasattr(os, "geteuid") else False,
            "hostname": socket.gethostname(),
            "ip": self.get_local_ip(),
            "apps": self._get_installed_apps()
        }

    def _get_installed_apps(self):
        apps = []
        try:
            desktop_dirs = ['/usr/share/applications/']
            for d in desktop_dirs:
                if not os.path.exists(d): continue
                for f in os.listdir(d):
                    if f.endswith('.desktop'):
                        try:
                            with open(os.path.join(d, f), 'r', errors='ignore') as file:
                                name, package = "", f.replace(".desktop", "")
                                for line in file:
                                    if line.startswith('Name='):
                                        name = line.split('=')[1].strip()
                                        break
                                if name:
                                    apps.append({"name": name, "package": package})
                        except:
                            pass
        except:
            pass
        return sorted(apps, key=lambda x: x['name'])[:50] # Limit for performance

    def get_local_ip(self):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            local_ip = s.getsockname()[0]
            s.close()
            return local_ip
        except Exception:
            try:
                return socket.gethostbyname(socket.gethostname())
            except:
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
            try:
                usage = psutil.disk_usage(part.mountpoint)
                disk_temp = 0.0
                # Try to get disk temp if smartmontools is lucky or via sysfs (hwmon)
                # For simplicity, we'll try to find it in sensors later
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
        
        # Network details
        interfaces = []
        try:
            addrs = psutil.net_if_addrs()
            stats = psutil.net_if_stats()
            for nic, addrs_list in addrs.items():
                nic_info = {
                    "name": nic,
                    "ip": "Unknown",
                    "mac": "Unknown",
                    "speed": 0,
                    "up": False
                }
                if nic in stats:
                    nic_info["speed"] = stats[nic].speed
                    nic_info["up"] = stats[nic].isup
                
                for addr in addrs_list:
                    if addr.family == socket.AF_INET:
                        nic_info["ip"] = addr.address
                    elif addr.family == psutil.AF_LINK:
                        nic_info["mac"] = addr.address
                
                interfaces.append(nic_info)
        except:
            pass
            
        net_info = {
            "bytesSent": net_io.bytes_sent,
            "bytesRecv": net_io.bytes_recv,
            "interfaces": interfaces
        }

        # Battery
        battery_info = None
        if hasattr(psutil, "sensors_battery"):
            try:
                bat = psutil.sensors_battery()
                if bat:
                    battery_info = {
                        "percent": bat.percent,
                        "powerPlugged": bat.power_plugged,
                        "secsLeft": bat.secsleft if bat.secsleft != psutil.POWER_TIME_UNLIMITED else -1,
                        "temp": 0.0 # Will be updated in _get_sensors
                    }
            except:
                pass

        # Top processes
        processes = []
        try:
            for proc in psutil.process_iter(['pid', 'name', 'cpu_percent', 'memory_info']):
                try:
                    pinfo = proc.info
                    processes.append({
                        "pid": pinfo['pid'],
                        "name": pinfo['name'] or "Unknown",
                        "cpuPercent": pinfo['cpu_percent'] or 0.0,
                        "memoryBytes": pinfo['memory_info'].rss
                    })
                except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                    pass
        except:
             pass
        
        processes.sort(key=lambda x: x['cpuPercent'], reverse=True)
        top_processes = processes[:Config.TOP_PROCESS_COUNT]

        boot_time = psutil.boot_time()
        uptime_seconds = int(current_time - boot_time)

        # Get detailed per-core frequencies
        freq_per_core = []
        try:
            # 1. Try reading from sysfs directly for better accuracy on Linux
            freq_files = glob.glob("/sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq")
            if freq_files:
                # Sort by core number to match usagePerCore order
                freq_files.sort(key=lambda x: int(re.search(r'cpu(\d+)', x).group(1)))
                for f_path in freq_files:
                    with open(f_path, "r") as f:
                        freq_per_core.append(int(f.read().strip()) / 1000.0) # KHz to MHz
            
            # 2. Fallback to psutil
            if not freq_per_core and hasattr(psutil, "cpu_freq"):
                per_cpu_freqs = psutil.cpu_freq(percpu=True)
                if per_cpu_freqs:
                    freq_per_core = [f.current for f in per_cpu_freqs]
                else:
                    # Global fallback if per-core is empty
                    global_freq = psutil.cpu_freq()
                    if global_freq:
                        freq_per_core = [global_freq.current] * psutil.cpu_count(logical=True)
        except:
            pass

        sensors = self._get_sensors()
        
        self.cached_data = {
            "hostname": self.static_info["hostname"],
            "ip": self.static_info["ip"],
            "os": self.static_info["osName"], 
            "uptime": uptime_seconds,
            
            # New Detailed Sections
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
                "freqPerCore": freq_per_core
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
            'cpuTemp': 0.0, 
            'coreTemps': [],
            'gpu': None, 
            'gpus': [],
            'boardTemp': 0.0, 
            'batteryTemp': 0.0,
            'storageTemp': 0.0,
            'fans': []
        }
        try:
            if hasattr(psutil, "sensors_temperatures"):
                temps = psutil.sensors_temperatures()
                
                cpu_keys = ['coretemp', 'k10temp', 'zenpower', 'cpu_thermal', 'soc_thermal', 'acpitz']
                for key in cpu_keys:
                    if key in temps and temps[key]:
                        valid_temps = [t.current for t in temps[key] if t.current > 0]
                        if valid_temps:
                            sensors['cpuTemp'] = sum(valid_temps) / len(valid_temps)
                            break
                
                board_keys = ['acpitz', 'pch_cannonlake', 'pch_skylake', 'pch_wildcatpoint', 'asus', 'thinkpad']
                for key in board_keys:
                    if key in temps and temps[key]:
                        sensors['boardTemp'] = temps[key][0].current
                        break
                
                if 'battery' in temps and temps['battery']:
                    sensors['batteryTemp'] = temps['battery'][0].current
                
                if 'coretemp' in temps:
                    sensors['coreTemps'] = [t.current for t in temps['coretemp']]
                elif 'k10temp' in temps:
                    sensors['coreTemps'] = [t.current for t in temps['k10temp']]

            # Fallback for CPU Temp (Universal for most Linux including ARM/SBCs)
            if sensors['cpuTemp'] == 0.0:
                try:
                    for zone_path in sorted(glob.glob("/sys/class/thermal/thermal_zone*")):
                        try:
                            with open(os.path.join(zone_path, "type"), "r") as f:
                                t_type = f.read().strip().lower()
                            # Common names for CPU thermal zones
                            if any(x in t_type for x in ["cpu", "soc", "package", "x86_pkg"]):
                                with open(os.path.join(zone_path, "temp"), "r") as f:
                                    temp = int(f.read().strip()) / 1000.0
                                    if temp > 0:
                                        sensors['cpuTemp'] = temp
                                        break
                        except: pass
                    # Absolute fallback to zone0 if still zero
                    if sensors['cpuTemp'] == 0.0 and os.path.exists("/sys/class/thermal/thermal_zone0/temp"):
                        with open("/sys/class/thermal/thermal_zone0/temp", "r") as f:
                            sensors['cpuTemp'] = int(f.read().strip()) / 1000.0
                except: pass

            if hasattr(psutil, "sensors_fans"):
                try:
                    fans = psutil.sensors_fans()
                    for name, entries in fans.items():
                        for entry in entries:
                            sensors['fans'].append({"name": f"{name} {entry.label}".strip(), "rpm": entry.current})
                except: pass

            # GPU Collection
            gpus = []
            # 1. NVIDIA
            try:
                import subprocess
                res = subprocess.check_output(['nvidia-smi', '--query-gpu=name,utilization.gpu,temperature.gpu,memory.used,memory.total', '--format=csv,noheader,nounits'], encoding='utf-8')
                if res:
                    for line in res.strip().split('\n'):
                        parts = line.split(',')
                        gpus.append({
                            "model": parts[0].strip(),
                            "usage": int(parts[1].strip()),
                            "temp": float(parts[2].strip()),
                            "memory": int((float(parts[3].strip()) / float(parts[4].strip())) * 100) if float(parts[4].strip()) > 0 else 0
                        })
            except: pass

            # 2. Others via lspci
            try:
                import subprocess
                res = subprocess.check_output("lspci | grep -iE 'vga|graphics|3d|display'", shell=True, encoding='utf-8')
                if res:
                    for line in [l for l in res.strip().split('\n') if l.strip()]:
                        if any(g["model"].lower() in line.lower() for g in gpus): continue
                        if "controller:" in line: raw_model = line.split("controller:")[1].strip()
                        else: raw_model = re.sub(r'^[0-9a-fA-F:.]+\s+', '', line).strip()
                        
                        curr = {"model": raw_model, "usage": 0, "temp": 0, "memory": 0}
                        if hasattr(psutil, "sensors_temperatures"):
                            t_all = psutil.sensors_temperatures()
                            if 'amdgpu' in t_all and t_all['amdgpu']: curr["temp"] = int(t_all['amdgpu'][0].current)
                            else:
                                for k in ['pch_skylake', 'pch_cannonlake', 'pch_haswell', 'coretemp']:
                                    if k in t_all and t_all[k]:
                                        curr["temp"] = int(t_all[k][0].current); break
                        
                        if any(x in raw_model for x in ["Intel", "Integrated", "Graphics"]):
                            try:
                                compositors = ['Xorg', 'gnome-shell', 'kwin_wayland', 'mutter']
                                gfx_load = 0.0
                                for p in psutil.process_iter(['name', 'cpu_percent']):
                                    if p.info['name'] in compositors: gfx_load += p.info['cpu_percent'] or 0.0
                                curr["usage"] = int(min(gfx_load, 100))
                                mem = psutil.virtual_memory()
                                if hasattr(mem, 'shared') and mem.total > 0: curr["memory"] = int((mem.shared / mem.total) * 100)
                            except: pass
                        gpus.append(curr)
            except: pass

            # 3. DRM (Direct Rendering Manager) - Universal for Linux graphics
            try:
                drm_paths = glob.glob("/sys/class/drm/card*-*") # Check for devices
                if not gpus or len(gpus) < 2: # Only if we haven't found multiple GPUs already
                    for card_path in glob.glob("/sys/class/drm/card[0-9]"):
                        try:
                            vendor_path = os.path.join(card_path, "device/vendor")
                            if os.path.exists(vendor_path):
                                with open(vendor_path, "r") as f:
                                    vendor_id = f.read().strip()
                                
                                # Skip if already found via lspci (deduplication)
                                if any(card_path in g.get("error", "") for g in gpus): continue
                                
                                # Identify by vendor ID if not in gpus
                                vendor_map = {"0x8086": "Intel", "0x1002": "AMD", "0x10de": "NVIDIA"}
                                v_name = vendor_map.get(vendor_id, "Integrated")
                                
                                # Check if already in gpus
                                if any(v_name in g["model"] for g in gpus): continue
                                
                                gpus.append({
                                    "model": f"{v_name} Graphics",
                                    "usage": 0,
                                    "temp": 0,
                                    "memory": 0
                                })
                        except: pass
            except: pass

            sensors['gpus'] = gpus
            sensors['gpu'] = gpus[0] if gpus else {"model": "None", "usage": 0, "temp": 0, "memory": 0}
        except: pass
        return sensors

    # ── Phase 3: Storage Intelligence ──────────────────────────────

    _prev_disk_io = {}
    _prev_disk_io_time = 0

    def get_smart_info(self):
        """Get S.M.A.R.T. health for all physical drives using smartctl."""
        drives = []
        try:
            import subprocess, json as _json
            scan = subprocess.run(
                ['sudo', 'smartctl', '--scan', '--json'],
                capture_output=True, text=True, timeout=10
            )
            scan_data = _json.loads(scan.stdout) if scan.returncode in (0, 1, 2, 4) else {}
            device_list = scan_data.get('devices', [])
            if not device_list:
                for pattern in ['/dev/sd?', '/dev/nvme?n?']:
                    for dev in glob.glob(pattern):
                        device_list.append({"name": dev})
            for dev_entry in device_list:
                dev_path = dev_entry.get("name", dev_entry.get("info_name", ""))
                if not dev_path:
                    continue
                try:
                    result = subprocess.run(
                        ['sudo', 'smartctl', '-a', '--json', dev_path],
                        capture_output=True, text=True, timeout=15
                    )
                    data = _json.loads(result.stdout)
                    model = data.get('model_name', data.get('model_family', 'Unknown'))
                    serial = data.get('serial_number', '')
                    fw = data.get('firmware_version', '')
                    smart_status = data.get('smart_status', {}).get('passed', None)
                    health = 'PASSED' if smart_status else ('FAILED' if smart_status is False else 'Unknown')
                    capacity_bytes = 0
                    user_cap = data.get('user_capacity', {})
                    if isinstance(user_cap, dict):
                        capacity_bytes = user_cap.get('bytes', 0)
                    rotation = data.get('rotation_rate', 0)
                    drive_type = 'SSD' if rotation == 0 else 'HDD'
                    temp = 0
                    temp_data = data.get('temperature', {})
                    if isinstance(temp_data, dict):
                        temp = temp_data.get('current', 0)
                    power_on_hours = 0
                    power_cycle_count = 0
                    ssd_life_left = -1
                    reallocated_sectors = 0
                    total_written_bytes = 0
                    attrs = data.get('ata_smart_attributes', {}).get('table', [])
                    for attr in attrs:
                        aid = attr.get('id', 0)
                        raw_val = attr.get('raw', {}).get('value', 0)
                        if aid == 9: power_on_hours = raw_val
                        elif aid == 12: power_cycle_count = raw_val
                        elif aid == 5: reallocated_sectors = raw_val
                        elif aid in (231, 233): ssd_life_left = attr.get('value', -1)
                        elif aid == 241: total_written_bytes = raw_val * 512
                    nvme_log = data.get('nvme_smart_health_information_log', {})
                    if nvme_log:
                        temp = nvme_log.get('temperature', temp)
                        power_on_hours = nvme_log.get('power_on_hours', power_on_hours)
                        power_cycle_count = nvme_log.get('power_cycles', power_cycle_count)
                        pct_used = nvme_log.get('percentage_used', 0)
                        ssd_life_left = max(0, 100 - pct_used) if pct_used > 0 else ssd_life_left
                        dw = nvme_log.get('data_units_written', 0)
                        total_written_bytes = dw * 512 * 1000 if dw > 0 else total_written_bytes
                    drives.append({
                        "device": dev_path, "model": model, "serial": serial,
                        "firmware": fw, "type": drive_type, "health": health,
                        "temp": temp, "capacityBytes": capacity_bytes,
                        "powerOnHours": power_on_hours, "powerCycleCount": power_cycle_count,
                        "ssdLifeLeft": ssd_life_left, "reallocatedSectors": reallocated_sectors,
                        "totalWrittenBytes": total_written_bytes
                    })
                except Exception as ex:
                    drives.append({"device": dev_path, "model": "Unknown", "health": "Error", "error": str(ex)})
        except FileNotFoundError:
            drives.append({"device": "N/A", "model": "smartmontools not installed", "health": "N/A"})
        except Exception as e:
            drives.append({"device": "N/A", "model": "Error", "health": "Error", "error": str(e)})
        return drives

    def get_disk_io(self):
        """Get real-time disk I/O rates (IOPS + throughput) per device."""
        result = []
        try:
            now = time.time()
            counters = psutil.disk_io_counters(perdisk=True)
            elapsed = now - self._prev_disk_io_time if self._prev_disk_io_time > 0 else 0
            for disk_name, stats in counters.items():
                if disk_name.startswith('loop') or disk_name.startswith('ram'):
                    continue
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
        except Exception as e:
            result.append({"name": "error", "error": str(e)})
        return result

    def get_large_files(self, path="/home", limit=20, min_size_mb=10):
        """Scan for the largest files under a given path with a timeout and depth limit."""
        large_files = []
        min_bytes = min_size_mb * 1024 * 1024
        start_time = time.time()
        timeout = 60 # Max 60 seconds to scan (increased for large HDDs)
        try:
            for dirpath, dirnames, filenames in os.walk(path):
                # Adaptive timeout check
                if time.time() - start_time > timeout:
                    break
                    
                # Skip system dirs and deep nested hidden folders
                skip = ('/proc', '/sys', '/dev', '/run', '/snap', '/tmp', '/.git', '/.cache')
                if any(dirpath.startswith(p) for p in skip):
                    continue
                
                # Limit search depth to avoid infinite loops/extreme I/O on HDD
                if dirpath.count(os.sep) > path.count(os.sep) + 5:
                    dirnames[:] = [] # stop going deeper in this branch
                    continue

                for fname in filenames:
                    fpath = os.path.join(dirpath, fname)
                    try:
                        st = os.lstat(fpath)
                        if st.st_size >= min_bytes:
                            large_files.append({
                                "path": fpath,
                                "sizeBytes": st.st_size,
                                "modified": int(st.st_mtime * 1000)
                            })
                    except (PermissionError, OSError, FileNotFoundError):
                        continue
        except Exception:
            pass
        large_files.sort(key=lambda x: x["sizeBytes"], reverse=True)
        return large_files[:limit]

    # ── Phase 4: Advanced Analytics & Connectivity ────────────────

    def run_speedtest(self):
        """Run an integrated internet speed test using speedtest-cli."""
        try:
            import subprocess, json as _json
            # speedtest-cli is widely available via apt/pip. 
            # Output format: {"ping": 10.5, "download": 100000000, "upload": 50000000, ...}
            cmd = ['speedtest-cli', '--json']
            try:
                res = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
                if res.returncode == 0:
                    data = _json.loads(res.stdout)
                    return {
                        "ping": data.get("ping", 0.0),
                        "downloadBits": data.get("download", 0.0),
                        "uploadBits": data.get("upload", 0.0),
                        "server": data.get("server", {}).get("name", "Unknown"),
                        "sponsor": data.get("server", {}).get("sponsor", "Unknown")
                    }
                else:
                    last_line = res.stderr.strip().split('\n')[-1] if res.stderr else "Return code non-zero"
                    return {"error": f"Speedtest failed ({res.returncode}): {last_line}", "details": res.stderr}
            except subprocess.TimeoutExpired:
                return {"error": "Speedtest timed out after 120s"}
        except FileNotFoundError:
             # Fallback if speedtest-cli isn't installed
             return {"error": "speedtest-cli is not installed on the host"}
        except Exception as e:
             # Final fallback for DNS/Resolution errors
             err_str = str(e)
             if "name resolution" in err_str.lower() or "temporary failure" in err_str.lower():
                 return {"error": "Speedtest failed: DNS Resolution Error. Please check your internet connection or DNS settings on the agent machine."}
             return {"error": err_str}

monitor = SystemMonitor()
