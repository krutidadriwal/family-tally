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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountingGroups
import com.example.ui.theme.TallyCreditGreen
import com.example.ui.theme.TallyDebitRed
import com.example.ui.theme.TallyGold
import com.example.ui.theme.TallyBlueHeader
import com.example.ui.viewmodel.TallyViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitAndLossScreen(
  viewModel: TallyViewModel,
  onNavigateBack: () -> Unit
) {
  val ledgerBalances by viewModel.ledgerBalances.collectAsState()
  val inrFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

  // Incomes (Direct + Indirect)
  val incomeLedgers = ledgerBalances.values.filter {
    it.ledger.group == AccountingGroups.DIRECT_INCOMES || it.ledger.group == AccountingGroups.INDIRECT_INCOMES
  }
  val totalIncome = incomeLedgers.sumOf {
    // Incomes normal balance is Cr
    if (it.closingType == "Cr") it.closingBalance else -it.closingBalance
  }

  // Expenses (Direct + Indirect)
  val expenseLedgers = ledgerBalances.values.filter {
    it.ledger.group == AccountingGroups.DIRECT_EXPENSES || it.ledger.group == AccountingGroups.INDIRECT_EXPENSES
  }
  val totalExpenses = expenseLedgers.sumOf {
    // Expenses normal balance is Dr
    if (it.closingType == "Dr") it.closingBalance else -it.closingBalance
  }

  val netSavings = totalIncome - totalExpenses
  val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100.0).coerceAtLeast(0.0) else 0.0

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "PROFIT & LOSS (SAVINGS)",
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

      // Net Savings Banner
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = if (netSavings >= 0) TallyBlueHeader else TallyDebitRed),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(18.dp)
          ) {
            Text(
              text = if (netSavings >= 0) "NET FAMILY SAVINGS (SURPLUS)" else "NET FAMILY DEFICIT",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TallyGold
              )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = inrFormat.format(netSavings),
              style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Savings Ratio: ${String.format(Locale.getDefault(), "%.1f", savingsRate)}% of total incoming revenue",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.85f)
            )
          }
        }
      }

      // 1. INCOMES SECTION
      item {
        Text(
          text = "INCOMES / REVENUE",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
          ),
          color = TallyCreditGreen
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (incomeLedgers.isEmpty()) {
              Text("No income records passed yet.", style = MaterialTheme.typography.bodySmall)
            } else {
              incomeLedgers.forEach { bal ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(bal.ledger.name, fontWeight = FontWeight.SemiBold)
                    Text(bal.ledger.group, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Text(
                    text = inrFormat.format(bal.closingBalance),
                    fontWeight = FontWeight.Bold,
                    color = TallyCreditGreen
                  )
                }
              }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("TOTAL INCOME", fontWeight = FontWeight.Bold)
              Text(inrFormat.format(totalIncome), fontWeight = FontWeight.Bold, color = TallyCreditGreen)
            }
          }
        }
      }

      // 2. EXPENSES SECTION
      item {
        Text(
          text = "EXPENSES / OUTFLOWS",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
          ),
          color = TallyDebitRed
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (expenseLedgers.isEmpty()) {
              Text("No expense records passed yet.", style = MaterialTheme.typography.bodySmall)
            } else {
              expenseLedgers.forEach { bal ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(bal.ledger.name, fontWeight = FontWeight.SemiBold)
                    Text(bal.ledger.group, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Text(
                    text = inrFormat.format(bal.closingBalance),
                    fontWeight = FontWeight.Bold,
                    color = TallyDebitRed
                  )
                }
              }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("TOTAL EXPENSES", fontWeight = FontWeight.Bold)
              Text(inrFormat.format(totalExpenses), fontWeight = FontWeight.Bold, color = TallyDebitRed)
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(40.dp)) }
    }
  }
}
