package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String,
    val frequency: String, // "Daily", "Twice Daily", "Weekly"
    val reminderTimes: String, // Comma-separated strings, e.g. "08:30" or "08:00,20:00"
    val startDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "intake_logs")
data class IntakeLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicineId: Int,
    val medicineName: String,
    val scheduledTime: Long, // Timestamp of scheduled reminder time
    val actualTime: Long, // Timestamp when logged
    val status: String, // "TAKEN", "SKIPPED"
    val dosageTaken: String
)

@Entity(tableName = "purchase_logs")
data class PurchaseLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicineId: Int,
    val medicineName: String,
    val purchaseDate: Long = System.currentTimeMillis(),
    val unitPrice: Double,
    val quantity: Int,
    val storeName: String,
    val notes: String = ""
)
