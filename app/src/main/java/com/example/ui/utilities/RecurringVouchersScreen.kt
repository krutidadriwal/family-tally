package com.example.ui.utilities

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Repeat
import com.example.ui.components.FullScreenDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ledger
import com.example.data.model.RecurringVoucher
import com.example.ui.theme.TallyBlueHeader
import com.example.ui.viewmodel.TallyViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringVouchersScreen(
  viewModel: TallyViewModel,
  onNavigateBack: () -> Unit
) {
  val recurringList by viewModel.recurring.collectAsState()
  val ledgers by viewModel.ledgers.collectAsState()
  var showDialog by remember { mutableStateOf(false) }
  val inrFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "RECURRING AUTO-VOUCHERS",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 1.sp
            ),
            color = Color.White
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = TallyBlueHeader)
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showDialog = true },
        containerColor = TallyBlueHeader,
        contentColor = Color.White,
        modifier = Modifier.testTag("fab_add_recurring")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Recurring Voucher")
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item { Spacer(modifier = Modifier.height(8.dp)) }

      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Automate fixed entries like Rent, SIP investments, EMI deductions, and subscriptions so you don't need to retype them.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      if (recurringList.isEmpty()) {
        item {
          Text(
            "No recurring vouchers scheduled yet. Tap + to add Rent, SIP or Subscriptions.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp)
          )
        }
      } else {
        items(recurringList, key = { it.id }) { rec ->
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = rec.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                  )
                  Text(
                    text = "Day ${rec.dayOfMonth} of every month • ${rec.voucherType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Text(
                  text = inrFormat.format(rec.amount),
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                )
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = "Dr: ${rec.debitLedgerName}  <--  Cr: ${rec.creditLedgerName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Spacer(modifier = Modifier.height(12.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedButton(
                  onClick = { viewModel.postRecurringNow(rec) },
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Post Voucher Now")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  Switch(
                    checked = rec.isActive,
                    onCheckedChange = { active ->
                      viewModel.saveRecurring(rec.copy(isActive = active))
                    }
                  )
                  IconButton(onClick = { viewModel.deleteRecurring(rec.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                  }
                }
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(60.dp)) }
    }
  }

  if (showDialog) {
    RecurringVoucherDialog(
      ledgers = ledgers,
      onDismiss = { showDialog = false },
      onSave = {
        viewModel.saveRecurring(it)
        showDialog = false
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringVoucherDialog(
  ledgers: List<Ledger>,
  onDismiss: () -> Unit,
  onSave: (RecurringVoucher) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var amountStr by remember { mutableStateOf("") }
  var debitLedger by remember { mutableStateOf(ledgers.firstOrNull()) }
  var creditLedger by remember { mutableStateOf(ledgers.getOrNull(1)) }
  var dayOfMonthStr by remember { mutableStateOf("1") }
  var narration by remember { mutableStateOf("") }

  var drExpanded by remember { mutableStateOf(false) }
  var crExpanded by remember { mutableStateOf(false) }

  FullScreenDialog(
    onDismissRequest = onDismiss,
    title = { Text("Schedule Recurring Voucher") },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Title (e.g. House Rent, SIP, Netflix)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        OutlinedTextField(
          value = amountStr,
          onValueChange = { amountStr = it },
          label = { Text("Amount (₹)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        // Debit Ledger Picker
        ExposedDropdownMenuBox(
          expanded = drExpanded,
          onExpandedChange = { drExpanded = !drExpanded }
        ) {
          OutlinedTextField(
            value = debitLedger?.name ?: "Select Debit Account",
            onValueChange = {},
            readOnly = true,
            label = { Text("Debit Ledger (Expense / Receiver)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = drExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(expanded = drExpanded, onDismissRequest = { drExpanded = false }) {
            ledgers.forEach { l ->
              DropdownMenuItem(
                text = { Text(l.name) },
                onClick = {
                  debitLedger = l
                  drExpanded = false
                }
              )
            }
          }
        }

        // Credit Ledger Picker
        ExposedDropdownMenuBox(
          expanded = crExpanded,
          onExpandedChange = { crExpanded = !crExpanded }
        ) {
          OutlinedTextField(
            value = creditLedger?.name ?: "Select Credit Account",
            onValueChange = {},
            readOnly = true,
            label = { Text("Credit Ledger (Bank / Source)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = crExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(expanded = crExpanded, onDismissRequest = { crExpanded = false }) {
            ledgers.forEach { l ->
              DropdownMenuItem(
                text = { Text(l.name) },
                onClick = {
                  creditLedger = l
                  crExpanded = false
                }
              )
            }
          }
        }

        OutlinedTextField(
          value = dayOfMonthStr,
          onValueChange = { dayOfMonthStr = it },
          label = { Text("Day of Month (1-31)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        OutlinedTextField(
          value = narration,
          onValueChange = { narration = it },
          label = { Text("Narration") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountStr.toDoubleOrNull() ?: 0.0
          if (title.isNotBlank() && amt > 0 && debitLedger != null && creditLedger != null) {
            onSave(
              RecurringVoucher(
                title = title.trim(),
                voucherType = "F5",
                amount = amt,
                debitLedgerId = debitLedger!!.id,
                debitLedgerName = debitLedger!!.name,
                creditLedgerId = creditLedger!!.id,
                creditLedgerName = creditLedger!!.name,
                frequency = "MONTHLY",
                dayOfMonth = dayOfMonthStr.toLongOrNull()?.coerceIn(1, 31) ?: 1L,
                isActive = true,
                narration = narration.ifBlank { "Auto-recurring: $title" }
              )
            )
          }
        },
        enabled = title.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0
      ) {
        Text("Save Recurring")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
