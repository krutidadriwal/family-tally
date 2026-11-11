package com.example.ui.masters

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import com.example.ui.components.FullScreenDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.data.model.AccountingGroup
import com.example.data.model.AccountingGroups
import com.example.data.model.Ledger
import com.example.ui.theme.TallyCreditGreen
import com.example.ui.theme.TallyDebitRed
import com.example.ui.theme.TallyBlueHeader
import com.example.ui.viewmodel.TallyViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgersScreen(
  viewModel: TallyViewModel,
  onNavigateBack: () -> Unit,
  onSelectLedgerStatement: (String) -> Unit = {},
  startInCreateMode: Boolean = false
) {
  val ledgers by viewModel.ledgers.collectAsState()
  val ledgerBalances by viewModel.ledgerBalances.collectAsState()
  val groups by viewModel.groups.collectAsState()
  val ledgerUsage by viewModel.ledgerUsage.collectAsState()
  var showDialog by remember { mutableStateOf(startInCreateMode) }
  var editingLedger by remember { mutableStateOf<Ledger?>(null) }
  val inrFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "LEDGER ACCOUNTS (MASTERS)",
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
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingLedger = null
          showDialog = true
        },
        containerColor = TallyBlueHeader,
        contentColor = Color.White,
        modifier = Modifier.testTag("fab_add_ledger")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Ledger")
      }
    }
  ) { padding ->
    val groupedLedgers = ledgers.groupBy { it.group }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item { Spacer(modifier = Modifier.height(4.dp)) }

      AccountingGroups.ALL.forEach { groupName ->
        val groupList = groupedLedgers[groupName] ?: emptyList()
        if (groupList.isNotEmpty()) {
          item {
            Text(
              text = groupName.uppercase(Locale.getDefault()),
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
              ),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
          }

          items(groupList, key = { it.id }) { ledger ->
            val bal = ledgerBalances[ledger.id]
            val balanceStr = bal?.let { "${inrFormat.format(it.closingBalance)} ${it.closingType}" } ?: "₹0.00"
            val isDebit = bal?.closingType == "Dr"

            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectLedgerStatement(ledger.id) }
                .testTag("ledger_card_${ledger.name.lowercase().replace(" ", "_")}"),
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
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                  )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = ledger.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                  )
                  if (ledger.budgetAmount != null && ledger.budgetAmount > 0) {
                    Text(
                      text = "Budget: ${inrFormat.format(ledger.budgetAmount)}/mo",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.secondary
                    )
                  }
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = balanceStr,
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (isDebit) TallyDebitRed else TallyCreditGreen
                    )
                  )
                  Row {
                    IconButton(
                      onClick = {
                        editingLedger = ledger
                        showDialog = true
                      },
                      modifier = Modifier.size(32.dp)
                    ) {
                      Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    val usageCount = ledgerUsage[ledger.id] ?: 0
                    val inUse = usageCount > 0 || ledger.isSystemLedger == true
                    IconButton(
                      onClick = {
                        if (inUse) {
                          viewModel.flashMessage("Cannot delete '${ledger.name}' — it is in use (${usageCount} entries)")
                        } else {
                          viewModel.deleteLedger(ledger.id)
                        }
                      },
                      enabled = !inUse,
                      modifier = Modifier.size(32.dp)
                    ) {
                      Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = if (inUse) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(70.dp)) }
    }
  }

  // Create / Edit Dialog
  if (showDialog) {
    LedgerEditDialog(
      existing = editingLedger,
      groups = groups,
      onDismiss = { showDialog = false },
      onSave = { ledger ->
        if (editingLedger == null) {
          viewModel.createLedger(ledger)
        } else {
          viewModel.updateLedger(ledger)
        }
        showDialog = false
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LedgerEditDialog(
  existing: Ledger?,
  groups: List<AccountingGroup>,
  onDismiss: () -> Unit,
  onSave: (Ledger) -> Unit
) {
  val groupNames = if (groups.isNotEmpty()) groups.map { it.name } else AccountingGroups.ALL
  var name by remember { mutableStateOf(existing?.name ?: "") }
  var group by remember { mutableStateOf(existing?.group ?: groupNames.firstOrNull() ?: AccountingGroups.INDIRECT_EXPENSES) }
  var openingBalStr by remember { mutableStateOf(existing?.openingBalance?.toString() ?: "0.0") }
  var budgetStr by remember { mutableStateOf(existing?.budgetAmount?.toString() ?: "") }
  var groupExpanded by remember { mutableStateOf(false) }

  // The Dr/Cr nature is inherited from the selected group's category, not entered here.
  fun natureForGroup(groupName: String): String =
    groups.find { it.name == groupName }?.category?.let { AccountingGroups.natureOfCategory(it) }
      ?: AccountingGroups.natureOfGroup(groupName)
  val derivedNature = natureForGroup(group)

  FullScreenDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (existing == null) "Create Ledger Account" else "Alter Ledger Account")
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Ledger Name") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("ledger_name_input"),
          singleLine = true
        )

        ExposedDropdownMenuBox(
          expanded = groupExpanded,
          onExpandedChange = { groupExpanded = !groupExpanded }
        ) {
          OutlinedTextField(
            value = group,
            onValueChange = {},
            readOnly = true,
            label = { Text("Primary Group") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
            modifier = Modifier
              .menuAnchor()
              .fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = groupExpanded,
            onDismissRequest = { groupExpanded = false }
          ) {
            groupNames.forEach { grp ->
              DropdownMenuItem(
                text = { Text(grp) },
                onClick = {
                  group = grp
                  groupExpanded = false
                }
              )
            }
          }
        }

        OutlinedTextField(
          value = openingBalStr,
          onValueChange = { openingBalStr = it },
          label = { Text("Opening Balance") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          supportingText = {
            Text("Nature: $derivedNature (inherited from ${group})")
          },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        OutlinedTextField(
          value = budgetStr,
          onValueChange = { budgetStr = it },
          label = { Text("Monthly Budget Limit (₹)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            val updated = Ledger(
              id = existing?.id ?: "",
              name = name.trim(),
              group = group,
              openingBalance = openingBalStr.toDoubleOrNull() ?: 0.0,
              openingBalanceType = natureForGroup(group),
              budgetAmount = budgetStr.toDoubleOrNull(),
              createdAt = existing?.createdAt
            )
            onSave(updated)
          }
        },
        enabled = name.isNotBlank()
      ) {
        Text("Save Ledger")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
