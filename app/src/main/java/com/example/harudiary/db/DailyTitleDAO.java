package com.example.harudiary.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DailyTitleDAO {

    private final DBHelper dbHelper;

    public DailyTitleDAO(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /** 하루 제목 저장 또는 업데이트 */
    public void saveTitle(int userId, String date, String title) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("date", date);
        values.put("title", title != null ? title.trim() : "");
        db.insertWithOnConflict("daily_titles", null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** 하루 제목 조회 (없으면 null) */
    public String getTitle(int userId, String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT title FROM daily_titles WHERE user_id=? AND date=?",
                new String[]{String.valueOf(userId), date});
        String title = null;
        if (c.moveToFirst()) title = c.getString(0);
        c.close();
        return title;
    }
}
