const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_selangor";
const BOB_UID = "bob_klang";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function tsNow() {
  return new Date(Date.now() - 1000);
}

function validOrderPayload(uid, overrides = {}) {
  return {
    userId: uid,
    ticketNumber: "#TK-001",
    orderType: "takeaway",
    tableOrPhone: "012-3456789",
    status: "queued",
    items: [
      {
        itemId: "nasi_lemak_ayam",
        itemName: "Nasi Lemak Ayam",
        quantity: 1,
        unitPriceRm: 12.5,
        spiceLevel: "Extra Pedas",
        sauceOption: "Sambal Separate",
        itemNote: "",
        verified: false,
      },
    ],
    totalRm: 12.5,
    isPeakHour: true,
    customerNote: "No cucumber",
    createdAt: tsNow(),
    updatedAt: tsNow(),
    ...overrides,
  };
}

function validMenuItemPayload(uid, overrides = {}) {
  return {
    userId: uid,
    name: "Nasi Lemak Ayam",
    category: "Mains",
    priceRm: 12.5,
    defaultSpice: "Standard",
    defaultSauce: "Standard",
    sortOrder: 1,
    isTopTen: true,
    createdAt: tsNow(),
    updatedAt: tsNow(),
    ...overrides,
  };
}

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

test("Unauthenticated user cannot read or write orders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("orders").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("orders").doc("order_1").set(validOrderPayload(ALICE_UID))
  );
});

test("Owner can create, update items/status, and list their own orders", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderRef = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("order_1");

  await assertSucceeds(orderRef.set(validOrderPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("orders").get());

  await assertSucceeds(
    orderRef.update({
      status: "completed",
      updatedAt: tsNow(),
    })
  );

  // Terminal state lock: once completed, further updates must fail
  await assertFails(
    orderRef.update({
      status: "queued",
      updatedAt: tsNow(),
    })
  );
});

test("Cross-user access to orders is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("order_1").set(validOrderPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("orders").doc("order_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("orders").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("orders").doc("order_2").set(validOrderPayload(BOB_UID))
  );
});

test("Shadow update with ghost field on orders is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderRef = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("order_shadow");

  await assertFails(
    orderRef.set(validOrderPayload(ALICE_UID, { isVerifiedAdmin: true }))
  );
});

test("Menu item CRUD enforces schema and owner isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const itemRef = aliceDb.collection("users").doc(ALICE_UID).collection("menu_items").doc("item_1");
  await assertSucceeds(itemRef.set(validMenuItemPayload(ALICE_UID)));
  await assertSucceeds(itemRef.update({ priceRm: 13.0, updatedAt: tsNow() }));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("menu_items").doc("item_1").delete());
});

test("Mistake log requires existing order and is append-only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderRef = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("order_1");
  await assertSucceeds(orderRef.set(validOrderPayload(ALICE_UID)));

  // Orphaned log (non-existent order) must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("mistake_logs").doc("log_orphan").set({
      userId: ALICE_UID,
      orderId: "nonexistent_order",
      ticketNumber: "#TK-999",
      outcome: "mistake_occurred",
      category: "missing_item",
      orderType: "takeaway",
      costImpactRm: 6.0,
      isPeakHour: true,
      note: "Forgot sambal",
      createdAt: tsNow(),
    })
  );

  // Valid log referencing existing order succeeds
  const logRef = aliceDb.collection("users").doc(ALICE_UID).collection("mistake_logs").doc("log_1");
  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      orderId: "order_1",
      ticketNumber: "#TK-001",
      outcome: "caught_at_pass",
      category: "wrong_spice_sauce",
      orderType: "takeaway",
      costImpactRm: 6.0,
      isPeakHour: true,
      note: "Caught Extra Pedas before packing",
      createdAt: tsNow(),
    })
  );

  // Append-only: update and delete must fail
  await assertFails(logRef.update({ costImpactRm: 0.0 }));
  await assertFails(logRef.delete());
});
