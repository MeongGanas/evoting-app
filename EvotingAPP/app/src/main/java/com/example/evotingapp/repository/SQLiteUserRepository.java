package com.example.evotingapp.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.evotingapp.role.KpuAdmin;
import com.example.evotingapp.role.Panwas;
import com.example.evotingapp.role.User;
import com.example.evotingapp.role.Voter;
import com.example.evotingapp.util.Hashing;

import java.util.ArrayList;
import java.util.List;

public class SQLiteUserRepository implements IUserRepository {
    private final ElectionDbHelper dbHelper;

    public SQLiteUserRepository(ElectionDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public User authenticate(String username, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String hash = Hashing.hashPassword(username, password);
        try (Cursor c = db.query(ElectionDbHelper.T_USERS,
                new String[]{"id", "username", "role", "nik", "has_voted"},
                "username = ? AND password_hash = ?", new String[]{username, hash},
                null, null, null)) {
            if (!c.moveToFirst()) {
                return null;
            }
            String id = c.getString(0);
            String name = c.getString(1);
            String role = c.getString(2);
            switch (role) {
                case User.ROLE_VOTER:
                    return new Voter(id, name, c.getString(3), c.getInt(4) == 1);
                case User.ROLE_PANWAS:
                    return new Panwas(id, name);
                case User.ROLE_KPU:
                    return new KpuAdmin(id, name);
                default:
                    return null;
            }
        }
    }

    @Override
    public boolean hasVoted(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(ElectionDbHelper.T_USERS, new String[]{"has_voted"},
                "id = ?", new String[]{userId}, null, null, null)) {
            return c.moveToFirst() && c.getInt(0) == 1;
        }
    }

    @Override
    public boolean markAsVoted(String userId) {
        ContentValues v = new ContentValues();
        v.put("has_voted", 1);
        // Hanya berhasil bila sebelumnya belum memilih (mencegah suara ganda).
        return dbHelper.getWritableDatabase().update(ElectionDbHelper.T_USERS, v,
                "id = ? AND has_voted = 0", new String[]{userId}) == 1;
    }

    @Override
    public boolean registerVoter(Voter voter, String rawPassword) {
        ContentValues v = new ContentValues();
        v.put("id", voter.getUserId());
        v.put("username", voter.getUsername());
        v.put("password_hash", Hashing.hashPassword(voter.getUsername(), rawPassword));
        v.put("role", User.ROLE_VOTER);
        v.put("nik", voter.getNik());
        v.put("has_voted", 0);
        return dbHelper.getWritableDatabase().insert(ElectionDbHelper.T_USERS, null, v) != -1;
    }

    @Override
    public boolean existsByUsername(String username) {
        return exists("username = ?", username);
    }

    @Override
    public boolean existsByNik(String nik) {
        return exists("nik = ?", nik);
    }

    @Override
    public List<Voter> findAllVoters() {
        List<Voter> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(ElectionDbHelper.T_USERS,
                new String[]{"id", "username", "nik", "has_voted"},
                "role = ?", new String[]{User.ROLE_VOTER}, null, null, "username ASC")) {
            while (c.moveToNext()) {
                list.add(new Voter(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) == 1));
            }
        }
        return list;
    }

    private boolean exists(String where, String arg) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(ElectionDbHelper.T_USERS, new String[]{"id"},
                where, new String[]{arg}, null, null, null, "1")) {
            return c.moveToFirst();
        }
    }

    @Override
    public boolean verifyRolePin(String role, String rawPin) {
        if (rawPin == null || rawPin.isEmpty()) {
            return false;
        }
        String hash = Hashing.hashPin(role, rawPin);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(ElectionDbHelper.T_USERS, new String[]{"id"},
                "role = ? AND pin_hash = ?", new String[]{role, hash},
                null, null, null, "1")) {
            return c.moveToFirst();
        }
    }
}
