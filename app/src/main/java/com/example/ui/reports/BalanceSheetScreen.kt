package com.example.ui.reports

import com.example.ui.theme.TallyTextBlue
import com.example.ui.theme.TallyBackground
import com.example.ui.theme.TallyBlueHeader
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountingGroups
import com.example.ui.theme.TallyCreditGreen
import com.example.ui.theme.TallyGold
import com.example.ui.viewmodel.TallyViewModel
import java.text.NumberFormat
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceSheetScreen(
  viewModel: TallyViewModel,
  onNavigateBack: () -> Unit
) {
  val ledgerBalances by viewModel.ledgerBalances.collectAsState()
  val inrFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

  // Incomes & Expenses to compute Net Profit / Surplus
  val incomeBalances = ledgerBalances.values.filter { AccountingGroups.isIncome(it.ledger.group) }
  val expenseBalances = ledgerBalances.values.filter { AccountingGroups.isExpense(it.ledger.group) }

  val totalIncome = incomeBalances.sumOf { it.closingBalance }
  val totalExpense = expenseBalances.sumOf { it.closingBalance }
  val netProfit = totalIncome - totalExpense

  // 1. LIABILITIES & CAPITAL
  val capitalBalances = ledgerBalances.values.filter { AccountingGroups.isEquity(it.ledger.group) }
  val baseCapital = capitalBalances.sumOf { it.closingBalance }
  val totalCapitalWithProfit = baseCapital + netProfit

  val loanBalances = ledgerBalances.values.filter { it.ledger.group == AccountingGroups.LOANS_LIABILITY }
  val creditCardBalances = ledgerBalances.values.filter { it.ledger.group == AccountingGroups.CREDIT_CARDS }

  val totalLoans = loanBalances.sumOf { it.closingBalance }
  val totalCreditCards = creditCardBalances.sumOf { it.closingBalance }
  val totalLiabilitiesAndCapital = totalCapitalWithProfit + totalLoans + totalCreditCards

  // 2. ASSETS
  val fixedAssetBalances = ledgerBalances.values.filter { it.ledger.group == AccountingGroups.FIXED_ASSETS }
  val investmentBalances = ledgerBalances.values.filter { it.ledger.group == AccountingGroups.INVESTMENTS }
  val cashBalances = ledgerBalances.values.filter { it.ledger.group == AccountingGroups.CASH_IN_HAND }
  val bankBalances = ledgerBalances.values.filter { it.ledger.group == AccountingGroups.BANK_ACCOUNTS }
  val debtorBalances = ledgerBalances.values.filter { it.ledger.group == AccountingGroups.SUNDRY_DEBTORS }

  val totalFixedAssets = fixedAssetBalances.sumOf { it.closingBalance }
  val totalInvestments = investmentBalances.sumOf { it.closingBalance }
  val totalCash = cashBalances.sumOf { it.closingBalance }
  val totalBank = bankBalances.sumOf { it.closingBalance }
  val totalDebtors = debtorBalances.sumOf { it.closingBalance }
  val totalCurrentAssets = totalCash + totalBank + totalDebtors

  val totalAssets = totalFixedAssets + totalInvestments + totalCurrentAssets

  val diff = kotlin.math.abs(totalAssets - totalLiabilitiesAndCapital)
  val isBalanced = diff < 0.01

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "BALANCE SHEET",
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

      // Double-entry Verification Banner
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isBalanced) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
          ),
          modifier = Modifier.fillMaxWidth().testTag("balance_sheet_status_banner")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (isBalanced) Icons.Default.CheckCircle else Icons.Default.Warning,
              contentDescription = null,
              tint = if (isBalanced) TallyCreditGreen else Color(0xFFC62828),
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = if (isBalanced) "Balance Sheet Perfectly Tallies" else "Opening Balance Difference",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isBalanced) Color(0xFF1B5E20) else Color(0xFFB71C1C)
              )
              Text(
                text = if (isBalanced)
                  "Total Capital & Liabilities = Total Assets (${inrFormat.format(totalAssets)})"
                else
                  "Difference in Opening Balance: ${inrFormat.format(diff)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isBalanced) Color(0xFF2E7D32) else Color(0xFFC62828)
              )
            }
          }
        }
      }

      // 1. LIABILITIES & CAPITAL SECTION
      item {
        BalanceSheetSectionCard(
          title = "CAPITAL & LIABILITIES",
          totalAmount = inrFormat.format(totalLiabilitiesAndCapital),
          headerColor = TallyBlueHeader
        ) {
          // Capital Account & Net Profit
          BalanceSheetSubGroup(
            name = "Capital Account",
            amount = inrFormat.format(totalCapitalWithProfit),
            items = listOf(
              "Family Capital (Corpus)" to inrFormat.format(baseCapital),
              "Add: Net Profit (Surplus from P&L)" to inrFormat.format(netProfit)
            )
          )

          // Loans (Liability)
          if (loanBalances.isNotEmpty()) {
            BalanceSheetSubGroup(
              name = "Loans (Liability)",
              amount = inrFormat.format(totalLoans),
              items = loanBalances.map { it.ledger.name to inrFormat.format(it.closingBalance) }
            )
          }

          // Current Liabilities
          if (creditCardBalances.isNotEmpty()) {
            BalanceSheetSubGroup(
              name = "Current Liabilities (Credit Cards)",
              amount = inrFormat.format(totalCreditCards),
              items = creditCardBalances.map { it.ledger.name to inrFormat.format(it.closingBalance) }
            )
          }
        }
      }

      // 2. ASSETS SECTION
      item {
        BalanceSheetSectionCard(
          title = "ASSETS",
          totalAmount = inrFormat.format(totalAssets),
          headerColor = Color(0xFF0D7C66)
        ) {
          // Fixed Assets
          if (fixedAssetBalances.isNotEmpty()) {
            BalanceSheetSubGroup(
              name = "Fixed Assets",
              amount = inrFormat.format(totalFixedAssets),
              items = fixedAssetBalances.map { it.ledger.name to inrFormat.format(it.closingBalance) }
            )
          }

          // Investments
          if (investmentBalances.isNotEmpty()) {
            BalanceSheetSubGroup(
              name = "Investments",
              amount = inrFormat.format(totalInvestments),
              items = investmentBalances.map { it.ledger.name to inrFormat.format(it.closingBalance) }
            )
          }

          // Current Assets
          BalanceSheetSubGroup(
            name = "Current Assets",
            amount = inrFormat.format(totalCurrentAssets),
            items = listOf(
              "Cash-in-Hand (${cashBalances.size} Wallets)" to inrFormat.format(totalCash),
              "Bank Accounts (${bankBalances.size} Banks)" to inrFormat.format(totalBank)
            ) + debtorBalances.map { it.ledger.name to inrFormat.format(it.closingBalance) }
          )
        }
      }

      item { Spacer(modifier = Modifier.height(30.dp)) }
    }
  }
}

@Composable
private fun BalanceSheetSectionCard(
  title: String,
  totalAmount: String,
  headerColor: Color,
  content: @Composable () -> Unit
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(headerColor)
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White
        )
        Text(
          text = totalAmount,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = TallyGold
        )
      }

      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        content()
      }
    }
  }
}

@Composable
private fun BalanceSheetSubGroup(
  name: String,
  amount: String,
  items: List<Pair<String, String>>
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = name,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        color = TallyTextBlue
      )
      Text(
        text = amount,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        color = Color(0xFF1E293B)
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    items.forEach { (subName, subAmt) ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(text = subName, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
        Text(text = subAmt, style = MaterialTheme.typography.bodySmall, color = Color(0xFF334155))
      }
    }
  }
}
