package com.example.db

import kotlinx.coroutines.flow.Flow

class DeviceRepository(private val deviceDao: DeviceDao) {
    val batteryLogs: Flow<List<BatteryLog>> = deviceDao.getBatteryLogs()
    val testRecords: Flow<List<TestRecord>> = deviceDao.getTestRecords()

    suspend fun insertBatteryLog(log: BatteryLog) {
        deviceDao.insertBatteryLog(log)
    }

    suspend fun insertTestRecord(record: TestRecord) {
        deviceDao.insertTestRecord(record)
    }

    suspend fun clearTestRecords() {
        deviceDao.clearTestRecords()
    }
}
