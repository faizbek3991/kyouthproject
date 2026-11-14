package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.KitchenStatus
import com.example.data.model.MistakeCategory
import com.example.data.model.MistakeLog
import com.example.data.model.MistakeOutcome
import com.example.data.model.OrderChannel
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderRecord
import com.example.data.model.QuickMenuItem
import com.example.data.model.SeedMenuItems
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DapurPassRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    auth = Firebase.auth
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun ordersCollection(userId: String) =
    db.collection("users").document(userId).collection("orders")

  private fun menuCollection(userId: String) =
    db.collection("users").document(userId).collection("menu_items")

  private fun mistakeLogsCollection(userId: String) =
    db.collection("users").document(userId).collection("mistake_logs")

  fun observeOrders(userId: String): Flow<List<OrderRecord>> {
    val path = "users/$userId/orders"
    return ordersCollection(userId)
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          OrderRecord::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      }
      .catch { error ->
        logSnapshotError(error, OperationType.LIST, path, userId)
        throw error
      }
  }

  fun observeMenuItems(userId: String): Flow<List<QuickMenuItem>> {
    val path = "users/$userId/menu_items"
    return menuCollection(userId)
      .orderBy("sortOrder", Query.Direction.ASCENDING)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          QuickMenuItem::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      }
      .catch { error ->
        logSnapshotError(error, OperationType.LIST, path, userId)
        throw error
      }
  }

  fun observeMistakeLogs(userId: String): Flow<List<MistakeLog>> {
    val path = "users/$userId/mistake_logs"
    return mistakeLogsCollection(userId)
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          MistakeLog::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      }
      .catch { error ->
        logSnapshotError(error, OperationType.LIST, path, userId)
        throw error
      }
  }

  private fun logSnapshotError(
    error: Throwable,
    operationType: OperationType,
    path: String,
    userId: String
  ) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
      handleFirestoreError(exceptionToLog, operationType, path, auth)
    }
  }

  suspend fun seedDefaultTopTenMenuIfEmpty(): Result<Unit> = runCatching {
    val uid = requireUserId()
    val collection = menuCollection(uid)
    val existing = collection.limit(1).get().await()
    if (existing.isEmpty) {
      val batch = db.batch()
      SeedMenuItems.defaultTopTen(uid).forEach { item ->
        val docRef = collection.document(item.id)
        val payload = mapOf(
          "userId" to uid,
          "name" to item.name,
          "category" to item.category,
          "priceRm" to item.priceRm,
          "defaultSpice" to item.defaultSpice,
          "defaultSauce" to item.defaultSauce,
          "sortOrder" to item.sortOrder,
          "isTopTen" to item.isTopTen,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        batch.set(docRef, payload)
      }
      batch.commit().await()
    }
  }.onFailure { error ->
    if (error is CancellationException) throw error
    handleFirestoreError(error, OperationType.WRITE, "users/${auth.currentUser?.uid}/menu_items", auth)
  }

  suspend fun createOrder(
    ticketNumber: String,
    orderType: OrderChannel,
    tableOrPhone: String,
    items: List<OrderLineItem>,
    isPeakHour: Boolean,
    customerNote: String
  ): Result<String> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val docRef = ordersCollection(uid).document()
    val totalRm = items.sumOf { it.unitPriceRm * it.quantity }
    val payload = mapOf(
      "userId" to uid,
      "ticketNumber" to ticketNumber.trim().take(24),
      "orderType" to orderType.wireValue,
      "tableOrPhone" to tableOrPhone.trim().take(64),
      "status" to KitchenStatus.QUEUED.wireValue,
      "items" to items.take(20).map { it.toMap() },
      "totalRm" to totalRm,
      "isPeakHour" to isPeakHour,
      "customerNote" to customerNote.trim().take(200),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun getOrderById(orderId: String): Result<OrderRecord?> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val docRef = ordersCollection(uid).document(orderId)
    return try {
      val snapshot = docRef.get().await()
      val record = snapshot.toObject(
        OrderRecord::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      )
      Result.success(record)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun updateOrderChecklistAndStatus(
    orderId: String,
    updatedItems: List<OrderLineItem>,
    newStatus: KitchenStatus
  ): Result<Unit> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val docRef = ordersCollection(uid).document(orderId)
    val updates = mapOf(
      "items" to updatedItems.map { it.toMap() },
      "status" to newStatus.wireValue,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun updateOrderStatus(
    orderId: String,
    newStatus: KitchenStatus
  ): Result<Unit> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val docRef = ordersCollection(uid).document(orderId)
    val updates = mapOf(
      "status" to newStatus.wireValue,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun cancelQueuedOrder(orderId: String): Result<Unit> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val docRef = ordersCollection(uid).document(orderId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun saveMenuItem(item: QuickMenuItem): Result<String> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val collection = menuCollection(uid)
    val isNew = item.id.isBlank()
    val docRef = if (isNew) collection.document() else collection.document(item.id)
    return try {
      if (isNew) {
        val payload = mapOf(
          "userId" to uid,
          "name" to item.name.trim().take(60),
          "category" to item.category,
          "priceRm" to item.priceRm,
          "defaultSpice" to item.defaultSpice,
          "defaultSauce" to item.defaultSauce,
          "sortOrder" to item.sortOrder,
          "isTopTen" to item.isTopTen,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
      } else {
        val updates = mapOf(
          "name" to item.name.trim().take(60),
          "category" to item.category,
          "priceRm" to item.priceRm,
          "defaultSpice" to item.defaultSpice,
          "defaultSauce" to item.defaultSauce,
          "sortOrder" to item.sortOrder,
          "isTopTen" to item.isTopTen,
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(updates).await()
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (isNew) OperationType.CREATE else OperationType.UPDATE,
        docRef.path,
        auth
      )
      Result.failure(e)
    }
  }

  suspend fun deleteMenuItem(menuItemId: String): Result<Unit> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val docRef = menuCollection(uid).document(menuItemId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun logMistakeOrSave(
    orderId: String,
    ticketNumber: String,
    outcome: MistakeOutcome,
    category: MistakeCategory,
    orderType: OrderChannel,
    isPeakHour: Boolean,
    note: String
  ): Result<String> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val docRef = mistakeLogsCollection(uid).document()
    val payload = mapOf(
      "userId" to uid,
      "orderId" to orderId,
      "ticketNumber" to ticketNumber.trim().take(24),
      "outcome" to outcome.wireValue,
      "category" to category.wireValue,
      "orderType" to orderType.wireValue,
      "costImpactRm" to SeedMenuItems.MISTAKE_COST_RM,
      "isPeakHour" to isPeakHour,
      "note" to note.trim().take(200),
      "createdAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      Result.failure(e)
    }
  }
}
