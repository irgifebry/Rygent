# Rygent System Monitor - Linux Agent

**Rygent Agent** is a background application (with a GUI) running on your Linux computer. It securely collects system metrics (CPU, RAM, Disk, Network) and serves them to the Android client app.

## Key Features
- **Fast & Lightweight**: Built with Python and Flask to ensure optimal system performance.
- **Remote Connection**: Fully integrated with Cloudflare Tunnels for secure, real-time remote access to your system without the hassle of setting up public IPs or port forwarding.
- **Simple GUI & System Tray**: Easily controllable via its graphical interface and a discreet System Tray icon.

## Installation Guide (Super Easy!)

An automated installer script is provided to handle dependencies, create application shortcuts, and configure your environment. It automatically supports both **Debian/Ubuntu (.deb)** and **RHEL/Fedora/Arch (.rpm/pacman)** based distributions!

### Steps to Install:
1. Clone this repository (specifically the **Linux** branch):
   ```bash
   git clone -b Linux https://github.com/irgifebry/Rygent.git
   cd Rygent
   ```
2. Grant execution permissions to the installer script:
   ```bash
   chmod +x installer.sh
   ```
3. Run the installer script as Root (Superuser):
   ```bash
   sudo ./installer.sh
   ```
4. You're all set! You can launch the application from your desktop's app menu by searching for **"Rygent Agent"**, or by typing the following command in your terminal:
   ```bash
   rygent-agent
   ```

## Troubleshooting

### 1. Installer Script Fails to Run
- **Error Message**: `Permission denied` or `Run as root`.
- **Solution**: Make sure you use `sudo` when running the installer (`sudo ./rygent-agent-installer.sh`). Also, ensure your internet connection is active, as the installer downloads Python packages and the Cloudflared binary.

### 2. App Fails to Start / GUI Does Not Appear
- **Cause**: Python `Tkinter` or `Pillow` packages are missing, or there are display session issues (Wayland compatibility).
- **Solution**: 
  - Run the app via terminal to view error logs: `rygent-agent`
  - If the error relates to the Display or GUI, make sure your system supports standard X11/Wayland windows. The installer attempts to mitigate this by setting `export DISPLAY=:0; unset WAYLAND_DISPLAY`.

### 3. Remote Tunnel (Cloudflare) Issues
- **Symptoms**: The Android app fails to pair or system metrics are not loading.
- **Solution**: 
  - Check your PC's internet connection.
  - Quit the Rygent Agent completely (including via the 'Quit' option in the System Tray icon) and re-open it to force a Cloudflare tunnel restart.
  - Verify that the Cloudflared binary was downloaded successfully (located in `/opt/rygent-agent/`).

## 🗂 Key File Structure
- `/opt/rygent-agent/` : The main installation directory.
- `~/.rygent/.env` : Current user configuration file containing pairing secrets/tokens.
- `/usr/bin/rygent-agent` : The primary execution shortcut.
