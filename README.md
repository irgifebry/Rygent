# Rygent System Monitor - Ubuntu/Linux Agent

**Rygent Agent** adalah program latar belakang (dan GUI) yang berjalan di komputer Ubuntu/Linux Anda. Program ini bertugas mengumpulkan data sistem (CPU, RAM, Disk, Jaringan) dan menyediakannya untuk aplikasi klien Android.

## 🌟 Fitur Utama
- **Ringan & Cepat**: Dibangun menggunakan Python dan Flask untuk performa yang optimal.
- **Koneksi Lokal & Remote**: 
  - *Lokal*: Menggunakan mDNS (Zeroconf) agar mudah ditemukan secara otomatis di jaringan lokal.
  - *Remote*: Terintegrasi dengan Cloudflare Tunnel untuk akses jarak jauh yang aman tanpa repot *port-forwarding*.
- **GUI Simple & System Tray**: Mudah dikontrol melalui antarmuka (GUI) maupun ikon kecil di System Tray (sudut layar).
- **Auto-Start**: Mendukung *Systemd Service* agar bisa langsung berjalan setiap kali PC dinyalakan.

## 🚀 Panduan Instalasi (Sangat Mudah!)

Kami telah menyediakan script installer yang akan mengatur semuanya untuk Anda (menginstal dependensi, membuat service, dan mengatur environment).

### Cara Instalasi:
1. Unduh proyek ini atau pindah ke branch **Linux**:
   ```bash
   git clone -b Linux https://github.com/irgifebry/Rygent.git
   cd Rygent
   ```
2. Berikan izin eksekusi pada script installer:
   ```bash
   chmod +x rygent-agent-installer.sh
   ```
3. Jalankan script installer sebagai Root (Superuser):
   ```bash
   sudo ./rygent-agent-installer.sh
   ```
4. Selesai! Anda bisa langsung menjalankan aplikasi dari menu aplikasi Ubuntu Anda dengan mencari **"Rygent Agent"**, atau ketik di terminal:
   ```bash
   rygent-agent
   ```

## 🛠️ Troubleshooting (Masalah Umum)

### 1. Script Installer Gagal Dijalankan
- **Pesan Error**: `Permission denied` atau `Run as root`.
- **Solusi**: Pastikan Anda menggunakan `sudo` saat menjalankan installer (`sudo ./rygent-agent-installer.sh`). Pastikan koneksi internet aktif karena installer akan mengunduh paket Python dan Cloudflared.

### 2. Aplikasi Gagal Berjalan / GUI Tidak Muncul
- **Penyebab**: Paket Tkinter atau Pillow untuk Python belum terinstal dengan benar, atau masalah pada Display Server (Wayland).
- **Solusi**: 
  - Jalankan via terminal untuk melihat pesan error: `rygent-agent`
  - Jika error terkait *Display* atau GUI, pastikan sistem Anda berjalan di sesi yang mendukung jendela aplikasi standar X11/Wayland. Installer sudah mengatur `export DISPLAY=:0; unset WAYLAND_DISPLAY` di eksekutornya untuk meminimalisir masalah ini.

### 3. HP (Android) Tidak Bisa Terhubung via Lokal
- **Penyebab**: Firewall memblokir port 5000 / mDNS.
- **Solusi**: Izinkan port 5000 di UFW:
   ```bash
   sudo ufw allow 5000/tcp
   sudo ufw allow 5353/udp
   ```

### 4. Remote Tunnel (Cloudflare) Mengalami Masalah
- **Gejala**: URL tunnel tidak muncul di layar PC, atau stuck saat *pairing*.
- **Solusi**: 
  - Pastikan binary Cloudflared berhasil diunduh (cek isi folder `/opt/rygent-agent/`).
  - Tutup aplikasi Rygent Agent sepenuhnya (juga dari ikon System Tray di sudut kanan bawah) dan buka kembali.

## 🗂 Struktur File Penting
- `/opt/rygent-agent/` : Titik utama aplikasi diinstal.
- `~/.rygent/.env` : File konfigurasi (Password / Token) milik pengguna saat ini.
- `/usr/bin/rygent-agent` : Pintasan utama (Shortcut) eksekusi.
