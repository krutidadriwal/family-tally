package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AccountingGroups
import com.example.data.model.Ledger
import com.example.data.model.Voucher
import com.example.data.model.VoucherEntry
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TallyRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createLedger_validPayload_createsDocumentAndReturnsId() {
    runBlocking {
      signInTestUser(ALICE_EMAIL)
      val repository = TallyRepository(firestore, auth)

      val ledger = Ledger(
        name = "HDFC Bank",
        group = AccountingGroups.BANK_ACCOUNTS,
        openingBalance = 50000.0,
        openingBalanceType = "Dr"
      )

      val result = withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createLedger(ledger)
      }
      assertTrue(
        "Expected success but was: ${result.exceptionOrNull()?.message} - cause: ${result.exceptionOrNull()?.cause?.message}",
        result.isSuccess
      )
      val ledgerId = result.getOrThrow()
      assertTrue(ledgerId.isNotEmpty())
    }
  }

  @Test
  fun observeLedgers_authenticatedOwner_emitsRealtimeUpdates() {
    runBlocking {
      signInTestUser(ALICE_EMAIL)
      val repository = TallyRepository(firestore, auth)

      val ledger = Ledger(
        name = "Grocery Expenses",
        group = AccountingGroups.INDIRECT_EXPENSES,
        openingBalance = 0.0,
        openingBalanceType = "Dr"
      )
      val ledgerId = withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createLedger(ledger).getOrThrow()
      }

      val emitted = withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeLedgers().first { list -> list.any { it.id == ledgerId } }
      }
      assertTrue(emitted.any { it.id == ledgerId })
    }
  }

  @Test
  fun createVoucher_validPayment_succeeds() {
    runBlocking {
      signInTestUser(ALICE_EMAIL)
      val repository = TallyRepository(firestore, auth)

      val entries = listOf(
        VoucherEntry(ledgerName = "Groceries", type = "Dr", amount = 1500.0),
        VoucherEntry(ledgerName = "Cash", type = "Cr", amount = 1500.0)
      )

      val voucher = Voucher(
        voucherNumber = "VCH-2026-001",
        voucherType = "F5",
        date = "2026-11-09",
        totalAmount = 1500.0,
        narration = "Groceries purchase",
        entriesJson = VoucherEntry.listToJson(entries)
      )

      val result = withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createVoucher(voucher)
      }
      assertTrue(result.isSuccess)
    }
  }

  @Test
  fun observeLedgers_unauthenticatedUser_failsWithPermissionDenied() {
    runBlocking {
      auth.signOut()
      val repository = TallyRepository(firestore, auth)

      var caughtException: Exception? = null
      try {
        withTimeout(FLOW_TIMEOUT_MS) {
          repository.observeLedgers().first()
        }
      } catch (e: Exception) {
        caughtException = e
      }

      assertNotNull("Expected exception when unauthenticated", caughtException)
      val firestoreException = caughtException?.unwrapFirestoreException()
      if (firestoreException != null) {
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
      } else {
        assertTrue(caughtException is IllegalStateException)
      }
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@familytally.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
