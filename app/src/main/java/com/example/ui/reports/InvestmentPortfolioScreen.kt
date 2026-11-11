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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import com.example.ui.components.FullScreenDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountingGroups
import com.example.data.model.Ledger
import com.example.ui.theme.TallyCreditGreen
import com.example.ui.theme.TallyGold
import com.example.ui.viewmodel.TallyViewModel
import java.text.NumberFormat
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentPortfolioScreen(
  viewModel: TallyViewModel,
  onNavigateBack: () -> Unit,
  onRecordNewInvestment: () -> Unit
) {
  val ledgerBalances by viewModel.ledgerBalances.collectAsState()
  val inrFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

  val investmentBalances = ledgerBalances.values.filter { it.ledger.group == AccountingGroups.INVESTMENTS }

  val totalInvested = investmentBalances.sumOf { it.closingBalance }
  val totalCurrentValuation = investmentBalances.sumOf { it.ledger.marketValue ?: it.closingBalance }
  val netProfit = totalCurrentValuation - totalInvested
  val netReturnsPct = if (totalInvested > 0) (netProfit / totalInvested) * 100.0 else 0.0

  var editingLedger by remember { mutableStateOf<Ledger?>(null) }
  var updatedValuationInput by remember { mutableStateOf("") }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "INVESTMENT PORTFOLIO",
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

      // Portfolio Summary Hero Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = TallyBlueHeader),
          modifier = Modifier.fillMaxWidth(),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = "PORTFOLIO VALUATION",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              ),
              color = TallyGold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = inrFormat.format(totalCurrentValuation),
              style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Invested Capital", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                Text(inrFormat.format(totalInvested), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
              }
              Column(horizontalAlignment = Alignment.End) {
                Text("Unrealized Returns", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = if (netProfit >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (netProfit >= 0) TallyGold else Color(0xFFFF8A80),
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "${if (netProfit >= 0) "+" else ""}${inrFormat.format(netProfit)} (${String.format(Locale.US, "%.1f", netReturnsPct)}%)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (netProfit >= 0) TallyGold else Color(0xFFFF8A80)
                    )
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
              onClick = onRecordNewInvestment,
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TallyGold, contentColor = Color.Black),
              modifier = Modifier.fillMaxWidth().testTag("add_investment_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Record New Investment / SIP (Payment F5)", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Investment Assets List
      item {
        Text(
          text = "FAMILY WEALTH ASSETS (${investmentBalances.size})",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
          ),
          color = TallyTextBlue
        )
      }

      items(investmentBalances, key = { it.ledger.id }) { bal ->
        val invested = bal.closingBalance
        val currentVal = bal.ledger.marketValue ?: invested
        val profit = currentVal - invested
        val pct = if (invested > 0) (profit / invested) * 100.0 else 0.0

        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth().testTag("investment_card_${bal.ledger.id}")
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
                  .background(Color(0xFFEDE7F6)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.ShowChart,
                  contentDescription = null,
                  tint = Color(0xFF512DA8),
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
                  text = bal.ledger.subGroup ?: "Wealth Asset",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.Gray
                )
              }

              IconButton(
                onClick = {
                  editingLedger = bal.ledger
                  updatedValuationInput = (bal.ledger.marketValue ?: bal.closingBalance).toString()
                }
              ) {
                Icon(Icons.Default.Edit, contentDescription = "Update Market Value", tint = TallyBlueHeader)
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Invested Cost", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(inrFormat.format(invested), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
              }

              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Current Valuation", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(inrFormat.format(currentVal), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TallyTextBlue))
              }

              Column(horizontalAlignment = Alignment.End) {
                Text("Gain / Loss", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                  text = "${if (profit >= 0) "+" else ""}${inrFormat.format(profit)}",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (profit >= 0) TallyCreditGreen else Color(0xFFC62828)
                  )
                )
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(30.dp)) }
    }
  }

  // Update Market Valuation Dialog
  if (editingLedger != null) {
    val ledger = editingLedger!!
    FullScreenDialog(
      onDismissRequest = { editingLedger = null },
      title = { Text("Update Current Valuation") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Asset: ${ledger.name}", fontWeight = FontWeight.Bold)
          Text("Enter the latest market valuation from your broker or bank statement.")
          OutlinedTextField(
            value = updatedValuationInput,
            onValueChange = { updatedValuationInput = it },
            label = { Text("Current Market Value (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("market_value_input")
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val newVal = updatedValuationInput.toDoubleOrNull()
            if (newVal != null && newVal >= 0) {
              viewModel.updateLedger(ledger.copy(marketValue = newVal))
            }
            editingLedger = null
          }
        ) {
          Text("Save Valuation", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { editingLedger = null }) {
          Text("Cancel")
        }
      }
    )
  }
}
