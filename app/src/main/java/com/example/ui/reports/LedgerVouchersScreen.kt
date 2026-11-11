package com.example.ui.reports

import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountingGroups
import com.example.ui.theme.TallyCreditGreen
import com.example.ui.theme.TallyDebitRed
import com.example.ui.theme.TallyBlueHeader
import com.example.ui.viewmodel.TallyViewModel
import java.text.NumberFormat
import java.util.Locale

data class StatementRow(
  val date: String,
  val voucherNumber: String,
  val voucherType: String,
  val particulars: String,
  val debit: Double,
  val credit: Double,
  val runningBalance: Double,
  val balanceType: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerVouchersScreen(
  initialLedgerId: String? = null,
  viewModel: TallyViewModel,
  onNavigateBack: () -> Unit
) {
  val context = LocalContext.current
  val ledgers by viewModel.ledgers.collectAsState()
  val vouchers by viewModel.vouchers.collectAsState()
  val ledgerBalances by viewModel.ledgerBalances.collectAsState()
  val inrFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

  var selectedLedgerId by remember(ledgers) {
    mutableStateOf(initialLedgerId ?: ledgers.firstOrNull()?.id ?: "")
  }

  val selectedLedger = ledgers.find { it.id == selectedLedgerId }

  // Compute Statement rows
  val statementRows by remember(selectedLedger, vouchers) {
    derivedStateOf {
      if (selectedLedger == null) return@derivedStateOf emptyList()

      val isAssetOrExpense = AccountingGroups.isAsset(selectedLedger.group) || AccountingGroups.isExpense(selectedLedger.group)
      var currentBal = if (selectedLedger.openingBalanceType.equals("Dr", true)) selectedLedger.openingBalance else -selectedLedger.openingBalance
      if (!isAssetOrExpense) currentBal = -currentBal

      val sortedVouchers = vouchers.sortedBy { it.date }
      val rows = mutableListOf<StatementRow>()

      for (v in sortedVouchers) {
        val entry = v.entries.find { it.ledgerId == selectedLedger.id || it.ledgerName.equals(selectedLedger.name, true) }
        if (entry != null) {
          val otherEntries = v.entries.filter { it != entry }
          val particulars = otherEntries.joinToString(", ") { it.ledgerName }.ifBlank { v.narration }

          val dr = if (entry.type.equals("Dr", true)) entry.amount else 0.0
          val cr = if (entry.type.equals("Cr", true)) entry.amount else 0.0

          if (isAssetOrExpense) {
            currentBal += (dr - cr)
          } else {
            currentBal += (cr - dr)
          }

          val bType = if (currentBal >= 0) {
            if (isAssetOrExpense) "Dr" else "Cr"
          } else {
            if (isAssetOrExpense) "Cr" else "Dr"
          }

          rows.add(
            StatementRow(
              date = v.date,
              voucherNumber = v.voucherNumber,
              voucherType = v.voucherType,
              particulars = particulars,
              debit = dr,
              credit = cr,
              runningBalance = kotlin.math.abs(currentBal),
              balanceType = bType
            )
          )
        }
      }
      rows
    }
  }

  var dropdownExpanded by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "LEDGER VOUCHERS",
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
        actions = {
          IconButton(
            onClick = {
              if (selectedLedger != null) {
                val statementText = buildString {
                  appendLine("=== FAMILY TALLY ACCOUNT STATEMENT ===")
                  appendLine("Ledger: ${selectedLedger.name}")
                  appendLine("Group: ${selectedLedger.group}")
                  appendLine("Opening Balance: ${inrFormat.format(selectedLedger.openingBalance)} ${selectedLedger.openingBalanceType}")
                  appendLine("---------------------------------------")
                  statementRows.forEach { r ->
                    appendLine("${r.date} | ${r.voucherNumber} (${r.voucherType}) | ${r.particulars}")
                    appendLine("Dr: ${if (r.debit > 0) inrFormat.format(r.debit) else "-"} | Cr: ${if (r.credit > 0) inrFormat.format(r.credit) else "-"} | Bal: ${inrFormat.format(r.runningBalance)} ${r.balanceType}")
                    appendLine()
                  }
                  val bal = ledgerBalances[selectedLedger.id]
                  appendLine("---------------------------------------")
                  appendLine("Closing Balance: ${bal?.let { "${inrFormat.format(it.closingBalance)} ${it.closingType}" } ?: "-"}")
                }

                val sendIntent = Intent().apply {
                  action = Intent.ACTION_SEND
                  putExtra(Intent.EXTRA_TEXT, statementText)
                  type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Ledger Statement"))
              }
            }
          ) {
            Icon(Icons.Default.Share, contentDescription = "Share Statement", tint = Color.White)
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
    ) {

      Spacer(modifier = Modifier.height(12.dp))

      // Ledger Selector Dropdown
      ExposedDropdownMenuBox(
        expanded = dropdownExpanded,
        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
      ) {
        OutlinedTextField(
          value = selectedLedger?.let { "${it.name} (${it.group})" } ?: "Select Ledger",
          onValueChange = {},
          readOnly = true,
          label = { Text("Account / Ledger") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
          modifier = Modifier
            .menuAnchor()
            .fillMaxWidth()
            .testTag("ledger_statement_selector")
        )
        ExposedDropdownMenu(
          expanded = dropdownExpanded,
          onDismissRequest = { dropdownExpanded = false }
        ) {
          ledgers.forEach { ledger ->
            DropdownMenuItem(
              text = {
                Column {
                  Text(ledger.name, fontWeight = FontWeight.Bold)
                  Text(ledger.group, style = MaterialTheme.typography.labelSmall)
                }
              },
              onClick = {
                selectedLedgerId = ledger.id
                dropdownExpanded = false
              }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (selectedLedger != null) {
        val bal = ledgerBalances[selectedLedger.id]

        // Account Header Card
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Opening: ${inrFormat.format(selectedLedger.openingBalance)} ${selectedLedger.openingBalanceType}",
                style = MaterialTheme.typography.bodySmall
              )
              Text(
                text = "Group: ${selectedLedger.group}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "Closing Balance",
                style = MaterialTheme.typography.labelSmall
              )
              Text(
                text = bal?.let { "${inrFormat.format(it.closingBalance)} ${it.closingType}" } ?: "₹0.00",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = if (bal?.closingType == "Dr") TallyDebitRed else TallyCreditGreen
                )
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Table Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Date / Particulars", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.8f))
          Text("Debit (Dr)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
          Text("Credit (Cr)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
          Text("Balance", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.2f))
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (statementRows.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              "No transactions recorded for this ledger yet.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(statementRows) { row ->
              Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1.8f)) {
                    Text(row.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(row.particulars, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                    Text("${row.voucherType} #${row.voucherNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                  }

                  Text(
                    text = if (row.debit > 0) inrFormat.format(row.debit) else "-",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (row.debit > 0) TallyDebitRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                  )

                  Text(
                    text = if (row.credit > 0) inrFormat.format(row.credit) else "-",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (row.credit > 0) TallyCreditGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                  )

                  Text(
                    text = "${inrFormat.format(row.runningBalance)} ${row.balanceType}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1.2f)
                  )
                }
              }
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
          }
        }
      }
    }
  }
}
