package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.VoucherType
import com.example.data.repository.TallyRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.dashboard.GatewayDashboardScreen
import com.example.ui.masters.AlterMasterScreen
import com.example.ui.masters.CreateMasterScreen
import com.example.ui.masters.FamilyMembersScreen
import com.example.ui.masters.GroupsScreen
import com.example.ui.masters.LedgersScreen
import com.example.ui.reports.BalanceSheetScreen
import com.example.ui.reports.BankBookScreen
import com.example.ui.reports.CashBankSummaryScreen
import com.example.ui.reports.CashBookScreen
import com.example.ui.reports.DaybookScreen
import com.example.ui.reports.InvestmentPortfolioScreen
import com.example.ui.reports.LedgerVouchersScreen
import com.example.ui.reports.ProfitAndLossScreen
import com.example.ui.reports.TrialBalanceScreen
import com.example.ui.reports.VisualAnalyticsScreen
import com.example.ui.theme.FamilyTallyTheme
import com.example.ui.utilities.ExpenseSplitterScreen
import com.example.ui.utilities.RecurringVouchersScreen
import com.example.ui.viewmodel.TallyViewModel
import com.example.ui.vouchers.VoucherEntryScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

enum class Screen {
  GATEWAY,
  VOUCHER_ENTRY,
  CREATE_MASTER,
  ALTER_MASTER,
  LEDGERS,
  GROUPS,
  FAMILY_MEMBERS,
  DAYBOOK,
  LEDGER_VOUCHERS,
  PROFIT_AND_LOSS,
  CASH_BANK,
  CASH_BOOK,
  BANK_BOOK,
  INVESTMENTS,
  BALANCE_SHEET,
  TRIAL_BALANCE,
  RECURRING,
  EXPENSE_SPLITTER,
  ANALYTICS
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // In debug builds, route Auth + Firestore to the local Firebase Emulator Suite
    // (firebase.json: auth 9099, firestore 8085) reached via the Android emulator's
    // host loopback 10.0.2.2. This lets anonymous sign-in and the real firestore.rules
    // run locally with no cloud-console configuration. Must happen before any other
    // use of these Firebase instances.
    // Opt-in only: set USE_FIREBASE_EMULATOR=true in .env. Otherwise debug builds
    // (e.g. `gradlew installDebug`) talk to the real Firebase project.
    if (BuildConfig.DEBUG &&
      BuildConfig.USE_FIREBASE_EMULATOR.equals("true", ignoreCase = true)
    ) {
      connectFirebaseEmulators()
    }

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
    )
    setContent {
      FamilyTallyTheme {
        AppContent()
      }
    }
  }

  private fun connectFirebaseEmulators() {
    // Android emulator reaches the dev machine via 10.0.2.2. A physical device
    // uses 127.0.0.1, which requires port forwarding:
    //   adb reverse tcp:9099 tcp:9099 && adb reverse tcp:8085 tcp:8085
    val host = if (isRunningOnEmulator()) "10.0.2.2" else "127.0.0.1"
    try {
      Firebase.auth.useEmulator(host, 9099)
    } catch (e: IllegalStateException) {
      // Already configured (e.g. activity recreated) — safe to ignore.
    }
    try {
      FirebaseFirestore.getInstance(getString(R.string.firestore_database_id))
        .useEmulator(host, 8085)
    } catch (e: IllegalStateException) {
      // Already configured or already in use — safe to ignore.
    }
  }

  private fun isRunningOnEmulator(): Boolean {
    val b = android.os.Build.FINGERPRINT + android.os.Build.MODEL + android.os.Build.HARDWARE
    return b.contains("generic") || b.contains("emulator") || b.contains("sdk_gphone") ||
      b.contains("goldfish") || b.contains("ranchu")
  }
}

@Composable
fun AppContent() {
  // Development-only login bypass, toggled by the DEV_SKIP_LOGIN env var (.env /
  // .env.example), surfaced here as BuildConfig.DEV_SKIP_LOGIN by the Secrets
  // Gradle Plugin. When true, the app signs in anonymously and skips the sign-in
  // screen. Set it to "false" (or remove it) before release to require real login.
  val devSkipLogin = BuildConfig.DEV_SKIP_LOGIN.equals("true", ignoreCase = true)

  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
  var authInProgress by remember { mutableStateOf(devSkipLogin && Firebase.auth.currentUser == null) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  // In the dev bypass, sign in anonymously so Firestore rules (request.auth != null)
  // are satisfied without a manual login. If it fails, the AuthScreen still shows.
  // With DEV_SKIP_LOGIN off, no auto sign-in happens and the login screen appears.
  LaunchedEffect(Unit) {
    if (devSkipLogin && Firebase.auth.currentUser == null) {
      try {
        Firebase.auth.signInAnonymously().await()
      } catch (e: Exception) {
        // Anonymous auth disabled or offline — the AuthScreen fallback handles it.
      }
    }
    authInProgress = false
  }

  val user = currentUser
  when {
    user != null -> key(user.uid) {
      MainTallyApp(currentUserId = user.uid)
    }
    authInProgress -> Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator()
    }
    else -> AuthScreen(onAuthSuccess = { /* AuthStateListener updates currentUser */ })
  }
}

@Composable
fun MainTallyApp(currentUserId: String) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  val repository = remember { TallyRepository(context) }
  val viewModel: TallyViewModel = viewModel(
    key = currentUserId,
    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return TallyViewModel(repository) as T
      }
    }
  )

  var currentScreen by remember { mutableStateOf(Screen.GATEWAY) }
  var voucherTypeForEntry by remember { mutableStateOf(VoucherType.PAYMENT) }
  var statementLedgerId by remember { mutableStateOf<String?>(null) }
  var ledgersStartCreate by remember { mutableStateOf(false) }
  var groupsStartCreate by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  val errorMsg by viewModel.errorMessage.collectAsState()
  val successMsg by viewModel.successMessage.collectAsState()

  LaunchedEffect(errorMsg) {
    errorMsg?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearMessages()
    }
  }

  LaunchedEffect(successMsg) {
    successMsg?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearMessages()
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = Modifier.fillMaxSize()
  ) { innerPadding ->
    Box(modifier = Modifier.padding(innerPadding)) {
      when (currentScreen) {
        Screen.GATEWAY -> {
          GatewayDashboardScreen(
            viewModel = viewModel,
            onNavigateToVoucher = { type ->
              voucherTypeForEntry = type
              currentScreen = Screen.VOUCHER_ENTRY
            },
            onNavigateToLedgers = {
              ledgersStartCreate = false
              currentScreen = Screen.LEDGERS
            },
            onNavigateToCreate = { currentScreen = Screen.CREATE_MASTER },
            onNavigateToAlter = { currentScreen = Screen.ALTER_MASTER },
            onNavigateToFamilyMembers = { currentScreen = Screen.FAMILY_MEMBERS },
            onNavigateToDaybook = { currentScreen = Screen.DAYBOOK },
            onNavigateToLedgerVouchers = {
              statementLedgerId = null
              currentScreen = Screen.LEDGER_VOUCHERS
            },
            onNavigateToProfitAndLoss = { currentScreen = Screen.PROFIT_AND_LOSS },
            onNavigateToCashBook = { currentScreen = Screen.CASH_BOOK },
            onNavigateToBankBook = { currentScreen = Screen.BANK_BOOK },
            onNavigateToInvestments = { currentScreen = Screen.INVESTMENTS },
            onNavigateToBalanceSheet = { currentScreen = Screen.BALANCE_SHEET },
            onNavigateToTrialBalance = { currentScreen = Screen.TRIAL_BALANCE },
            onNavigateToRecurring = { currentScreen = Screen.RECURRING },
            onNavigateToSplitter = { currentScreen = Screen.EXPENSE_SPLITTER },
            onNavigateToAnalytics = { currentScreen = Screen.ANALYTICS },
            onSignOut = {
              signOut(credentialManager, coroutineScope)
            }
          )
        }

        Screen.VOUCHER_ENTRY -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          VoucherEntryScreen(
            initialType = voucherTypeForEntry,
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY }
          )
        }

        Screen.CREATE_MASTER -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          CreateMasterScreen(
            onNavigateBack = { currentScreen = Screen.GATEWAY },
            onCreateLedger = { ledgersStartCreate = true; currentScreen = Screen.LEDGERS },
            onCreateGroup = { groupsStartCreate = true; currentScreen = Screen.GROUPS }
          )
        }

        Screen.ALTER_MASTER -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          AlterMasterScreen(
            onNavigateBack = { currentScreen = Screen.GATEWAY },
            onAlterLedger = { ledgersStartCreate = false; currentScreen = Screen.LEDGERS },
            onAlterGroup = { groupsStartCreate = false; currentScreen = Screen.GROUPS }
          )
        }

        Screen.LEDGERS -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          LedgersScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY },
            onSelectLedgerStatement = { id ->
              statementLedgerId = id
              currentScreen = Screen.LEDGER_VOUCHERS
            },
            startInCreateMode = ledgersStartCreate
          )
        }

        Screen.GROUPS -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          GroupsScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY },
            startInCreateMode = groupsStartCreate
          )
        }

        Screen.FAMILY_MEMBERS -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          FamilyMembersScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY }
          )
        }

        Screen.DAYBOOK -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          DaybookScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY }
          )
        }

        Screen.LEDGER_VOUCHERS -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          LedgerVouchersScreen(
            initialLedgerId = statementLedgerId,
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY }
          )
        }

        Screen.PROFIT_AND_LOSS -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          ProfitAndLossScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY }
          )
        }

        Screen.CASH_BANK -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          CashBankSummaryScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY },
            onNavigateToContra = {
              voucherTypeForEntry = VoucherType.CONTRA
              currentScreen = Screen.VOUCHER_ENTRY
            }
          )
        }

        Screen.CASH_BOOK -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          CashBookScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY },
            onNavigateToContra = {
              voucherTypeForEntry = VoucherType.CONTRA
              currentScreen = Screen.VOUCHER_ENTRY
            },
            onSelectLedgerStatement = { id ->
              statementLedgerId = id
              currentScreen = Screen.LEDGER_VOUCHERS
            }
          )
        }

        Screen.BANK_BOOK -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          BankBookScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY },
            onNavigateToContra = {
              voucherTypeForEntry = VoucherType.CONTRA
              currentScreen = Screen.VOUCHER_ENTRY
            },
            onSelectLedgerStatement = { id ->
              statementLedgerId = id
              currentScreen = Screen.LEDGER_VOUCHERS
            }
          )
        }

        Screen.INVESTMENTS -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          InvestmentPortfolioScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY },
            onRecordNewInvestment = {
              voucherTypeForEntry = VoucherType.PAYMENT
              currentScreen = Screen.VOUCHER_ENTRY
            }
          )
        }

        Screen.BALANCE_SHEET -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          BalanceSheetScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY }
          )
        }

        Screen.TRIAL_BALANCE -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          TrialBalanceScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY }
          )
        }

        Screen.RECURRING -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          RecurringVouchersScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY }
          )
        }

        Screen.EXPENSE_SPLITTER -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          ExpenseSplitterScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY }
          )
        }

        Screen.ANALYTICS -> {
          BackHandler { currentScreen = Screen.GATEWAY }
          VisualAnalyticsScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = Screen.GATEWAY }
          )
        }
      }
    }
  }
}
