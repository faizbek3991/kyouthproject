package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.KitchenStatus
import com.example.data.model.MistakeCategory
import com.example.data.model.MistakeLog
import com.example.data.model.MistakeOutcome
import com.example.data.model.OrderChannel
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderRecord
import com.example.data.model.QuickMenuItem
import com.example.data.model.SauceOption
import com.example.data.model.SeedMenuItems
import com.example.data.model.SpiceLevel
import com.example.data.repository.DapurPassRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

data class CartLineDraft(
  val draftId: String,
  val menuItemId: String,
  val itemName: String,
  val quantity: Long,
  val unitPriceRm: Double,
  val spiceLevel: SpiceLevel,
  val sauceOption: SauceOption,
  val itemNote: String = ""
)

data class OrderDraftState(
  val channel: OrderChannel = OrderChannel.TAKEAWAY,
  val tableOrPhone: String = "",
  val customerNote: String = "",
  val isPeakHour: Boolean = isCurrentlyPeakDinnerHour(),
  val lines: List<CartLineDraft> = emptyList(),
  val isSubmitting: Boolean = false
) {
  val totalRm: Double get() = lines.sumOf { it.unitPriceRm * it.quantity }
  val totalItemCount: Long get() = lines.sumOf { it.quantity }
}

data class AnalyticsSummary(
  val totalOrders: Int,
  val takeawayCount: Int,
  val phoneTakeawayCount: Int,
  val dineInCount: Int,
  val peakHourOrders: Int,
  val mistakesOccurred: Int,
  val caughtAtPass: Int,
  val actualMistakeRatePercent: Double,
  val peakMistakeRatePercent: Double,
  val directMistakeLossRm: Double,
  val caughtSavingsRm: Double,
  val estimatedBaselineSavingsRm: Double,
  val roiProgressFraction: Float,
  val missingItemCaught: Int,
  val missingItemSlipped: Int,
  val wrongItemCaught: Int,
  val wrongItemSlipped: Int,
  val wrongSpiceCaught: Int,
  val wrongSpiceSlipped: Int
)

private fun isCurrentlyPeakDinnerHour(): Boolean {
  val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
  return hour in 19..20
}

class DapurPassViewModel(
  private val repository: DapurPassRepository,
  private val currentUserId: String
) : ViewModel() {

  val ordersState: StateFlow<UiState<List<OrderRecord>>> = repository.observeOrders(currentUserId)
    .map<List<OrderRecord>, UiState<List<OrderRecord>>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val rootError = firestoreError ?: error
      Log.w(TAG, "Error observing orders", rootError)
      emit(UiState.Error(rootError.localizedMessage ?: "Failed to load kitchen orders"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
      initialValue = UiState.Loading
    )

  val menuItemsState: StateFlow<UiState<List<QuickMenuItem>>> = repository.observeMenuItems(currentUserId)
    .map<List<QuickMenuItem>, UiState<List<QuickMenuItem>>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val rootError = firestoreError ?: error
      Log.w(TAG, "Error observing menu items", rootError)
      emit(UiState.Error(rootError.localizedMessage ?: "Failed to load menu items"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
      initialValue = UiState.Loading
    )

  val mistakeLogsState: StateFlow<UiState<List<MistakeLog>>> = repository.observeMistakeLogs(currentUserId)
    .map<List<MistakeLog>, UiState<List<MistakeLog>>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val rootError = firestoreError ?: error
      Log.w(TAG, "Error observing mistake logs", rootError)
      emit(UiState.Error(rootError.localizedMessage ?: "Failed to load quality analytics"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
      initialValue = UiState.Loading
    )

  private val _draft = MutableStateFlow(OrderDraftState())
  val draft: StateFlow<OrderDraftState> = _draft.asStateFlow()

  private val _bannerMessage = MutableStateFlow<String?>(null)
  val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

  init {
    viewModelScope.launch {
      repository.seedDefaultTopTenMenuIfEmpty()
        .onFailure { Log.w(TAG, "Menu seed check failed: ${it.message}") }
    }
  }

  fun clearBanner() {
    _bannerMessage.value = null
  }

  fun setChannel(channel: OrderChannel) {
    _draft.update { it.copy(channel = channel) }
  }

  fun setTableOrPhone(value: String) {
    _draft.update { it.copy(tableOrPhone = value) }
  }

  fun setCustomerNote(value: String) {
    _draft.update { it.copy(customerNote = value) }
  }

  fun togglePeakHour(enabled: Boolean) {
    _draft.update { it.copy(isPeakHour = enabled) }
  }

  fun quickAddMenuItem(item: QuickMenuItem) {
    val defaultSpice = SpiceLevel.entries.firstOrNull { it.wireValue == item.defaultSpice } ?: SpiceLevel.STANDARD
    val defaultSauce = SauceOption.entries.firstOrNull { it.wireValue == item.defaultSauce } ?: SauceOption.STANDARD
    addConfiguredLine(
      menuItem = item,
      spiceLevel = defaultSpice,
      sauceOption = defaultSauce,
      note = ""
    )
  }

  fun addConfiguredLine(
    menuItem: QuickMenuItem,
    spiceLevel: SpiceLevel,
    sauceOption: SauceOption,
    note: String
  ) {
    _draft.update { current ->
      val trimmedNote = note.trim()
      val existingIndex = current.lines.indexOfFirst {
        it.menuItemId == menuItem.id &&
          it.spiceLevel == spiceLevel &&
          it.sauceOption == sauceOption &&
          it.itemNote == trimmedNote
      }
      val updatedLines = if (existingIndex >= 0) {
        current.lines.mapIndexed { idx, line ->
          if (idx == existingIndex) line.copy(quantity = line.quantity + 1L) else line
        }
      } else {
        current.lines + CartLineDraft(
          draftId = "${menuItem.id}_${System.nanoTime()}",
          menuItemId = menuItem.id,
          itemName = menuItem.name,
          quantity = 1L,
          unitPriceRm = menuItem.priceRm,
          spiceLevel = spiceLevel,
          sauceOption = sauceOption,
          itemNote = trimmedNote
        )
      }
      current.copy(lines = updatedLines)
    }
  }

  fun updateDraftLineQuantity(draftId: String, delta: Long) {
    _draft.update { current ->
      val updated = current.lines.mapNotNull { line ->
        if (line.draftId == draftId) {
          val newQty = line.quantity + delta
          if (newQty <= 0L) null else line.copy(quantity = newQty)
        } else {
          line
        }
      }
      current.copy(lines = updated)
    }
  }

  fun updateDraftLineModifiers(
    draftId: String,
    spiceLevel: SpiceLevel,
    sauceOption: SauceOption,
    note: String
  ) {
    _draft.update { current ->
      current.copy(
        lines = current.lines.map { line ->
          if (line.draftId == draftId) {
            line.copy(spiceLevel = spiceLevel, sauceOption = sauceOption, itemNote = note.trim())
          } else {
            line
          }
        }
      )
    }
  }

  fun clearDraftCart() {
    _draft.update {
      it.copy(tableOrPhone = "", customerNote = "", lines = emptyList())
    }
  }

  fun submitOrder(existingOrderCount: Int) {
    val currentDraft = _draft.value
    if (currentDraft.lines.isEmpty() || currentDraft.isSubmitting) return

    _draft.update { it.copy(isSubmitting = true) }
    val nextSeq = (existingOrderCount + 1).toString().padStart(3, '0')
    val ticketNum = "#${currentDraft.channel.prefix}-$nextSeq"
    val lineItems = currentDraft.lines.map { line ->
      OrderLineItem(
        itemId = line.menuItemId,
        itemName = line.itemName,
        quantity = line.quantity,
        unitPriceRm = line.unitPriceRm,
        spiceLevel = line.spiceLevel.wireValue,
        sauceOption = line.sauceOption.wireValue,
        itemNote = line.itemNote,
        verified = false
      )
    }
    val channelLabel = currentDraft.tableOrPhone.ifBlank {
      when (currentDraft.channel) {
        OrderChannel.TAKEAWAY -> "Walk-in Takeaway"
        OrderChannel.PHONE_TAKEAWAY -> "Phone Pickup"
        OrderChannel.DINE_IN -> "Counter Table"
      }
    }

    viewModelScope.launch {
      val result = repository.createOrder(
        ticketNumber = ticketNum,
        orderType = currentDraft.channel,
        tableOrPhone = channelLabel,
        items = lineItems,
        isPeakHour = currentDraft.isPeakHour,
        customerNote = currentDraft.customerNote
      )
      if (result.isSuccess) {
        _draft.update {
          it.copy(
            tableOrPhone = "",
            customerNote = "",
            lines = emptyList(),
            isSubmitting = false
          )
        }
        _bannerMessage.value = "Ticket $ticketNum sent to Kitchen Pass"
      } else {
        _draft.update { it.copy(isSubmitting = false) }
        _bannerMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Could not send order"
      }
    }
  }

  fun toggleItemVerified(order: OrderRecord, itemIndex: Int) {
    if (order.status == KitchenStatus.COMPLETED.wireValue) return
    val updatedItems = order.items.mapIndexed { idx, item ->
      if (idx == itemIndex) item.copy(verified = !item.verified) else item
    }
    val currentStatus = KitchenStatus.fromWire(order.status)
    val nextStatus = if (currentStatus == KitchenStatus.QUEUED) KitchenStatus.COOKING else currentStatus
    viewModelScope.launch {
      repository.updateOrderChecklistAndStatus(order.id, updatedItems, nextStatus)
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to update checklist" }
    }
  }

  fun advanceOrderStatus(order: OrderRecord, targetStatus: KitchenStatus) {
    if (order.status == KitchenStatus.COMPLETED.wireValue) return
    val finalItems = if (targetStatus == KitchenStatus.COMPLETED) {
      order.items.map { it.copy(verified = true) }
    } else {
      order.items
    }
    viewModelScope.launch {
      val res = if (targetStatus == KitchenStatus.COMPLETED) {
        repository.updateOrderChecklistAndStatus(order.id, finalItems, targetStatus)
      } else {
        repository.updateOrderStatus(order.id, targetStatus)
      }
      res.onSuccess {
        if (targetStatus == KitchenStatus.COMPLETED) {
          _bannerMessage.value = "${order.ticketNumber} verified & handed off!"
        }
      }.onFailure {
        _bannerMessage.value = it.localizedMessage ?: "Status update failed"
      }
    }
  }

  fun cancelQueuedOrder(order: OrderRecord) {
    if (order.status != KitchenStatus.QUEUED.wireValue) return
    viewModelScope.launch {
      repository.cancelQueuedOrder(order.id)
        .onSuccess { _bannerMessage.value = "Cancelled ${order.ticketNumber}" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Could not cancel order" }
    }
  }

  fun logQualityEvent(
    order: OrderRecord,
    outcome: MistakeOutcome,
    category: MistakeCategory,
    note: String
  ) {
    viewModelScope.launch {
      repository.logMistakeOrSave(
        orderId = order.id,
        ticketNumber = order.ticketNumber,
        outcome = outcome,
        category = category,
        orderType = OrderChannel.fromWire(order.orderType),
        isPeakHour = order.isPeakHour,
        note = note
      ).onSuccess {
        val msg = if (outcome == MistakeOutcome.CAUGHT_AT_PASS) {
          "Pass catch logged for ${order.ticketNumber} (+RM 6 saved)"
        } else {
          "Mistake logged for ${order.ticketNumber} (-RM 6 cost)"
        }
        _bannerMessage.value = msg
      }.onFailure {
        _bannerMessage.value = it.localizedMessage ?: "Failed to record quality log"
      }
    }
  }

  fun saveMenuItem(item: QuickMenuItem) {
    viewModelScope.launch {
      repository.saveMenuItem(item)
        .onSuccess { _bannerMessage.value = "Saved ${item.name}" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to save dish" }
    }
  }

  fun deleteMenuItem(item: QuickMenuItem) {
    viewModelScope.launch {
      repository.deleteMenuItem(item.id)
        .onSuccess { _bannerMessage.value = "Removed ${item.name}" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to delete dish" }
    }
  }

  fun computeAnalytics(
    orders: List<OrderRecord>,
    logs: List<MistakeLog>
  ): AnalyticsSummary {
    val totalOrders = orders.size
    val takeawayCount = orders.count { it.orderType == OrderChannel.TAKEAWAY.wireValue }
    val phoneCount = orders.count { it.orderType == OrderChannel.PHONE_TAKEAWAY.wireValue }
    val dineInCount = orders.count { it.orderType == OrderChannel.DINE_IN.wireValue }
    val peakOrders = orders.count { it.isPeakHour }

    val mistakes = logs.filter { it.outcome == MistakeOutcome.MISTAKE_OCCURRED.wireValue }
    val caught = logs.filter { it.outcome == MistakeOutcome.CAUGHT_AT_PASS.wireValue }

    val peakMistakes = mistakes.count { it.isPeakHour }
    val actualRate = if (totalOrders > 0) (mistakes.size * 100.0) / totalOrders else 0.0
    val peakRate = if (peakOrders > 0) (peakMistakes * 100.0) / peakOrders else 0.0

    val directLoss = mistakes.sumOf { it.costImpactRm }
    val caughtSavings = caught.sumOf { it.costImpactRm }

    // Case study baseline: 50% mistake rate during 7-9 PM peak voice ordering (RM 6 per mistake)
    val expectedBaselinePeakMistakes = peakOrders * 0.50
    val avoidedPeakMistakes = (expectedBaselinePeakMistakes - peakMistakes).coerceAtLeast(0.0)
    val baselineSavingsRm = avoidedPeakMistakes * SeedMenuItems.MISTAKE_COST_RM
    val totalSavingsTowardsTablet = caughtSavings + baselineSavingsRm
    val roiFraction = (totalSavingsTowardsTablet / 1500.0).toFloat().coerceIn(0f, 1f)

    return AnalyticsSummary(
      totalOrders = totalOrders,
      takeawayCount = takeawayCount,
      phoneTakeawayCount = phoneCount,
      dineInCount = dineInCount,
      peakHourOrders = peakOrders,
      mistakesOccurred = mistakes.size,
      caughtAtPass = caught.size,
      actualMistakeRatePercent = actualRate,
      peakMistakeRatePercent = peakRate,
      directMistakeLossRm = directLoss,
      caughtSavingsRm = caughtSavings,
      estimatedBaselineSavingsRm = baselineSavingsRm,
      roiProgressFraction = roiFraction,
      missingItemCaught = caught.count { it.category == MistakeCategory.MISSING_ITEM.wireValue },
      missingItemSlipped = mistakes.count { it.category == MistakeCategory.MISSING_ITEM.wireValue },
      wrongItemCaught = caught.count { it.category == MistakeCategory.WRONG_ITEM.wireValue },
      wrongItemSlipped = mistakes.count { it.category == MistakeCategory.WRONG_ITEM.wireValue },
      wrongSpiceCaught = caught.count { it.category == MistakeCategory.WRONG_SPICE_SAUCE.wireValue },
      wrongSpiceSlipped = mistakes.count { it.category == MistakeCategory.WRONG_SPICE_SAUCE.wireValue }
    )
  }

  private companion object {
    const val TAG = "DapurPassVM"
    const val STOP_TIMEOUT_MS = 5000L
  }
}
