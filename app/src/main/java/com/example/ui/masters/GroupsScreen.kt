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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountingGroup
import com.example.data.model.AccountingGroups
import com.example.ui.components.FullScreenDialog
import com.example.ui.theme.TallyBlueHeader
import com.example.ui.viewmodel.TallyViewModel
import java.util.Locale

private val CATEGORIES = listOf("Asset", "Liability", "Equity", "Income", "Expense")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
  viewModel: TallyViewModel,
  onNavigateBack: () -> Unit,
  startInCreateMode: Boolean = false
) {
  val groups by viewModel.groups.collectAsState()
  val groupUsage by viewModel.groupUsage.collectAsState()
  var showDialog by remember { mutableStateOf(startInCreateMode) }
  var editing by remember { mutableStateOf<AccountingGroup?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "ACCOUNTING GROUPS",
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = TallyBlueHeader)
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { editing = null; showDialog = true },
        containerColor = TallyBlueHeader,
        contentColor = Color.White
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Group")
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      item { Spacer(modifier = Modifier.height(4.dp)) }

      items(groups.sortedBy { it.name }, key = { it.id }) { grp ->
        val usageCount = groupUsage[grp.name] ?: 0
        val inUse = usageCount > 0 || grp.isSystemGroup == true
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(grp.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
              Text(
                text = "${grp.category} · ${AccountingGroups.natureOfCategory(grp.category)}" +
                  (grp.parentGroup?.let { " · under $it" } ?: "") +
                  (if (grp.isSystemGroup == true) " · system" else ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            IconButton(onClick = { editing = grp; showDialog = true }, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
            }
            IconButton(
              onClick = {
                if (inUse) {
                  val reason = if (grp.isSystemGroup == true) "it is a system group" else "$usageCount ledger(s) use it"
                  viewModel.flashMessage("Cannot delete '${grp.name}' — $reason")
                } else {
                  viewModel.deleteGroup(grp.id)
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

      item { Spacer(modifier = Modifier.height(70.dp)) }
    }
  }

  if (showDialog) {
    GroupEditDialog(
      existing = editing,
      onDismiss = { showDialog = false },
      onSave = { grp ->
        viewModel.saveGroup(grp)
        showDialog = false
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupEditDialog(
  existing: AccountingGroup?,
  onDismiss: () -> Unit,
  onSave: (AccountingGroup) -> Unit
) {
  var name by remember { mutableStateOf(existing?.name ?: "") }
  var category by remember { mutableStateOf(existing?.category ?: "Expense") }
  var parentGroup by remember { mutableStateOf(existing?.parentGroup ?: "") }
  var catExpanded by remember { mutableStateOf(false) }

  FullScreenDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existing == null) "Create Group" else "Alter Group") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Group Name") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
          OutlinedTextField(
            value = "$category  (${AccountingGroups.natureOfCategory(category)})",
            onValueChange = {},
            readOnly = true,
            label = { Text("Nature / Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
            supportingText = { Text("Ledgers in this group are ${AccountingGroups.natureOfCategory(category)} by nature") },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
            CATEGORIES.forEach { c ->
              DropdownMenuItem(
                text = { Text("$c  (${AccountingGroups.natureOfCategory(c)})") },
                onClick = { category = c; catExpanded = false }
              )
            }
          }
        }

        OutlinedTextField(
          value = parentGroup,
          onValueChange = { parentGroup = it },
          label = { Text("Parent Group (optional)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            onSave(
              AccountingGroup(
                id = existing?.id ?: "",
                name = name.trim(),
                parentGroup = parentGroup.trim().ifBlank { null },
                isSystemGroup = existing?.isSystemGroup ?: false,
                category = category,
                createdAt = existing?.createdAt
              )
            )
          }
        },
        enabled = name.isNotBlank()
      ) {
        Text("Save Group")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}
