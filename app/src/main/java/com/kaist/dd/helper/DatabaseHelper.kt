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
        private const val DATABASE_NAME = "drowsinessDatabase"
        private const val ALERT_TABLE_NAME = "drowsinessAlert"
        private const val EAR_TABLE_NAME = "drowsinessEar"

        // Column names
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_LEVEL = "level"
        private const val COLUMN_RUNNING = "running"
        private const val COLUMN_EAR = "ear"
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
        val createTableQuery1 = """
            CREATE TABLE IF NOT EXISTS $ALERT_TABLE_NAME (
                $COLUMN_TIMESTAMP TEXT,
                $COLUMN_RUNNING TEXT,
                $COLUMN_LEVEL INTEGER
            )
        """.trimIndent()
        db.execSQL(createTableQuery1)

        val createTableQuery2 = """
            CREATE TABLE IF NOT EXISTS $EAR_TABLE_NAME (
                $COLUMN_TIMESTAMP TEXT,
                $COLUMN_EAR REAL
            )
        """.trimIndent()
        db.execSQL(createTableQuery2)
    }

    private fun setFormatTimeMillis(timeMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis) % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun addAlertLog(level: Int, startTime: Long) {
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
        db.insert(ALERT_TABLE_NAME, null, values)
        db.close()
    }

    fun addEarLog(ear: Double) {
        val db = this.writableDatabase
        val values = ContentValues()

        // 현재 시간을 포맷팅하여 문자열로 저장
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        values.put(COLUMN_TIMESTAMP, timestamp)
        values.put(COLUMN_EAR, ear)

        // 데이터베이스에 로그 추가
        db.insert(EAR_TABLE_NAME, null, values)
        db.close()
    }

    @SuppressLint("Range")
    fun getAllAlertLogs(): List<AlertData> {
        val logList = mutableListOf<AlertData>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $ALERT_TABLE_NAME", null)

        if (cursor.moveToFirst()) {
            do {
                val timestamp = cursor.getString(cursor.getColumnIndex(COLUMN_TIMESTAMP))
                val level = cursor.getInt(cursor.getColumnIndex(COLUMN_LEVEL))
                val running = cursor.getString(cursor.getColumnIndex(COLUMN_RUNNING))
                val log = AlertData(timestamp, level, running)
                logList.add(log)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return logList
    }

    @SuppressLint("Range")
    fun getLastRangeEars(): ArrayList<Double> {
        val logList = ArrayList<Double>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $EAR_TABLE_NAME ORDER BY $COLUMN_TIMESTAMP DESC LIMIT 60", null)

        if (cursor.moveToFirst()) {
            do {
                val ear = cursor.getDouble(cursor.getColumnIndex(COLUMN_EAR))
                logList.add(ear)
            } while (cursor.moveToNext())
        }
        logList.reverse()

        cursor.close()
        db.close()

        return logList
    }
}