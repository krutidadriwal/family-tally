package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject

data class AccountingGroupInfo(
  val name: String,
  val parentGroup: String? = null,
  val isSystemGroup: Boolean = true,
  val category: String // "Asset", "Liability", "Equity", "Income", "Expense"
)

object AccountingGroups {
  // Primary and Sub-Groups
  const val CASH_IN_HAND = "Cash-in-Hand"
  const val BANK_ACCOUNTS = "Bank Accounts"
  const val INVESTMENTS = "Investments"
  const val SUNDRY_DEBTORS = "Sundry Debtors / Money Owed to Us"
  const val FIXED_ASSETS = "Fixed Assets"

  const val CREDIT_CARDS = "Credit Cards"
  const val LOANS_LIABILITY = "Loans (Liability)"
  const val CAPITAL_ACCOUNT = "Capital Account"

  const val DIRECT_INCOMES = "Direct Incomes"
  const val INDIRECT_INCOMES = "Indirect Incomes"

  const val DIRECT_EXPENSES = "Direct Expenses"
  const val INDIRECT_EXPENSES = "Indirect Expenses"

  val GROUP_DEFINITIONS = listOf(
    AccountingGroupInfo(CASH_IN_HAND, "Current Assets", true, "Asset"),
    AccountingGroupInfo(BANK_ACCOUNTS, "Current Assets", true, "Asset"),
    AccountingGroupInfo(INVESTMENTS, "Assets", true, "Asset"),
    AccountingGroupInfo(SUNDRY_DEBTORS, "Current Assets", true, "Asset"),
    AccountingGroupInfo(FIXED_ASSETS, null, true, "Asset"),

    AccountingGroupInfo(CREDIT_CARDS, "Current Liabilities", true, "Liability"),
    AccountingGroupInfo(LOANS_LIABILITY, null, true, "Liability"),
    AccountingGroupInfo(CAPITAL_ACCOUNT, null, true, "Equity"),

    AccountingGroupInfo(DIRECT_INCOMES, null, true, "Income"),
    AccountingGroupInfo(INDIRECT_INCOMES, null, true, "Income"),

    AccountingGroupInfo(DIRECT_EXPENSES, null, true, "Expense"),
    AccountingGroupInfo(INDIRECT_EXPENSES, null, true, "Expense")
  )

  val ALL = GROUP_DEFINITIONS.map { it.name }

  fun isAsset(group: String) = group in listOf(CASH_IN_HAND, BANK_ACCOUNTS, INVESTMENTS, SUNDRY_DEBTORS, FIXED_ASSETS)
  fun isLiability(group: String) = group in listOf(CREDIT_CARDS, LOANS_LIABILITY)
  fun isEquity(group: String) = group == CAPITAL_ACCOUNT
  fun isIncome(group: String) = group in listOf(DIRECT_INCOMES, INDIRECT_INCOMES)
  fun isExpense(group: String) = group in listOf(DIRECT_EXPENSES, INDIRECT_EXPENSES)

  /** The natural Dr/Cr side for an accounting category. A ledger inherits this from its group. */
  fun natureOfCategory(category: String): String =
    if (category.equals("Asset", true) || category.equals("Expense", true)) "Dr" else "Cr"

  /** Category for a built-in group name, or null for a custom group (whose category is stored on the group). */
  fun categoryOf(group: String): String? = GROUP_DEFINITIONS.find { it.name == group }?.category

  /** The natural Dr/Cr side for a built-in group name (falls back to Dr). */
  fun natureOfGroup(group: String): String = categoryOf(group)?.let { natureOfCategory(it) } ?: "Dr"
}

@IgnoreExtraProperties
data class AccountingGroup(
  @DocumentId val id: String = "",
  val name: String = "",
  val parentGroup: String? = null,
  val isSystemGroup: Boolean? = true,
  val category: String = "Expense", // "Asset", "Liability", "Equity", "Income", "Expense"
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "parentGroup" to parentGroup,
    "isSystemGroup" to isSystemGroup,
    "category" to category
  ).filterValues { it != null }
}

enum class VoucherType(
  val code: String,
  val keyName: String,
  val title: String,
  val description: String,
  val colorHex: Long
) {
  PAYMENT("P5", "F5", "Payment", "Outgoing money / expense", 0xFFC62828),
  RECEIPT("R6", "F6", "Receipt", "Incoming money / salary", 0xFF1565C0),
  JOURNAL("J7", "F7", "Journal Entry", "Adjustment / split / opening", 0xFF6A1B9A),
  CONTRA("C4", "F4", "Contra", "Bank <-> Cash transfer", 0xFF00796B);

  companion object {
    fun fromCode(code: String): VoucherType = entries.find { it.code == code || it.keyName == code } ?: PAYMENT
  }
}

@IgnoreExtraProperties
data class Ledger(
  @DocumentId val id: String = "",
  val name: String = "",
  val group: String = AccountingGroups.INDIRECT_EXPENSES,
  val subGroup: String? = null,
  val openingBalance: Double = 0.0,
  val openingBalanceType: String = "Dr", // "Dr" or "Cr"
  val marketValue: Double? = null, // for investments (current valuation)
  val budgetAmount: Double? = null,
  val notes: String? = null,
  val isSystemLedger: Boolean? = false, // e.g. Family Capital
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "group" to group,
    "subGroup" to subGroup,
    "openingBalance" to openingBalance,
    "openingBalanceType" to openingBalanceType,
    "marketValue" to marketValue,
    "budgetAmount" to budgetAmount,
    "notes" to notes,
    "isSystemLedger" to isSystemLedger
  ).filterValues { it != null }
}

data class VoucherEntry(
  val ledgerId: String = "",
  val ledgerName: String = "",
  val type: String = "Dr", // "Dr" or "Cr"
  val amount: Double = 0.0
) {
  fun toJson(): JSONObject = JSONObject().apply {
    put("ledgerId", ledgerId)
    put("ledgerName", ledgerName)
    put("type", type)
    put("amount", amount)
  }

  companion object {
    fun fromJson(obj: JSONObject): VoucherEntry = VoucherEntry(
      ledgerId = obj.optString("ledgerId", ""),
      ledgerName = obj.optString("ledgerName", ""),
      type = obj.optString("type", "Dr"),
      amount = obj.optDouble("amount", 0.0)
    )

    fun listToJson(list: List<VoucherEntry>): String {
      val array = JSONArray()
      list.forEach { array.put(it.toJson()) }
      return array.toString()
    }

    fun listFromJson(jsonStr: String): List<VoucherEntry> {
      if (jsonStr.isBlank()) return emptyList()
      return try {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<VoucherEntry>()
        for (i in 0 until array.length()) {
          list.add(fromJson(array.getJSONObject(i)))
        }
        list
      } catch (e: Exception) {
        emptyList()
      }
    }
  }
}

@IgnoreExtraProperties
data class Voucher(
  @DocumentId val id: String = "",
  val voucherNumber: String = "",
  val voucherType: String = "F5",
  val date: String = "", // YYYY-MM-DD
  val totalAmount: Double = 0.0,
  val narration: String = "",
  val entriesJson: String = "",
  val paymentMode: String? = null, // "UPI", "CASH", "BANK", "CARD"
  val upiId: String? = null,
  val upiTransactionRef: String? = null,
  val familyMember: String? = null,
  val attachmentUri: String? = null,
  val isSplit: Boolean? = false,
  val splitDetails: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val entries: List<VoucherEntry> get() = VoucherEntry.listFromJson(entriesJson)

  fun toMap(): Map<String, Any?> = mapOf(
    "voucherNumber" to voucherNumber,
    "voucherType" to voucherType,
    "date" to date,
    "totalAmount" to totalAmount,
    "narration" to narration,
    "entriesJson" to entriesJson,
    "paymentMode" to paymentMode,
    "upiId" to upiId,
    "upiTransactionRef" to upiTransactionRef,
    "familyMember" to familyMember,
    "attachmentUri" to attachmentUri,
    "isSplit" to isSplit,
    "splitDetails" to splitDetails
  ).filterValues { it != null }
}

@IgnoreExtraProperties
data class Budget(
  @DocumentId val id: String = "",
  val ledgerId: String = "",
  val ledgerName: String = "",
  val monthlyLimit: Double = 0.0,
  val month: String = "", // YYYY-MM
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toMap(): Map<String, Any?> = mapOf(
    "ledgerId" to ledgerId,
    "ledgerName" to ledgerName,
    "monthlyLimit" to monthlyLimit,
    "month" to month
  ).filterValues { it != null }
}

@IgnoreExtraProperties
data class RecurringVoucher(
  @DocumentId val id: String = "",
  val title: String = "",
  val voucherType: String = "F5",
  val amount: Double = 0.0,
  val debitLedgerId: String = "",
  val debitLedgerName: String = "",
  val creditLedgerId: String = "",
  val creditLedgerName: String = "",
  val frequency: String = "MONTHLY",
  val dayOfMonth: Long = 1L,
  val isActive: Boolean = true,
  val narration: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "voucherType" to voucherType,
    "amount" to amount,
    "debitLedgerId" to debitLedgerId,
    "debitLedgerName" to debitLedgerName,
    "creditLedgerId" to creditLedgerId,
    "creditLedgerName" to creditLedgerName,
    "frequency" to frequency,
    "dayOfMonth" to dayOfMonth,
    "isActive" to isActive,
    "narration" to narration
  ).filterValues { it != null }
}

@IgnoreExtraProperties
data class FamilyMember(
  @DocumentId val id: String = "",
  val name: String = "",
  val role: String = "Member", // "Admin", "Member"
  val email: String? = null,
  val colorHex: String = "#1976D2",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "role" to role,
    "email" to email,
    "colorHex" to colorHex
  ).filterValues { it != null }
}
