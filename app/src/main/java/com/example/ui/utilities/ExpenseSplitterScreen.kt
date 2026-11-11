package com.example.ui.utilities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.model.AccountingGroups
import com.example.data.model.Ledger
import com.example.data.model.Voucher
import com.example.data.model.VoucherEntry
import com.example.ui.theme.TallyJournalPurple
import com.example.ui.theme.TallyBlueHeader
import com.example.ui.viewmodel.TallyViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseSplitterScreen(
  viewModel: TallyViewModel,
  onNavigateBack: () -> Unit
) {
  val ledgers by viewModel.ledgers.collectAsState()
  val familyMembers by viewModel.familyMembers.collectAsState()
  val inrFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

  var billTitle by remember { mutableStateOf("Family Dinner & Swiggy") }
  var totalAmountStr by remember { mutableStateOf("3000") }
  val totalAmount by remember { derivedStateOf { totalAmountStr.toDoubleOrNull() ?: 0.0 } }

  var expenseLedger by remember(ledgers) {
    mutableStateOf(ledgers.find { it.group == AccountingGroups.INDIRECT_EXPENSES } ?: ledgers.firstOrNull())
  }
  var paidByLedger by remember(ledgers) {
    mutableStateOf(ledgers.find { it.group == AccountingGroups.BANK_ACCOUNTS } ?: ledgers.firstOrNull())
  }

  // Member selection
  val memberNames = remember(familyMembers) {
    if (familyMembers.isEmpty()) listOf("Self", "Spouse", "Brother", "Sister") else familyMembers.map { it.name }
  }
  val selectedMembers = remember {
    mutableStateMapOf<String, Boolean>().apply {
      memberNames.forEach { put(it, true) }
    }
  }

  val activeMemberCount by remember {
    derivedStateOf { selectedMembers.values.count { it }.coerceAtLeast(1) }
  }
  val perPersonShare by remember(totalAmount, activeMemberCount) {
    derivedStateOf { if (activeMemberCount > 0) totalAmount / activeMemberCount else 0.0 }
  }

  var expExpanded by remember { mutableStateOf(false) }
  var paidExpanded by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "EXPENSE SPLITTER (JOURNAL)",
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
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item_gap()

      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CallSplit, contentDescription = null, tint = TallyJournalPurple)
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Split a joint family expense. Automatically posts a balanced double-entry voucher allocating shares across members.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      OutlinedTextField(
        value = billTitle,
        onValueChange = { billTitle = it },
        label = { Text("Expense Title / Purpose") },
        modifier = Modifier.fillMaxWidth().testTag("split_title_input"),
        singleLine = true
      )

      OutlinedTextField(
        value = totalAmountStr,
        onValueChange = { totalAmountStr = it },
        label = { Text("Total Bill Amount (₹)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().testTag("split_amount_input"),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
      )

      // Expense Account
      ExposedDropdownMenuBox(
        expanded = expExpanded,
        onExpandedChange = { expExpanded = !expExpanded }
      ) {
        OutlinedTextField(
          value = expenseLedger?.name ?: "Select Expense Account",
          onValueChange = {},
          readOnly = true,
          label = { Text("Expense Category (Debit Dr)") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expExpanded) },
          modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expExpanded, onDismissRequest = { expExpanded = false }) {
          ledgers.forEach { l ->
            DropdownMenuItem(
              text = { Text(l.name) },
              onClick = {
                expenseLedger = l
                expExpanded = false
              }
            )
          }
        }
      }

      // Paid By Account
      ExposedDropdownMenuBox(
        expanded = paidExpanded,
        onExpandedChange = { paidExpanded = !paidExpanded }
      ) {
        OutlinedTextField(
          value = paidByLedger?.name ?: "Select Payment Account",
          onValueChange = {},
          readOnly = true,
          label = { Text("Paid From (Bank / Cash Credit Cr)") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paidExpanded) },
          modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = paidExpanded, onDismissRequest = { paidExpanded = false }) {
          ledgers.forEach { l ->
            DropdownMenuItem(
              text = { Text(l.name) },
              onClick = {
                paidByLedger = l
                paidExpanded = false
              }
            )
          }
        }
      }

      // Member Selection Checkboxes
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "SPLIT AMONG MEMBERS (${inrFormat.format(perPersonShare)} EACH):",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = TallyJournalPurple
          )
          Spacer(modifier = Modifier.height(8.dp))

          memberNames.forEach { name ->
            val checked = selectedMembers[name] ?: true
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(name, fontWeight = FontWeight.SemiBold)
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (checked) {
                  Text(inrFormat.format(perPersonShare), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Checkbox(
                  checked = checked,
                  onCheckedChange = { isChecked -> selectedMembers[name] = isChecked }
                )
              }
            }
          }
        }
      }

      // Post Voucher Button
      Button(
        onClick = {
          if (totalAmount <= 0 || expenseLedger == null || paidByLedger == null) return@Button

          val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
          val vchNum = "SPLIT-${System.currentTimeMillis().toString().takeLast(4)}"
          val included = selectedMembers.filter { it.value }.keys.joinToString(", ")

          val entries = listOf(
            VoucherEntry(
              ledgerId = expenseLedger!!.id,
              ledgerName = expenseLedger!!.name,
              type = "Dr",
              amount = totalAmount
            ),
            VoucherEntry(
              ledgerId = paidByLedger!!.id,
              ledgerName = paidByLedger!!.name,
              type = "Cr",
              amount = totalAmount
            )
          )

          val vch = Voucher(
            voucherNumber = vchNum,
            voucherType = "F7",
            date = currentDate,
            totalAmount = totalAmount,
            narration = "$billTitle split among $included (${inrFormat.format(perPersonShare)} each)",
            entriesJson = VoucherEntry.listToJson(entries),
            familyMember = included,
            isSplit = true,
            splitDetails = "Split between $activeMemberCount members"
          )

          viewModel.createVoucher(vch) { success ->
            if (success) {
              onNavigateBack()
            }
          }
        },
        enabled = totalAmount > 0 && expenseLedger != null && paidByLedger != null,
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("post_split_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TallyBlueHeader)
      ) {
        Icon(Icons.Default.Check, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "GENERATE JOURNAL VOUCHER (F7)",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }

      Spacer(modifier = Modifier.height(40.dp))
    }
  }
}

@Composable
private fun item_gap() {
  Spacer(modifier = Modifier.height(4.dp))
}
