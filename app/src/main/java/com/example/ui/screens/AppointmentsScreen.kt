package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.AppointmentEntity
import com.example.ui.theme.AccountingOrange
import com.example.ui.theme.AccountingRed
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.viewmodel.AccountingViewModel

@Composable
fun AppointmentsScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appointments by viewModel.appointments.collectAsState()
    val settings by viewModel.companySettings.collectAsState()

    var selectedFilter by remember { mutableStateOf("PENDING") } // PENDING, OVERDUE, ALL
    var selectedAppointmentToReschedule by remember { mutableStateOf<AppointmentEntity?>(null) }

    val now = System.currentTimeMillis()
    val filtered = appointments.filter { appt ->
        when (selectedFilter) {
            "OVERDUE" -> appt.status == "PENDING" && appt.dueDate < now
            "PENDING" -> appt.status == "PENDING"
            else -> true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppHeader(
                title = "المواعيد ووعود السداد",
                subtitle = "متابعة تحصيل ديون العملاء",
                showBackButton = true,
                onBackClick = { viewModel.navigateTo(com.example.ui.viewmodel.ScreenDestination.Home) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "PENDING",
                    onClick = { selectedFilter = "PENDING" },
                    label = { Text("المستحقة (${appointments.count { it.status == "PENDING" }})") }
                )
                FilterChip(
                    selected = selectedFilter == "OVERDUE",
                    onClick = { selectedFilter = "OVERDUE" },
                    label = { Text("المتأخرات (${appointments.count { it.status == "PENDING" && it.dueDate < now }})") }
                )
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("الكل (${appointments.size})") }
                )
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد مواعيد سداد في هذا القسم", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered) { appt ->
                        val isOverdue = appt.status == "PENDING" && appt.dueDate < now
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isOverdue) AccountingRed.copy(alpha = 0.15f)
                                                    else EmeraldLight.copy(alpha = 0.15f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isOverdue) Icons.Default.Warning else Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = if (isOverdue) AccountingRed else EmeraldDark,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(appt.partyName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text(
                                                "تاريخ الاستحقاق: ${formatDate(appt.dueDate)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOverdue) AccountingRed else Color.DarkGray
                                            )
                                        }
                                    }
                                    Text(
                                        formatCurrency(appt.amount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (isOverdue) AccountingRed else EmeraldDark
                                    )
                                }

                                if (appt.rescheduleReason != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "تم التمديد سابقاً: ${appt.rescheduleReason} (الأصلي: ${formatDate(appt.originalDueDate)})",
                                        fontSize = 11.sp,
                                        color = AccountingOrange
                                    )
                                }

                                if (appt.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(appt.notes, fontSize = 11.sp, color = Color.Gray)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // WhatsApp debt reminder button
                                    Button(
                                        onClick = {
                                            val companyName = settings?.companyName ?: "المحاسب الذكي"
                                            val msg = viewModel.repository.buildDebtReminderWhatsAppMessage(
                                                companyName = companyName,
                                                customerName = appt.partyName,
                                                amount = appt.amount,
                                                dueDate = appt.dueDate
                                            )
                                            openWhatsApp(context, appt.phone, msg)
                                        },
                                        modifier = Modifier.weight(1.3f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تذكير واتساب", fontSize = 12.sp)
                                    }

                                    // Dial phone
                                    OutlinedButton(
                                        onClick = { dialPhoneNumber(context, appt.phone) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("اتصال", fontSize = 12.sp)
                                    }

                                    // Reschedule
                                    OutlinedButton(
                                        onClick = { selectedAppointmentToReschedule = appt },
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تمديد مهلة", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // RESCHEDULE DIALOG
    selectedAppointmentToReschedule?.let { appt ->
        RescheduleDialog(
            appointment = appt,
            onDismiss = { selectedAppointmentToReschedule = null },
            onConfirm = { newDate, reason ->
                viewModel.rescheduleAppointment(appt.id, newDate, reason)
                selectedAppointmentToReschedule = null
            }
        )
    }
}

@Composable
fun RescheduleDialog(
    appointment: AppointmentEntity,
    onDismiss: () -> Unit,
    onConfirm: (newDate: Long, reason: String) -> Unit
) {
    var daysToAddText by remember { mutableStateOf("7") }
    var reasonText by remember { mutableStateOf("طلب العميل مهلة إضافية") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تمديد موعد السداد", fontWeight = FontWeight.Bold, color = EmeraldDark) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("العميل: ${appointment.partyName}", fontWeight = FontWeight.Bold)
                Text("المبلغ: ${formatCurrency(appointment.amount)}")
                Text("الموعد الحالي: ${formatDate(appointment.dueDate)}")
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = daysToAddText,
                    onValueChange = { daysToAddText = it },
                    label = { Text("عدد أيام التمديد الإضافية") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text("سبب التأجيل والملاحظات") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = daysToAddText.toLongOrNull() ?: 7L
                    val newDate = appointment.dueDate + (days * 86400000)
                    onConfirm(newDate, reasonText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
            ) {
                Text("تأكيد التمديد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
