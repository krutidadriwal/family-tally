package com.example.ui.reports

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualAnalyticsScreen(
  viewModel: TallyViewModel,
  onNavigateBack: () -> Unit
) {
  val ledgerBalances by viewModel.ledgerBalances.collectAsState()
  val vouchers by viewModel.vouchers.collectAsState()
  val inrFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

  // Collect Expense Ledgers with non-zero balances
  val expenseLedgers = ledgerBalances.values
    .filter { (AccountingGroups.isExpense(it.ledger.group)) && it.closingBalance > 0 }
    .sortedByDescending { it.closingBalance }

  val totalExpenses = expenseLedgers.sumOf { it.closingBalance }

  // Slice colors for pie chart
  val sliceColors = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047),
    Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF00ACC1),
    Color(0xFFFDD835), Color(0xFFD81B60), Color(0xFF3949AB)
  )

  // Income vs Expense
  val totalIncome = ledgerBalances.values
    .filter { AccountingGroups.isIncome(it.ledger.group) }
    .sumOf { it.closingBalance }

  // Family Member spending
  val memberSpendMap = mutableMapOf<String, Double>()
  vouchers.filter { it.voucherType == "F5" }.forEach { v ->
    val m = v.familyMember?.ifBlank { "Unassigned" } ?: "Unassigned"
    memberSpendMap[m] = (memberSpendMap[m] ?: 0.0) + v.totalAmount
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "VISUAL ANALYTICS",
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

      // 1. EXPENSE BREAKDOWN PIE CHART
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "EXPENSE BREAKDOWN BY CATEGORY",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
              ),
              color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (totalExpenses <= 0) {
              Text("No expenses recorded to visualize.", style = MaterialTheme.typography.bodySmall)
            } else {
              // Custom Canvas Pie Chart
              Canvas(modifier = Modifier.size(200.dp)) {
                var startAngle = -90f
                expenseLedgers.forEachIndexed { index, bal ->
                  val sweepAngle = ((bal.closingBalance / totalExpenses) * 360f).toFloat()
                  val color = sliceColors[index % sliceColors.size]
                  drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = size
                  )
                  startAngle += sweepAngle
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Legend
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                expenseLedgers.forEachIndexed { index, bal ->
                  val color = sliceColors[index % sliceColors.size]
                  val pct = if (totalExpenses > 0) (bal.closingBalance / totalExpenses) * 100.0 else 0.0

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(
                        modifier = Modifier
                          .size(12.dp)
                          .clip(CircleShape)
                          .background(color)
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(bal.ledger.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                    Text(
                      text = "${inrFormat.format(bal.closingBalance)} (${String.format(Locale.getDefault(), "%.1f", pct)}%)",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 2. CASH FLOW COMPARISON (INFLOW VS OUTFLOW)
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "INFLOW VS OUTFLOW",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
              ),
              color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(14.dp))

            val maxVal = maxOf(totalIncome, totalExpenses, 1.0)
            val incomeRatio = (totalIncome / maxVal).toFloat()
            val expenseRatio = (totalExpenses / maxVal).toFloat()

            // Inflow Bar
            Text("Total Income: ${inrFormat.format(totalIncome)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth(incomeRatio)
                .height(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(TallyCreditGreen)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Outflow Bar
            Text("Total Expenses: ${inrFormat.format(totalExpenses)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth(expenseRatio)
                .height(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(TallyDebitRed)
            )
          }
        }
      }

      // 3. SPENDING BY FAMILY MEMBER
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "SPENDING BY FAMILY MEMBER",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
              ),
              color = MaterialTheme.colorScheme.primary
            )

            if (memberSpendMap.isEmpty()) {
              Text("No member expense vouchers passed yet.", style = MaterialTheme.typography.bodySmall)
            } else {
              memberSpendMap.forEach { (member, amount) ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(member, fontWeight = FontWeight.SemiBold)
                  Text(inrFormat.format(amount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(40.dp)) }
    }
  }
}
