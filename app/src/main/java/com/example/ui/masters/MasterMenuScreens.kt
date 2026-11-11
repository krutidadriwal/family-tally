package com.example.ui.masters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.MenuBook
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TallyBlueHeader

@Composable
fun CreateMasterScreen(
  onNavigateBack: () -> Unit,
  onCreateLedger: () -> Unit,
  onCreateGroup: () -> Unit
) {
  MasterMenuScaffold(
    title = "CREATE",
    subtitle = "Create a new master",
    onNavigateBack = onNavigateBack
  ) {
    MasterMenuRow("Ledger", "A new account under a group", Icons.Default.MenuBook, onCreateLedger)
    MasterMenuRow("Group", "A chart-of-accounts classification", Icons.Default.AccountTree, onCreateGroup)
  }
}

@Composable
fun AlterMasterScreen(
  onNavigateBack: () -> Unit,
  onAlterLedger: () -> Unit,
  onAlterGroup: () -> Unit
) {
  MasterMenuScaffold(
    title = "ALTER",
    subtitle = "Edit or delete an existing master",
    onNavigateBack = onNavigateBack
  ) {
    MasterMenuRow("Ledger", "Edit accounts (delete blocked when in use)", Icons.Default.MenuBook, onAlterLedger)
    MasterMenuRow("Group", "Edit groups (delete blocked when in use)", Icons.Default.AccountTree, onAlterGroup)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MasterMenuScaffold(
  title: String,
  subtitle: String,
  onNavigateBack: () -> Unit,
  content: @Composable () -> Unit
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = title,
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
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      content()
    }
  }
}

@Composable
private fun MasterMenuRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
  }
}
