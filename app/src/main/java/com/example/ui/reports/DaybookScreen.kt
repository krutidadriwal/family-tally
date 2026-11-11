package com.example.ui.reports

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import com.example.ui.components.FullScreenDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Voucher
import com.example.data.model.VoucherType
import com.example.ui.theme.TallyBlueHeader
import com.example.ui.viewmodel.TallyViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaybookScreen(
  viewModel: TallyViewModel,
  onNavigateBack: () -> Unit
) {
  val vouchers by viewModel.vouchers.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilterType by remember { mutableStateOf<String?>("ALL") }
  var inspectingVoucher by remember { mutableStateOf<Voucher?>(null) }
  val inrFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

  val filteredVouchers = vouchers.filter { v ->
    val matchesType = selectedFilterType == "ALL" || v.voucherType == selectedFilterType
    val matchesSearch = searchQuery.isBlank() ||
      v.narration.contains(searchQuery, ignoreCase = true) ||
      v.voucherNumber.contains(searchQuery, ignoreCase = true) ||
      v.entriesJson.contains(searchQuery, ignoreCase = true) ||
      v.date.contains(searchQuery, ignoreCase = true)
    matchesType && matchesSearch
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "DAYBOOK (TRANSACTIONS)",
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
        .padding(horizontal = 16.dp)
    ) {

      Spacer(modifier = Modifier.height(12.dp))

      // Search field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by narration, ledger or date...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("daybook_search_input"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Filter chips
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item {
          FilterChip(
            selected = selectedFilterType == "ALL",
            onClick = { selectedFilterType = "ALL" },
            label = { Text("All Vouchers (${vouchers.size})") }
          )
        }
        items(VoucherType.entries) { vType ->
          FilterChip(
            // Compare against keyName (F4-F7) — the canonical value stored on vouchers.
            selected = selectedFilterType == vType.keyName,
            onClick = { selectedFilterType = vType.keyName },
            label = { Text("${vType.code} ${vType.title}") }
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (filteredVouchers.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "No vouchers found for selected filters.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredVouchers, key = { it.id }) { voucher ->
            val vType = VoucherType.fromCode(voucher.voucherType)

            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { inspectingVoucher = voucher }
                .testTag("voucher_row_${voucher.voucherNumber}"),
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  color = Color(vType.colorHex),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.size(44.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = vType.code,
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = Color.White
                    )
                  }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = voucher.voucherNumber,
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = Color(vType.colorHex)
                    )
                    Text(
                      text = voucher.date,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  Text(
                    text = voucher.narration,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                  )

                  if (!voucher.familyMember.isNullOrBlank()) {
                    Text(
                      text = "By: ${voucher.familyMember}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.secondary
                    )
                  }
                }

                Spacer(modifier = Modifier.width(12.dp))

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
          item { Spacer(modifier = Modifier.height(60.dp)) }
        }
      }
    }
  }

  // Voucher Detail Inspector Dialog
  if (inspectingVoucher != null) {
    val v = inspectingVoucher!!
    val vType = VoucherType.fromCode(v.voucherType)

    FullScreenDialog(
      onDismissRequest = { inspectingVoucher = null },
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("${vType.title} Voucher #${v.voucherNumber}")
          Surface(
            color = Color(vType.colorHex),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              vType.code,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              fontSize = 11.sp
            )
          }
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Date: ${v.date}", fontWeight = FontWeight.SemiBold)
          Text("Total Amount: ${inrFormat.format(v.totalAmount)}", fontWeight = FontWeight.Bold, color = Color(vType.colorHex))
          Text("Narration: ${v.narration}")
          if (!v.familyMember.isNullOrBlank()) {
            Text("Family Member: ${v.familyMember}")
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text("Accounting Entries (Dr / Cr):", fontWeight = FontWeight.Bold)

          v.entries.forEach { entry ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("${entry.type}. ${entry.ledgerName}")
              Text(inrFormat.format(entry.amount), fontWeight = FontWeight.Bold)
            }
          }

          if (!v.attachmentUri.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Receipt Attachment:", fontWeight = FontWeight.Bold)
            AsyncImage(
              model = v.attachmentUri,
              contentDescription = "Receipt Attachment",
              modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(8.dp))
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { inspectingVoucher = null }) {
          Text("Close")
        }
      },
      dismissButton = {
        IconButton(
          onClick = {
            viewModel.deleteVoucher(v.id)
            inspectingVoucher = null
          }
        ) {
          Icon(Icons.Default.Delete, contentDescription = "Delete Voucher", tint = MaterialTheme.colorScheme.error)
        }
      }
    )
  }
}
