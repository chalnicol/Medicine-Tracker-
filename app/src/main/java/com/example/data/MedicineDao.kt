package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    // Medicine queries
    @Query("SELECT * FROM medicines ORDER BY name ASC")
    fun getAllMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    suspend fun getMedicineById(id: Int): Medicine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    // Intake Log queries
    @Query("SELECT * FROM intake_logs ORDER BY scheduledTime DESC")
    fun getAllIntakeLogs(): Flow<List<IntakeLog>>

    @Query("SELECT * FROM intake_logs WHERE scheduledTime BETWEEN :startTime AND :endTime ORDER BY scheduledTime ASC")
    fun getIntakeLogsBetween(startTime: Long, endTime: Long): Flow<List<IntakeLog>>

    @Query("SELECT * FROM intake_logs WHERE medicineId = :medicineId ORDER BY scheduledTime DESC")
    fun getIntakeLogsForMedicine(medicineId: Int): Flow<List<IntakeLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntakeLog(log: IntakeLog): Long

    @Delete
    suspend fun deleteIntakeLog(log: IntakeLog)

    // Purchase Log queries
    @Query("SELECT * FROM purchase_logs ORDER BY purchaseDate DESC")
    fun getAllPurchaseLogs(): Flow<List<PurchaseLog>>

    @Query("SELECT * FROM purchase_logs WHERE medicineId = :medicineId ORDER BY purchaseDate ASC")
    fun getPurchaseLogsForMedicine(medicineId: Int): Flow<List<PurchaseLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseLog(log: PurchaseLog): Long

    @Delete
    suspend fun deletePurchaseLog(log: PurchaseLog)
}
