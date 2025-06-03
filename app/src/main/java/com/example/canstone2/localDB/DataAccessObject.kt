package com.example.canstone2.localDB

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface DaoSensorData {
    // 가져오기
    @Query("SELECT * FROM sensor_table")
    suspend fun loadAll(): List<DataTableSensor>

    // 날짜로 가져오기
    @Query("SELECT * FROM sensor_table " +
            "WHERE timestamp = :selectedDate")
    suspend fun findByDate(selectedDate: String) : List<DataTableSensor>

    @Insert
    // vararg : 호출 인자 갯수를 유동적으로 지정
    suspend fun insertAll(vararg data: DataTableSensor) // 변수 : datas // 배열로 들어옴

    @Delete
    suspend fun delete(table: DataTableSensor)
}

@Dao
interface DaoSensorClassData {
    @Insert
    suspend fun insertClass(classEntity: DataTableSensorClass)

    @Query("SELECT * FROM class_table")
    suspend fun getAllSensors(): List<DataTableSensorClass>

    // 날짜로 가져오기
    @Query("SELECT * FROM class_table " +
            "WHERE timestamp = :selectedDate")
    suspend fun findByDate(selectedDate: String) : List<DataTableSensorClass>
}


@Dao
interface DaoAlarmData {
    // 가져오기
    @Query("SELECT * FROM alarm_table")
    suspend fun loadAll(): List<DataTableAlarm>

    // 날짜로 가져오기
    @Query("SELECT * FROM alarm_table " +
            "WHERE timestamp = :selectedDate")
    suspend fun findByDate(selectedDate: String) : List<DataTableAlarm>

    @Insert
    // vararg : 호출 인자 갯수를 유동적으로 지정
    suspend fun insertAll(vararg data: DataTableAlarm) // 변수 : datas // 배열로 들어옴

    @Delete
    suspend fun delete(table: DataTableAlarm)
}