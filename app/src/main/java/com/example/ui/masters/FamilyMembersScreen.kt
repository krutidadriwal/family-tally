package com.example.ui.masters

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import com.example.ui.components.FullScreenDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import com.example.data.model.FamilyMember
import com.example.ui.theme.TallyBlueHeader
import com.example.ui.viewmodel.TallyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMembersScreen(
  viewModel: TallyViewModel,
  onNavigateBack: () -> Unit
) {
  val familyMembers by viewModel.familyMembers.collectAsState()
  var showDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "FAMILY MEMBERS & ROLES",
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
        onClick = { showDialog = true },
        containerColor = TallyBlueHeader,
        contentColor = Color.White,
        modifier = Modifier.testTag("fab_add_member")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Member")
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item { Spacer(modifier = Modifier.height(8.dp)) }

      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Role-based family access: 'Admins' can create/alter ledgers and view P&L reports, while 'Members' can enter daily expense vouchers.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      items(familyMembers, key = { it.id }) { member ->
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
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(android.graphics.Color.parseColor(member.colorHex.ifBlank { "#1E88E5" }))),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = member.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
              )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = member.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              if (!member.email.isNullOrBlank()) {
                Text(
                  text = member.email,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Surface(
              color = if (member.role == "Admin") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = member.role,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (member.role == "Admin") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            IconButton(onClick = { viewModel.deleteFamilyMember(member.id) }) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(60.dp)) }
    }
  }

  if (showDialog) {
    MemberDialog(
      onDismiss = { showDialog = false },
      onSave = {
        viewModel.saveFamilyMember(it)
        showDialog = false
      }
    )
  }
}

@Composable
private fun MemberDialog(
  onDismiss: () -> Unit,
  onSave: (FamilyMember) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var role by remember { mutableStateOf("Member") }
  val colors = listOf("#1E88E5", "#E91E63", "#4CAF50", "#FF9800", "#9C27B0")
  var selectedColor by remember { mutableStateOf(colors[0]) }

  FullScreenDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Family Member") },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Display Name (e.g. Papa, Mummy, Self)") },
          modifier = Modifier.fillMaxWidth().testTag("member_name_input"),
          singleLine = true
        )

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Google Account Email (Optional)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Text("Permission Role:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = role == "Admin",
            onClick = { role = "Admin" },
            label = { Text("Admin (Full Masters & Reports)") }
          )
          FilterChip(
            selected = role == "Member",
            onClick = { role = "Member" },
            label = { Text("Member (Vouchers only)") }
          )
        }

        Text("Avatar Color Tag:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          colors.forEach { cHex ->
            val isSelected = selectedColor == cHex
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(android.graphics.Color.parseColor(cHex)))
                .padding(2.dp)
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            onSave(
              FamilyMember(
                name = name.trim(),
                role = role,
                email = email.ifBlank { null },
                colorHex = selectedColor
              )
            )
          }
        },
        enabled = name.isNotBlank()
      ) {
        Text("Save Member")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
