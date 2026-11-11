package com.example.ui.dashboard

import com.example.ui.theme.TallySubHeader
import com.example.ui.theme.TallyTextBlue
import com.example.ui.theme.TallyBackground
import com.example.ui.theme.TallyBlueHeader
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FullScreenDialog
import com.example.data.model.VoucherType
import com.example.ui.theme.TallyGold
import com.example.ui.viewmodel.TallyViewModel
import java.text.NumberFormat
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewayDashboardScreen(
  viewModel: TallyViewModel,
  onNavigateToVoucher: (VoucherType) -> Unit,
  onNavigateToLedgers: () -> Unit,
  onNavigateToCreate: () -> Unit,
  onNavigateToAlter: () -> Unit,
  onNavigateToFamilyMembers: () -> Unit,
  onNavigateToDaybook: () -> Unit,
  onNavigateToLedgerVouchers: () -> Unit,
  onNavigateToProfitAndLoss: () -> Unit,
  onNavigateToCashBook: () -> Unit,
  onNavigateToBankBook: () -> Unit,
  onNavigateToInvestments: () -> Unit,
  onNavigateToBalanceSheet: () -> Unit,
  onNavigateToTrialBalance: () -> Unit,
  onNavigateToRecurring: () -> Unit,
  onNavigateToSplitter: () -> Unit,
  onNavigateToAnalytics: () -> Unit,
  onSignOut: () -> Unit
) {
  var showVoucherTypesPopup by remember { mutableStateOf(false) }
  var showMoreReportsDialog by remember { mutableStateOf(false) }
  var selectedBottomTab by remember { mutableStateOf(0) }
  val budgetStatuses by viewModel.budgetStatuses.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
              text = "Gateway of Tally",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.5.sp
              ),
              color = Color.White
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = TallyBlueHeader)
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
      ) {
        NavigationBarItem(
          icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
          label = { Text("Home", fontSize = 11.sp) },
          selected = selectedBottomTab == 0,
          onClick = { selectedBottomTab = 0 },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = TallyBlueHeader,
            selectedTextColor = TallyBlueHeader,
            indicatorColor = TallyBackground
          )
        )
        NavigationBarItem(
          icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
          label = { Text("Search", fontSize = 11.sp) },
          selected = selectedBottomTab == 1,
          onClick = {
            selectedBottomTab = 1
            onNavigateToDaybook()
          },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = TallyBlueHeader,
            selectedTextColor = TallyBlueHeader
          )
        )
        NavigationBarItem(
          icon = { Icon(Icons.Default.Notifications, contentDescription = "Notifications") },
          label = { Text("Notifications", fontSize = 11.sp) },
          selected = selectedBottomTab == 2,
          onClick = {
            selectedBottomTab = 2
            onNavigateToProfitAndLoss()
          },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = TallyBlueHeader,
            selectedTextColor = TallyBlueHeader
          )
        )
        NavigationBarItem(
          icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
          label = { Text("Profile", fontSize = 11.sp) },
          selected = selectedBottomTab == 3,
          onClick = {
            selectedBottomTab = 3
            onNavigateToFamilyMembers()
          },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = TallyBlueHeader,
            selectedTextColor = TallyBlueHeader
          )
        )
      }
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(TallyBackground)
        .padding(padding)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {

        // 1. MASTERS
        TallySectionHeader("MASTERS")
        TallyHotKeyItem("Create", 'C', onClick = onNavigateToCreate)
        TallyHotKeyItem("Alter", 'A', onClick = onNavigateToAlter)
        TallyHotKeyItem("Chart of Accounts", 'C', onClick = onNavigateToLedgers)

        Spacer(modifier = Modifier.height(10.dp))

        // 2. TRANSACTIONS
        TallySectionHeader("TRANSACTIONS")

        TallyHotKeyItem("Vouchers", 'V', onClick = { showVoucherTypesPopup = true })
        TallyHotKeyItem("Day Book", 'D', onClick = onNavigateToDaybook)

        Spacer(modifier = Modifier.height(10.dp))

        // 3. UTILITIES
        TallySectionHeader("UTILITIES")
        TallyHotKeyItem("Banking", 'B', onClick = onNavigateToBankBook)
        TallyHotKeyItem("Cash Book", 'C', onClick = onNavigateToCashBook)
        TallyHotKeyItem("Recurring Auto-Vouchers", 'R', onClick = onNavigateToRecurring)
        TallyHotKeyItem("Expense Splitting (Journal)", 'E', onClick = onNavigateToSplitter)

        Spacer(modifier = Modifier.height(10.dp))

        // 4. REPORTS
        TallySectionHeader("REPORTS")
        TallyHotKeyItem("Income & Expenditure Statement", 'P', onClick = onNavigateToProfitAndLoss)
        TallyHotKeyItem("Balance Sheet", 'B', onClick = onNavigateToBalanceSheet)
        TallyHotKeyItem("Trial Balance", 'T', onClick = onNavigateToTrialBalance)
        TallyHotKeyItem("Display More Reports", 'D', onClick = { showMoreReportsDialog = true })

        Spacer(modifier = Modifier.height(10.dp))

        // 5. QUIT
        TallyHotKeyItem("Quit", 'Q', onClick = onSignOut)

        Spacer(modifier = Modifier.height(30.dp))
      }
    }
  }

  // VOUCHER TYPES (full screen)
  if (showVoucherTypesPopup) {
    FullScreenDialog(
      onDismissRequest = { showVoucherTypesPopup = false },
      title = { Text("Voucher Types") },
      headerColor = TallyBlueHeader,
      text = {
          Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            VoucherTypeRow(
              title = "Payment (P5)",
              code = "F5",
              icon = Icons.Default.Payments,
              iconColor = Color(0xFFC62828),
              onClick = {
                showVoucherTypesPopup = false
                onNavigateToVoucher(VoucherType.PAYMENT)
              }
            )
            DividerLine()
            VoucherTypeRow(
              title = "Receipt (R6)",
              code = "F6",
              icon = Icons.Default.Receipt,
              iconColor = Color(0xFF1565C0),
              onClick = {
                showVoucherTypesPopup = false
                onNavigateToVoucher(VoucherType.RECEIPT)
              }
            )
            DividerLine()
            VoucherTypeRow(
              title = "Journal Entry (J7)",
              code = "F7",
              icon = Icons.Default.Description,
              iconColor = Color(0xFF6A1B9A),
              onClick = {
                showVoucherTypesPopup = false
                onNavigateToVoucher(VoucherType.JOURNAL)
              }
            )
            DividerLine()
            VoucherTypeRow(
              title = "Contra (C4)",
              code = "F4",
              icon = Icons.Default.SwapHoriz,
              iconColor = Color(0xFF00796B),
              onClick = {
                showVoucherTypesPopup = false
                onNavigateToVoucher(VoucherType.CONTRA)
              }
            )
          }
      }
    )
  }

  // DISPLAY MORE REPORTS (full screen)
  if (showMoreReportsDialog) {
    FullScreenDialog(
      onDismissRequest = { showMoreReportsDialog = false },
      title = { Text("Display More Reports") },
      headerColor = TallyBlueHeader,
      text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MoreReportRow(title = "Granular Cash Book (Wallets)", onClick = { showMoreReportsDialog = false; onNavigateToCashBook() })
            DividerLine()
            MoreReportRow(title = "Consolidated Bank Book", onClick = { showMoreReportsDialog = false; onNavigateToBankBook() })
            DividerLine()
            MoreReportRow(title = "Investment Portfolio Dashboard", onClick = { showMoreReportsDialog = false; onNavigateToInvestments() })
            DividerLine()
            MoreReportRow(title = "Visual Analytics & Flow Charts", onClick = { showMoreReportsDialog = false; onNavigateToAnalytics() })
          }
      }
    )
  }
}

@Composable
private fun TallySectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelSmall.copy(
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.2.sp
    ),
    color = TallySubHeader,
    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
  )
}

@Composable
private fun TallyHotKeyItem(
  text: String,
  hotKey: Char,
  onClick: () -> Unit
) {
  val annotated = buildAnnotatedString {
    val idx = text.indexOf(hotKey, ignoreCase = false).let { if (it >= 0) it else text.indexOf(hotKey, ignoreCase = true) }
    if (idx >= 0) {
      append(text.substring(0, idx))
      withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = TallyTextBlue)) {
        append(text[idx])
      }
      append(text.substring(idx + 1))
    } else {
      append(text)
    }
  }

  Text(
    text = annotated,
    style = MaterialTheme.typography.titleMedium.copy(
      fontWeight = FontWeight.SemiBold,
      color = TallyTextBlue
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 6.dp)
  )
}

@Composable
private fun VoucherTypeRow(
  title: String,
  code: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconColor: Color,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = iconColor,
      modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.width(14.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = Color(0xFF1E293B)
    )
  }
}

@Composable
private fun MoreReportRow(title: String, onClick: () -> Unit) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = TallyTextBlue),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp)
  )
}

@Composable
private fun DividerLine() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(1.dp)
      .background(Color(0xFFE2E8F0))
  )
}
