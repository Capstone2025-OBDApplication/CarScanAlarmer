package com.example.canstone2.localDB

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DataTableSensor::class, DataTableSensorClass::class, DataTableAlarm::class],
    version = 6
)
abstract class DatabaseClass : RoomDatabase() {
    // 자동으로 DAO값 채워줌
    abstract fun sensorDAO(): DaoSensorData
    abstract fun alarmDAO(): DaoAlarmData
    abstract fun sensorClassDAO(): DaoSensorClassData

    // 인스턴스 중복 생성 방지 (싱글톤 패턴 : 하나의 앱에 하나의 DB 전역적으로 재활용)
    companion object {
        // database 객체
        @Volatile
        private var INSTANCE: DatabaseClass? = null

        @Synchronized // 여러 스레드에서 하나의 자원에 접근 방지
        fun getInstance(context: Context): DatabaseClass? {
            return INSTANCE ?: synchronized(this) { // null일때 초기화 // 이 클래스가 점유하고 있음을 뜻함
                val instance = Room.databaseBuilder(
                    context.applicationContext, // 전역
                    DatabaseClass::class.java,
                    "local_database" // 이름 다르게
                )
                    .fallbackToDestructiveMigration() // 스키마 변경시 기존 데이터 삭제
                    .build()
                    INSTANCE = instance
                    instance
            }
        }

        fun deleteInstance() {
            INSTANCE = null
        }

        // Define your migration from version 1 to 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Example: If you added a new 'last_update' column to the User table
                // db.execSQL("ALTER TABLE User ADD COLUMN last_update INTEGER")

                // If you made more complex changes, write the appropriate SQL here.
                // If you simply added a new table, you might not need to do anything here,
                // as Room will create it. But for schema alterations, you need SQL.
            }
        }
    }
}