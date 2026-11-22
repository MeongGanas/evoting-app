# EvotingAPP — Sistem E-Voting Terenkripsi & Transparansi Pemilu

**EvotingAPP** adalah aplikasi Android berbasis Java yang dikembangkan untuk mendukung **SDG 16: Peace, Justice, and Strong Institution**. Aplikasi ini menghadirkan mekanisme pemungutan suara elektronik yang aman dengan enkripsi **AES** dan audit integritas berbasis **SHA-256 Hash Chain**, sekaligus membuka akses transparansi profil kandidat, laporan dana kampanye, dan pelaporan dugaan pelanggaran secara lokal melalui SQLite.

---

## Fitur Utama (5 Fitur Inti)

1. **Papan Transparansi Pemilu**
   Menampilkan daftar kandidat, visi-misi, ringkasan laporan dana kampanye, serta hasil rekapitulasi akhir setelah kotak suara resmi dibuka.
2. **E-Voting Terenkripsi (AES) & Verifikasi Receipt Hash**
   Pemilih terdaftar (DPT) dapat menyalurkan suara satu kali selama pemungutan suara berstatus `OPEN`. Pilihan dienkripsi menggunakan AES dan disimpan tanpa identitas pemilih maupun waktu mencoblos. Pemilih menerima *Receipt Hash* (SHA-256) yang dapat diverifikasi keberadaannya di papan publik.
3. **Pelaporan Dugaan Pelanggaran (Mode Anonim & Teridentifikasi)**
   Pengguna dapat melaporkan indikasi kecurangan dengan memilih mode identitas anonim atau teridentifikasi (*Strategy Pattern*), lalu memantau status tindak lanjut dari Pengawas melalui **Kode Tiket** yang diberikan saat laporan dikirim (tetap berlaku untuk laporan anonim).
4. **Audit Integritas Kotak Suara (*Tamper Check* Hash Chain)**
   Pengawas (Panwas) dapat memvalidasi seluruh rantai hash SHA-256 pada data surat suara untuk mendeteksi manipulasi data (*Tamper Alert*).
5. **Manajemen Pemilu & Otorisasi Pembukaan Kotak Suara (2 PIN)**
   Panitia (KPU) mendaftarkan pemilih ke **Daftar Pemilih Tetap (DPT)** (satu NIK satu akun; pemilih tidak dapat mendaftar sendiri), mengelola data kandidat dan dana kampanye, serta membuka/menutup pemungutan suara. Dekripsi dan penghitungan suara hanya dapat dilakukan apabila **PIN KPU** dan **PIN Panwas** valid serta kotak suara lolos *Tamper Check*.

### Siklus Status Pemilu (`ElectionState`)

```text
NOT_STARTED ──(KPU buka)──> OPEN ──(KPU tutup)──> CLOSED ──(2 PIN valid + Tamper Check lolos)──> PUBLISHED
                                                     │
                                                     └──(Tamper Check gagal)──> TAMPERED
```

---

## Peran Pengguna & Akun Demo

Aplikasi mendukung **3 peran utama** dengan data *seeding* otomatis saat database pertama kali dibuat untuk mempermudah pengujian (*user testing*) dan demonstrasi:

| Peran | Username | Password | PIN Otorisasi | Hak Akses Utama |
| :--- | :--- | :--- | :--- | :--- |
| **Pemilih / Publik** | `pemilih1` – `pemilih5` | `123456` | - | Lihat papan transparansi, mencoblos, cek *receipt hash*, lapor & lacak pelanggaran |
| **Pengawas (Panwas)** | `panwas1` | `123456` | `2222` | Jalankan *Tamper Check*, tindak lanjut status laporan, input PIN buka kotak |
| **Panitia (KPU)** | `kpu1` | `123456` | `1111` | Daftarkan pemilih (DPT), kelola kandidat & dana kampanye, buka/tutup pemungutan suara, input PIN buka kotak |

> **Fitur Bantu Demo & Evaluasi:**
> - **Isi Akun Demo** di halaman Login untuk berpindah peran dengan cepat saat *user testing*.
> - **Simulasi Manipulasi DB** di dashboard Panwas untuk menguji deteksi *Tamper Alert* secara langsung.
> - **Reset Data Demo** di dashboard KPU untuk mengembalikan database ke kondisi awal sebelum sesi *user testing* berikutnya.
> - Tersedia 5 akun pemilih agar minimal 5 user berbeda dapat menguji alur mencoblos.

### Skenario Uji Cepat

1. Login `kpu1` → (opsional) daftarkan pemilih baru di halaman **Daftar Pemilih Tetap** → **Buka** pemungutan suara.
2. Login `pemilih1` … `pemilih5` → Bilik Suara → pilih kandidat → simpan *Receipt Hash* → verifikasi di Papan Transparansi.
3. (Opsional) Kirim laporan anonim, catat **Kode Tiket**, lalu login `panwas1` untuk mengubah statusnya.
4. Login `kpu1` → **Tutup** → isi PIN KPU `1111` + PIN Panwas `2222` → **Buka Kotak & Hitung Suara**.
5. Untuk menguji *Tamper Alert*: sebelum langkah 4, login `panwas1` → **Simulasi Manipulasi DB** → **Jalankan Tamper Check**.

---

## Diagram UML

Diagram hanya memuat **fitur utama**. Bagian pendukung yang bersifat umum (layar/Activity, login, perakitan dependensi, dan *helper* database SQLite) sengaja tidak dicantumkan.

| Diagram | Gambar | Sumber PlantUML |
| :--- | :--- | :--- |
| Use Case Diagram | [use case.png](use%20case.png) | [use_case.puml](use_case.puml) |
| Class Diagram | [class diagram.png](class%20diagram.png) | [class_diagram.puml](class_diagram.puml) |
| Activity Diagram | [activity diagram.png](activity%20diagram.png) | [activity_diagram.puml](activity_diagram.puml) |

### Pemetaan Fitur ke Kelas Utama

| Fitur | Service | Pendukung |
| :--- | :--- | :--- |
| 1. Papan Transparansi | `CandidateService`, `TallyService.getPublishedResults()` | `ICandidateRepository`, `IElectionRepository` |
| 2. E-Voting & Receipt Hash | `VotingService.castVote()`, `BallotAuditService.verifyReceipt()` | `IBallotCipher` (AES), `IHashChainService` (SHA-256), `IBallotRepository` |
| 3. Pelaporan Pelanggaran | `ReportService` | `IIdentityStrategy` (`AnonymousStrategy` / `IdentifiedStrategy`), `IReportRepository` |
| 4. Tamper Check | `BallotAuditService.runTamperCheck()` | `IHashChainService`, `IBallotRepository` |
| 5. Manajemen Pemilu & Buka Kotak | `VoterRegistryService`, `ElectionService`, `CandidateService`, `TallyService.unlockAndTally()` | `IUserRepository` (`registerVoter()`, `verifyRolePin()`), `IElectionRepository` |

---

## Daftar Layar & Alur UI (Referensi Desain Stitch)

Aplikasi memakai tema gelap (latar biru-abu tua, aksen toska) dengan 7 layar utama:

1. **Login Screen (`LoginActivity`)**
   - Input username & password, tombol Login, serta tombol pintas *Isi Akun Demo* (Pemilih / Panwas / KPU).
2. **Home / Papan Transparansi (`TransparencyActivity`)**
   - Tab/Bagian: Daftar Kandidat (nama, visi-misi, total & rincian dana kampanye), Hasil Rekapitulasi (terkunci sebelum dibuka, tampil grafik/persentase setelah *PUBLISHED*), dan kotak input **Verifikasi Receipt Hash**.
3. **Bilik Suara & Struk Digital (`VotingActivity`)** *(Khusus Pemilih)*
   - Daftar kartu kandidat dengan tombol pilih & dialog konfirmasi.
   - Setelah suara terkirim, menampilkan kartu **Struk Receipt Hash** (kode SHA-256) dengan tombol salin.
4. **Laporan Pelanggaran (`ReportActivity`)** *(Pemilih & Panwas)*
   - Form kirim laporan dengan *switch/toggle* **Kirim sebagai Anonim**; setelah terkirim, pelapor menerima **Kode Tiket**.
   - Kolom **Lacak Laporan** berdasarkan Kode Tiket beserta badge status (`SUBMITTED`, `IN_REVIEW`, `RESOLVED`, `REJECTED`).
   - Khusus Panwas: daftar seluruh laporan dan tombol ubah status.
5. **Dashboard Pengawas (`PanwasActivity`)** *(Khusus Panwas)*
   - Kartu status integritas kotak suara (Aman / *TAMPERED*), tombol **Jalankan Tamper Check**, tombol **Simulasi Manipulasi DB**, dan akses ke daftar tindak lanjut laporan.
6. **Dashboard Panitia & Buka Kotak (`KpuActivity`)** *(Khusus KPU)*
   - Tautan ke halaman **Daftar Pemilih Tetap** beserta ringkasan jumlah pemilih.
   - Form tambah/edit kandidat & laporan dana kampanye, tombol **Buka / Tutup Pemungutan Suara**, serta panel **Buka Kotak Suara** yang meminta input **PIN KPU** + **PIN Panwas** untuk memicu dekripsi dan publikasi hasil.

7. **Daftar Pemilih Tetap (`VoterRegistryActivity`)** *(Khusus KPU)*
   - Ringkasan jumlah pemilih terdaftar dan yang sudah memilih, formulir pendaftaran (NIK 16 digit, username, kata sandi awal), pencarian username, dan daftar pemilih dengan NIK disamarkan serta status sudah/belum memilih.

---

## Arsitektur & Penerapan Prinsip SOLID

Aplikasi menggunakan **Layered Architecture** (`ui` → `service` → `repository`) agar ringan dikembangkan di Android Java namun tetap bersih dan terstruktur.

### Struktur Package (`com.example.evotingapp`)

```text
com.example.evotingapp/
├── model/          # Entitas data & enum
│                   #   Candidate, EncryptedBallot, ViolationReport, TallyResult, ReportStatus, ElectionState
├── role/           # Hierarki pengguna & interface hak akses
│                   #   User, Voter, Panwas, KpuAdmin, IVotable, IAuditCapable, IElectionManager
├── repository/     # Kontrak (interface) & implementasi SQLite
│                   #   IUserRepository, IBallotRepository, ICandidateRepository,
│                   #   IElectionRepository, IReportRepository + SQLite*Repository
├── service/        # Logika bisnis, satu tanggung jawab per service
│                   #   VotingService, BallotAuditService, TallyService,
│                   #   ElectionService, CandidateService, ReportService, VoterRegistryService
│                   #   + AesBallotCipher, Sha256HashChainService,
│                   #     IIdentityStrategy (AnonymousStrategy / IdentifiedStrategy)
└── ui/             # Activity & Adapter Android XML
```

> Kelas pendukung umum seperti `LoginActivity`, `AuthService`, `AppContainer` (perakit dependensi), dan `ElectionDbHelper` (pembuat tabel & *seeding* SQLite) tetap ada di kode, tetapi tidak dicantumkan di Class Diagram karena bukan bagian dari fitur utama.

### Pemetaan SOLID

| Prinsip | Penerapan |
| :--- | :--- |
| **S**ingle Responsibility | Setiap service hanya punya satu alasan berubah: `VotingService` (mencoblos), `BallotAuditService` (verifikasi receipt & tamper check), `TallyService` (buka kotak & rekap), `ElectionService` (status pemilu), `CandidateService` (kandidat & dana), `ReportService` (laporan pelanggaran), `VoterRegistryService` (pendaftaran DPT). |
| **O**pen/Closed | Mode identitas pelapor baru cukup menambah kelas `IIdentityStrategy` baru tanpa mengubah `ReportService`. Algoritma enkripsi/hash dapat diganti lewat `IBallotCipher` / `IHashChainService`. |
| **L**iskov Substitution | `Voter`, `Panwas`, `KpuAdmin` dapat dipakai di mana pun `User` diharapkan (mis. `ReportService.submitReport(User, ...)`). |
| **I**nterface Segregation | Hak akses dipecah menjadi interface kecil (`IVotable`, `IAuditCapable`, `IElectionManager`) sehingga tiap peran hanya mengimplementasikan kemampuan yang dimilikinya. |
| **D**ependency Inversion | Service bergantung pada interface (`I*Repository`, `IBallotCipher`, `IHashChainService`) yang disuntikkan lewat constructor, bukan pada kelas SQLite/AES konkret. |

### Batasan Keamanan (Catatan Akademik)

- Kunci AES dan PIN disimpan di perangkat yang sama dengan aplikasi, sehingga mekanisme ini merupakan **simulasi** konsep e-voting aman, bukan sistem produksi.
- Tabel surat suara tidak menyimpan `userId` maupun `timestamp` (hanya nomor urut rantai hash) agar suara tidak dapat dikaitkan kembali dengan pemilih.

---

## Build & Pengujian

- **APK siap pasang:** [`EvotingAPP.apk`](EvotingAPP.apk) (debug build, minSdk 24 / Android 7.0+).
- **Android Studio:** buka folder `EvotingAPP/`, tunggu Gradle sync, lalu *Run* atau *Build → Build APK(s)*.
- **Command line (Windows):** gunakan JDK bawaan Android Studio (JBR 25).

```powershell
$env:JAVA_HOME = "D:\Android\Android Studio\jbr"
cd EvotingAPP
.\gradlew assembleDebug "-Porg.gradle.java.installations.auto-detect=false" "-Porg.gradle.java.installations.paths=D:/Android/Android Studio/jbr"
```

- **Unit test** (logika inti tanpa emulator, memakai repository palsu di memori berkat DIP):

```powershell
.\gradlew testDebugUnitTest "-Porg.gradle.java.installations.auto-detect=false" "-Porg.gradle.java.installations.paths=D:/Android/Android Studio/jbr"
```

  Mencakup: enkripsi AES (IV acak), vektor uji SHA-256, deteksi manipulasi hash chain (ubah / hapus / tukar urutan), alur pemilu lengkap, penolakan suara ganda, validasi 2 PIN, pendaftaran DPT (format NIK, NIK/username ganda, DPT terkunci setelah ditutup), pembekuan saat *TAMPERED*, transisi status pemilu, serta laporan anonim/teridentifikasi dan pelacakan Kode Tiket.
