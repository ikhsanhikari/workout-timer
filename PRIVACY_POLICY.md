# Kebijakan Privasi / Privacy Policy - GasCount

**Tanggal Berlaku:** 8 November 2026  
**Aplikasi:** GasCount (Interval & Circuit Workout Timer)  
**Tautan Publik (GitHub Pages):** [https://ikhsanhikari.github.io/workout-timer/](https://ikhsanhikari.github.io/workout-timer/)

---

## 🇮🇩 Bahasa Indonesia

### 1. Komitmen Privasi (Offline-First)
Aplikasi **GasCount** beroperasi sepenuhnya dengan arsitektur **Offline-First**. Kami tidak pernah mengumpulkan, mencatat, melacak, membagikan, atau menjual data pribadi Anda. Seluruh riwayat latihan, konfigurasi waktu, dan preferensi aplikasi tersimpan 100% di memori lokal perangkat Anda.

### 2. Informasi yang Diproses
Aplikasi **tidak mengumpulkan data identitas pribadi (PII)** seperti nama, email, lokasi GPS, atau nomor telepon. Data yang diproses hanya data latihan lokal:
* Nama rutinitas dan durasi interval (work/rest).
* Preferensi suara (audio coach), musik, dan getaran haptic.
* Waktu alarm pengingat latihan harian.

### 3. Izin Android & Tujuannya
* `POST_NOTIFICATIONS`: Menampilkan status hitung mundur timer dan pengingat latihan harian.
* `FOREGROUND_SERVICE_HEALTH`: Menjaga timer dan audio coach tetap aktif saat layar mati.
* `SCHEDULE_EXACT_ALARM` & `USE_EXACT_ALARM`: Memastikan alarm pengingat harian berbunyi tepat waktu.
* `RECEIVE_BOOT_COMPLETED`: Menjadwalkan ulang pengingat latihan setelah ponsel direstart.
* `VIBRATE` & `MODIFY_AUDIO_SETTINGS`: Umpan balik getaran dan pengaturan volume suara pelatih.
* `CAMERA` / `READ_MEDIA_*`: Opsional, hanya jika pengguna memilih untuk menyertakan video demonstrasi gerakan sendiri ke katalog lokal.

### 4. Layanan Pihak Ketiga & Iklan
GasCount **tidak menggunakan SDK analitik pihak ketiga** dan **tidak memuat iklan**. Tidak ada data pengguna yang dikirim ke jaringan luar.

### 5. Penghapusan Data
Pengguna memiliki kontrol penuh. Menghapus data aplikasi melalui pengaturan ponsel atau mencopot (uninstall) aplikasi akan langsung menghapus seluruh data secara permanen.

### 6. Kontak Pengembang
* **Pengembang:** Ikhsan (GasCount Developer)
* **Email:** ikhsan.hikari@gmail.com
* **Repository:** [https://github.com/ikhsanhikari/workout-timer](https://github.com/ikhsanhikari/workout-timer)

---

## 🇬🇧 English

### 1. Offline-First Privacy Guarantee
**GasCount** operates exclusively as an **Offline-First** application. We do not collect, store, track, share, or sell any personal information. All your workouts, timer profiles, and activity logs remain strictly on your local device storage.

### 2. Data Collected
We collect **zero Personally Identifiable Information (PII)**. No online accounts or logins are required. All workout history and audio coach settings reside locally in the device's Room database.

### 3. Android Permissions
* `POST_NOTIFICATIONS`: For ongoing timer countdown alerts and daily reminders.
* `FOREGROUND_SERVICE_HEALTH`: Ensures precise interval tracking and voice cues when the screen is locked.
* `SCHEDULE_EXACT_ALARM` & `USE_EXACT_ALARM`: Triggers workout reminders at the exact scheduled time.
* `RECEIVE_BOOT_COMPLETED`: Restores scheduled alarms following device reboot.
* `VIBRATE` & `MODIFY_AUDIO_SETTINGS`: Haptic pulses and audio ducking.
* `CAMERA` / `READ_MEDIA_*`: Optional, only for custom exercise demonstration videos recorded by the user.

### 4. Third-Party Services
GasCount contains no third-party tracking frameworks, analytics services, or advertisements.

### 5. Contact
* **Developer:** Ikhsan (GasCount Developer)
* **Email:** ikhsan.hikari@gmail.com
* **Repository:** [https://github.com/ikhsanhikari/workout-timer](https://github.com/ikhsanhikari/workout-timer)

