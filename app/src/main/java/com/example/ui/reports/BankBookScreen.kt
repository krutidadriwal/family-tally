package com.example.ui.reports

import com.example.ui.theme.TallyTextBlue
import com.example.ui.theme.TallyBackground
import com.example.ui.theme.TallyBlueHeader
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountingGroups
import com.example.data.model.VoucherType
import com.example.ui.theme.TallyCreditGreen
import com.example.ui.theme.TallyGold
import com.example.ui.viewmodel.TallyViewModel
import java.text.NumberFormat
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankBookScreen(
  viewModel: TallyViewModel,
  onNavigateBack: () -> Unit,
  onNavigateToContra: () -> Unit,
  onSelectLedgerStatement: (String) -> Unit
) {
  val ledgerBalances by viewModel.ledgerBalances.collectAsState()
  val vouchers by viewModel.vouchers.collectAsState()
  val inrFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

  val bankAccounts = ledgerBalances.values.filter { it.ledger.group == AccountingGroups.BANK_ACCOUNTS }
  val totalBankBalance = bankAccounts.sumOf { if (it.closingType == "Dr") it.closingBalance else -it.closingBalance }

  var selectedBankId by remember { mutableStateOf<String?>("ALL") }

  val bankLedgerIds = bankAccounts.map { it.ledger.id }.toSet()
  val relevantVouchers = vouchers.filter { v ->
    if (selectedBankId == "ALL") {
      v.entries.any { it.ledgerId in bankLedgerIds }
    } else {
      v.entries.any { it.ledgerId == selectedBankId }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "CONSOLIDATED BANK BOOK",
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
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(TallyBackground)
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item { Spacer(modifier = Modifier.height(6.dp)) }

      // Consolidated Bank Balance Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = TallyBlueHeader),
          modifier = Modifier.fillMaxWidth(),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "CONSOLIDATED BANK BALANCE",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = TallyGold
              )
              Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "${bankAccounts.size} Accounts",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = inrFormat.format(totalBankBalance),
              style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
              onClick = onNavigateToContra,
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TallyGold, contentColor = Color.Black),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("bankbook_contra_button")
            ) {
              Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Fund Transfer / Inter-Bank Contra (F4)", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Bank Accounts Summary
      item {
        Text(
          text = "BANK ACCOUNTS BREAKDOWN",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
          ),
          color = TallyTextBlue
        )
      }

      items(bankAccounts, key = { it.ledger.id }) { bal ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectLedgerStatement(bal.ledger.id) }
            .testTag("bank_account_card_${bal.ledger.id}")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFE8EAF6)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.AccountBalance,
                  contentDescription = null,
                  tint = Color(0xFF283593),
                  modifier = Modifier.size(22.dp)
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = bal.ledger.name,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF1E293B)
                )
                Text(
                  text = "Opening Bal: ${inrFormat.format(bal.ledger.openingBalance)} ${bal.ledger.openingBalanceType}",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.Gray
                )
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = inrFormat.format(bal.closingBalance),
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TallyCreditGreen
                  )
                )
                Text(
                  text = "${bal.closingType} Available",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.Gray
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Deposits (Dr) and Withdrawals (Cr)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = TallyCreditGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Deposits: ${inrFormat.format(bal.debitTotal)}", style = MaterialTheme.typography.labelSmall)
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Withdrawals: ${inrFormat.format(bal.creditTotal)}", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      }

      // Filter by bank
      item {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "BANK STATEMENT TRANSACTIONS",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
          ),
          color = TallyTextBlue
        )
      }

      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            FilterChip(
              selected = selectedBankId == "ALL",
              onClick = { selectedBankId = "ALL" },
              label = { Text("All Accounts") }
            )
          }
          items(bankAccounts) { b ->
            FilterChip(
              selected = selectedBankId == b.ledger.id,
              onClick = { selectedBankId = b.ledger.id },
              label = { Text(b.ledger.name) }
            )
          }
        }
      }

      if (relevantVouchers.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("No bank transactions found.", color = Color.Gray)
            }
          }
        }
      } else {
        items(relevantVouchers, key = { it.id }) { voucher ->
          val vType = VoucherType.fromCode(voucher.voucherType)
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                color = Color(vType.colorHex),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = vType.code,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                  )
                }
              }

              Spacer(modifier = Modifier.width(10.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = voucher.narration.ifBlank { "Bank Voucher #${voucher.voucherNumber}" },
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = Color(0xFF1E293B),
                  maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = "${voucher.date} • #${voucher.voucherNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                  )
                  if (!voucher.paymentMode.isNullOrBlank()) {
                    Text(
                      text = "• ${voucher.paymentMode}",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = TallyBlueHeader
                    )
                  }
                }
              }

              Text(
                text = inrFormat.format(voucher.totalAmount),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color(vType.colorHex)
                )
              )
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(30.dp)) }
    }
  }
}
