package com.example.evotingapp.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.evotingapp.model.Candidate;

import java.util.ArrayList;
import java.util.List;

public class SQLiteCandidateRepository implements ICandidateRepository {
    private final ElectionDbHelper dbHelper;

    public SQLiteCandidateRepository(ElectionDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public List<Candidate> findAll() {
        List<Candidate> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(ElectionDbHelper.T_CANDIDATES,
                new String[]{"id", "full_name", "vision_mission", "fund_amount", "fund_summary"},
                null, null, null, null, "id ASC")) {
            while (c.moveToNext()) {
                list.add(new Candidate(c.getString(0), c.getString(1), c.getString(2),
                        c.getDouble(3), c.getString(4)));
            }
        }
        return list;
    }

    @Override
    public boolean save(Candidate candidate) {
        ContentValues v = new ContentValues();
        v.put("id", candidate.getCandidateId());
        v.put("full_name", candidate.getFullName());
        v.put("vision_mission", candidate.getVisionMission());
        v.put("fund_amount", candidate.getCampaignFundAmount());
        v.put("fund_summary", candidate.getFundSummary());
        return dbHelper.getWritableDatabase().insertWithOnConflict(ElectionDbHelper.T_CANDIDATES,
                null, v, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }
}
