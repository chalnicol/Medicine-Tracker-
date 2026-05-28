package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import com.example.data.IntakeLog
import com.example.data.PurchaseLog
import java.text.SimpleDateFormat
import java.util.*

/**
 * Custom Rolling Weekly Calendar Row for the Home view
 */
@Composable
fun RollingCalendarRow(
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    val dates = remember(selectedDate) {
        val list = mutableListOf<Calendar>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            add(Calendar.DAY_OF_YEAR, -3)
        }
        repeat(7) {
            list.add(cal.clone() as Calendar)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val dayFormat = SimpleDateFormat("E", Locale.getDefault())
    val dateFormat = SimpleDateFormat("d", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Shown Month Info Header
        Text(
            text = monthFormat.format(selectedDate.time),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(dates) { date ->
                val isSelected = isSameDay(date, selectedDate)
                val isToday = isSameDay(date, Calendar.getInstance())

                Column(
                    modifier = Modifier
                        .width(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else if (isToday) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .clickable { onDateSelected(date) }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayFormat.format(date.time),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateFormat.format(date.time),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Custom Canvas-based Line Chart for trend monitoring of unit purchase prices
 */
@Composable
fun PriceTrendChart(
    purchases: List<PurchaseLog>,
    modifier: Modifier = Modifier
) {
    if (purchases.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No purchase history logged for this medicine.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    if (purchases.size == 1) {
        val single = purchases.first()
        val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(single.purchaseDate))
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Need at least 2 logs to chart a trend.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Current: $${String.format("%.2f", single.unitPrice)} on $formattedDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val dates = purchases.map { it.purchaseDate }
    val prices = purchases.map { it.unitPrice }

    val minDate = dates.minOrNull() ?: 0L
    val maxDate = dates.maxOrNull() ?: 0L
    val dateRange = maxOf(1L, maxDate - minDate)

    val minPrice = prices.minOrNull() ?: 0.0
    val maxPrice = prices.maxOrNull() ?: 0.0
    val priceRange = maxOf(0.1, maxPrice - minPrice)

    // Add padding to price boundaries on the charts
    val chartMinY = maxOf(0.0, minPrice - (priceRange * 0.15))
    val chartMaxY = maxPrice + (priceRange * 0.15)
    val chartYRange = chartMaxY - chartMinY

    val dateFormatShort = SimpleDateFormat("MM/dd", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(16.dp)
    ) {
        Text(
            text = "Price History Trend (Unit Price)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val width = size.width
            val height = size.height

            val leftPadding = 120f
            val bottomPadding = 60f
            val rightPadding = 20f
            val topPadding = 20f

            val chartWidth = width - leftPadding - rightPadding
            val chartHeight = height - bottomPadding - topPadding

            // 1. Draw horizontal gridlines and Y labels
            val divisionCount = 4
            for (i in 0..divisionCount) {
                val fraction = i.toFloat() / divisionCount
                val yVal = chartMinY + (fraction * chartYRange)
                val canvasY = height - bottomPadding - (fraction * chartHeight)

                // Gridline
                drawLine(
                    color = gridColor,
                    start = Offset(leftPadding, canvasY),
                    end = Offset(width - rightPadding, canvasY),
                    strokeWidth = 2f
                )

                // Label Text
                drawText(
                    textMeasurer = textMeasurer,
                    text = "$${String.format("%.2f", yVal)}",
                    topLeft = Offset(10f, canvasY - 20f),
                    style = TextStyle(fontSize = 11.sp, color = labelColor)
                )
            }

            // Map data items to coordinates on viewport coordinates
            val coords = purchases.map { item ->
                val xFraction = if (dateRange == 0L) 0.5f else (item.purchaseDate - minDate).toFloat() / dateRange
                val yFraction = ((item.unitPrice - chartMinY) / chartYRange).toFloat()

                val cx = leftPadding + (xFraction * chartWidth)
                val cy = height - bottomPadding - (yFraction * chartHeight)
                Offset(cx, cy)
            }

            // 2. Draw gradient area fill
            if (coords.isNotEmpty()) {
                val fillPath = Path().apply {
                    moveTo(coords.first().x, height - bottomPadding)
                    for (pt in coords) {
                        lineTo(pt.x, pt.y)
                    }
                    lineTo(coords.last().x, height - bottomPadding)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.linearGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent),
                        start = Offset(0f, topPadding),
                        end = Offset(0f, height - bottomPadding)
                    )
                )
            }

            // 3. Draw connecting line trend
            if (coords.size >= 2) {
                val linePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        lineTo(coords[i].x, coords[i].y)
                    }
                }

                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(width = 6f, pathEffect = PathEffect.cornerPathEffect(30f))
                )
            }

            // 4. Draw data points and metadata labels
            for ((index, pt) in coords.withIndex()) {
                val purchase = purchases[index]

                // Point container circles
                drawCircle(
                    color = primaryColor,
                    center = pt,
                    radius = 12f
                )
                drawCircle(
                    color = secondaryColor,
                    center = pt,
                    radius = 6f
                )

                // Draw vertical indicator line for selected dots optionally
                drawLine(
                    color = gridColor,
                    start = Offset(pt.x, pt.y),
                    end = Offset(pt.x, height - bottomPadding),
                    strokeWidth = 2f
                )

                // Render matching X-Axis labels for each point (or stagger them to avoid collide)
                if (index == 0 || index == coords.size - 1 || coords.size < 5) {
                    val labelText = dateFormatShort.format(Date(purchase.purchaseDate))
                    val textLayoutResult = textMeasurer.measure(labelText, style = TextStyle(fontSize = 10.sp, color = labelColor))
                    val tw = textLayoutResult.size.width

                    drawText(
                        textMeasurer = textMeasurer,
                        text = labelText,
                        topLeft = Offset(pt.x - (tw / 2), height - bottomPadding + 8f),
                        style = TextStyle(fontSize = 10.sp, color = labelColor)
                    )
                }
            }
        }
    }
}

/**
 * Custom Month Adherence Report Calendar Grid interface
 */
@Composable
fun MonthlyAdherenceGrid(
    logs: List<IntakeLog>,
    currentMonth: Calendar,
    onDaySelected: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    val year = currentMonth.get(Calendar.YEAR)
    val month = currentMonth.get(Calendar.MONTH)

    val monthDays = remember(logs, year, month) {
        val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 2 = Mon ...
        // Align grid layout (Sunday start index, e.g. empty grid prefix padding)
        val paddingPrefix = firstDayOfWeek - 1

        val list = mutableListOf<CalendarDayData>()

        // Append padding items
        repeat(paddingPrefix) {
            list.add(CalendarDayData(isValid = false))
        }

        // Evaluate logs for actual days in month
        for (day in 1..daysInMonth) {
            val dayCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = dayCal.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1

            val dayLogs = logs.filter { it.scheduledTime in startOfDay..endOfDay }

            val status = when {
                dayLogs.isEmpty() -> AdherenceDayStatus.UNRECORDED
                dayLogs.all { it.status == "TAKEN" } -> AdherenceDayStatus.FULLY_TAKEN
                dayLogs.all { it.status == "SKIPPED" } -> AdherenceDayStatus.MISSED
                else -> AdherenceDayStatus.PARTIALLY_TAKEN
            }

            list.add(
                CalendarDayData(
                    dayNumber = day,
                    calendar = dayCal,
                    isValid = true,
                    status = status,
                    takenCount = dayLogs.count { it.status == "TAKEN" },
                    totalCount = dayLogs.size
                )
            )
        }

        list
    }

    val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(modifier = modifier) {
        // Week Header Line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (weekday in weekdays) {
                Text(
                    text = weekday,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dynamic chunked Row Grids (7 columns per row)
        val rows = monthDays.chunked(7)
        for (row in rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (item in row) {
                    if (item.isValid) {
                        val bgColor = when (item.status) {
                            AdherenceDayStatus.FULLY_TAKEN -> Color(0xFF4CAF50) // Emerald Green
                            AdherenceDayStatus.PARTIALLY_TAKEN -> Color(0xFFFF9800) // Amber Orange
                            AdherenceDayStatus.MISSED -> Color(0xFFF44336) // Auburn Red
                            AdherenceDayStatus.UNRECORDED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }

                        val fontColor = if (item.status == AdherenceDayStatus.UNRECORDED) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            Color.White
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .clickable { item.calendar?.let { onDaySelected(it) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = item.dayNumber.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = fontColor
                                )
                                if (item.totalCount > 0) {
                                    Text(
                                        text = "${item.takenCount}/${item.totalCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        color = fontColor.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    } else {
                        // Empty cell spacer
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}

data class CalendarDayData(
    val dayNumber: Int = 0,
    val calendar: Calendar? = null,
    val isValid: Boolean = false,
    val status: AdherenceDayStatus = AdherenceDayStatus.UNRECORDED,
    val takenCount: Int = 0,
    val totalCount: Int = 0
)

enum class AdherenceDayStatus {
    FULLY_TAKEN,
    PARTIALLY_TAKEN,
    MISSED,
    UNRECORDED
}

/**
 * Standard Utility to check if two Calendar dates represent the same year, month, and day
 */
fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
