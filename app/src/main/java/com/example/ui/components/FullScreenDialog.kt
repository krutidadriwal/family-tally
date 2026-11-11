package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Full-screen replacement for AlertDialog. Same slot names (title / text /
 * confirmButton / dismissButton) so call sites read like a dialog, but it
 * renders as a full screen page with a top app bar and a back arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenDialog(
  onDismissRequest: () -> Unit,
  title: @Composable () -> Unit,
  text: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  headerColor: Color = MaterialTheme.colorScheme.primary,
  headerContentColor: Color = Color.White,
  confirmButton: (@Composable () -> Unit)? = null,
  dismissButton: (@Composable () -> Unit)? = null,
) {
  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false,
    ),
  ) {
    Scaffold(
      modifier = modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        TopAppBar(
          title = title,
          navigationIcon = {
            IconButton(onClick = onDismissRequest) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = headerColor,
            titleContentColor = headerContentColor,
            navigationIconContentColor = headerContentColor,
            actionIconContentColor = headerContentColor,
          ),
        )
      },
      bottomBar = {
        if (confirmButton != null || dismissButton != null) {
          Surface(tonalElevation = 3.dp) {
            Column {
              HorizontalDivider()
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .navigationBarsPadding()
                  .imePadding()
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                dismissButton?.invoke()
                confirmButton?.invoke()
              }
            }
          }
        }
      },
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      ) {
        text()
      }
    }
  }
}
