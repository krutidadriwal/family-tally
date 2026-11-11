package com.example.ui.reports

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountingGroups
import com.example.data.model.VoucherType
import com.example.ui.theme.TallyCreditGreen
import com.example.ui.theme.TallyGold
import com.example.ui.theme.TallyBlueDark
import com.example.ui.theme.TallyBlueHeader
import com.example.ui.viewmodel.TallyViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashBankSummaryScreen(
  viewModel: TallyViewModel,
  onNavigateBack: () -> Unit,
  onNavigateToContra: () -> Unit
) {
  val ledgerBalances by viewModel.ledgerBalances.collectAsState()
  val inrFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

  val cashLedgers = ledgerBalances.values.filter { it.ledger.group == AccountingGroups.CASH_IN_HAND }
  val bankLedgers = ledgerBalances.values.filter { it.ledger.group == AccountingGroups.BANK_ACCOUNTS }

  val totalCash = cashLedgers.sumOf { if (it.closingType == "Dr") it.closingBalance else -it.closingBalance }
  val totalBank = bankLedgers.sumOf { if (it.closingType == "Dr") it.closingBalance else -it.closingBalance }
  val grandTotal = totalCash + totalBank

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "CASH & BANK SUMMARY",
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
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item { Spacer(modifier = Modifier.height(8.dp)) }

      // Total Liquidity Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = TallyBlueDark),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = "TOTAL LIQUID BALANCE",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TallyGold
              )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = inrFormat.format(grandTotal),
              style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
              onClick = onNavigateToContra,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.SwapHoriz, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Pass F4 Contra Entry (Bank <-> Cash)")
            }
          }
        }
      }

      // CASH ACCOUNTS
      item {
        Text(
          text = "CASH-IN-HAND (${inrFormat.format(totalCash)})",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
          ),
          color = MaterialTheme.colorScheme.primary
        )
      }

      items(cashLedgers) { bal ->
        AccountRow(
          name = bal.ledger.name,
          subtitle = "Liquid Physical Cash",
          amount = inrFormat.format(bal.closingBalance),
          closingType = bal.closingType,
          icon = Icons.Default.Payments
        )
      }

      // BANK ACCOUNTS
      item {
        Text(
          text = "BANK ACCOUNTS (${inrFormat.format(totalBank)})",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
          ),
          color = MaterialTheme.colorScheme.primary
        )
      }

      items(bankLedgers) { bal ->
        AccountRow(
          name = bal.ledger.name,
          subtitle = "Savings / Current Bank",
          amount = inrFormat.format(bal.closingBalance),
          closingType = bal.closingType,
          icon = Icons.Default.AccountBalance
        )
      }

      item { Spacer(modifier = Modifier.height(40.dp)) }
    }
  }
}

@Composable
private fun AccountRow(
  name: String,
  subtitle: String,
  amount: String,
  closingType: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

      Text(
        text = "$amount $closingType",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TallyCreditGreen)
      )
    }
  }
}
