package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccountingGroup
import com.example.data.model.AccountingGroups
import com.example.data.model.Budget
import com.example.data.model.FamilyMember
import com.example.data.model.Ledger
import com.example.data.model.RecurringVoucher
import com.example.data.model.Voucher
import com.example.data.model.VoucherEntry
import com.example.data.repository.TallyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LedgerBalance(
  val ledger: Ledger,
  val debitTotal: Double,
  val creditTotal: Double,
  val closingBalance: Double,
  val closingType: String // "Dr" or "Cr"
)

data class BudgetStatus(
  val ledger: Ledger,
  val budgetLimit: Double,
  val currentSpent: Double,
  val percentageUsed: Double,
  val isExceeded: Boolean
)

data class NetWorthSummary(
  val totalAssets: Double,
  val totalLiabilities: Double,
  val netWorth: Double,
  val totalCash: Double,
  val totalBank: Double,
  val totalInvestments: Double,
  val totalFixedAssets: Double
)

class TallyViewModel(private val repository: TallyRepository) : ViewModel() {

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  private val _successMessage = MutableStateFlow<String?>(null)
  val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

  val ledgers: StateFlow<List<Ledger>> = repository.observeLedgers()
    .catch { e -> _errorMessage.value = "Failed to load ledgers: ${e.message}" }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val vouchers: StateFlow<List<Voucher>> = repository.observeVouchers()
    .catch { e -> _errorMessage.value = "Failed to load vouchers: ${e.message}" }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val budgets: StateFlow<List<Budget>> = repository.observeBudgets()
    .catch { e -> _errorMessage.value = "Failed to load budgets: ${e.message}" }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val recurring: StateFlow<List<RecurringVoucher>> = repository.observeRecurring()
    .catch { e -> _errorMessage.value = "Failed to load recurring vouchers: ${e.message}" }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val familyMembers: StateFlow<List<FamilyMember>> = repository.observeFamilyMembers()
    .catch { e -> _errorMessage.value = "Failed to load family members: ${e.message}" }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val groups: StateFlow<List<AccountingGroup>> = repository.observeGroups()
    .catch { e -> _errorMessage.value = "Failed to load groups: ${e.message}" }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Usage maps drive "cannot delete while in use" protection in the Alter screens.
  // A ledger is in use if any real (non opening-balance) voucher, budget, or recurring
  // template references it; a group is in use if any ledger belongs to it.
  val ledgerUsage: StateFlow<Map<String, Int>> =
    combine(ledgers, vouchers, budgets, recurring) { ls, vs, bs, rs ->
      ls.associate { l ->
        val vUse = vs.count { v ->
          !v.voucherNumber.startsWith("OB-") &&
            v.entries.any { it.ledgerId == l.id || it.ledgerName.equals(l.name, ignoreCase = true) }
        }
        val bUse = bs.count { it.ledgerId == l.id || it.ledgerName.equals(l.name, ignoreCase = true) }
        val rUse = rs.count {
          it.debitLedgerId == l.id || it.creditLedgerId == l.id ||
            it.debitLedgerName.equals(l.name, ignoreCase = true) ||
            it.creditLedgerName.equals(l.name, ignoreCase = true)
        }
        l.id to (vUse + bUse + rUse)
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

  val groupUsage: StateFlow<Map<String, Int>> =
    combine(groups, ledgers) { gs, ls ->
      gs.associate { g -> g.name to ls.count { it.group == g.name || it.subGroup == g.name } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

  // Computed Ledger Balances (Opening + Debits - Credits according to double-entry accounting)
  val ledgerBalances: StateFlow<Map<String, LedgerBalance>> = combine(ledgers, vouchers) { lList, vList ->
    val debitMap = mutableMapOf<String, Double>()
    val creditMap = mutableMapOf<String, Double>()

    for (v in vList) {
      for (entry in v.entries) {
        val lId = entry.ledgerId.ifBlank {
          lList.find { it.name.equals(entry.ledgerName, ignoreCase = true) }?.id ?: ""
        }
        if (lId.isNotBlank()) {
          if (entry.type.equals("Dr", ignoreCase = true)) {
            debitMap[lId] = (debitMap[lId] ?: 0.0) + entry.amount
          } else {
            creditMap[lId] = (creditMap[lId] ?: 0.0) + entry.amount
          }
        }
      }
    }

    val result = mutableMapOf<String, LedgerBalance>()
    for (ledger in lList) {
      val drSum = debitMap[ledger.id] ?: 0.0
      val crSum = creditMap[ledger.id] ?: 0.0

      val isAssetOrExpense = AccountingGroups.isAsset(ledger.group) || AccountingGroups.isExpense(ledger.group)

      // Opening balances are posted as an Opening-Balance journal that offsets
      // against Family Capital (see TallyRepository.createOpeningBalanceJournal),
      // so they already appear in drSum/crSum. Counting ledger.openingBalance again
      // here would double-count them and unbalance the Balance Sheet.
      val totalDr = drSum
      val totalCr = crSum

      val netDiff = if (isAssetOrExpense) (totalDr - totalCr) else (totalCr - totalDr)
      val closingType = if (netDiff >= 0) {
        if (isAssetOrExpense) "Dr" else "Cr"
      } else {
        if (isAssetOrExpense) "Cr" else "Dr"
      }

      result[ledger.id] = LedgerBalance(
        ledger = ledger,
        debitTotal = totalDr,
        creditTotal = totalCr,
        closingBalance = kotlin.math.abs(netDiff),
        closingType = closingType
      )
    }
    result
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

  // Net Worth & Balance Sheet summary
  val netWorthSummary: StateFlow<NetWorthSummary> = ledgerBalances.combine(ledgers) { balances, _ ->
    var assets = 0.0
    var liabilities = 0.0
    var cash = 0.0
    var bank = 0.0
    var investments = 0.0
    var fixedAssets = 0.0

    balances.values.forEach { bal ->
      val amt = if (bal.closingType == "Dr") bal.closingBalance else -bal.closingBalance
      when (bal.ledger.group) {
        AccountingGroups.CASH_IN_HAND -> {
          cash += bal.closingBalance
          assets += bal.closingBalance
        }
        AccountingGroups.BANK_ACCOUNTS -> {
          bank += bal.closingBalance
          assets += bal.closingBalance
        }
        AccountingGroups.INVESTMENTS -> {
          val valAmt = bal.ledger.marketValue ?: bal.closingBalance
          investments += valAmt
          assets += valAmt
        }
        AccountingGroups.FIXED_ASSETS -> {
          fixedAssets += bal.closingBalance
          assets += bal.closingBalance
        }
        AccountingGroups.SUNDRY_DEBTORS -> {
          assets += bal.closingBalance
        }
        AccountingGroups.CREDIT_CARDS, AccountingGroups.LOANS_LIABILITY -> {
          liabilities += bal.closingBalance
        }
      }
    }

    NetWorthSummary(
      totalAssets = assets,
      totalLiabilities = liabilities,
      netWorth = assets - liabilities,
      totalCash = cash,
      totalBank = bank,
      totalInvestments = investments,
      totalFixedAssets = fixedAssets
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetWorthSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))

  val currentMonth: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

  // Budget Status & Variance
  val budgetStatuses: StateFlow<List<BudgetStatus>> = combine(ledgers, vouchers, budgets) { lList, vList, bList ->
    val currentMonthVouchers = vList.filter { it.date.startsWith(currentMonth) }
    val expenseMap = mutableMapOf<String, Double>()

    for (v in currentMonthVouchers) {
      for (entry in v.entries) {
        if (entry.type.equals("Dr", ignoreCase = true)) {
          val l = lList.find { it.id == entry.ledgerId || it.name.equals(entry.ledgerName, ignoreCase = true) }
          if (l != null && AccountingGroups.isExpense(l.group)) {
            expenseMap[l.id] = (expenseMap[l.id] ?: 0.0) + entry.amount
          }
        }
      }
    }

    lList.filter { (it.budgetAmount != null && it.budgetAmount > 0) || bList.any { b -> b.ledgerId == it.id } }
      .map { ledger ->
        val explicitBudget = bList.find { it.ledgerId == ledger.id && it.month == currentMonth }?.monthlyLimit
        val limit = explicitBudget ?: (ledger.budgetAmount ?: 0.0)
        val spent = expenseMap[ledger.id] ?: 0.0
        val percentage = if (limit > 0) (spent / limit) * 100.0 else 0.0
        BudgetStatus(
          ledger = ledger,
          budgetLimit = limit,
          currentSpent = spent,
          percentageUsed = percentage,
          isExceeded = spent > limit
        )
      }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  init {
    viewModelScope.launch {
      try {
        repository.seedStarterPackIfEmpty()
      } catch (e: Exception) {
        // Handled silently
      }
    }
  }

  fun clearMessages() {
    _errorMessage.value = null
    _successMessage.value = null
  }

  /** Surface a one-off message to the user (e.g. "cannot delete while in use"). */
  fun flashMessage(message: String) {
    _errorMessage.value = message
  }

  // --- CRUD ACTIONS ---

  fun createLedger(ledger: Ledger, onComplete: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val res = repository.createLedger(ledger)
      if (res.isSuccess) {
        _successMessage.value = "Ledger '${ledger.name}' created with opening balance offset"
        onComplete(true)
      } else {
        _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to create ledger"
        onComplete(false)
      }
    }
  }

  fun updateLedger(ledger: Ledger, onComplete: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val res = repository.updateLedger(ledger)
      if (res.isSuccess) {
        _successMessage.value = "Ledger '${ledger.name}' updated"
        onComplete(true)
      } else {
        _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update ledger"
        onComplete(false)
      }
    }
  }

  fun deleteLedger(ledgerId: String) {
    viewModelScope.launch {
      val res = repository.deleteLedger(ledgerId)
      if (res.isSuccess) {
        _successMessage.value = "Ledger deleted"
      } else {
        _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete ledger"
      }
    }
  }

  // --- GROUPS ---

  fun saveGroup(group: AccountingGroup, onComplete: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val res = repository.saveGroup(group)
      if (res.isSuccess) {
        _successMessage.value = "Group '${group.name}' saved"
        onComplete(true)
      } else {
        _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save group"
        onComplete(false)
      }
    }
  }

  fun deleteGroup(groupId: String) {
    viewModelScope.launch {
      val res = repository.deleteGroup(groupId)
      if (res.isSuccess) {
        _successMessage.value = "Group deleted"
      } else {
        _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete group"
      }
    }
  }

  fun createVoucher(voucher: Voucher, onComplete: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val res = repository.createVoucher(voucher)
      if (res.isSuccess) {
        _successMessage.value = "${voucher.voucherType} Voucher #${voucher.voucherNumber} recorded"
        onComplete(true)
      } else {
        _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save voucher"
        onComplete(false)
      }
    }
  }

  fun deleteVoucher(voucherId: String) {
    viewModelScope.launch {
      val res = repository.deleteVoucher(voucherId)
      if (res.isSuccess) {
        _successMessage.value = "Voucher deleted"
      } else {
        _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete voucher"
      }
    }
  }

  fun saveBudget(budget: Budget, onComplete: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val res = repository.saveBudget(budget)
      if (res.isSuccess) {
        _successMessage.value = "Budget saved for ${budget.ledgerName}"
        onComplete(true)
      } else {
        _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save budget"
        onComplete(false)
      }
    }
  }

  fun saveRecurring(recurring: RecurringVoucher, onComplete: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val res = repository.saveRecurring(recurring)
      if (res.isSuccess) {
        _successMessage.value = "Recurring voucher '${recurring.title}' saved"
        onComplete(true)
      } else {
        _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save recurring voucher"
        onComplete(false)
      }
    }
  }

  fun deleteRecurring(recurringId: String) {
    viewModelScope.launch {
      val res = repository.deleteRecurring(recurringId)
      if (res.isSuccess) {
        _successMessage.value = "Recurring voucher deleted"
      } else {
        _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete recurring voucher"
      }
    }
  }

  fun postRecurringNow(rec: RecurringVoucher) {
    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val num = "REC-${System.currentTimeMillis().toString().takeLast(4)}"
    val entries = listOf(
      VoucherEntry(ledgerId = rec.debitLedgerId, ledgerName = rec.debitLedgerName, type = "Dr", amount = rec.amount),
      VoucherEntry(ledgerId = rec.creditLedgerId, ledgerName = rec.creditLedgerName, type = "Cr", amount = rec.amount)
    )
    val vch = Voucher(
      voucherNumber = num,
      voucherType = rec.voucherType,
      date = dateStr,
      totalAmount = rec.amount,
      narration = rec.narration ?: "Auto-posted recurring entry: ${rec.title}",
      entriesJson = VoucherEntry.listToJson(entries)
    )
    createVoucher(vch)
  }

  fun saveFamilyMember(member: FamilyMember, onComplete: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val res = repository.saveFamilyMember(member)
      if (res.isSuccess) {
        _successMessage.value = "Family member '${member.name}' saved"
        onComplete(true)
      } else {
        _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save member"
        onComplete(false)
      }
    }
  }

  fun deleteFamilyMember(memberId: String) {
    viewModelScope.launch {
      val res = repository.deleteFamilyMember(memberId)
      if (res.isSuccess) {
        _successMessage.value = "Member removed"
      } else {
        _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to remove member"
      }
    }
  }
}
