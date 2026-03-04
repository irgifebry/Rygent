# Rygent System Monitor - Ubuntu/Linux Agent

**Rygent Agent** adalah program latar belakang (dan GUI) yang berjalan di komputer Ubuntu/Linux Anda. Program ini bertugas mengumpulkan data sistem (CPU, RAM, Disk, Jaringan) dan menampilkannya untuk aplikasi klien Android.

## 🌟 Fitur Utama
- **Ringan & Cepat**: Dibangun menggunakan Python dan Flask untuk performa yang optimal.
- **Koneksi Remote**: Terintegrasi penuh dengan Cloudflare Tunnel untuk akses jarak jauh yang aman dan *real-time* ke sistem Anda tanpa repot mengatur *IP Publik* atau *Port Forwarding*.
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
- **Penyebab**: Paket Tkinter atau Pillow untuk Python belum terinstal dengan benar, atau rintangan Display Server (Wayland).
- **Solusi**: 
  - Jalankan via terminal untuk melihat pesan error: `rygent-agent`
  - Jika error terkait *Display* atau GUI, pastikan sistem Anda berjalan di sesi yang mendukung jendela aplikasi standar X11/Wayland. Installer sudah mengatur `export DISPLAY=:0; unset WAYLAND_DISPLAY` di eksekutornya untuk meminimalisir masalah ini.

### 3. Koneksi Remote Tunnel (Cloudflare) Bermasalah
- **Gejala**: HP gagal pairing atau data pemantauan tidak jalan.
- **Solusi**: 
  - Periksa jaringan internet komputer Ubuntu Anda.
  - Tutup aplikasi Rygent Agent sepenuhnya (juga dari *Quit* di ikon System Tray) dan buka kembali agar tunnel Cloudflared dipanggil ulang secara otomatis.
  - Pastikan file biner dari Cloudflare berhasil terunduh dengan baik (Ada di dalam `/opt/rygent-agent/`).

## 🗂 Struktur File Penting
- `/opt/rygent-agent/` : Titik utama aplikasi diinstal.
- `~/.rygent/.env` : File konfigurasi (Password / Token) milik pengguna saat ini.
- `/usr/bin/rygent-agent` : Pintasan utama (Shortcut) eksekusi.
