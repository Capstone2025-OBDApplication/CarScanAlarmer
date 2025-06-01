package com.example.canstone2.localDB

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

// 복합 키 지정
// @Entity(primaryKeys = ["id", "vin"])
@Entity(tableName = "sensor_table")
data class DataTableSensor(
    // TODO: 차량 고유 번호 VIN 복합 primary Key 지정
    // 항목 유지 안하는 경우 @Ignore 또는 @Entity(ignoredColumns =[""])
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val id: Int = 0,
    @ColumnInfo(name = "timestamp") val date: Long?,
    @ColumnInfo(name = "accelerator") val accel: Int?,
    @ColumnInfo(name = "rpm") val rpm : Float?,
    @ColumnInfo(name = "speed") val speed : Int?,
    @ColumnInfo(name = "brake") val brake : String?,
)

@Entity(
    tableName = "class_table",
    foreignKeys = [
        ForeignKey(
            entity = DataTableClass::class,
            parentColumns = ["rowid"],
            childColumns = ["sensor_id"],
            onDelete =  ForeignKey.CASCADE
        )
    ]
)
data class DataTableClass(
    @PrimaryKey @ColumnInfo(name = "rowid") val id: Int,
    @ColumnInfo(name = "class") val clazz: String?
)

@Entity(tableName = "alarm_table")
data class DataTableAlarm(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val id: Int = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "type") val type: String?,
)