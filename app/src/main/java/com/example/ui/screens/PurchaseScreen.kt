package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import com.example.data.Medicine
import com.example.data.PurchaseLog
import com.example.ui.MedicineViewModel
import com.example.ui.components.PriceTrendChart
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PurchaseScreen(
    viewModel: MedicineViewModel,
    modifier: Modifier = Modifier
) {
    val medicines by viewModel.medicines.collectAsState()
    val allPurchases by viewModel.allPurchaseLogs.collectAsState()
    val priceHistoryPoints by viewModel.priceHistoryForSelectedMed.collectAsState()
    val selectedPriceMedId by viewModel.selectedPriceMedId.collectAsState()

    var isAddPurchaseOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Screen Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Purchase & Price History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = { isAddPurchaseOpen = true },
                colors = IconButtonDefaults.filledTonalIconButtonColors()
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = "Add Purchase")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Interactive Trend Monitoring dropdown selector & Custom Canvas chart
        if (medicines.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Trace Price Trends:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var dropdownExpanded by remember { mutableStateOf(false) }
                    val currentSelectionName = medicines.find { it.id == selectedPriceMedId }?.name ?: "Select Medicine"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { dropdownExpanded = true }
                            .padding(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currentSelectionName,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            medicines.forEach { medicine ->
                                DropdownMenuItem(
                                    text = { Text(medicine.name, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        viewModel.selectPriceMedicineId(medicine.id)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Embedded Canvas Custom Trend Line Plot Chart
                    PriceTrendChart(
                        purchases = priceHistoryPoints,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Listing of all logged Purchase histories
        Text(
            text = "Receipt Log History (${allPurchases.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (allPurchases.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = "No Purchases",
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No Receipts Registered",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Log prescription drug buy prices to monitor savings and price fluctuations.",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(allPurchases, key = { it.id }) { purchase ->
                    PurchaseListItem(
                        purchase = purchase,
                        onDelete = { viewModel.deletePurchase(purchase) }
                    )
                }
            }
        }

        // Add purchase prompt
        if (isAddPurchaseOpen) {
            AddPurchaseDialog(
                medicines = medicines,
                onDismiss = { isAddPurchaseOpen = false },
                onSave = { medId, medName, price, quantity, store, notes ->
                    viewModel.addPurchase(medId, medName, System.currentTimeMillis(), price, quantity, store, notes)
                    isAddPurchaseOpen = false
                }
            )
        }
    }
}

@Composable
fun PurchaseListItem(
    purchase: PurchaseLog,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(purchase.purchaseDate))
    var isDeleteOpen by remember { mutableStateOf(false) }

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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = purchase.medicineName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Qty: ${purchase.quantity}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "@ $${String.format("%.2f", purchase.unitPrice)} each",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Text(
                        text = "${purchase.storeName} • $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format("%.2f", purchase.unitPrice * purchase.quantity)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = { isDeleteOpen = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete record",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (isDeleteOpen) {
        AlertDialog(
            onDismissRequest = { isDeleteOpen = false },
            title = { Text("Delete Purchase Log") },
            text = { Text("Are you sure you want to permanently remove this receipt log?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        isDeleteOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddPurchaseDialog(
    medicines: List<Medicine>,
    onDismiss: () -> Unit,
    onSave: (medId: Int, name: String, price: Double, quantity: Int, store: String, notes: String) -> Unit
) {
    var selectedIndex by remember { mutableStateOf(0) }
    var priceText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var storeName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var isDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Add Purchase Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (medicines.isEmpty()) {
                    Text(
                        text = "You need to add a medicine to your database catalog first before logging purchases.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text("Close")
                    }
                    return@Card
                }

                // Selection Dropdown
                Text(
                    text = "Select Medicine:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { isDropdownExpanded = true }
                        .padding(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(medicines[selectedIndex].name, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        medicines.forEachIndexed { index, medicine ->
                            DropdownMenuItem(
                                text = { Text(medicine.name) },
                                onClick = {
                                    selectedIndex = index
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Price textfield
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.toDoubleOrNull() != null) {
                            priceText = input
                        }
                    },
                    label = { Text("Unit Price ($)") },
                    placeholder = { Text("e.g. 5.99") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quantity Textfield
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.toIntOrNull() != null) {
                            quantityText = input
                        }
                    },
                    label = { Text("Quantity Purchased") },
                    placeholder = { Text("e.g. 1, 2, 5") },
                    leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Store Location Name
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Store / Pharmacy Name") },
                    placeholder = { Text("e.g. CVS Pharmacy, Hospital, Boots") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes Optional
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Purchase Notes (Optional)") },
                    placeholder = { Text("e.g. Applied prescription discount code") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Row Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val price = priceText.toDoubleOrNull() ?: 0.0
                            val qty = quantityText.toIntOrNull() ?: 1
                            val finalStoreName = if (storeName.trim().isEmpty()) "Pharmacy" else storeName.trim()
                            onSave(medicines[selectedIndex].id, medicines[selectedIndex].name, price, qty, finalStoreName, notes.trim())
                        },
                        enabled = priceText.isNotEmpty() && (priceText.toDoubleOrNull() ?: 0.0) >= 0 && quantityText.isNotEmpty() && (quantityText.toIntOrNull() ?: 0) > 0,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Receipt")
                    }
                }
            }
        }
    }
}
