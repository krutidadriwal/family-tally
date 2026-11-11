package com.example.ui.vouchers

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AccountingGroups
import com.example.data.model.Ledger
import com.example.data.model.Voucher
import com.example.data.model.VoucherEntry
import com.example.data.model.VoucherType
import com.example.ui.theme.TallyBlueHeader
import com.example.ui.viewmodel.TallyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherEntryScreen(
  initialType: VoucherType = VoucherType.PAYMENT,
  viewModel: TallyViewModel,
  onNavigateBack: () -> Unit
) {
  val context = LocalContext.current
  var selectedType by remember { mutableStateOf(initialType) }
  val ledgers by viewModel.ledgers.collectAsState()
  val familyMembers by viewModel.familyMembers.collectAsState()
  val budgetStatuses by viewModel.budgetStatuses.collectAsState()

  val currentDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
  var voucherNumber by remember { mutableStateOf("VCH-${System.currentTimeMillis().toString().takeLast(5)}") }
  var dateStr by remember { mutableStateOf(currentDate) }
  var narration by remember { mutableStateOf("") }
  var selectedMember by remember { mutableStateOf(familyMembers.firstOrNull()?.name ?: "Self") }
  var attachmentUri by remember { mutableStateOf<Uri?>(null) }

  // Payment Mode: "UPI", "BANK", "CASH"
  var paymentMode by remember { mutableStateOf("UPI") }
  var upiId by remember { mutableStateOf("") }
  var payeeName by remember { mutableStateOf("") }

  // Quick mode ledgers
  var fromLedger by remember(selectedType, ledgers) {
    mutableStateOf(
      when (selectedType) {
        VoucherType.PAYMENT -> ledgers.find { it.group == AccountingGroups.BANK_ACCOUNTS || it.group == AccountingGroups.CASH_IN_HAND }
        VoucherType.RECEIPT -> ledgers.find { it.group == AccountingGroups.DIRECT_INCOMES || it.group == AccountingGroups.INDIRECT_INCOMES }
        VoucherType.CONTRA -> ledgers.find { it.group == AccountingGroups.CASH_IN_HAND }
        VoucherType.JOURNAL -> ledgers.firstOrNull()
      }
    )
  }

  var toLedger by remember(selectedType, ledgers) {
    mutableStateOf(
      when (selectedType) {
        VoucherType.PAYMENT -> ledgers.find { it.group == AccountingGroups.DIRECT_EXPENSES || it.group == AccountingGroups.INDIRECT_EXPENSES }
        VoucherType.RECEIPT -> ledgers.find { it.group == AccountingGroups.BANK_ACCOUNTS || it.group == AccountingGroups.CASH_IN_HAND }
        VoucherType.CONTRA -> ledgers.find { it.group == AccountingGroups.BANK_ACCOUNTS }
        VoucherType.JOURNAL -> ledgers.getOrNull(1)
      }
    )
  }

  var amountStr by remember { mutableStateOf("") }
  val amountValue by remember { derivedStateOf { amountStr.toDoubleOrNull() ?: 0.0 } }

  // Photo Picker
  val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    attachmentUri = uri
  }

  // Budget variance check for payments
  val budgetWarning by remember(toLedger, amountValue, budgetStatuses) {
    derivedStateOf {
      if (selectedType == VoucherType.PAYMENT && toLedger != null) {
        val status = budgetStatuses.find { it.ledger.id == toLedger?.id }
        if (status != null && status.budgetLimit > 0) {
          val projected = status.currentSpent + amountValue
          if (projected > status.budgetLimit) {
            val overrun = projected - status.budgetLimit
            "Warning: This entry exceeds ${status.ledger.name}'s monthly budget (₹${status.budgetLimit.toInt()}) by ₹${overrun.toInt()}!"
          } else null
        } else null
      } else null
    }
  }

  // UPI Launch Helper
  fun triggerUpiPayment(amount: Double, vpa: String, name: String, note: String) {
    val cleanVpa = vpa.trim().ifBlank { "electroworldgondia@okhdfcbank" }
    val cleanName = name.trim().ifBlank { toLedger?.name ?: "Family Expense" }
    val cleanNote = note.trim().ifBlank { "Family Tally Payment" }

    val upiUri = Uri.parse(
      "upi://pay?pa=$cleanVpa&pn=${Uri.encode(cleanName)}&am=${String.format(Locale.US, "%.2f", amount)}&cu=INR&tn=${Uri.encode(cleanNote)}"
    )

    val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
    try {
      context.startActivity(Intent.createChooser(upiIntent, "Pay with UPI (GPay, PhonePe, Paytm)"))
    } catch (e: Exception) {
      Toast.makeText(context, "No UPI app found on device. Recording voucher in Family Tally.", Toast.LENGTH_LONG).show()
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "VOUCHER ENTRY (${selectedType.code})",
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
        .verticalScroll(rememberScrollState())
    ) {

      // 1. Voucher Type Selector Tabs
      TabRow(
        selectedTabIndex = selectedType.ordinal,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedType.ordinal]),
            color = Color(selectedType.colorHex),
            height = 3.dp
          )
        }
      ) {
        VoucherType.entries.forEach { type ->
          Tab(
            selected = selectedType == type,
            onClick = { selectedType = type },
            text = {
              Text(
                text = "${type.code} ${type.title}",
                fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal,
                color = if (selectedType == type) Color(type.colorHex) else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          )
        }
      }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {

        // Voucher Meta Row (Number & Date)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedTextField(
            value = voucherNumber,
            onValueChange = { voucherNumber = it },
            label = { Text("Voucher No.") },
            modifier = Modifier.weight(1f).testTag("voucher_number_input"),
            singleLine = true
          )
          OutlinedTextField(
            value = dateStr,
            onValueChange = { dateStr = it },
            label = { Text("Date (YYYY-MM-DD)") },
            modifier = Modifier.weight(1f).testTag("voucher_date_input"),
            singleLine = true
          )
        }

        // Amount Input (Large & Clear)
        OutlinedTextField(
          value = amountStr,
          onValueChange = { amountStr = it },
          label = { Text("Transaction Amount (₹)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("voucher_amount_input"),
          textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
          leadingIcon = {
            Text(
              "₹",
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = Color(selectedType.colorHex)
            )
          },
          singleLine = true
        )

        // Payment Mode Selector (UPI, Bank, Cash) - Especially prominent for F5 Payment!
        if (selectedType == VoucherType.PAYMENT) {
          Column {
            Text(
              text = "PAYMENT MODE:",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("UPI", "BANK TRANSFER", "CASH").forEach { mode ->
                FilterChip(
                  selected = paymentMode == mode,
                  onClick = { paymentMode = mode },
                  label = { Text(mode) }
                )
              }
            }
          }

          // Direct UPI Details Box
          if (paymentMode == "UPI") {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Direct UPI App Integration",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                  )
                }
                OutlinedTextField(
                  value = upiId,
                  onValueChange = { upiId = it },
                  label = { Text("Payee UPI ID / VPA (e.g. name@okhdfcbank)") },
                  modifier = Modifier.fillMaxWidth().testTag("upi_id_input"),
                  singleLine = true
                )
                OutlinedTextField(
                  value = payeeName,
                  onValueChange = { payeeName = it },
                  label = { Text("Payee Name / Merchant") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  listOf("grocery@upi", "swiggy@icici", "landlord@sbi").forEach { sample ->
                    FilterChip(
                      selected = upiId == sample,
                      onClick = { upiId = sample },
                      label = { Text(sample, fontSize = 10.sp) }
                    )
                  }
                }
              }
            }
          }
        }

        // Budget Warning Card
        if (budgetWarning != null) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Warning, contentDescription = "Budget Warning", tint = Color(0xFF856404), modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = budgetWarning!!,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF856404)
              )
            }
          }
        }

        // Accounting Dr / Cr Double-Entry Pickers
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            val fromLabel = when (selectedType) {
              VoucherType.PAYMENT -> "From (Credit Account - Paid Out)"
              VoucherType.RECEIPT -> "From (Credit Account - Income Source)"
              VoucherType.CONTRA -> "From Account (Cr)"
              VoucherType.JOURNAL -> "Credit Account (Cr - Source)"
            }

            val toLabel = when (selectedType) {
              VoucherType.PAYMENT -> "To (Debit Account - Expense)"
              VoucherType.RECEIPT -> "To (Debit Account - Bank / Cash Deposit)"
              VoucherType.CONTRA -> "To Account (Dr)"
              VoucherType.JOURNAL -> "Debit Account (Dr - Destination)"
            }

            // Restrict each picker to the accounts valid for this voucher type:
            //   Payment -> Debit an Expense, Credit Cash/Bank
            //   Receipt -> Debit Cash/Bank, Credit an Income
            //   Contra / Journal -> all ledgers
            val isCashOrBank: (Ledger) -> Boolean = {
              it.group == AccountingGroups.CASH_IN_HAND || it.group == AccountingGroups.BANK_ACCOUNTS
            }
            val debitLedgers = when (selectedType) {
              VoucherType.PAYMENT -> ledgers.filter { AccountingGroups.isExpense(it.group) }
              VoucherType.RECEIPT -> ledgers.filter(isCashOrBank)
              else -> ledgers
            }
            val creditLedgers = when (selectedType) {
              VoucherType.PAYMENT -> ledgers.filter(isCashOrBank)
              VoucherType.RECEIPT -> ledgers.filter { AccountingGroups.isIncome(it.group) }
              else -> ledgers
            }

            // Debit Account Picker
            LedgerDropdown(
              label = toLabel,
              selected = toLedger,
              ledgers = debitLedgers,
              tag = "to_ledger_dropdown",
              onSelect = { toLedger = it }
            )

            // Credit Account Picker
            LedgerDropdown(
              label = fromLabel,
              selected = fromLedger,
              ledgers = creditLedgers,
              tag = "from_ledger_dropdown",
              onSelect = { fromLedger = it }
            )
          }
        }

        // Family Member who spent / received
        FamilyMemberPicker(
          members = familyMembers,
          selected = selectedMember,
          onSelect = { selectedMember = it }
        )

        // Narration
        OutlinedTextField(
          value = narration,
          onValueChange = { narration = it },
          label = { Text("Narration (Remarks / Details)") },
          modifier = Modifier.fillMaxWidth().testTag("voucher_narration_input"),
          maxLines = 3
        )

        // Common Narration Chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("Monthly Groceries", "UPI Transfer", "Dinner & Food", "ATM Cash").forEach { chip ->
            FilterChip(
              selected = narration == chip,
              onClick = { narration = chip },
              label = { Text(chip, fontSize = 11.sp) }
            )
          }
        }

        // Bill / Receipt Photo Attachment
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              }
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.AttachFile, contentDescription = "Attach bill", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (attachmentUri != null) "Receipt Attached" else "Attach Bill / Receipt Photo",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
              )
              Text(
                text = if (attachmentUri != null) "Tap to change image" else "Select photo from gallery or camera",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            if (attachmentUri != null) {
              AsyncImage(
                model = attachmentUri,
                contentDescription = "Attached receipt preview",
                modifier = Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(8.dp))
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Save Voucher Button
        Button(
          onClick = {
            if (amountValue <= 0 || toLedger == null || fromLedger == null) return@Button

            val entries = listOf(
              VoucherEntry(
                ledgerId = toLedger!!.id,
                ledgerName = toLedger!!.name,
                type = "Dr",
                amount = amountValue
              ),
              VoucherEntry(
                ledgerId = fromLedger!!.id,
                ledgerName = fromLedger!!.name,
                type = "Cr",
                amount = amountValue
              )
            )

            val vch = Voucher(
              voucherNumber = voucherNumber,
              voucherType = selectedType.keyName,
              date = dateStr,
              totalAmount = amountValue,
              narration = narration.ifBlank { "${selectedType.title} to ${toLedger?.name}" },
              entriesJson = VoucherEntry.listToJson(entries),
              paymentMode = paymentMode,
              upiId = upiId.ifBlank { null },
              familyMember = selectedMember,
              attachmentUri = attachmentUri?.toString()
            )

            // Trigger UPI Intent if UPI was chosen
            if (selectedType == VoucherType.PAYMENT && paymentMode == "UPI") {
              triggerUpiPayment(amountValue, upiId, payeeName, narration)
            }

            viewModel.createVoucher(vch) { success ->
              if (success) {
                onNavigateBack()
              }
            }
          },
          enabled = amountValue > 0 && toLedger != null && fromLedger != null,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("save_voucher_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(selectedType.colorHex))
        ) {
          Icon(Icons.Default.Check, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (selectedType == VoucherType.PAYMENT && paymentMode == "UPI") "PAY VIA UPI & SAVE VOUCHER" else "ACCEPT VOUCHER (SAVE)",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
          )
        }

        Spacer(modifier = Modifier.height(40.dp))
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LedgerDropdown(
  label: String,
  selected: Ledger?,
  ledgers: List<Ledger>,
  tag: String,
  onSelect: (Ledger) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = Modifier.testTag(tag)
  ) {
    OutlinedTextField(
      value = selected?.let { "${it.name} (${it.group})" } ?: "Select Ledger",
      onValueChange = {},
      readOnly = true,
      label = { Text(label) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier = Modifier
        .menuAnchor()
        .fillMaxWidth()
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      ledgers.forEach { ledger ->
        DropdownMenuItem(
          text = {
            Column {
              Text(ledger.name, fontWeight = FontWeight.Bold)
              Text(
                ledger.group,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          },
          onClick = {
            onSelect(ledger)
            expanded = false
          }
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamilyMemberPicker(
  members: List<com.example.data.model.FamilyMember>,
  selected: String,
  onSelect: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded }
  ) {
    OutlinedTextField(
      value = "Spent / Received By: $selected",
      onValueChange = {},
      readOnly = true,
      label = { Text("Family Member") },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier = Modifier
        .menuAnchor()
        .fillMaxWidth()
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      val names = if (members.isEmpty()) listOf("Self", "Spouse", "Dad", "Mom", "Kids") else members.map { it.name }
      names.forEach { name ->
        DropdownMenuItem(
          text = { Text(name) },
          onClick = {
            onSelect(name)
            expanded = false
          }
        )
      }
    }
  }
}
