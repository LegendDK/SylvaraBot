# 🌲 SylvaraBot v1.0 - Ultimate Automation & Lumberjack Mod

**SylvaraBot** adalah mod otomatisasi tingkat lanjut yang dibangun khusus untuk ekosistem server kustom Minecraft berbasis Fabric (1.21.1+). Mod ini dirancang untuk memaksimalkan efisiensi *farming*, manajemen rantai produksi (*auto-chain*), manajemen logistik ransel, konsumsi item dinamis, serta perlindungan keamanan akun dari interupsi staf maupun bahaya in-game.

---

## 🛠️ Versi & Kebutuhan Sistem (Compatibility)

Untuk menjalankan SylvaraBot dengan lancar tanpa kendala *crash*, pastikan lingkungan pengembangan atau *runtime* game Anda memenuhi spesifikasi berikut:

* **Minecraft Version:** `1.21.1` (Kompatibel hingga `1.21.11`)
* **Mod Loader:** Fabric Loader (`0.19.3` atau versi terbaru)
* **Java Version:** Java `21`
* **Library Utama:** Fabric API, Cloth Config, Gson (Built-in)

---

## ✨ Daftar Fitur Lengkap

1. **Auto Lumberjack & A\* Pathfinding**
   * Navigasi otomatis menggunakan algoritma pencarian jalur *A\* Pathfinding* untuk berjalan dari satu titik *waypoint* ke titik lainnya dengan aman.
   * Deteksi otomatis objek pohon/area tebang dan sistem anti-cooldown waypoint.
   * Penyelesaian otomatis minigame GUI server (seperti mengklik *lime dye* atau *lime stained glass pane* secara presisi).

2. **Smart Auto Chain (Rantai Produksi Otomatis)**
   * **Full Chain:** Otomatis memotong Log menjadi Plank di *Saw Bench*, lalu melanjutkan pemrosesan Plank menjadi *Wooden Box* di *Craft Bench*.
   * **Saw Only:** Hanya memproses Log menjadi Plank secara terus-menerus.
   * **Craft Only:** Hanya memproses Plank menjadi *Wooden Box*.
   * *Safety Stop:* Otomatis melakukan *disconnect* yang aman saat seluruh rantai tugas selesai dan sisa material habis terproses.

3. **Integrasi Ransel Pintar (`/ransel`)**
   * **Auto Deposit:** Saat tumpukan log di inventaris mencapai $\ge 18$ stack, bot otomatis membuka perintah `/ransel` dan menyimpan seluruh log ke dalam penyimpanan ransel.
   * **Auto Withdraw Material:** Jika bahan mentah (log/plank) habis di inventaris, bot otomatis mengambil suplai dari ransel berdasarkan sisa slot kosong yang tersedia.
   * **Auto Restock Consumables:** Mengambil cadangan makanan atau minuman dari ransel jika stok di *hotbar* habis.

4. **Multi-Item Auto Eat & Auto Drink (JSON-Based)**
   * Mendeteksi makanan dan minuman kustom server menggunakan kombinasi **Custom Model Data** (seperti `10052.0` s.d. `10055.0`), animasi item (`EAT` / `DRINK`), serta kecocokan nama item.
   * **Multi-Select Toggles:** Pengguna dapat memilih dan mencentang lebih dari satu jenis makanan atau minuman secara fleksibel.
   * Dikelola melalui file eksternal `sylvarabot_consumables.json`.

5. **Proteksi Keamanan (Anti-Staff & Safety Disconnect)**
   * **Anti-Staff Pause:** Memantau chat server secara real-time. Jika mendeteksi pesan bahwa Staff masuk mode *on duty* (`is now on duty!`), bot akan langsung menjeda total seluruh pergerakan dan aktivitas untuk menghindari risiko pelanggaran/banned. Bot akan melanjutkan aktivitas otomatis setelah staff *off duty*.
   * **Safety Disconnect Instan:** Bot langsung memutuskan koneksi game jika darah pemain kritis di bawah 5 bar, pemain mengalami *knocked/down* (terdeteksi dari chat atau status player), atau karakter diangkat/dinaiki oleh pemain lain terlalu lama.

6. **Modern GUI Control Center & Emergency Shortcut**
   * Antarmuka grafis modern berbasis *card layout* terbagi menjadi panel kontrol utama, pengaturan bench/toggles, dan manajemen waypoint.
   * **Shortcut Tombol Darurat (`X`):** Tombol pintas cepat untuk melakukan *emergency stop* dan menghentikan seluruh tindakan bot secara instan di dalam game.

---

## ⚙️ Cara Kerja Sistem (How It Works)

* **Farming & Task Loop:** Ketika tombol *Start Farm* ditekan, bot mengeksekusi rute waypoint secara berurutan. Saat tiba di pohon, bot menyelaraskan sudut kamera (`yaw` dan `pitch`) menggunakan fungsi interpolasi halus (`0.15F`) untuk menghindari masalah *flicker*, lalu melakukan klik interaksi.
* **GUI Solver Loop:** Setelah bench diklik, bot mendeteksi container GUI yang terbuka, mencari slot item verifikasi minigame, mengeksekusinya dalam beberapa tick, lalu menunggu progress selesai.
* **Consumable Loop:** Saat tingkat kelaparan $\le 6$ atau ada peringatan haus dari server (*"Kamu mulai haus"* / *"Air:"*), bot menjeda aktivitas utama sementara, memindahkan item makanan ke hotbar slot 7 atau minuman ke slot 8, menahan tombol *use* (klik kanan) hingga animasi konsumsi selesai, lalu melanjutkan kembali tugas utamanya tanpa hambatan.

---

## 📂 Panduan Konfigurasi (Cara Setting)

SylvaraBot menggunakan file konfigurasi berbasis JSON yang terletak di dalam folder **`config/`** direktori Minecraft Anda.

### 1. `sylvarabot_benches.json`
Menyimpan data waypoint rute, koordinat serta arah hadap (*yaw/pitch*) untuk *Saw Bench* dan *Craft Bench*, serta status *toggles* global (Backpack, Auto Eat, Auto Drink, Anti-Staff). File ini otomatis diperbarui saat Anda menekan tombol *Set Saw* atau *Set Craft* di dalam GUI.

### 2. `sylvarabot_consumables.json`
Menyimpan daftar nama makanan dan minuman yang diizinkan untuk dikonsumsi bot. File ini memiliki struktur multi-pilihan dengan parameter `enabled: true/false`. 

Contoh konfigurasi **`sylvarabot_consumables.json`**:
```json
{
  "foods": [
    { "name": "Roasted Beef and Egg", "enabled": true },
    { "name": "Roasted Chicken", "enabled": true }
  ],
  "drinks": [
    { "name": "Matcha Milkshake", "enabled": true },
    { "name": "Melon Juice", "enabled": false }
  ]
}
```
*Catatan: Anda dapat mengedit file ini secara bebas menggunakan Notepad atau Text Editor kapan saja.*

---

## 🎮 Panduan Penggunaan (Cara Pakai)

1. **Membuka GUI Utama:**
   * Masuk ke dalam game, lalu tekan tombol pintas untuk membuka layar **Sylvara GUI** (`SylvaraScreen`).
2. **Menyetel Waypoint & Bench:**
   * Berdirilah di dekat rute pohon, lalu klik tombol **+ Add Position** di panel *Waypoint Management*.
   * Berdirilah di depan *Saw Bench*, hadap ke arah yang benar, lalu klik tombol **Set Saw**.
   * Lakukan hal yang sama di depan *Craft Bench* lalu klik tombol **Set Craft**.
3. **Mengatur Makanan & Toggles:**
   * Di panel kiri bawah, klik tombol **⚙ Atur Makanan & Toggles** untuk masuk ke sub-menu `SylvaraSettingsScreen`.
   * Di sini Anda dapat mengaktifkan/menonaktifkan *Backpack*, *Staff Pause*, serta mencentang item makanan & minuman mana saja yang boleh dikonsumsi bot.
   * Klik **← Kembali ke Menu Utama**.
4. **Memulai Bot:**
   * Klik tombol **▶ Start Farm** untuk memulai *farming* waypoint biasa, atau klik **⚡ Auto Chain** untuk langsung menjalankan mode pemrosesan kayu berantai.
   * Tekan tombol **`X`** di keyboard kapan saja jika Anda ingin melakukan *Emergency Stop* darurat.