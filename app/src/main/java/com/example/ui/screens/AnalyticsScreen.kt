package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IntakeLog
import com.example.ui.MedicineViewModel
import com.example.ui.components.MonthlyAdherenceGrid
import com.example.ui.components.isSameDay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalyticsScreen(
    viewModel: MedicineViewModel,
    modifier: Modifier = Modifier
) {
    val allLogs by viewModel.allIntakeLogs.collectAsState()

    var currentDisplayMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedReportDay by remember { mutableStateOf<Calendar?>(Calendar.getInstance()) }

    // Derive compliance statistics matching currently requested month
    val monthlyStats = remember(allLogs, currentDisplayMonth) {
        val year = currentDisplayMonth.get(Calendar.YEAR)
        val month = currentDisplayMonth.get(Calendar.MONTH)

        val calStart = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val calEnd = (calStart.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        val monthLogs = allLogs.filter { it.scheduledTime in calStart.timeInMillis..calEnd.timeInMillis }
        val totalRecorded = monthLogs.size
        val takenLogs = monthLogs.count { it.status == "TAKEN" }
        val skippedLogs = monthLogs.count { it.status == "SKIPPED" }

        val complianceRate = if (totalRecorded > 0) {
            (takenLogs.toFloat() / totalRecorded) * 100
        } else {
            0f
        }

        // Calculate consecutive Taken Streak
        var currentStreak = 0
        val sortedAllTakenDates = allLogs
            .filter { it.status == "TAKEN" }
            .map {
                val c = Calendar.getInstance()
                c.timeInMillis = it.scheduledTime
                c.set(Calendar.HOUR_OF_DAY, 0)
                c.set(Calendar.MINUTE, 0)
                c.set(Calendar.SECOND, 0)
                c.set(Calendar.MILLISECOND, 0)
                c.timeInMillis
            }
            .distinct()
            .sortedDescending() // newest first

        if (sortedAllTakenDates.isNotEmpty()) {
            val checkCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            var streakValid = true
            var dayOffset = 0

            // If user has taken today, check streak starting today.
            // If user hasn't taken today yet but took yesterday, check streak starting yesterday.
            var lastTakenDayMillis = sortedAllTakenDates.first()
            val diffToday = (checkCal.timeInMillis - lastTakenDayMillis) / (24 * 60 * 60 * 1000)

            if (diffToday <= 1) {
                while (streakValid) {
                    val dateToCheck = checkCal.timeInMillis - (dayOffset * 24L * 60L * 60L * 1000L)
                    if (sortedAllTakenDates.contains(dateToCheck)) {
                        currentStreak++
                        dayOffset++
                    } else {
                        streakValid = false
                    }
                }
            }
        }

        MonthlyStatsOutput(
            totalRecorded = totalRecorded,
            takenLogged = takenLogs,
            skippedLogged = skippedLogs,
            compliancePercent = complianceRate.toInt(),
            consecutiveStreak = currentStreak
        )
    }

    // Filter logs matching selected day inside calendar grid
    val filteredLogsForSelectedDay = remember(allLogs, selectedReportDay) {
        val activeDay = selectedReportDay ?: return@remember emptyList<IntakeLog>()
        val startCal = Calendar.getInstance().apply {
            timeInMillis = activeDay.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            add(Calendar.MILLISECOND, -1)
        }
        allLogs.filter { it.scheduledTime in startCal.timeInMillis..endCal.timeInMillis }
    }

    val selectedDayLabel = selectedReportDay?.let {
        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(it.time)
    } ?: "Tap a day to see logs"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Headers
        Text(
            text = "Adherence & Compliance Report",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Month switcher UI header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val updated = (currentDisplayMonth.clone() as Calendar).apply {
                        add(Calendar.MONTH, -1)
                    }
                    currentDisplayMonth = updated
                    selectedReportDay = null
                }
            ) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous Month", tint = MaterialTheme.colorScheme.primary)
            }

            val monthHeadingFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            Text(
                text = monthHeadingFormatter.format(currentDisplayMonth.time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(
                onClick = {
                    val updated = (currentDisplayMonth.clone() as Calendar).apply {
                        add(Calendar.MONTH, 1)
                    }
                    currentDisplayMonth = updated
                    selectedReportDay = null
                }
            ) {
                Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Statistical Indicators Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Monthly Adherence",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${monthlyStats.compliancePercent}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (monthlyStats.compliancePercent >= 85) Color(0xFF2E7D32)
                        else if (monthlyStats.compliancePercent >= 50) Color(0xFFEF6C00)
                        else Color(0xFFC62828)
                    )
                    Text(
                        text = "${monthlyStats.takenLogged}/${monthlyStats.totalRecorded} taken",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Active Taken Streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${monthlyStats.consecutiveStreak} Days",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "consecutive daily taken",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calendar Grid Report Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                MonthlyAdherenceGrid(
                    logs = allLogs,
                    currentMonth = currentDisplayMonth,
                    onDaySelected = { date -> selectedReportDay = date },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Adherence Report Key
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColorKeyCell(color = Color(0xFF4CAF50), label = "Taken")
                    ColorKeyCell(color = Color(0xFFFF9800), label = "Partial")
                    ColorKeyCell(color = Color(0xFFF44336), label = "Missed")
                    ColorKeyCell(color = MaterialTheme.colorScheme.surfaceVariant, label = "Empty")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Day Drawer Logs Panel
        Text(
            text = "$selectedDayLabel Logs",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (filteredLogsForSelectedDay.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No intake history logs recorded for this day.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredLogsForSelectedDay, key = { it.id }) { log ->
                    DayLogItem(
                        log = log,
                        onDelete = { viewModel.undoIntake(log) }
                    )
                }
            }
        }
    }
}

@Composable
fun DayLogItem(
    log: IntakeLog,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val actualTimeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.actualTime))
    val scheduledTimeStr = timeFormat.format(Date(log.scheduledTime))

    val isTaken = log.status == "TAKEN"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isTaken) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isTaken) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.medicineName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isTaken) {
                        "Taken at $actualTimeStr • Scheduled: $scheduledTimeStr"
                    } else {
                        "Skipped • Scheduled: $scheduledTimeStr"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ColorKeyCell(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

data class MonthlyStatsOutput(
    val totalRecorded: Int,
    val takenLogged: Int,
    val skippedLogged: Int,
    val compliancePercent: Int,
    val consecutiveStreak: Int
)
