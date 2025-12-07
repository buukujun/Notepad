package com.example.android.notepad;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class DatabaseInspector {
    private static final String TAG = "DatabaseInspector";

    public static void inspectDatabase(Context context) {
        Log.i(TAG, "=== 开始检查数据库结构 ===");

        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            // 方法1：通过ContentProvider访问（推荐）
            inspectViaContentProvider(context);

            // 方法2：直接打开数据库文件
            String dbPath = context.getDatabasePath("note_pad.db").getPath();
            Log.d(TAG, "数据库路径: " + dbPath);

            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);

            // 检查所有表
            inspectAllTables(db);

            // 检查notes表结构
            inspectNotesTable(db);

            // 检查notes表数据样例
            inspectNotesData(db);

        } catch (Exception e) {
            Log.e(TAG, "数据库检查失败: ", e);
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    /**
     * 通过ContentProvider检查数据库
     */
    private static void inspectViaContentProvider(Context context) {
        try {
            Uri notesUri = Uri.parse("content://com.google.provider.NotePad/notes");
            Cursor cursor = context.getContentResolver().query(
                    notesUri,
                    null,  // 所有列
                    null, null, null
            );

            if (cursor != null) {
                Log.i(TAG, "✅ 通过ContentProvider可以访问notes表");
                Log.i(TAG, "列数: " + cursor.getColumnCount());

                String[] columnNames = cursor.getColumnNames();
                Log.i(TAG, "列名: ");
                for (int i = 0; i < columnNames.length; i++) {
                    Log.i(TAG, "  [" + i + "] " + columnNames[i]);
                }

                cursor.close();
            } else {
                Log.e(TAG, "❌ ContentProvider查询失败");
            }

        } catch (Exception e) {
            Log.e(TAG, "ContentProvider检查失败: ", e);
        }
    }

    /**
     * 检查所有表
     */
    private static void inspectAllTables(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table'",
                    null
            );

            Log.i(TAG, "=== 数据库中的所有表 ===");
            while (cursor.moveToNext()) {
                String tableName = cursor.getString(0);
                Log.i(TAG, "📊 表名: " + tableName);

                // 如果是notes表，额外显示创建语句
                if ("notes".equals(tableName)) {
                    Cursor schemaCursor = db.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='notes'", null);
                    if (schemaCursor.moveToFirst()) {
                        String createSql = schemaCursor.getString(0);
                        Log.i(TAG, "📝 CREATE语句: " + createSql);
                    }
                    schemaCursor.close();
                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * 检查notes表结构详情
     */
    private static void inspectNotesTable(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("PRAGMA table_info(notes)", null);

            Log.i(TAG, "=== notes表结构详情 ===");
            Log.i(TAG, "列名\t\t类型\t\t非空\t默认值\t主键");
            Log.i(TAG, "----------------------------------------");

            while (cursor.moveToNext()) {
                String columnName = cursor.getString(1);
                String columnType = cursor.getString(2);
                int notNull = cursor.getInt(3);
                String defaultValue = cursor.getString(4);
                int primaryKey = cursor.getInt(5);

                String notNullStr = (notNull == 1) ? "YES" : "NO";
                String defaultStr = (defaultValue != null) ? defaultValue : "(null)";
                String pkStr = (primaryKey == 1) ? "YES" : "NO";

                Log.i(TAG, columnName + "\t\t" +
                        columnType + "\t\t" +
                        notNullStr + "\t" +
                        defaultStr + "\t" +
                        pkStr);

                // 特别检查category列
                if ("category".equals(columnName)) {
                    Log.i(TAG, "🎉 找到category列! 类型: " + columnType + ", 默认值: " + defaultValue);
                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * 检查notes表数据样例
     */
    private static void inspectNotesData(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM notes LIMIT 5", null);

            Log.i(TAG, "=== notes表数据样例（前5条）===");

            if (cursor.getCount() == 0) {
                Log.i(TAG, "表为空，没有数据");
                return;
            }

            String[] columnNames = cursor.getColumnNames();

            // 显示列头
            StringBuilder header = new StringBuilder();
            for (String colName : columnNames) {
                header.append(String.format("%-15s", colName));
            }
            Log.i(TAG, header.toString());
            Log.i(TAG, "------------------------------------------------------------");

            // 显示数据行
            while (cursor.moveToNext()) {
                StringBuilder row = new StringBuilder();
                for (int i = 0; i < columnNames.length; i++) {
                    String value = cursor.getString(i);
                    if (value == null) value = "NULL";
                    row.append(String.format("%-15s", value));
                }
                Log.i(TAG, row.toString());
            }

        } finally {
            if (cursor != null) cursor.close();
        }
    }
}
