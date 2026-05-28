package com.example.data

import kotlinx.coroutines.flow.Flow

class MedicineRepository(private val medicineDao: MedicineDao) {

    val allMedicines: Flow<List<Medicine>> = medicineDao.getAllMedicines()
    val activeMedicines: Flow<List<Medicine>> = medicineDao.getActiveMedicines()
    val allIntakeLogs: Flow<List<IntakeLog>> = medicineDao.getAllIntakeLogs()
    val allPurchaseLogs: Flow<List<PurchaseLog>> = medicineDao.getAllPurchaseLogs()

    suspend fun getMedicineById(id: Int): Medicine? {
        return medicineDao.getMedicineById(id)
    }

    suspend fun insertMedicine(medicine: Medicine): Long {
        return medicineDao.insertMedicine(medicine)
    }

    suspend fun updateMedicine(medicine: Medicine) {
        medicineDao.updateMedicine(medicine)
    }

    suspend fun deleteMedicine(medicine: Medicine) {
        medicineDao.deleteMedicine(medicine)
    }

    fun getIntakeLogsBetween(startTime: Long, endTime: Long): Flow<List<IntakeLog>> {
        return medicineDao.getIntakeLogsBetween(startTime, endTime)
    }

    fun getIntakeLogsForMedicine(medicineId: Int): Flow<List<IntakeLog>> {
        return medicineDao.getIntakeLogsForMedicine(medicineId)
    }

    suspend fun insertIntakeLog(log: IntakeLog): Long {
        return medicineDao.insertIntakeLog(log)
    }

    suspend fun deleteIntakeLog(log: IntakeLog) {
        medicineDao.deleteIntakeLog(log)
    }

    fun getPurchaseLogsForMedicine(medicineId: Int): Flow<List<PurchaseLog>> {
        return medicineDao.getPurchaseLogsForMedicine(medicineId)
    }

    suspend fun insertPurchaseLog(log: PurchaseLog): Long {
        return medicineDao.insertPurchaseLog(log)
    }

    suspend fun deletePurchaseLog(log: PurchaseLog) {
        medicineDao.deletePurchaseLog(log)
    }
}
