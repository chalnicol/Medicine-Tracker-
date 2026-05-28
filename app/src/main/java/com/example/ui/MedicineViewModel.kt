package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.receiver.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class MedicineViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = MedicineRepository(db.medicineDao())
    private val context = application.applicationContext

    // Flow of all medications
    val medicines: StateFlow<List<Medicine>> = repository.allMedicines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow of active medications
    val activeMedicines: StateFlow<List<Medicine>> = repository.activeMedicines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow of all intake logs
    val allIntakeLogs: StateFlow<List<IntakeLog>> = repository.allIntakeLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow of all purchase logs
    val allPurchaseLogs: StateFlow<List<PurchaseLog>> = repository.allPurchaseLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active selected date for Today's schedule tab (defaults to current day)
    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

    // Combined Daily Schedule Slots based on selectedDate
    val dailyScheduleSlots: StateFlow<List<MedicationSlot>> = combine(
        activeMedicines,
        allIntakeLogs,
        selectedDate
    ) { meds, logs, date ->
        calculateSlotsForDate(meds, logs, date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected medicine for price history trend visualization
    private val _selectedPriceMedId = MutableStateFlow<Int?>(null)
    val selectedPriceMedId: StateFlow<Int?> = _selectedPriceMedId.asStateFlow()

    // Filter purchase logs for selected medicine
    val priceHistoryForSelectedMed: StateFlow<List<PurchaseLog>> = combine(
        allPurchaseLogs,
        selectedPriceMedId
    ) { purchases, medId ->
        if (medId == null) {
            emptyList()
        } else {
            purchases.filter { it.medicineId == medId }.sortedBy { it.purchaseDate }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Set first medicine as default for price tracking if available
        viewModelScope.launch {
            medicines.collect { list ->
                if (_selectedPriceMedId.value == null && list.isNotEmpty()) {
                    _selectedPriceMedId.value = list.first().id
                }
            }
        }
    }

    fun selectDate(calendar: Calendar) {
        _selectedDate.value = calendar
    }

    fun selectPriceMedicineId(id: Int) {
        _selectedPriceMedId.value = id
    }

    // Medicine CRUD
    fun addMedicine(name: String, dosage: String, frequency: String, times: List<String>, notes: String) {
        viewModelScope.launch {
            val timesString = times.joinToString(",")
            val medicine = Medicine(
                name = name,
                dosage = dosage,
                frequency = frequency,
                reminderTimes = timesString,
                notes = notes,
                isActive = true
            )
            val generatedId = repository.insertMedicine(medicine)
            val savedMedicine = medicine.copy(id = generatedId.toInt())
            
            // Register Alarms
            AlarmScheduler.scheduleAlarmsForMedicine(context, savedMedicine)
        }
    }

    fun updateMedicine(medicine: Medicine) {
        viewModelScope.launch {
            // First cancel existing alarms
            AlarmScheduler.cancelAlarmsForMedicine(context, medicine)
            // Update db
            repository.updateMedicine(medicine)
            // Schedule new alarms if active
            if (medicine.isActive) {
                AlarmScheduler.scheduleAlarmsForMedicine(context, medicine)
            }
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarmsForMedicine(context, medicine)
            repository.deleteMedicine(medicine)
        }
    }

    // Intake Logging
    fun recordIntake(slot: MedicationSlot, status: String) {
        viewModelScope.launch {
            val log = IntakeLog(
                medicineId = slot.medicine.id,
                medicineName = slot.medicine.name,
                scheduledTime = slot.scheduledTimestamp,
                actualTime = System.currentTimeMillis(),
                status = status,
                dosageTaken = slot.medicine.dosage
            )
            repository.insertIntakeLog(log)
        }
    }

    fun undoIntake(log: IntakeLog) {
        viewModelScope.launch {
            repository.deleteIntakeLog(log)
        }
    }

    // Purchase logging
    fun addPurchase(medicineId: Int, medicineName: String, date: Long, unitPrice: Double, quantity: Int, store: String, notes: String) {
        viewModelScope.launch {
            val purchase = PurchaseLog(
                medicineId = medicineId,
                medicineName = medicineName,
                purchaseDate = date,
                unitPrice = unitPrice,
                quantity = quantity,
                storeName = store,
                notes = notes
            )
            repository.insertPurchaseLog(purchase)
        }
    }

    fun deletePurchase(purchase: PurchaseLog) {
        viewModelScope.launch {
            repository.deletePurchaseLog(purchase)
        }
    }

    // Calculation Helpers
    private fun calculateSlotsForDate(
        meds: List<Medicine>,
        logs: List<IntakeLog>,
        date: Calendar
    ): List<MedicationSlot> {
        val slots = mutableListOf<MedicationSlot>()

        for (med in meds) {
            val times = med.reminderTimes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            for (timeStr in times) {
                val parts = timeStr.split(":")
                if (parts.size != 2) continue
                val hour = parts[0].toIntOrNull() ?: continue
                val minute = parts[1].toIntOrNull() ?: continue

                // Construct scheduled timestamp today
                val scheduledCal = Calendar.getInstance().apply {
                    timeInMillis = date.timeInMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val scheduledTimestamp = scheduledCal.timeInMillis

                // Look for existing intake log matching this scheduled time
                // Margin of error 1 hour to prevent timestamp alignment drift
                val matchingLog = logs.find { log ->
                    log.medicineId == med.id &&
                    Math.abs(log.scheduledTime - scheduledTimestamp) < 60 * 60 * 1000 // 1 hour threshold
                }

                slots.add(
                    MedicationSlot(
                        medicine = med,
                        timeLabel = timeStr,
                        scheduledTimestamp = scheduledTimestamp,
                        log = matchingLog
                    )
                )
            }
        }

        // Sort by chronological slot order
        return slots.sortedBy { it.scheduledTimestamp }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            if (medicines.value.isNotEmpty()) return@launch

            val med1Id = repository.insertMedicine(
                Medicine(
                    name = "Amlodipine",
                    dosage = "5 mg",
                    frequency = "Daily",
                    reminderTimes = "08:00",
                    notes = "Take in the morning with water"
                )
            )
            val med2Id = repository.insertMedicine(
                Medicine(
                    name = "Metformin",
                    dosage = "500 mg",
                    frequency = "Twice Daily",
                    reminderTimes = "08:00,20:00",
                    notes = "Take with dinner to reduce stomach upset"
                )
            )

            val m1 = repository.getMedicineById(med1Id.toInt())
            val m2 = repository.getMedicineById(med2Id.toInt())

            if (m1 != null) AlarmScheduler.scheduleAlarmsForMedicine(context, m1)
            if (m2 != null) AlarmScheduler.scheduleAlarmsForMedicine(context, m2)

            val now = System.currentTimeMillis()
            val oneDay = 24 * 60 * 60 * 1000L
            val ninetyDays = 90 * oneDay
            val sixtyDays = 60 * oneDay
            val thirtyDays = 30 * oneDay
            val tenDays = 10 * oneDay

            // Amlodipine purchases (prices over time)
            repository.insertPurchaseLog(PurchaseLog(medicineId = med1Id.toInt(), medicineName = "Amlodipine", purchaseDate = now - ninetyDays, unitPrice = 0.12, quantity = 30, storeName = "CVS"))
            repository.insertPurchaseLog(PurchaseLog(medicineId = med1Id.toInt(), medicineName = "Amlodipine", purchaseDate = now - sixtyDays, unitPrice = 0.14, quantity = 30, storeName = "CVS"))
            repository.insertPurchaseLog(PurchaseLog(medicineId = med1Id.toInt(), medicineName = "Amlodipine", purchaseDate = now - thirtyDays, unitPrice = 0.15, quantity = 30, storeName = "Walgreens"))
            repository.insertPurchaseLog(PurchaseLog(medicineId = med1Id.toInt(), medicineName = "Amlodipine", purchaseDate = now - tenDays, unitPrice = 0.18, quantity = 30, storeName = "CVS"))

            // Metformin purchases (prices over time)
            repository.insertPurchaseLog(PurchaseLog(medicineId = med2Id.toInt(), medicineName = "Metformin", purchaseDate = now - ninetyDays, unitPrice = 0.45, quantity = 60, storeName = "Walgreens"))
            repository.insertPurchaseLog(PurchaseLog(medicineId = med2Id.toInt(), medicineName = "Metformin", purchaseDate = now - sixtyDays, unitPrice = 0.42, quantity = 60, storeName = "Boots Pharmacy"))
            repository.insertPurchaseLog(PurchaseLog(medicineId = med2Id.toInt(), medicineName = "Metformin", purchaseDate = now - thirtyDays, unitPrice = 0.38, quantity = 60, storeName = "CVS"))
            repository.insertPurchaseLog(PurchaseLog(medicineId = med2Id.toInt(), medicineName = "Metformin", purchaseDate = now - tenDays, unitPrice = 0.35, quantity = 60, storeName = "Walgreens"))

            // Last 15 days of intake logs
            for (i in 0..15) {
                val dateOffset = now - (i * oneDay)
                val cal = Calendar.getInstance().apply { timeInMillis = dateOffset }
                
                val m1Cal = (cal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 8)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                if (i == 3 || i == 7) {
                    repository.insertIntakeLog(IntakeLog(medicineId = med1Id.toInt(), medicineName = "Amlodipine", scheduledTime = m1Cal.timeInMillis, actualTime = m1Cal.timeInMillis, status = "SKIPPED", dosageTaken = "5 mg"))
                } else {
                    repository.insertIntakeLog(IntakeLog(medicineId = med1Id.toInt(), medicineName = "Amlodipine", scheduledTime = m1Cal.timeInMillis, actualTime = m1Cal.timeInMillis + (15 * 60 * 1000), status = "TAKEN", dosageTaken = "5 mg"))
                }

                val m2Cal1 = (cal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 8)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val m2Cal2 = (cal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 20)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }

                repository.insertIntakeLog(IntakeLog(medicineId = med2Id.toInt(), medicineName = "Metformin", scheduledTime = m2Cal1.timeInMillis, actualTime = m2Cal1.timeInMillis + (30 * 60 * 1000), status = "TAKEN", dosageTaken = "500 mg"))
                if (i == 4) {
                    repository.insertIntakeLog(IntakeLog(medicineId = med2Id.toInt(), medicineName = "Metformin", scheduledTime = m2Cal2.timeInMillis, actualTime = m2Cal2.timeInMillis, status = "SKIPPED", dosageTaken = "500 mg"))
                } else {
                    repository.insertIntakeLog(IntakeLog(medicineId = med2Id.toInt(), medicineName = "Metformin", scheduledTime = m2Cal2.timeInMillis, actualTime = m2Cal2.timeInMillis + (40 * 60 * 1000), status = "TAKEN", dosageTaken = "500 mg"))
                }
            }
        }
    }
}

data class MedicationSlot(
    val medicine: Medicine,
    val timeLabel: String,
    val scheduledTimestamp: Long,
    val log: IntakeLog?
)
