# Rygent System Monitor - Android Client

Rygent (Android) adalah aplikasi klien pemantau sistem (System Monitor) yang memungkinkan Anda untuk memantau status PC/Server (Ubuntu/Linux) Anda secara real-time dari perangkat Android. Aplikasi ini berpasangan dengan "Rygent Agent" yang berjalan di komputer Anda.

## 🌟 Fitur Utama
- **Pemantauan Real-time**: Melihat penggunaan CPU, RAM, Disk, dan Jaringan komputer secara langsung.
- **Koneksi Jarak Jauh (Remote)**: Terhubung ke komputer Anda dari mana saja melalui internet menggunakan Cloudflare Tunnel.
- **Deteksi Otomatis (Local Network)**: Menemukan komputer di jaringan lokal secara otomatis tanpa perlu memasukkan IP manual.
- **Tampilan Modern**: Antarmuka pengguna yang bersih, modern, dan mudah digunakan (dibangun dengan Jetpack Compose).

## 🚀 Panduan Instalasi (Development)

Karena ini adalah proyek kode sumber (source code), Anda memerlukan **Android Studio** untuk membangun (build) dan menginstal aplikasinya.

### Persyaratan Sistem
- Android Studio (versi terbaru yang mendukung Gradle 8+ disarankan).
- JDK 17 atau lebih baru.
- Perangkat Android fisik atau Emulator (Android 8.0+ / API Level 26+).

### Langkah-langkah Build:
1. Pindah (Checkout) ke branch **Android**:
   ```bash
   git clone https://github.com/irgifebry/Rygent.git
   cd Rygent
   git checkout Android
   ```
2. Buka folder proyek ini di **Android Studio**.
3. Tunggu hingga proses Sinkronisasi Gradle (Gradle Sync) selesai.
4. Hubungkan HP Android Anda (pastikan USB Debugging aktif) atau jalankan Emulator.
5. Klik tombol **"Run"** (Simbol Play warna hijau) di Android Studio.

## 🛠️ Troubleshooting (Masalah Umum)

### 1. Aplikasi Gagal Build / Sinkronisasi Gradle Error
- **Penyebab**: Versi JDK tidak sesuai atau cache Gradle rusak.
- **Solusi**: 
  - Pastikan Android Studio menggunakan JDK 17 (Cek di `File > Project Structure > SDK Location`).
  - Lakukan `File > Invalidate Caches / Restart`.
  - Coba bersihkan proyek dengan: `Build > Clean Project` lalu `Build > Rebuild Project`.

### 2. Tidak Bisa Menemukan PC (Local Network)
- **Penyebab**: Koneksi WiFi berbeda, isolasi jaringan, atau *Rygent Agent* belum berjalan di PC.
- **Solusi**:
  - Pastikan HP dan PC terhubung ke WiFi/jaringan yang **sama**.
  - Pastikan aplikasi *Rygent Agent* di Ubuntu sudah berjalan dan tidak diblokir oleh Firewall (UFW).
  - Jika masih gagal, Anda bisa menggunakan mode "Add by IP" secara manual.

### 3. Koneksi Remote (Cloudflare) Gagal
- **Penyebab**: URL tunnel salah, atau internet memblokir koneksi.
- **Solusi**:
  - Pastikan Anda memindai QR Code atau memasukkan URL Cloudflare dengan benar (berakhiran `.trycloudflare.com`).
  - Pastikan PC Anda terhubung ke internet yang stabil untuk menjalankan tunnel.

---
*Dibuat untuk mempermudah pemantauan sistem Anda di mana saja.*
