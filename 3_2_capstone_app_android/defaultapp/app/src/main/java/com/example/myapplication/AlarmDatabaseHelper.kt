package com.example.myapplication

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AlarmDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // 알람 데이터를 저장할 테이블 생성
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                device_id TEXT,
                count INTEGER,
                timestamp TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }


    fun saveAlarm(deviceId: String, count: Int, timestamp: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("device_id", deviceId)
            put("count", count)
            put("timestamp", timestamp)
        }
        db.insert(TABLE_NAME, null, values)
    }

    fun getAlarms(startDate: String, endDate: String): Cursor {
        val db = this.readableDatabase
        return db.rawQuery(
            "SELECT * FROM $TABLE_NAME WHERE timestamp BETWEEN ? AND ?",
            arrayOf(startDate, endDate)
        )
    }

    @SuppressLint("Range")
    fun getClosestCount(deviceId: String, time: String): Int {
        val db = this.readableDatabase
        val query = """
        SELECT count
        FROM $TABLE_NAME
        WHERE device_id = ? AND timestamp <= ?
        ORDER BY timestamp DESC
        LIMIT 1
    """
        val cursor = db.rawQuery(query, arrayOf(deviceId, time))
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(cursor.getColumnIndex("count"))
        }
        cursor.close()
        return count
    }


    companion object {
        private const val DATABASE_NAME = "alarms.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "alarms"
    }
}
