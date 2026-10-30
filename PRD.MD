# Product Requirements Document (PRD): Custom Workout Timer (Android)

**Versi Dokumen:** 1.0  
**Platform:** Android (Native - Kotlin & Jetpack Compose)  
**Arsitektur Penyimpanan:** 100% Local-First (Offline-Capable)  
**Referensi Desain & UX:** Apple Fitness+ (OLED Dark Mode, Neon Rings, Fluid Motion)

---

## 1. Product Overview & Visi Produk

Aplikasi ini adalah **Custom Interval Workout Timer** berbasis Android yang dirancang dengan filosofi *zero-friction* (tanpa hambatan). Aplikasi ini menyelesaikan masalah utama pengguna olahraga mandiri: aplikasi timer standar terlalu kaku dan membosankan, sedangkan aplikasi kebugaran komersial terlalu berat, mewajibkan langganan/login, dan tidak mengizinkan pengguna memasukkan panduan video/GIF gerakan mereka sendiri.

### Nilai Utama (Core Value Proposition)
1. **100% Local & Pribadi:** Tidak perlu registrasi akun, tidak butuh koneksi internet, semua data dan media tersimpan aman di penyimpanan lokal HP pengguna.
2. **Kustomisasi Penuh & Fleksibel:** Pengguna bebas merancang struktur latihan, durasi per gerakan, pengulangan *set/block*, hingga memasang video MP4 atau GIF kustom dari galeri mereka sendiri.
3. **Pengalaman Eksekusi Kelas Atas (Apple Fitness Vibe):** Antarmuka gelap pekat (hemat baterai di layar AMOLED), cincin progres 60fps yang mulus, tipografi angka presisi, serta transisi video tanpa jeda layar hitam (*zero black-flash*).

---

## 2. Target Pengguna & Use Cases

* **Praktisi Home Workout & Calisthenics:** Membutuhkan timer interval (seperti Tabata/HIIT) yang bisa menampilkan klip gerakan referensi tanpa harus bolak-balik membuka YouTube/TikTok.
* **Gym Goers & Program Mandiri:** Menyimpan variasi gerakan dari *coach* atau media sosial dalam bentuk potongan video/GIF pendek dan menyusunnya menjadi jadwal latihan harian berdasarkan durasi waktu luang.

---

## 3. Ruang Lingkup Fitur (Functional Requirements)

| ID | Modul Fitur | Deskripsi & Spesifikasi Teknis | Prioritas |
| :--- | :--- | :--- | :--- |
| **F-01** | **Onboarding & Inisialisasi Lokal** | - Tanpa layar login/register.<br>- Input opsional nama panggilan & pilihan warna aksen utama.<br>- *Database Pre-population*: Otomatis menyuntikkan 2 sampel rutinitas latihan bawaan saat aplikasi pertama kali diinstal. | **P0 (Wajib)** |
| **F-02** | **Home Dashboard & Ringkasan** | - Menampilkan metrik aktivitas mingguan lokal (Total Menit Latihan & Jumlah Sesi Selesai) dengan visualisasi *Activity Ring*.<br>- Menampilkan daftar kartu rutinitas latihan (*Workout Cards*) dengan tombol aksi cepat **"Mulai"**. | **P0 (Wajib)** |
| **F-03** | **Multi-Filter Search Engine** | - **Search by Keyword:** Pencarian *real-time* berdasarkan nama latihan.<br>- **Filter by Category:** *Filter chips* untuk kategori bawaan (`HIIT`, `Strength`, `Core`, `Mobility`) dan kategori kustom buatan pengguna.<br>- **Filter by Total Time:** Filter rentang total durasi (`< 10 Menit`, `10 - 20 Menit`, `> 20 Menit`).<br>- **Sorting:** Urutkan berdasarkan *Favorit*, *Terakhir Dilakukan*, atau *Durasi Tercepat*. | **P0 (Wajib)** |
| **F-04** | **Workout Builder (Manajemen Latihan)** | - Pembuatan dan pengeditan rutinitas latihan.<br>- Mendukung 4 tipe interval: `WARMUP` (Kuning), `WORK` (Hijau Neon), `REST` (Biru Sian), `COOLDOWN` (Ungu).<br>- **Set / Block Looping:** Mengelompokkan beberapa gerakan ke dalam satu blok dan mengatur jumlah pengulangan (misal: `3x Sets`) tanpa duplikasi manual.<br>- **Kamus Gerakan Lokal (Auto-complete):** Menyimpan riwayat nama gerakan beserta video/GIF-nya agar bisa dipakai ulang di rutinitas lain dengan sekali ketuk. | **P0 (Wajib)** |
| **F-05** | **Custom Media Pipeline (Video/GIF)** | - Integrasi *Android Photo Picker* untuk memilih file MP4/WebM atau GIF dari galeri.<br>- Menyalin file ke penyimpanan internal aplikasi (`files/workout_media/`).<br>- Otomatis memotong jalur audio (*strip audio track*) dan mengekstrak *frame* pertama sebagai *thumbnail* menggunakan Media3 Transformer. | **P0 (Wajib)** |
| **F-06** | **Workout Player & Mesin Timer** | - Hitung mundur persiapan (`3 - 2 - 1 - GO`) sebelum sesi dimulai.<br>- Lingkaran progres (*Circular Ring*) 60fps berbasis `SystemClock.elapsedRealtime()`.<br>- Pemutar video *looping* tanpa suara dan transisi antar gerakan otomatis (*Auto-advance*).<br>- Menampilkan informasi **"Up Next"** (gerakan berikutnya).<br>- Berjalan di atas `ForegroundService` dengan notifikasi *live update* saat layar terkunci atau aplikasi di-*minimize*. | **P0 (Wajib)** |
| **F-07** | **Local Backup & Restore** | - Ekspor seluruh database Room (JSON) dan folder `workout_media/` menjadi satu file arsip `.zip`.<br>- Impor file `.zip` untuk memulihkan data saat pengguna berganti perangkat Android. | **P1 (Penting)** |

---

## 4. Spesifikasi UX Behavior & Interaksi Mikro

Bagian ini merupakan standar mutlak kualitas produk agar aplikasi mudah diatur (*manageable*) dan nyaman dioperasikan saat kondisi fisik pengguna sedang lelah/berkeringat.

### 4.1. UX Behavior pada *Workout Builder* (Saat Menyusun Latihan)
1. **Drag-and-Drop Reordering:** Pengguna dapat menekan tahan (*long-press*) pada ikon *handle* di samping item gerakan untuk menggeser urutan ke atas/bawah, disertai umpan balik getaran halus (*light haptic tick*) setiap melewati item lain.
2. **Swipe Actions:**
   * **Geser Kanan (*Swipe Right*):** Menduplikasi item gerakan atau blok *set* secara instan.
   * **Geser Kiri (*Swipe Left*):** Menghapus item gerakan, dilengkapi dengan *Snackbar* **"Undo / Batal"** selama 4 detik untuk mencegah kesalahan hapus.
3. **Quick Time Presets:** Saat mengatur durasi gerakan, selain pemilih angka gulir (*wheel picker*), tersedia tombol pintas instan (`+5s`, `+15s`, `30s`, `45s`, `60s`) untuk input satu jari yang cepat.
4. **Kalkulasi Waktu Real-Time:** Setiap kali pengguna menambah gerakan, mengubah durasi, atau mengubah jumlah pengulangan *Set*, label **"Total Estimasi Waktu"** dan **"Total Gerakan"** di bagian atas layar Builder langsung ter-update secara *real-time*.

### 4.2. UX Behavior pada *Workout Player* (Saat Sesi Latihan Berlangsung)
1. **Zero Black-Screen Video Transition:** Sistem melakukan *pre-loading* pada video gerakan berikutnya saat timer menyisakan 3 detik terakhir. Ketika waktu menyentuh `00:00`, video baru langsung tampil menggunakan animasi *Crossfade* 200ms tanpa jeda layar hitam.
2. **Rest State Preview:** Saat memasuki fase `REST` (Istirahat), warna cincin berubah menjadi Biru Sian dan pemutar media langsung menampilkan *preview* video dari **gerakan yang akan datang** agar pengguna bisa mengambil posisi lebih awal.
3. **On-the-Fly Rest Adjustment:** Khusus pada fase istirahat, muncul dua tombol besar yang mudah dijangkau ibu jari:
   * **`+15 Detik`**: Menambah waktu istirahat secara dinamis jika pengguna masih kelelahan.
   * **`Lewati Istirahat (Skip)`**: Langsung melompat ke gerakan berikutnya jika pengguna sudah siap.
4. **Sweat-Proof Controls (Kontrol Tahan Keringat):**
   * **Ketuk Area Tengah Ring:** Untuk melakukan *Pause / Resume* dengan cepat tanpa perlu mengincar ikon kecil.
   * **Hold-to-End (Tahan 1.5 Detik untuk Berhenti):** Tombol keluar/akhiri latihan tidak akan berfungsi jika hanya diketuk sekali. Pengguna harus menahan tombol selama 1.5 detik (diikuti animasi cincin merah yang terisi penuh + getaran) untuk mencegah latihan terhenti akibat layar tidak sengaja tersentuh.
5. **Smart Audio Ducking:** Suara aba-aba *beep* (pada detik ke-3, 2, 1) tidak boleh mematikan musik dari aplikasi lain (Spotify/YouTube Music). Sistem hanya mengecilkan volume musik latar selama 0.5 detik (*Audio Ducking*) lalu mengembalikannya ke volume normal.
6. **Adaptive Screen Orientation & WakeLock:**
   * Selama timer berstatus `RUNNING`, layar dipaksa tetap menyala (`FLAG_KEEP_SCREEN_ON`).
   * Mendukung orientasi **Portrait** (susunan vertikal: Video di atas, Ring Timer di bawah) dan **Landscape/Tablet** (susunan dua kolom berdampingan secara proporsional menggunakan `WindowSizeClass`).

---

## 5. Arsitektur Data & Skema Database (Room SQLite)

Untuk menjamin performa pencarian filter yang instan dan mencegah duplikasi file video di memori internal, database dirancang dengan 4 tabel relasional berikut:

```text
+-------------------------+       +-------------------------+
|     WorkoutRoutine      |1     *|      WorkoutBlock       |
+-------------------------+------>+-------------------------+
| PK id: Long             |       | PK id: Long             |
|    name: String (Index) |       | FK routineId: Long      |
|    category: String(Idx)|       |    orderIndex: Int      |
|    totalDurationSec: Int|       |    repeatCount: Int     | (1 = Normal, >1 = Loop Set)
|    totalExercises: Int  |       +------------+------------+
|    accentColor: String  |                    | 1
|    thumbnailPath: String|                    |
|    isFavorite: Boolean  |                    | *
|    lastPerformedAt: Long|       +------------v------------+
+-------------------------+       |     WorkoutExercise     |
                                  +-------------------------+
+-------------------------+       | PK id: Long             |
|    ExerciseTemplate     |1     *| FK blockId: Long        |
|    (Kamus Gerakan)      |------>| FK templateId: Long?    |
+-------------------------+       |    orderIndex: Int      |
| PK id: Long             |       |    title: String        |
|    name: String (Unique)|       |    durationSeconds: Int |
|    mediaUri: String?    |       |    type: IntervalType   | (WARMUP, WORK, REST, COOLDOWN)
|    mediaType: MediaType |       |    customMediaUri: Str? |
|    defaultDuration: Int |       +-------------------------+
+-------------------------+
```

### Manajemen Garbage Collection Media (`MediaFileManager`)
* Setiap file video/GIF disimpan di `/data/data/<package_name>/files/workout_media/`.
* Ketika sebuah latihan atau item gerakan dihapus, sistem melakukan pengecekan *Reference Count* pada kolom `mediaUri` di tabel `ExerciseTemplate` dan `WorkoutExercise`.
* Apabila sebuah file video sudah tidak direferensikan oleh entitas mana pun (`referenceCount == 0`), file fisik tersebut otomatis dihapus dari penyimpanan internal agar ukuran aplikasi tetap ramping.

---

## 6. Spesifikasi Desain Visual (Apple Fitness Aesthetic)

| Elemen | Spesifikasi Nilai (Hex / Token) | Penggunaan |
| :--- | :--- | :--- |
| **Background Primary** | `#000000` (Pure OLED Black) | Latar belakang utama seluruh layar untuk kontras maksimal & penghematan daya. |
| **Surface / Card** | `#1C1C1E` (Dark Charcoal) | Latar belakang kartu latihan, *bottom sheet*, dan *search bar* dengan `RoundedCornerShape(20.dp)`. |
| **Surface Elevated** | `#2C2C2E` | Komponen *Filter Chips* dan kontrol tombol sekunder. |
| **Accent - Work (Move)** | `#A4FF00` (Volt Neon Green) | Warna cincin progres utama saat fase gerakan latihan (`WORK`). |
| **Accent - High Intensity** | `#FA114F` (Neon Crimson) | Warna alternatif kartu kategori HIIT/Tabata & indikator *Hold-to-Stop*. |
| **Accent - Rest** | `#00F5EA` (Electric Cyan) | Warna cincin progres saat fase istirahat (`REST`). |
| **Accent - Warmup/Cool** | `#FFD60A` (Amber Yellow) | Warna cincin progres saat pemanasan atau pendinginan. |
| **Typography - Timer** | `Sans-Serif Condensed Bold`, `64.sp`, `fontFeatureSettings = "tnum"` | Wajib menggunakan *Tabular Numbers* agar lebar digit angka selalu identik dan tidak bergeser saat detik berjalan mundur. |

---

## 7. Non-Functional Requirements (Standar Kualitas Teknis)

1. **Akurasi Timer (Zero-Drift):** Selisih akurasi timer tidak boleh lebih dari `±50 milidetik` selama sesi 60 menit, baik saat layar menyala maupun saat perangkat dalam kondisi *Doze Mode / Screen Off*.
2. **Kecepatan Query Search & Filter:** Eksekusi pencarian kombinasi (Teks + Kategori + Durasi) harus merender ulang daftar UI di bawah **16 milidetik** (tidak ada *frame drop* saat mengetik).
3. **Kinerja Memori Pemutar Media:** Maksimal hanya boleh ada **2 instance** `ExoPlayer` aktif secara bersamaan (1 *Current Player* + 1 *Next Preload Buffer*) untuk mencegah `OutOfMemoryError` pada perangkat Android kelas menengah ke bawah.
4. **Kompatibilitas Android:** Minimum SDK **API 26 (Android 8.0)** hingga Target SDK **API 35 (Android 15)** dengan dukungan penuh izin `FOREGROUND_SERVICE_MEDIA_PLAYBACK` atau `SPECIAL_USE`.

---

## 8. Tahapan Implementasi (Development Milestones)

- [ ] **Milestone 1 — Core Data & Media Layer:** Setup proyek Jetpack Compose, skema Room Database (4 tabel), query *Multi-Filter Search*, serta `MediaFileManager` (salin file, kompresi/ekstrak thumbnail, dan *garbage collection*).
- [ ] **Milestone 2 — Workout Builder & Home Search UI:** Implementasi layar Beranda dengan *Filter Chips* interaktif, serta layar *Workout Builder* dengan fitur *drag-and-drop*, *Block/Set Looping*, dan kamus *auto-complete*.
- [ ] **Milestone 3 — Timer Engine & Foreground Service:** Pembuatan `WorkoutTimerService`, integrasi *SoundPool* (dengan *audio ducking*), getaran *haptic*, dan sinkronisasi notifikasi *lock-screen*.
- [ ] **Milestone 4 — Apple Fitness Player UI & Polish:** Pembuatan *Custom Canvas Ring* 60fps, integrasi *dual-buffer* ExoPlayer & Coil GIF, tata letak adaptif Portrait/Landscape, serta fitur *Local Backup/Restore* (`.zip`).