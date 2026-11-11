const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read ledgers", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("ledgers").get());
});

test("Authenticated user: cannot read another user's ledgers", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("ledgers").doc("bank_hdfc").set({
      name: "HDFC Bank",
      group: "Bank Accounts",
      openingBalance: 50000,
      openingBalanceType: "Dr",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("ledgers").doc("bank_hdfc").get());
});

test("Authenticated user: can create and read their own ledger", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ledgers").doc("cash").set({
      name: "Cash",
      group: "Cash-in-Hand",
      openingBalance: 10000,
      openingBalanceType: "Dr",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ledgers").doc("cash").get()
  );
});

test("Authenticated user: can create and read their own accounting group", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("groups").doc("grp_bank_accounts").set({
      name: "Bank Accounts",
      parentGroup: "Current Assets",
      isSystemGroup: true,
      category: "Asset",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("groups").doc("grp_bank_accounts").get()
  );
});

test("Validation: Rejects group with invalid category", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("groups").doc("grp_bad").set({
      name: "Mystery Group",
      category: "Nonsense", // Invalid! Must be Asset, Liability, Equity, Income, or Expense
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot read another user's groups", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("groups").doc("grp_cash_in_hand").set({
      name: "Cash-in-Hand",
      category: "Asset",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("groups").doc("grp_cash_in_hand").get());
});

test("Validation: Rejects voucher with invalid voucher type", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("vouchers").doc("vch_1").set({
      voucherNumber: "VCH-01",
      voucherType: "F99", // Invalid! Must be F4, F5, F6, or F7
      date: "2026-11-09",
      totalAmount: 500,
      narration: "Grocery items",
      entriesJson: "[]",
      createdAt: new Date(),
    })
  );
});

test("Validation: Accepts valid payment voucher", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("vouchers").doc("vch_1").set({
      voucherNumber: "VCH-01",
      voucherType: "F5",
      date: "2026-11-09",
      totalAmount: 1200,
      narration: "Grocery items at supermarket",
      entriesJson: JSON.stringify([
        { ledgerName: "Grocery Expenses", type: "Dr", amount: 1200 },
        { ledgerName: "Cash", type: "Cr", amount: 1200 }
      ]),
      createdAt: new Date(),
    })
  );
});
