package com.example.evotingapp.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.evotingapp.model.ElectionState;
import com.example.evotingapp.role.User;
import com.example.evotingapp.util.Hashing;

/**
 * Pembuat skema SQLite dan data seeding untuk demo / user testing.
 */
public class ElectionDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "evoting.db";
    private static final int DB_VERSION = 1;

    public static final String DEMO_PASSWORD = "123456";
    public static final String KPU_PIN = "1111";
    public static final String PANWAS_PIN = "2222";

    // ---- tabel & kolom ----
    static final String T_USERS = "users";
    static final String T_CANDIDATES = "candidates";
    static final String T_BALLOTS = "ballots";
    static final String T_REPORTS = "reports";
    static final String T_ELECTION = "election";
    static final String T_RESULTS = "results";

    public ElectionDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_USERS + " ("
                + "id TEXT PRIMARY KEY, "
                + "username TEXT UNIQUE NOT NULL, "
                + "password_hash TEXT NOT NULL, "
                + "role TEXT NOT NULL, "
                + "nik TEXT, "
                + "has_voted INTEGER NOT NULL DEFAULT 0, "
                + "pin_hash TEXT)");

        db.execSQL("CREATE TABLE " + T_CANDIDATES + " ("
                + "id TEXT PRIMARY KEY, "
                + "full_name TEXT NOT NULL, "
                + "vision_mission TEXT, "
                + "fund_amount REAL NOT NULL DEFAULT 0, "
                + "fund_summary TEXT)");

        // Sengaja tanpa kolom user_id maupun timestamp (anonimitas suara).
        db.execSQL("CREATE TABLE " + T_BALLOTS + " ("
                + "seq INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "payload TEXT NOT NULL, "
                + "previous_hash TEXT NOT NULL, "
                + "receipt_hash TEXT UNIQUE NOT NULL)");

        db.execSQL("CREATE TABLE " + T_REPORTS + " ("
                + "id TEXT PRIMARY KEY, "
                + "ticket_code TEXT UNIQUE NOT NULL, "
                + "reporter_id TEXT, "
                + "reporter_display TEXT NOT NULL, "
                + "description TEXT NOT NULL, "
                + "status TEXT NOT NULL, "
                + "created_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE " + T_ELECTION + " ("
                + "id INTEGER PRIMARY KEY CHECK (id = 1), "
                + "state TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + T_RESULTS + " ("
                + "candidate_id TEXT PRIMARY KEY, "
                + "candidate_name TEXT NOT NULL, "
                + "total_votes INTEGER NOT NULL, "
                + "percentage REAL NOT NULL)");

        seed(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
        dropAll(db);
        onCreate(db);
    }

    /** Mengembalikan database ke kondisi awal (untuk sesi user testing baru). */
    public void resetDemoData() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            dropAll(db);
            onCreate(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Fitur bantu demo: mengubah isi surat suara pertama secara langsung di SQLite
     * (tanpa memperbarui hash) untuk membuktikan Tamper Check mendeteksi manipulasi.
     *
     * @return false bila belum ada surat suara yang bisa dimanipulasi.
     */
    public boolean simulateTamper() {
        SQLiteDatabase db = getWritableDatabase();
        try (Cursor c = db.query(T_BALLOTS, new String[]{"seq", "payload"},
                null, null, null, null, "seq ASC", "1")) {
            if (!c.moveToFirst()) {
                return false;
            }
            int seq = c.getInt(0);
            String payload = c.getString(1);
            char first = payload.charAt(0);
            char flipped = first == 'a' ? 'b' : 'a';
            ContentValues v = new ContentValues();
            v.put("payload", flipped + payload.substring(1));
            return db.update(T_BALLOTS, v, "seq = ?", new String[]{String.valueOf(seq)}) == 1;
        }
    }

    private void dropAll(SQLiteDatabase db) {
        for (String t : new String[]{T_USERS, T_CANDIDATES, T_BALLOTS, T_REPORTS, T_ELECTION, T_RESULTS}) {
            db.execSQL("DROP TABLE IF EXISTS " + t);
        }
    }

    private void seed(SQLiteDatabase db) {
        // 5 akun pemilih untuk user testing (minimal 5 user berbeda)
        for (int i = 1; i <= 5; i++) {
            insertUser(db, "U-V" + i, "pemilih" + i, User.ROLE_VOTER, "320100000000000" + i, null);
        }
        insertUser(db, "U-P1", "panwas1", User.ROLE_PANWAS, null, Hashing.hashPin(User.ROLE_PANWAS, PANWAS_PIN));
        insertUser(db, "U-K1", "kpu1", User.ROLE_KPU, null, Hashing.hashPin(User.ROLE_KPU, KPU_PIN));

        insertCandidate(db, "C01", "01 - Andi Pratama & Siti Rahma",
                "Visi: Kampus inklusif dan transparan.\nMisi: Dashboard anggaran terbuka, forum aspirasi bulanan, beasiswa berbasis kebutuhan.",
                85_000_000, "Iuran anggota Rp 40 jt, donasi alumni Rp 35 jt, sponsor UMKM Rp 10 jt.");
        insertCandidate(db, "C02", "02 - Budi Santoso & Rina Wijaya",
                "Visi: Organisasi yang melayani dan berintegritas.\nMisi: Layanan pengaduan 24 jam, audit kegiatan terbuka, program magang mitra industri.",
                120_000_000, "Donasi perorangan Rp 70 jt, penggalangan dana acara Rp 50 jt.");
        insertCandidate(db, "C03", "03 - Citra Lestari & Dimas Saputra",
                "Visi: Lingkungan hijau dan setara untuk semua.\nMisi: Program kampus bebas plastik, akses fasilitas disabilitas, kesetaraan gender dalam kepengurusan.",
                64_500_000, "Iuran anggota Rp 30 jt, donasi komunitas Rp 34,5 jt.");

        ContentValues state = new ContentValues();
        state.put("id", 1);
        state.put("state", ElectionState.NOT_STARTED.name());
        db.insert(T_ELECTION, null, state);
    }

    private void insertUser(SQLiteDatabase db, String id, String username, String role,
                            String nik, String pinHash) {
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("username", username);
        v.put("password_hash", Hashing.hashPassword(username, DEMO_PASSWORD));
        v.put("role", role);
        v.put("nik", nik);
        v.put("has_voted", 0);
        v.put("pin_hash", pinHash);
        db.insert(T_USERS, null, v);
    }

    private void insertCandidate(SQLiteDatabase db, String id, String name, String vm,
                                 double fund, String summary) {
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("full_name", name);
        v.put("vision_mission", vm);
        v.put("fund_amount", fund);
        v.put("fund_summary", summary);
        db.insert(T_CANDIDATES, null, v);
    }
}
