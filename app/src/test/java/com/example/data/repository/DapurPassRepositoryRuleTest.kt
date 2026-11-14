package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.KitchenStatus
import com.example.data.model.MistakeCategory
import com.example.data.model.MistakeOutcome
import com.example.data.model.OrderChannel
import com.example.data.model.OrderLineItem
import com.example.data.model.SauceOption
import com.example.data.model.SpiceLevel
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DapurPassRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createAndObserveOrder_authenticatedOwner_emitsRealtimeTicket() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = DapurPassRepository(firestore, auth)

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createOrder(
        ticketNumber = "#TK-101",
        orderType = OrderChannel.PHONE_TAKEAWAY,
        tableOrPhone = "012-8889999",
        items = listOf(
          OrderLineItem(
            itemId = "nasi_lemak_ayam",
            itemName = "Nasi Lemak Ayam",
            quantity = 2L,
            unitPriceRm = 12.50,
            spiceLevel = SpiceLevel.EXTRA_PEDAS.wireValue,
            sauceOption = SauceOption.SAMBAL_SEPARATE.wireValue,
            verified = false
          )
        ),
        isPeakHour = true,
        customerNote = "Separate sambal for kids"
      )
    }

    assertTrue(createResult.isSuccess)
    val orderId = createResult.getOrThrow()
    assertTrue(orderId.isNotBlank())

    val orders = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeOrders(uid).first { list -> list.any { it.id == orderId } }
    }
    val created = orders.first { it.id == orderId }
    assertEquals("#TK-101", created.ticketNumber)
    assertEquals(25.0, created.totalRm, 0.01)
    assertEquals(1, created.items.size)
    assertEquals(SpiceLevel.EXTRA_PEDAS.wireValue, created.items.first().spiceLevel)
  }

  @Test
  fun updateChecklistAndCompleteOrder_locksTerminalState() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = DapurPassRepository(firestore, auth)

    val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createOrder(
        ticketNumber = "#TK-102",
        orderType = OrderChannel.TAKEAWAY,
        tableOrPhone = "Walk-in",
        items = listOf(
          OrderLineItem(
            itemId = "ckt",
            itemName = "Char Kuey Teow",
            quantity = 1L,
            unitPriceRm = 10.50,
            verified = false
          )
        ),
        isPeakHour = true,
        customerNote = ""
      ).getOrThrow()
    }

    val verifyResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateOrderChecklistAndStatus(
        orderId = orderId,
        updatedItems = listOf(
          OrderLineItem(
            itemId = "ckt",
            itemName = "Char Kuey Teow",
            quantity = 1L,
            unitPriceRm = 10.50,
            verified = true
          )
        ),
        newStatus = KitchenStatus.COMPLETED
      )
    }
    assertTrue(verifyResult.isSuccess)

    // Attempt to mutate completed order must be rejected by terminal-state rule
    val postCompleteUpdate = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateOrderStatus(orderId, KitchenStatus.QUEUED)
    }
    assertTrue(postCompleteUpdate.isFailure)
    val firestoreEx = postCompleteUpdate.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(firestoreEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx!!.code)
  }

  @Test
  fun logMistakeOrSave_validOrder_persistsAndLinksToOrder() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = DapurPassRepository(firestore, auth)

    val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createOrder(
        ticketNumber = "#TK-103",
        orderType = OrderChannel.TAKEAWAY,
        tableOrPhone = "019-2223333",
        items = listOf(
          OrderLineItem(
            itemId = "teh_tarik",
            itemName = "Teh Tarik Ais",
            quantity = 1L,
            unitPriceRm = 3.80
          )
        ),
        isPeakHour = true,
        customerNote = ""
      ).getOrThrow()
    }

    val logResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.logMistakeOrSave(
        orderId = orderId,
        ticketNumber = "#TK-103",
        outcome = MistakeOutcome.CAUGHT_AT_PASS,
        category = MistakeCategory.MISSING_ITEM,
        orderType = OrderChannel.TAKEAWAY,
        isPeakHour = true,
        note = "Checklist caught missing Teh Tarik before handoff"
      )
    }
    assertTrue(logResult.isSuccess)
    val logId = logResult.getOrThrow()

    val logs = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMistakeLogs(uid).first { list -> list.any { it.id == logId } }
    }
    assertEquals(6.0, logs.first { it.id == logId }.costImpactRm, 0.01)
  }

  @Test
  fun observeOrders_crossUserAndUnauthenticated_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = DapurPassRepository(firestore, auth)
    val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createOrder(
        ticketNumber = "#TK-104",
        orderType = OrderChannel.DINE_IN,
        tableOrPhone = "Table 3",
        items = listOf(OrderLineItem(itemId = "roti", itemName = "Roti Canai Telur", unitPriceRm = 4.50)),
        isPeakHour = false,
        customerNote = ""
      ).getOrThrow()
    }

    // Cross-user read attempt by Bob
    signInTestUser(BOB_EMAIL)
    val bobFlowError = runCatching {
      withTimeout(FLOW_TIMEOUT_MS) { aliceRepo.observeOrders(aliceUid).first() }
    }.exceptionOrNull()
    assertNotNull(bobFlowError)
    val bobFirestoreEx = bobFlowError!!.unwrapFirestoreException()
    assertNotNull(bobFirestoreEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, bobFirestoreEx!!.code)

    // Unauthenticated read attempt
    auth.signOut()
    val unauthFlowError = runCatching {
      withTimeout(FLOW_TIMEOUT_MS) { aliceRepo.observeOrders(aliceUid).first() }
    }.exceptionOrNull()
    assertNotNull(unauthFlowError)
    val unauthFirestoreEx = unauthFlowError!!.unwrapFirestoreException()
    assertNotNull(unauthFirestoreEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, unauthFirestoreEx!!.code)
  }

  private companion object {
    const val ALICE_EMAIL = "selangor_owner@test.com"
    const val BOB_EMAIL = "other_cafe@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
