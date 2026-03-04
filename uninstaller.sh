#!/bin/bash
# Uninstall Rygent Agent
set -e

# Support for sudo
if [[ $EUID -ne 0 ]]; then
  echo "Error: Run as root (sudo)"
  exit 1
fi

echo ">> Stopping and disabling any running services..."
systemctl stop rygent-agent.service 2>/dev/null || true
systemctl disable rygent-agent.service 2>/dev/null || true
systemctl stop system-monitor.service 2>/dev/null || true
systemctl disable system-monitor.service 2>/dev/null || true

echo ">> Stopping processes..."
pkill -f "python3 agent_gui.py" || true
pkill -f "cloudflared" || true

echo ">> Removing system-wide files..."
rm -vf /usr/bin/rygent-agent
rm -vf /usr/share/applications/rygent-agent.desktop
rm -vf /usr/share/icons/hicolor/512x512/apps/rygent-agent.png
rm -vf /etc/systemd/system/rygent-agent.service
rm -vf /etc/systemd/system/system-monitor.service

echo ">> Removing install directory..."
rm -rfv /opt/rygent-agent

# Reload systemd
systemctl daemon-reload || true

echo ">> Success! Rygent Agent has been uninstalled."
exit 0
