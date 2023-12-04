package com.kaist.dd

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DatabaseHelper (val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "LogDatabase"
        private const val TABLE_NAME = "drowsiness"

        // Column names
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_LEVEL = "level"
        private const val COLUMN_RUNNING = "running"
    }

    init {
        val db = this.writableDatabase
        createTable(db)
    }

    override fun onCreate(p0: SQLiteDatabase?) {
        // no-op
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        // no-op
    }

    private fun createTable(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COLUMN_TIMESTAMP TEXT,
                $COLUMN_RUNNING TEXT,
                $COLUMN_LEVEL INTEGER
            )
        """.trimIndent()

        db.execSQL(createTableQuery)
    }

    private fun setFormatTimeMillis(timeMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis) % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun addLog(level: Int, startTime: Long) {
        val db = this.writableDatabase
        val values = ContentValues()

        // 현재 시간을 포맷팅하여 문자열로 저장
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val currentTime = System.currentTimeMillis()
        val running = setFormatTimeMillis(currentTime - startTime)

        values.put(COLUMN_TIMESTAMP, timestamp)
        values.put(COLUMN_LEVEL, level)
        values.put(COLUMN_RUNNING, running)

        // 데이터베이스에 로그 추가
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    @SuppressLint("Range")
    fun getAllLogs(): List<LogData> {
        val logList = mutableListOf<LogData>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        if (cursor.moveToFirst()) {
            do {
                val timestamp = cursor.getString(cursor.getColumnIndex(COLUMN_TIMESTAMP))
                val level = cursor.getInt(cursor.getColumnIndex(COLUMN_LEVEL))
                val running = cursor.getString(cursor.getColumnIndex(COLUMN_RUNNING))
                val log = LogData(timestamp, level, running)
                logList.add(log)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return logList
    }
}