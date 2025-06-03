package com.example.canstone2.repository

import com.example.canstone2.localDB.DaoSensorData
import com.example.canstone2.localDB.DataTableSensor

class RepositorySensor(private val dao: DaoSensorData) {
    suspend fun insertSensor(data: DataTableSensor) {
        dao.insertAll(data)
    }
}