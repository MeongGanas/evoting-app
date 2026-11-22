package com.example.evotingapp.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.evotingapp.model.ReportStatus;
import com.example.evotingapp.model.ViolationReport;

import java.util.ArrayList;
import java.util.List;

public class SQLiteReportRepository implements IReportRepository {
    private static final String[] COLUMNS = {
            "id", "ticket_code", "reporter_id", "reporter_display", "description", "status", "created_at"};

    private final ElectionDbHelper dbHelper;

    public SQLiteReportRepository(ElectionDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public boolean insert(ViolationReport report) {
        ContentValues v = new ContentValues();
        v.put("id", report.getReportId());
        v.put("ticket_code", report.getTicketCode());
        v.put("reporter_id", report.getReporterId());
        v.put("reporter_display", report.getReporterDisplay());
        v.put("description", report.getDescription());
        v.put("status", report.getStatus().name());
        v.put("created_at", report.getCreatedAt());
        return dbHelper.getWritableDatabase().insert(ElectionDbHelper.T_REPORTS, null, v) != -1;
    }

    @Override
    public boolean updateStatus(String reportId, ReportStatus status) {
        ContentValues v = new ContentValues();
        v.put("status", status.name());
        return dbHelper.getWritableDatabase().update(ElectionDbHelper.T_REPORTS, v,
                "id = ?", new String[]{reportId}) == 1;
    }

    @Override
    public ViolationReport findByTicketCode(String ticketCode) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(ElectionDbHelper.T_REPORTS, COLUMNS,
                "ticket_code = ?", new String[]{ticketCode}, null, null, null, "1")) {
            return c.moveToFirst() ? read(c) : null;
        }
    }

    @Override
    public List<ViolationReport> findAll() {
        List<ViolationReport> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(ElectionDbHelper.T_REPORTS, COLUMNS,
                null, null, null, null, "created_at DESC")) {
            while (c.moveToNext()) {
                list.add(read(c));
            }
        }
        return list;
    }

    private ViolationReport read(Cursor c) {
        return new ViolationReport(c.getString(0), c.getString(1), c.getString(2), c.getString(3),
                c.getString(4), ReportStatus.valueOf(c.getString(5)), c.getLong(6));
    }
}
