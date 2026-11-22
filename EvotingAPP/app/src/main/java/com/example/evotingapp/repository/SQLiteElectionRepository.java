package com.example.evotingapp.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.evotingapp.model.ElectionState;
import com.example.evotingapp.model.TallyResult;

import java.util.ArrayList;
import java.util.List;

public class SQLiteElectionRepository implements IElectionRepository {
    private final ElectionDbHelper dbHelper;

    public SQLiteElectionRepository(ElectionDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public ElectionState getState() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(ElectionDbHelper.T_ELECTION, new String[]{"state"},
                "id = 1", null, null, null, null)) {
            return c.moveToFirst() ? ElectionState.valueOf(c.getString(0)) : ElectionState.NOT_STARTED;
        }
    }

    @Override
    public boolean updateState(ElectionState state) {
        ContentValues v = new ContentValues();
        v.put("id", 1);
        v.put("state", state.name());
        return dbHelper.getWritableDatabase().insertWithOnConflict(ElectionDbHelper.T_ELECTION,
                null, v, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    @Override
    public boolean saveResults(List<TallyResult> results) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(ElectionDbHelper.T_RESULTS, null, null);
            for (TallyResult r : results) {
                ContentValues v = new ContentValues();
                v.put("candidate_id", r.getCandidateId());
                v.put("candidate_name", r.getCandidateName());
                v.put("total_votes", r.getTotalVotes());
                v.put("percentage", r.getPercentage());
                if (db.insert(ElectionDbHelper.T_RESULTS, null, v) == -1) {
                    return false;
                }
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public List<TallyResult> getResults() {
        List<TallyResult> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(ElectionDbHelper.T_RESULTS,
                new String[]{"candidate_id", "candidate_name", "total_votes", "percentage"},
                null, null, null, null, "total_votes DESC, candidate_id ASC")) {
            while (c.moveToNext()) {
                list.add(new TallyResult(c.getString(0), c.getString(1), c.getInt(2), c.getDouble(3)));
            }
        }
        return list;
    }
}
