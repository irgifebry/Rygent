import sys
import json
from monitor import SystemMonitor

def main():
    try:
        print("Initializing System Monitor...")
        monitor = SystemMonitor()
        
        print("\n--- System Info ---")
        info = monitor.get_system_info()
        print(f"Hostname: {info.get('hostname')}")
        print(f"CPU Load: {info.get('cpuLoad')}%")
        
        print("\n--- Storage Intelligence ---")
        smart = monitor.get_smart_info()
        print(f"SMART Drives count: {len(smart)}")
        # diskio = monitor.get_disk_io()
        # print(f"Disk IO count: {len(diskio)}")
        # large_files = monitor.get_large_files(path="/home")
        # print(f"Large Files count: {len(large_files)}")
        
        print("\n--- Advanced Analytics ---")
        history = monitor.get_process_history()
        print(f"Process History timeline length: {len(history)}")
        if history:
            print(f"Latest timestamp: {history[-1]['timestamp']}")
            
        alerts = monitor.get_alerts()
        print(f"Active Alerts: {len(alerts)}")
        
        # print("\n--- Speed Test ---")
        # speed = monitor.run_speedtest()
        # print(f"Speed Test Ping: {speed.get('ping', 'N/A')} ms")
        # print(f"Speed Test Download: {speed.get('downloadBits', 0) / 1000000:.2f} Mbps")
        
        print("\nAll Tests Executed Successfully")
    except Exception as e:
        print(f"Exception during tests: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
