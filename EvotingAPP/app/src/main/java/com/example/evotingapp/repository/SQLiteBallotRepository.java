package com.example.evotingapp.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.evotingapp.model.EncryptedBallot;

import java.util.ArrayList;
import java.util.List;

public class SQLiteBallotRepository implements IBallotRepository {
    private final ElectionDbHelper dbHelper;

    public SQLiteBallotRepository(ElectionDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public boolean insertBallot(EncryptedBallot ballot) {
        ContentValues v = new ContentValues();
        v.put("payload", ballot.getEncryptedPayload());
        v.put("previous_hash", ballot.getPreviousHash());
        v.put("receipt_hash", ballot.getReceiptHash());
        return dbHelper.getWritableDatabase().insert(ElectionDbHelper.T_BALLOTS, null, v) != -1;
    }

    @Override
    public String getLatestHash() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(ElectionDbHelper.T_BALLOTS, new String[]{"receipt_hash"},
                null, null, null, null, "seq DESC", "1")) {
            return c.moveToFirst() ? c.getString(0) : EncryptedBallot.GENESIS_HASH;
        }
    }

    @Override
    public List<EncryptedBallot> getAllBallotsOrdered() {
        List<EncryptedBallot> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(ElectionDbHelper.T_BALLOTS,
                new String[]{"seq", "payload", "previous_hash", "receipt_hash"},
                null, null, null, null, "seq ASC")) {
            while (c.moveToNext()) {
                list.add(new EncryptedBallot(c.getInt(0), c.getString(1), c.getString(2), c.getString(3)));
            }
        }
        return list;
    }

    @Override
    public boolean existsByReceiptHash(String receiptHash) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(ElectionDbHelper.T_BALLOTS, new String[]{"seq"},
                "receipt_hash = ?", new String[]{receiptHash}, null, null, null, "1")) {
            return c.moveToFirst();
        }
    }
}
