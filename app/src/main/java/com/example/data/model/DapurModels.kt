package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class OrderChannel(val wireValue: String, val label: String, val prefix: String) {
  TAKEAWAY("takeaway", "Takeaway", "TK"),
  PHONE_TAKEAWAY("phone_takeaway", "Phone Order", "PH"),
  DINE_IN("dine_in", "Dine-In", "DI");

  companion object {
    fun fromWire(value: String): OrderChannel =
      entries.firstOrNull { it.wireValue == value } ?: TAKEAWAY
  }
}

enum class KitchenStatus(val wireValue: String, val label: String) {
  QUEUED("queued", "Queued"),
  COOKING("cooking", "Cooking"),
  PACKED("packed", "Packed"),
  COMPLETED("completed", "Handed Off");

  companion object {
    fun fromWire(value: String): KitchenStatus =
      entries.firstOrNull { it.wireValue == value } ?: QUEUED
  }
}

enum class SpiceLevel(val wireValue: String, val shortLabel: String) {
  NO_CHILI("No Chili", "No Chili"),
  MILD("Mild", "Mild"),
  STANDARD("Standard", "Standard"),
  EXTRA_PEDAS("Extra Pedas", "Extra Pedas")
}

enum class SauceOption(val wireValue: String, val shortLabel: String) {
  STANDARD("Standard", "Standard"),
  SAMBAL_SEPARATE("Sambal Separate", "Sambal Sep."),
  EXTRA_GRAVY("Extra Gravy", "Extra Gravy"),
  NO_SAUCE("No Sauce", "No Sauce")
}

enum class MistakeCategory(val wireValue: String, val label: String) {
  MISSING_ITEM("missing_item", "Missing Item"),
  WRONG_ITEM("wrong_item", "Wrong Item"),
  WRONG_SPICE_SAUCE("wrong_spice_sauce", "Wrong Spice / Sauce"),
  NONE("none", "Pass Verified");

  companion object {
    fun fromWire(value: String): MistakeCategory =
      entries.firstOrNull { it.wireValue == value } ?: MISSING_ITEM
  }
}

enum class MistakeOutcome(val wireValue: String, val label: String) {
  MISTAKE_OCCURRED("mistake_occurred", "Mistake Reported (-RM 6)"),
  CAUGHT_AT_PASS("caught_at_pass", "Caught at Pass (+RM 6 Saved)");

  companion object {
    fun fromWire(value: String): MistakeOutcome =
      entries.firstOrNull { it.wireValue == value } ?: MISTAKE_OCCURRED
  }
}

@IgnoreExtraProperties
data class OrderLineItem(
  val itemId: String = "",
  val itemName: String = "",
  val quantity: Long = 1L,
  val unitPriceRm: Double = 0.0,
  val spiceLevel: String = SpiceLevel.STANDARD.wireValue,
  val sauceOption: String = SauceOption.STANDARD.wireValue,
  val itemNote: String = "",
  val verified: Boolean = false
) {
  fun toMap(): Map<String, Any> = mapOf(
    "itemId" to itemId,
    "itemName" to itemName,
    "quantity" to quantity,
    "unitPriceRm" to unitPriceRm,
    "spiceLevel" to spiceLevel,
    "sauceOption" to sauceOption,
    "itemNote" to itemNote,
    "verified" to verified
  )
}

@IgnoreExtraProperties
data class OrderRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val ticketNumber: String = "",
  val orderType: String = OrderChannel.TAKEAWAY.wireValue,
  val tableOrPhone: String = "",
  val status: String = KitchenStatus.QUEUED.wireValue,
  val items: List<OrderLineItem> = emptyList(),
  val totalRm: Double = 0.0,
  val isPeakHour: Boolean = false,
  val customerNote: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class QuickMenuItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val category: String = "Mains",
  val priceRm: Double = 0.0,
  val defaultSpice: String = SpiceLevel.STANDARD.wireValue,
  val defaultSauce: String = SauceOption.STANDARD.wireValue,
  val sortOrder: Long = 0L,
  val isTopTen: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MistakeLog(
  @DocumentId val id: String = "",
  val userId: String = "",
  val orderId: String = "",
  val ticketNumber: String = "",
  val outcome: String = MistakeOutcome.CAUGHT_AT_PASS.wireValue,
  val category: String = MistakeCategory.NONE.wireValue,
  val orderType: String = OrderChannel.TAKEAWAY.wireValue,
  val costImpactRm: Double = 6.0,
  val isPeakHour: Boolean = false,
  val note: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

object SeedMenuItems {
  const val MISTAKE_COST_RM = 6.0

  fun defaultTopTen(userId: String): List<QuickMenuItem> = listOf(
    QuickMenuItem(
      id = "top_01_nasi_lemak_ayam",
      userId = userId,
      name = "Nasi Lemak Ayam",
      category = "Mains",
      priceRm = 12.50,
      defaultSpice = SpiceLevel.STANDARD.wireValue,
      defaultSauce = SauceOption.SAMBAL_SEPARATE.wireValue,
      sortOrder = 1L,
      isTopTen = true
    ),
    QuickMenuItem(
      id = "top_02_char_kuey_teow",
      userId = userId,
      name = "Char Kuey Teow",
      category = "Noodles",
      priceRm = 10.50,
      defaultSpice = SpiceLevel.STANDARD.wireValue,
      defaultSauce = SauceOption.STANDARD.wireValue,
      sortOrder = 2L,
      isTopTen = true
    ),
    QuickMenuItem(
      id = "top_03_mee_goreng_mamak",
      userId = userId,
      name = "Mee Goreng Mamak",
      category = "Noodles",
      priceRm = 9.50,
      defaultSpice = SpiceLevel.STANDARD.wireValue,
      defaultSauce = SauceOption.STANDARD.wireValue,
      sortOrder = 3L,
      isTopTen = true
    ),
    QuickMenuItem(
      id = "top_04_ayam_penyet",
      userId = userId,
      name = "Nasi Ayam Penyet",
      category = "Mains",
      priceRm = 13.90,
      defaultSpice = SpiceLevel.EXTRA_PEDAS.wireValue,
      defaultSauce = SauceOption.SAMBAL_SEPARATE.wireValue,
      sortOrder = 4L,
      isTopTen = true
    ),
    QuickMenuItem(
      id = "top_05_nasi_goreng_kampung",
      userId = userId,
      name = "Nasi Goreng Kampung",
      category = "Mains",
      priceRm = 10.90,
      defaultSpice = SpiceLevel.STANDARD.wireValue,
      defaultSauce = SauceOption.STANDARD.wireValue,
      sortOrder = 5L,
      isTopTen = true
    ),
    QuickMenuItem(
      id = "top_06_roti_canai_telur",
      userId = userId,
      name = "Roti Canai Telur",
      category = "Sides",
      priceRm = 4.50,
      defaultSpice = SpiceLevel.MILD.wireValue,
      defaultSauce = SauceOption.EXTRA_GRAVY.wireValue,
      sortOrder = 6L,
      isTopTen = true
    ),
    QuickMenuItem(
      id = "top_07_curry_laksa",
      userId = userId,
      name = "Selangor Curry Laksa",
      category = "Noodles",
      priceRm = 11.50,
      defaultSpice = SpiceLevel.STANDARD.wireValue,
      defaultSauce = SauceOption.SAMBAL_SEPARATE.wireValue,
      sortOrder = 7L,
      isTopTen = true
    ),
    QuickMenuItem(
      id = "top_08_satay_ayam_10",
      userId = userId,
      name = "Satay Ayam (10 pcs)",
      category = "Sides",
      priceRm = 15.00,
      defaultSpice = SpiceLevel.MILD.wireValue,
      defaultSauce = SauceOption.SAMBAL_SEPARATE.wireValue,
      sortOrder = 8L,
      isTopTen = true
    ),
    QuickMenuItem(
      id = "top_09_teh_tarik_ais",
      userId = userId,
      name = "Teh Tarik Ais",
      category = "Drinks",
      priceRm = 3.80,
      defaultSpice = SpiceLevel.NO_CHILI.wireValue,
      defaultSauce = SauceOption.NO_SAUCE.wireValue,
      sortOrder = 9L,
      isTopTen = true
    ),
    QuickMenuItem(
      id = "top_10_limau_ais",
      userId = userId,
      name = "Limau Ais",
      category = "Drinks",
      priceRm = 3.20,
      defaultSpice = SpiceLevel.NO_CHILI.wireValue,
      defaultSauce = SauceOption.NO_SAUCE.wireValue,
      sortOrder = 10L,
      isTopTen = true
    )
  )
}
