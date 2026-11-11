package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.AccountingGroup
import com.example.data.model.AccountingGroups
import com.example.data.model.Budget
import com.example.data.model.FamilyMember
import com.example.data.model.Ledger
import com.example.data.model.RecurringVoucher
import com.example.data.model.Voucher
import com.example.data.model.VoucherEntry
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TallyRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance(db.app)
) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid ?: "dev_family_user"
  }

  // --- ACCOUNTING GROUPS ---

  private fun groupDocId(name: String): String {
    val slug = name.lowercase()
      .replace(Regex("[^a-z0-9]+"), "_")
      .trim('_')
      .take(100)
    return "grp_" + slug.ifBlank { UUID.randomUUID().toString().replace("-", "").take(12) }
  }

  fun observeGroups(): Flow<List<AccountingGroup>> = flow {
    val uid = requireUserId()
    val path = "users/$uid/groups"
    emitAll(
      db.collection("users").document(uid).collection("groups")
        .snapshots()
        .map { snapshot -> snapshot.toObjects(AccountingGroup::class.java) }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun saveGroup(group: AccountingGroup): Result<String> {
    val uid = requireUserId()
    val docId = group.id.ifBlank { groupDocId(group.name) }
    val docRef = db.collection("users").document(uid).collection("groups").document(docId)

    val payload = group.toMap().toMutableMap().apply {
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }

    return try {
      docRef.set(payload).await()
      Result.success(docId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteGroup(groupId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("groups").document(groupId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- LEDGERS ---

  fun observeLedgers(): Flow<List<Ledger>> = flow {
    val uid = requireUserId()
    val path = "users/$uid/ledgers"
    emitAll(
      db.collection("users").document(uid).collection("ledgers")
        .snapshots()
        .map { snapshot -> snapshot.toObjects(Ledger::class.java) }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun createLedger(ledger: Ledger, autoCreateOpeningVoucher: Boolean = true): Result<String> {
    val uid = requireUserId()
    val docId = ledger.id.ifBlank { "ledg_" + UUID.randomUUID().toString().replace("-", "").take(12) }
    val docRef = db.collection("users").document(uid).collection("ledgers").document(docId)

    val payload = ledger.toMap().toMutableMap().apply {
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }

    return try {
      docRef.set(payload).await()

      // Dynamic Double-Entry Offset against "Family Capital" for Opening Balances
      if (autoCreateOpeningVoucher && ledger.openingBalance > 0 && !ledger.name.equals("Family Capital", ignoreCase = true)) {
        createOpeningBalanceJournal(ledger.copy(id = docId))
      }

      Result.success(docId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  private suspend fun createOpeningBalanceJournal(ledger: Ledger) {
    try {
      val uid = requireUserId()
      val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
      val voucherNum = "OB-${System.currentTimeMillis().toString().takeLast(4)}"

      // Offset every opening balance against Family Capital so the books always
      // balance, whatever the account type:
      //   Dr-opening (assets, expenses) -> Dr ledger,  Cr Family Capital
      //   Cr-opening (liabilities, income, capital) -> Dr Family Capital, Cr ledger
      // This posts the "difference in opening balance" straight to capital, so the
      // Trial Balance and Balance Sheet always tally.
      val isDrOpening = ledger.openingBalanceType.equals("Dr", ignoreCase = true)
      val entries = if (isDrOpening) {
        listOf(
          VoucherEntry(ledgerId = ledger.id, ledgerName = ledger.name, type = "Dr", amount = ledger.openingBalance),
          VoucherEntry(ledgerId = "system_family_capital", ledgerName = "Family Capital", type = "Cr", amount = ledger.openingBalance)
        )
      } else {
        listOf(
          VoucherEntry(ledgerId = "system_family_capital", ledgerName = "Family Capital", type = "Dr", amount = ledger.openingBalance),
          VoucherEntry(ledgerId = ledger.id, ledgerName = ledger.name, type = "Cr", amount = ledger.openingBalance)
        )
      }

      val voucher = Voucher(
        voucherNumber = voucherNum,
        voucherType = "F7",
        date = dateStr,
        totalAmount = ledger.openingBalance,
        narration = "Opening Balance Offset for ${ledger.name} against Family Capital",
        entriesJson = VoucherEntry.listToJson(entries),
        familyMember = "System"
      )
      createVoucher(voucher)
    } catch (e: Exception) {
      // Non-critical journal generation
    }
  }

  suspend fun updateLedger(ledger: Ledger): Result<Unit> {
    val uid = requireUserId()
    require(ledger.id.isNotBlank()) { "Ledger ID must not be blank for update" }
    val docRef = db.collection("users").document(uid).collection("ledgers").document(ledger.id)

    val payload = ledger.toMap().toMutableMap().apply {
      put("updatedAt", FieldValue.serverTimestamp())
    }

    return try {
      docRef.update(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteLedger(ledgerId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("ledgers").document(ledgerId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- VOUCHERS ---

  fun observeVouchers(): Flow<List<Voucher>> = flow {
    val uid = requireUserId()
    val path = "users/$uid/vouchers"
    emitAll(
      db.collection("users").document(uid).collection("vouchers")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(Voucher::class.java).sortedByDescending { it.date }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun createVoucher(voucher: Voucher): Result<String> {
    val uid = requireUserId()
    val docId = voucher.id.ifBlank { "vch_" + UUID.randomUUID().toString().replace("-", "").take(12) }
    val docRef = db.collection("users").document(uid).collection("vouchers").document(docId)

    val payload = voucher.toMap().toMutableMap().apply {
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }

    return try {
      docRef.set(payload).await()
      Result.success(docId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateVoucher(voucher: Voucher): Result<Unit> {
    val uid = requireUserId()
    require(voucher.id.isNotBlank()) { "Voucher ID must not be blank" }
    val docRef = db.collection("users").document(uid).collection("vouchers").document(voucher.id)

    val payload = voucher.toMap().toMutableMap().apply {
      put("updatedAt", FieldValue.serverTimestamp())
    }

    return try {
      docRef.update(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteVoucher(voucherId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("vouchers").document(voucherId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- BUDGETS ---

  fun observeBudgets(): Flow<List<Budget>> = flow {
    val uid = requireUserId()
    val path = "users/$uid/budgets"
    emitAll(
      db.collection("users").document(uid).collection("budgets")
        .snapshots()
        .map { snapshot -> snapshot.toObjects(Budget::class.java) }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun saveBudget(budget: Budget): Result<String> {
    val uid = requireUserId()
    val docId = budget.id.ifBlank { "bgt_" + UUID.randomUUID().toString().replace("-", "").take(12) }
    val docRef = db.collection("users").document(uid).collection("budgets").document(docId)

    val payload = budget.toMap().toMutableMap().apply {
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }

    return try {
      docRef.set(payload).await()
      Result.success(docId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteBudget(budgetId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("budgets").document(budgetId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- RECURRING VOUCHERS ---

  fun observeRecurring(): Flow<List<RecurringVoucher>> = flow {
    val uid = requireUserId()
    val path = "users/$uid/recurring"
    emitAll(
      db.collection("users").document(uid).collection("recurring")
        .snapshots()
        .map { snapshot -> snapshot.toObjects(RecurringVoucher::class.java) }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun saveRecurring(recurring: RecurringVoucher): Result<String> {
    val uid = requireUserId()
    val docId = recurring.id.ifBlank { "rec_" + UUID.randomUUID().toString().replace("-", "").take(12) }
    val docRef = db.collection("users").document(uid).collection("recurring").document(docId)

    val payload = recurring.toMap().toMutableMap().apply {
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }

    return try {
      docRef.set(payload).await()
      Result.success(docId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteRecurring(recurringId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("recurring").document(recurringId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- FAMILY MEMBERS ---

  fun observeFamilyMembers(): Flow<List<FamilyMember>> = flow {
    val uid = requireUserId()
    val path = "users/$uid/family_members"
    emitAll(
      db.collection("users").document(uid).collection("family_members")
        .snapshots()
        .map { snapshot -> snapshot.toObjects(FamilyMember::class.java) }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun saveFamilyMember(member: FamilyMember): Result<String> {
    val uid = requireUserId()
    val docId = member.id.ifBlank { "mem_" + UUID.randomUUID().toString().replace("-", "").take(12) }
    val docRef = db.collection("users").document(uid).collection("family_members").document(docId)

    val payload = member.toMap().toMutableMap().apply {
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }

    return try {
      docRef.set(payload).await()
      Result.success(docId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteFamilyMember(memberId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("family_members").document(memberId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- SEED ENHANCED TALLY ARCHITECTURE & FAMILY STARTER PACK ---
  suspend fun seedStarterPackIfEmpty() {
    val uid = requireUserId()
    val existing = db.collection("users").document(uid).collection("ledgers").limit(1).get().await()
    if (!existing.isEmpty) return

    // 0. Seed the chart-of-accounts groups from the default definitions
    AccountingGroups.GROUP_DEFINITIONS.forEach { def ->
      saveGroup(
        AccountingGroup(
          id = groupDocId(def.name),
          name = def.name,
          parentGroup = def.parentGroup,
          isSystemGroup = def.isSystemGroup,
          category = def.category
        )
      )
    }

    // 1. Foundational Capital Ledger
    createLedger(
      Ledger(
        id = "system_family_capital",
        name = "Family Capital",
        group = AccountingGroups.CAPITAL_ACCOUNT,
        openingBalance = 0.0,
        openingBalanceType = "Cr",
        isSystemLedger = true,
        notes = "Master offset equity ledger representing initial family net worth"
      ),
      autoCreateOpeningVoucher = false
    )

    // 2. Wallets (Cash-in-Hand sub-group)
    val wallets = listOf(
      Ledger(name = "Husband's Wallet", group = AccountingGroups.CASH_IN_HAND, subGroup = "Physical Wallets", openingBalance = 8000.0, openingBalanceType = "Dr"),
      Ledger(name = "Spouse's Purse", group = AccountingGroups.CASH_IN_HAND, subGroup = "Physical Wallets", openingBalance = 6000.0, openingBalanceType = "Dr"),
      Ledger(name = "Home Safe Cash", group = AccountingGroups.CASH_IN_HAND, subGroup = "Physical Wallets", openingBalance = 25000.0, openingBalanceType = "Dr"),
      Ledger(name = "Emergency Cash Envelope", group = AccountingGroups.CASH_IN_HAND, subGroup = "Physical Wallets", openingBalance = 10000.0, openingBalanceType = "Dr")
    )
    wallets.forEach { createLedger(it) }

    // 3. Bank Accounts (Bank Accounts sub-group)
    val banks = listOf(
      Ledger(name = "HDFC Salary Account", group = AccountingGroups.BANK_ACCOUNTS, openingBalance = 145000.0, openingBalanceType = "Dr"),
      Ledger(name = "SBI Joint Family Account", group = AccountingGroups.BANK_ACCOUNTS, openingBalance = 75000.0, openingBalanceType = "Dr"),
      Ledger(name = "ICICI Savings Account", group = AccountingGroups.BANK_ACCOUNTS, openingBalance = 30000.0, openingBalanceType = "Dr")
    )
    banks.forEach { createLedger(it) }

    // 4. Investments (Investments sub-group)
    val investments = listOf(
      Ledger(name = "HDFC Fixed Deposit", group = AccountingGroups.INVESTMENTS, subGroup = "Term Deposits", openingBalance = 200000.0, openingBalanceType = "Dr", marketValue = 206000.0),
      Ledger(name = "SBI Small Cap MF (SIP)", group = AccountingGroups.INVESTMENTS, subGroup = "Mutual Funds", openingBalance = 120000.0, openingBalanceType = "Dr", marketValue = 138500.0),
      Ledger(name = "Reliance Shares (Zerodha)", group = AccountingGroups.INVESTMENTS, subGroup = "Direct Equity", openingBalance = 95000.0, openingBalanceType = "Dr", marketValue = 104200.0),
      Ledger(name = "Public Provident Fund (PPF)", group = AccountingGroups.INVESTMENTS, subGroup = "Retirement", openingBalance = 150000.0, openingBalanceType = "Dr", marketValue = 150000.0)
    )
    investments.forEach { createLedger(it) }

    // 5. Sundry Debtors
    createLedger(Ledger(name = "Loan to Brother", group = AccountingGroups.SUNDRY_DEBTORS, openingBalance = 15000.0, openingBalanceType = "Dr"))

    // 6. Fixed Assets
    val fixedAssets = listOf(
      Ledger(name = "Primary Residence Flat", group = AccountingGroups.FIXED_ASSETS, openingBalance = 6500000.0, openingBalanceType = "Dr"),
      Ledger(name = "Car (Family Vehicle)", group = AccountingGroups.FIXED_ASSETS, openingBalance = 850000.0, openingBalanceType = "Dr"),
      Ledger(name = "Gold Jewelry", group = AccountingGroups.FIXED_ASSETS, openingBalance = 420000.0, openingBalanceType = "Dr")
    )
    fixedAssets.forEach { createLedger(it) }

    // 7. Liabilities (Credit Cards & Loans)
    createLedger(Ledger(name = "HDFC Regalia Credit Card", group = AccountingGroups.CREDIT_CARDS, openingBalance = 18500.0, openingBalanceType = "Cr"))
    createLedger(Ledger(name = "Amazon Pay ICICI Card", group = AccountingGroups.CREDIT_CARDS, openingBalance = 6200.0, openingBalanceType = "Cr"))
    createLedger(Ledger(name = "SBI Home Loan", group = AccountingGroups.LOANS_LIABILITY, openingBalance = 2800000.0, openingBalanceType = "Cr"))

    // 8. Incomes
    createLedger(Ledger(name = "Husband's Salary", group = AccountingGroups.DIRECT_INCOMES, openingBalance = 0.0, openingBalanceType = "Cr"))
    createLedger(Ledger(name = "Spouse's Salary", group = AccountingGroups.DIRECT_INCOMES, openingBalance = 0.0, openingBalanceType = "Cr"))
    createLedger(Ledger(name = "Bank Interest & Dividends", group = AccountingGroups.INDIRECT_INCOMES, openingBalance = 0.0, openingBalanceType = "Cr"))

    // 9. Expenses (Essential + Discretionary)
    val expenses = listOf(
      Ledger(name = "Groceries & Supermarket", group = AccountingGroups.DIRECT_EXPENSES, openingBalance = 0.0, openingBalanceType = "Dr", budgetAmount = 20000.0),
      Ledger(name = "House Rent & Maintenance", group = AccountingGroups.DIRECT_EXPENSES, openingBalance = 0.0, openingBalanceType = "Dr", budgetAmount = 30000.0),
      Ledger(name = "Electricity & Utility Bills", group = AccountingGroups.DIRECT_EXPENSES, openingBalance = 0.0, openingBalanceType = "Dr", budgetAmount = 6000.0),
      Ledger(name = "School Fees & Kids Education", group = AccountingGroups.DIRECT_EXPENSES, openingBalance = 0.0, openingBalanceType = "Dr", budgetAmount = 15000.0),
      Ledger(name = "Fuel & Vehicle Commute", group = AccountingGroups.DIRECT_EXPENSES, openingBalance = 0.0, openingBalanceType = "Dr", budgetAmount = 8000.0),
      Ledger(name = "Dining Out & Food Delivery", group = AccountingGroups.INDIRECT_EXPENSES, openingBalance = 0.0, openingBalanceType = "Dr", budgetAmount = 10000.0),
      Ledger(name = "Movies & Entertainment", group = AccountingGroups.INDIRECT_EXPENSES, openingBalance = 0.0, openingBalanceType = "Dr", budgetAmount = 5000.0),
      Ledger(name = "Online Shopping & Apparel", group = AccountingGroups.INDIRECT_EXPENSES, openingBalance = 0.0, openingBalanceType = "Dr", budgetAmount = 8000.0),
      Ledger(name = "OTT & App Subscriptions", group = AccountingGroups.INDIRECT_EXPENSES, openingBalance = 0.0, openingBalanceType = "Dr", budgetAmount = 2000.0)
    )
    expenses.forEach { createLedger(it) }

    // Default members
    saveFamilyMember(FamilyMember(name = "Husband", role = "Admin", colorHex = "#1E88E5"))
    saveFamilyMember(FamilyMember(name = "Spouse", role = "Admin", colorHex = "#E91E63"))
    saveFamilyMember(FamilyMember(name = "Kids", role = "Member", colorHex = "#4CAF50"))

    // Default recurring template
    saveRecurring(
      RecurringVoucher(
        title = "House Rent",
        voucherType = "F5",
        amount = 30000.0,
        debitLedgerId = "",
        debitLedgerName = "House Rent & Maintenance",
        creditLedgerId = "",
        creditLedgerName = "HDFC Salary Account",
        frequency = "MONTHLY",
        dayOfMonth = 1L,
        isActive = true,
        narration = "Monthly House Rent Transfer"
      )
    )
  }
}
