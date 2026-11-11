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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TallyCreditGreen
import com.example.ui.theme.TallyGold
import com.example.ui.viewmodel.TallyViewModel
import java.text.NumberFormat
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrialBalanceScreen(
  viewModel: TallyViewModel,
  onNavigateBack: () -> Unit
) {
  val ledgerBalances by viewModel.ledgerBalances.collectAsState()
  val inrFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
  var searchQuery by remember { mutableStateOf("") }

  val balanceList = ledgerBalances.values.toList()
    .filter { searchQuery.isBlank() || it.ledger.name.contains(searchQuery, ignoreCase = true) || it.ledger.group.contains(searchQuery, ignoreCase = true) }
    .sortedBy { it.ledger.name }

  val totalDebits = ledgerBalances.values.sumOf { if (it.closingType == "Dr") it.closingBalance else 0.0 }
  val totalCredits = ledgerBalances.values.sumOf { if (it.closingType == "Cr") it.closingBalance else 0.0 }
  val diff = kotlin.math.abs(totalDebits - totalCredits)
  val isBalanced = diff < 0.01

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "TRIAL BALANCE",
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
        .background(TallyBackground)
        .padding(padding)
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(10.dp))

      // Verification Badge
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isBalanced) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        ),
        modifier = Modifier.fillMaxWidth().testTag("trial_balance_status_banner")
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (isBalanced) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isBalanced) TallyCreditGreen else Color(0xFFC62828),
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = if (isBalanced) "Trial Balance is Balanced (Total Dr == Total Cr)" else "Difference in Trial Balance: ${inrFormat.format(diff)}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isBalanced) Color(0xFF1B5E20) else Color(0xFFB71C1C)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Search bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Filter ledgers in Trial Balance...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().testTag("trial_balance_search"),
        singleLine = true,
        shape = RoundedCornerShape(10.dp)
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Table Header
      Surface(
        color = TallyBlueHeader,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("PARTICULARS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.weight(1.8f))
          Text("DEBIT (₹)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.weight(1f))
          Text("CREDIT (₹)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.weight(1f))
        }
      }

      // Ledger Rows
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .background(Color.White),
        verticalArrangement = Arrangement.spacedBy(1.dp)
      ) {
        items(balanceList, key = { it.ledger.id }) { bal ->
          val dr = if (bal.closingType == "Dr") bal.closingBalance else 0.0
          val cr = if (bal.closingType == "Cr") bal.closingBalance else 0.0

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color.White)
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1.8f)) {
              Text(
                text = bal.ledger.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF1E293B)
              )
              Text(
                text = bal.ledger.group,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
              )
            }

            Text(
              text = if (dr > 0) inrFormat.format(dr) else "-",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = if (dr > 0) Color(0xFF1E293B) else Color.LightGray,
              modifier = Modifier.weight(1f)
            )

            Text(
              text = if (cr > 0) inrFormat.format(cr) else "-",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = if (cr > 0) Color(0xFF1E293B) else Color.LightGray,
              modifier = Modifier.weight(1f)
            )
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(Color(0xFFF1F5F9))
          )
        }
      }

      // Table Footer / Total
      Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("TOTAL", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TallyTextBlue, modifier = Modifier.weight(1.8f))
          Text(inrFormat.format(totalDebits), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = TallyBlueHeader, modifier = Modifier.weight(1f))
          Text(inrFormat.format(totalCredits), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = TallyBlueHeader, modifier = Modifier.weight(1f))
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
