package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.KitchenStatus
import com.example.data.model.MistakeCategory
import com.example.data.model.MistakeOutcome
import com.example.data.model.OrderChannel
import com.example.data.model.OrderRecord
import com.example.ui.UiState
import com.example.ui.theme.MonoFontFamily
import java.text.SimpleDateFormat
import java.util.Locale

private enum class KitchenFilter(val label: String) {
  ACTIVE("Active Queue"),
  TAKEAWAY_ALL("Takeaway & Phone"),
  DINE_IN("Dine-In"),
  COMPLETED("Handed Off")
}

@Composable
fun KitchenPassScreen(
  ordersState: UiState<List<OrderRecord>>,
  onToggleItemVerified: (OrderRecord, Int) -> Unit,
  onAdvanceStatus: (OrderRecord, KitchenStatus) -> Unit,
  onCancelQueued: (OrderRecord) -> Unit,
  onLogQualityEvent: (OrderRecord, MistakeOutcome, MistakeCategory, String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedFilter by remember { mutableStateOf(KitchenFilter.ACTIVE) }
  var qualityLogTargetOrder by remember { mutableStateOf<OrderRecord?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(KitchenFilter.entries) { filter ->
        FilterChip(
          selected = selectedFilter == filter,
          onClick = { selectedFilter = filter },
          label = { Text(filter.label) },
          modifier = Modifier.testTag("kitchen_filter_${filter.name}")
        )
      }
    }

    when (ordersState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }
      is UiState.Error -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(text = ordersState.message, color = MaterialTheme.colorScheme.error)
        }
      }
      is UiState.Success -> {
        val filteredOrders = remember(ordersState.data, selectedFilter) {
          ordersState.data.filter { order ->
            val isCompleted = order.status == KitchenStatus.COMPLETED.wireValue
            when (selectedFilter) {
              KitchenFilter.ACTIVE -> !isCompleted
              KitchenFilter.TAKEAWAY_ALL ->
                !isCompleted && (order.orderType == OrderChannel.TAKEAWAY.wireValue ||
                  order.orderType == OrderChannel.PHONE_TAKEAWAY.wireValue)
              KitchenFilter.DINE_IN ->
                !isCompleted && order.orderType == OrderChannel.DINE_IN.wireValue
              KitchenFilter.COMPLETED -> isCompleted
            }
          }
        }

        if (filteredOrders.isEmpty()) {
          EmptyKitchenState()
        } else {
          LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
          ) {
            items(filteredOrders, key = { it.id }) { order ->
              KitchenTicketCard(
                order = order,
                onToggleItemVerified = { idx -> onToggleItemVerified(order, idx) },
                onAdvanceStatus = { status -> onAdvanceStatus(order, status) },
                onCancelQueued = { onCancelQueued(order) },
                onOpenQualityLog = { qualityLogTargetOrder = order }
              )
            }
          }
        }
      }
    }
  }

  qualityLogTargetOrder?.let { order ->
    QualityLogSheet(
      order = order,
      onDismiss = { qualityLogTargetOrder = null },
      onSubmit = { outcome, category, note ->
        onLogQualityEvent(order, outcome, category, note)
        qualityLogTargetOrder = null
      }
    )
  }
}

@Composable
private fun EmptyKitchenState() {
  Box(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.size(72.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(36.dp)
          )
        }
      }
      Text(
        text = stringResource(R.string.empty_kitchen_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground
      )
      Text(
        text = stringResource(R.string.empty_kitchen_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun KitchenTicketCard(
  order: OrderRecord,
  onToggleItemVerified: (Int) -> Unit,
  onAdvanceStatus: (KitchenStatus) -> Unit,
  onCancelQueued: () -> Unit,
  onOpenQualityLog: () -> Unit
) {
  val channel = OrderChannel.fromWire(order.orderType)
  val status = KitchenStatus.fromWire(order.status)
  val isDineIn = channel == OrderChannel.DINE_IN
  val isCompleted = status == KitchenStatus.COMPLETED

  val bannerColor = when (channel) {
    OrderChannel.DINE_IN -> MaterialTheme.colorScheme.secondaryContainer
    OrderChannel.PHONE_TAKEAWAY -> MaterialTheme.colorScheme.tertiaryContainer
    OrderChannel.TAKEAWAY -> MaterialTheme.colorScheme.primaryContainer
  }
  val onBannerColor = when (channel) {
    OrderChannel.DINE_IN -> MaterialTheme.colorScheme.onSecondaryContainer
    OrderChannel.PHONE_TAKEAWAY -> MaterialTheme.colorScheme.onTertiaryContainer
    OrderChannel.TAKEAWAY -> MaterialTheme.colorScheme.onPrimaryContainer
  }
  val channelIcon = when (channel) {
    OrderChannel.TAKEAWAY -> Icons.Default.ShoppingBag
    OrderChannel.PHONE_TAKEAWAY -> Icons.Default.Call
    OrderChannel.DINE_IN -> Icons.Default.LocalDining
  }

  val verifiedCount = order.items.count { it.verified }
  val allVerified = order.items.isNotEmpty() && verifiedCount == order.items.size
  val timeLabel = remember(order.createdAt) {
    val date = order.createdAt?.toDate()
    if (date != null) SimpleDateFormat("h:mm a", Locale.US).format(date) else "Just now"
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(
      width = if (allVerified && !isCompleted) 2.dp else 1.dp,
      color = if (allVerified && !isCompleted) {
        MaterialTheme.colorScheme.secondary
      } else {
        MaterialTheme.colorScheme.outlineVariant
      }
    ),
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("ticket_card_${order.id}")
  ) {
    Column {
      // High-contrast Channel Header prevents Takeaway vs Dine-In mix-ups
      Surface(color = bannerColor, contentColor = onBannerColor) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(imageVector = channelIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
              text = "${order.ticketNumber} • ${channel.label.uppercase(Locale.US)}",
              fontFamily = MonoFontFamily,
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleSmall
            )
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            if (order.isPeakHour) {
              Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(12.dp)
                  )
                  Text(
                    text = "7–9 PM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
            Text(
              text = timeLabel,
              style = MaterialTheme.typography.labelSmall
            )
          }
        }
      }

      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = order.tableOrPhone.ifBlank { if (isDineIn) "Counter Table" else "Takeaway" },
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
            if (order.customerNote.isNotBlank()) {
              Text(
                text = "Kitchen Note: ${order.customerNote}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(10.dp)
          ) {
            Text(
              text = "${status.label} • $verifiedCount/${order.items.size}",
              style = MaterialTheme.typography.labelMedium,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }

        HorizontalDivider()

        // Line-item Packing Verification Checklist (eliminates missing & wrong items)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          order.items.forEachIndexed { idx, line ->
            Surface(
              color = if (line.verified) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
              } else {
                MaterialTheme.colorScheme.surface
              },
              shape = RoundedCornerShape(12.dp),
              onClick = { if (!isCompleted) onToggleItemVerified(idx) },
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = line.verified,
                  onCheckedChange = { if (!isCompleted) onToggleItemVerified(idx) },
                  enabled = !isCompleted
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${line.quantity}x ${line.itemName}",
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (line.verified) TextDecoration.LineThrough else null,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "${line.spiceLevel} • ${line.sauceOption}" +
                      if (line.itemNote.isNotBlank()) " • ${line.itemNote}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (!isCompleted) {
            if (status == KitchenStatus.QUEUED) {
              IconButton(
                onClick = onCancelQueued,
                modifier = Modifier.size(48.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Cancel queued ticket",
                  tint = MaterialTheme.colorScheme.error
                )
              }
              OutlinedButton(
                onClick = { onAdvanceStatus(KitchenStatus.COOKING) },
                modifier = Modifier.weight(1f).height(48.dp)
              ) {
                Text("Start Wok")
              }
            } else if (status == KitchenStatus.COOKING) {
              OutlinedButton(
                onClick = { onAdvanceStatus(KitchenStatus.PACKED) },
                modifier = Modifier.weight(1f).height(48.dp)
              ) {
                Text(if (isDineIn) "Ready to Serve" else "Mark Packed")
              }
            }

            Button(
              onClick = { onAdvanceStatus(KitchenStatus.COMPLETED) },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (allVerified) {
                  MaterialTheme.colorScheme.secondary
                } else {
                  MaterialTheme.colorScheme.primary
                }
              ),
              modifier = Modifier
                .weight(1.25f)
                .height(48.dp)
                .testTag("complete_ticket_${order.id}")
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.size(6.dp))
              Text(if (allVerified) "Verify & Hand Off" else "Complete Pass")
            }
          }

          TextButton(
            onClick = onOpenQualityLog,
            modifier = Modifier.height(48.dp)
          ) {
            Icon(
              imageVector = if (isCompleted) Icons.Default.ReportProblem else Icons.Default.Verified,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(if (isCompleted) "Log Issue" else "Quality Log")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun QualityLogSheet(
  order: OrderRecord,
  onDismiss: () -> Unit,
  onSubmit: (MistakeOutcome, MistakeCategory, String) -> Unit
) {
  var selectedOutcome by remember { mutableStateOf(MistakeOutcome.CAUGHT_AT_PASS) }
  var selectedCategory by remember { mutableStateOf(MistakeCategory.WRONG_SPICE_SAUCE) }
  var note by remember { mutableStateOf("") }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Text(
        text = "Quality & RM 6 Impact • ${order.ticketNumber}",
        style = MaterialTheme.typography.titleLarge
      )
      Text(
        text = "Track whether the digital checklist prevented a RM 6 mistake or if a customer reported an error.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Text("Outcome", style = MaterialTheme.typography.titleSmall)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MistakeOutcome.entries.forEach { outcome ->
          FilterChip(
            selected = selectedOutcome == outcome,
            onClick = { selectedOutcome = outcome },
            label = { Text(outcome.label) }
          )
        }
      }

      Text("Issue Category", style = MaterialTheme.typography.titleSmall)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(
          MistakeCategory.MISSING_ITEM,
          MistakeCategory.WRONG_ITEM,
          MistakeCategory.WRONG_SPICE_SAUCE
        ).forEach { cat ->
          FilterChip(
            selected = selectedCategory == cat,
            onClick = { selectedCategory = cat },
            label = { Text(cat.label) }
          )
        }
      }

      OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("Details (e.g. Caught separate sambal before bag seal)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )

      Button(
        onClick = { onSubmit(selectedOutcome, selectedCategory, note) },
        modifier = Modifier.fillMaxWidth().height(50.dp)
      ) {
        Text("Save Quality Entry")
      }
    }
  }
}
