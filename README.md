# Rygent System Monitor - Android Client

Rygent (Android) is a system monitoring client app that allows you to monitor the real-time status of your PC/Server (Linux) directly from your Android device. It pairs perfectly with the "Rygent Agent" running on your computer.

## 🌟 Key Features
- **Real-time Monitoring**: Get live metrics of your computer's CPU, RAM, Disk, and Network usage.
- **Remote Connection**: Connect to your computer from anywhere over the internet securely via Cloudflare Tunnel.
- **Modern Interface**: A clean, sleek, and intuitive User Interface designed for easy navigation.

## 🚀 Installation Guide

> [!IMPORTANT]
> **Need to install the Rygent Agent on your PC?** 
> The Linux agent code and its installation instructions are located in a separate branch. Please switch to the **[Linux Branch](https://github.com/irgifebry/Rygent/tree/Linux)** to view that documentation.

### 📱 Android App Installation
Installing the Android app is incredibly simple. There is no need to compile the code manually using Android Studio.

1. Navigate to the **[Releases](https://github.com/irgifebry/Rygent/releases)** page of this GitHub repository.
2. Download the latest `.apk` file (e.g., `Rygent-v1.0.apk`).
3. Open the downloaded `.apk` file on your Android device. (You may need to select "Install Anyway" and "Allow from unknown sources" if prompted, as this app is not distributed via the Play Store).
4. Once installed, launch the app and pair it with your PC using the QR code.

## 🛠️ Troubleshooting

### 1. App Fails to Install
- **Cause**: Android security settings blocking the installation.
- **Solution**: Navigate to your device settings and ensure that the "Install from Unknown Sources" option is enabled.

### 2. Remote Connection Fails
- **Cause**: Your PC might be offline, misconfigured, or the Cloudflare Tunnel might be down.
- **Solution**:
  - Ensure your Linux PC is powered on and connected to a stable internet network.
  - Re-scan the pairing QR code from the Rygent Agent desktop app.
  - Try restarting the app on your Android device.

---
*Built to simplify your system monitoring anywhere, anytime.*
