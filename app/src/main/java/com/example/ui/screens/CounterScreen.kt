package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderChannel
import com.example.data.model.QuickMenuItem
import com.example.data.model.SauceOption
import com.example.data.model.SpiceLevel
import com.example.ui.CartLineDraft
import com.example.ui.OrderDraftState
import com.example.ui.UiState
import com.example.ui.theme.MonoFontFamily
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
  menuItemsState: UiState<List<QuickMenuItem>>,
  draft: OrderDraftState,
  existingOrderCount: Int,
  onSelectChannel: (OrderChannel) -> Unit,
  onTableOrPhoneChange: (String) -> Unit,
  onCustomerNoteChange: (String) -> Unit,
  onTogglePeakHour: (Boolean) -> Unit,
  onQuickTapItem: (QuickMenuItem) -> Unit,
  onAddConfiguredItem: (QuickMenuItem, SpiceLevel, SauceOption, String) -> Unit,
  onUpdateLineQty: (String, Long) -> Unit,
  onUpdateLineModifiers: (String, SpiceLevel, SauceOption, String) -> Unit,
  onClearCart: () -> Unit,
  onSubmitOrder: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  var customizingMenuItem by remember { mutableStateOf<QuickMenuItem?>(null) }
  var editingDraftLine by remember { mutableStateOf<CartLineDraft?>(null) }
  var showCompactCartSheet by remember { mutableStateOf(false) }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isTabletTwoPane = maxWidth >= 720.dp

    if (isTabletTwoPane) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Column(modifier = Modifier.weight(1.35f).fillMaxHeight()) {
          CounterHeaderControls(
            draft = draft,
            onSelectChannel = onSelectChannel,
            onTogglePeakHour = onTogglePeakHour
          )
          Spacer(modifier = Modifier.height(12.dp))
          QuickTapMenuGrid(
            menuItemsState = menuItemsState,
            onQuickTap = onQuickTapItem,
            onCustomizeTap = { customizingMenuItem = it },
            modifier = Modifier.weight(1f)
          )
        }
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          shape = RoundedCornerShape(24.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.weight(0.95f).fillMaxHeight()
        ) {
          OrderCartPanel(
            draft = draft,
            onTableOrPhoneChange = onTableOrPhoneChange,
            onCustomerNoteChange = onCustomerNoteChange,
            onUpdateLineQty = onUpdateLineQty,
            onEditLine = { editingDraftLine = it },
            onClearCart = onClearCart,
            onSubmit = { onSubmitOrder(existingOrderCount) },
            modifier = Modifier.fillMaxSize().padding(16.dp)
          )
        }
      }
    } else {
      Box(modifier = Modifier.fillMaxSize()) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          CounterHeaderControls(
            draft = draft,
            onSelectChannel = onSelectChannel,
            onTogglePeakHour = onTogglePeakHour
          )
          Spacer(modifier = Modifier.height(10.dp))
          QuickTapMenuGrid(
            menuItemsState = menuItemsState,
            onQuickTap = onQuickTapItem,
            onCustomizeTap = { customizingMenuItem = it },
            bottomExtraPadding = if (draft.lines.isNotEmpty()) 92.dp else 12.dp,
            modifier = Modifier.weight(1f)
          )
        }

        AnimatedVisibility(
          visible = draft.lines.isNotEmpty(),
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
          CompactCartBar(
            draft = draft,
            onOpenSheet = { showCompactCartSheet = true },
            onQuickSend = { onSubmitOrder(existingOrderCount) }
          )
        }
      }
    }
  }

  if (showCompactCartSheet) {
    ModalBottomSheet(onDismissRequest = { showCompactCartSheet = false }) {
      OrderCartPanel(
        draft = draft,
        onTableOrPhoneChange = onTableOrPhoneChange,
        onCustomerNoteChange = onCustomerNoteChange,
        onUpdateLineQty = onUpdateLineQty,
        onEditLine = { editingDraftLine = it },
        onClearCart = {
          onClearCart()
          showCompactCartSheet = false
        },
        onSubmit = {
          onSubmitOrder(existingOrderCount)
          showCompactCartSheet = false
        },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp)
      )
    }
  }

  customizingMenuItem?.let { item ->
    val initialSpice = SpiceLevel.entries.firstOrNull { it.wireValue == item.defaultSpice } ?: SpiceLevel.STANDARD
    val initialSauce = SauceOption.entries.firstOrNull { it.wireValue == item.defaultSauce } ?: SauceOption.STANDARD
    ModifierDialogSheet(
      title = item.name,
      subtitle = String.format(Locale.US, "RM %.2f • Customize spice & sauce", item.priceRm),
      initialSpice = initialSpice,
      initialSauce = initialSauce,
      initialNote = "",
      confirmLabel = "Add to Ticket",
      onDismiss = { customizingMenuItem = null },
      onConfirm = { spice, sauce, note ->
        onAddConfiguredItem(item, spice, sauce, note)
        customizingMenuItem = null
      }
    )
  }

  editingDraftLine?.let { line ->
    ModifierDialogSheet(
      title = line.itemName,
      subtitle = "Update spice level, sauce packing, or kitchen note",
      initialSpice = line.spiceLevel,
      initialSauce = line.sauceOption,
      initialNote = line.itemNote,
      confirmLabel = "Update Item",
      onDismiss = { editingDraftLine = null },
      onConfirm = { spice, sauce, note ->
        onUpdateLineModifiers(line.draftId, spice, sauce, note)
        editingDraftLine = null
      }
    )
  }
}

@Composable
private fun CounterHeaderControls(
  draft: OrderDraftState,
  onSelectChannel: (OrderChannel) -> Unit,
  onTogglePeakHour: (Boolean) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      OrderChannel.entries.forEachIndexed { index, channel ->
        val selected = draft.channel == channel
        val icon = when (channel) {
          OrderChannel.TAKEAWAY -> Icons.Default.ShoppingBag
          OrderChannel.PHONE_TAKEAWAY -> Icons.Default.Call
          OrderChannel.DINE_IN -> Icons.Default.LocalDining
        }
        SegmentedButton(
          selected = selected,
          onClick = { onSelectChannel(channel) },
          shape = SegmentedButtonDefaults.itemShape(index = index, count = OrderChannel.entries.size),
          icon = {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
          },
          modifier = Modifier.testTag("channel_${channel.wireValue}")
        ) {
          Text(
            text = channel.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium
          )
        }
      }
    }

    Surface(
      color = if (draft.isPeakHour) {
        MaterialTheme.colorScheme.tertiaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceContainerLow
      },
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = null,
            tint = if (draft.isPeakHour) {
              MaterialTheme.colorScheme.onTertiaryContainer
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(18.dp)
          )
          Column {
            Text(
              text = if (draft.isPeakHour) "7–9 PM Peak Rush Active" else "Standard Shift",
              style = MaterialTheme.typography.labelLarge,
              color = if (draft.isPeakHour) {
                MaterialTheme.colorScheme.onTertiaryContainer
              } else {
                MaterialTheme.colorScheme.onSurface
              }
            )
            Text(
              text = "Tags ticket for peak-hour accuracy analytics",
              style = MaterialTheme.typography.bodySmall,
              color = if (draft.isPeakHour) {
                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              }
            )
          }
        }
        Switch(
          checked = draft.isPeakHour,
          onCheckedChange = onTogglePeakHour,
          modifier = Modifier.testTag("peak_hour_switch")
        )
      }
    }
  }
}

@Composable
private fun QuickTapMenuGrid(
  menuItemsState: UiState<List<QuickMenuItem>>,
  onQuickTap: (QuickMenuItem) -> Unit,
  onCustomizeTap: (QuickMenuItem) -> Unit,
  modifier: Modifier = Modifier,
  bottomExtraPadding: androidx.compose.ui.unit.Dp = 12.dp
) {
  when (menuItemsState) {
    is UiState.Loading -> {
      Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }
    is UiState.Error -> {
      Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(text = menuItemsState.message, color = MaterialTheme.colorScheme.error)
      }
    }
    is UiState.Success -> {
      val topTenItems = remember(menuItemsState.data) {
        val flagged = menuItemsState.data.filter { it.isTopTen }
        (flagged.ifEmpty { menuItemsState.data }).take(10)
      }
      Column(modifier = modifier) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Top 10 Quick-Tap Items",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "Tap to add • Tune for spice",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = 156.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = bottomExtraPadding),
          modifier = Modifier.fillMaxSize()
        ) {
          items(topTenItems, key = { it.id }) { item ->
            QuickTapDishCard(
              item = item,
              onQuickTap = { onQuickTap(item) },
              onCustomize = { onCustomizeTap(item) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun QuickTapDishCard(
  item: QuickMenuItem,
  onQuickTap: () -> Unit,
  onCustomize: () -> Unit
) {
  Card(
    onClick = onQuickTap,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("quick_tap_${item.id}")
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = item.category.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
          )
        }
        IconButton(
          onClick = onCustomize,
          modifier = Modifier
            .size(48.dp)
            .testTag("customize_${item.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Customize spice and sauce for ${item.name}",
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }

      Text(
        text = item.name,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = String.format(Locale.US, "RM %.2f", item.priceRm),
          fontFamily = MonoFontFamily,
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = item.defaultSpice,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1
        )
      }
    }
  }
}

@Composable
private fun CompactCartBar(
  draft: OrderDraftState,
  onOpenSheet: () -> Unit,
  onQuickSend: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    shape = RoundedCornerShape(20.dp),
    shadowElevation = 6.dp,
    onClick = onOpenSheet,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("compact_cart_bar")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${draft.totalItemCount} item(s) • ${draft.channel.label}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = String.format(Locale.US, "RM %.2f • Tap to edit or add phone/table", draft.totalRm),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
        )
      }
      Button(
        onClick = onQuickSend,
        enabled = !draft.isSubmitting,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag("quick_send_order_button")
      ) {
        Text("Send Pass", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun OrderCartPanel(
  draft: OrderDraftState,
  onTableOrPhoneChange: (String) -> Unit,
  onCustomerNoteChange: (String) -> Unit,
  onUpdateLineQty: (String, Long) -> Unit,
  onEditLine: (CartLineDraft) -> Unit,
  onClearCart: () -> Unit,
  onSubmit: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Current Kitchen Ticket",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${draft.channel.label} • ${draft.totalItemCount} items",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary
        )
      }
      if (draft.lines.isNotEmpty()) {
        TextButton(onClick = onClearCart) {
          Text("Clear")
        }
      }
    }

    OutlinedTextField(
      value = draft.tableOrPhone,
      onValueChange = onTableOrPhoneChange,
      label = {
        Text(
          when (draft.channel) {
            OrderChannel.TAKEAWAY -> "Customer Name / Tag (Optional)"
            OrderChannel.PHONE_TAKEAWAY -> "Caller Phone / Name (e.g. 012-3456789)"
            OrderChannel.DINE_IN -> "Table Number (e.g. Table 4)"
          }
        )
      },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("table_or_phone_input")
    )

    OutlinedTextField(
      value = draft.customerNote,
      onValueChange = onCustomerNoteChange,
      label = { Text("Order Note (e.g. Less oil, no cucumber)") },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("customer_note_input")
    )

    HorizontalDivider()

    if (draft.lines.isEmpty()) {
      Box(
        modifier = Modifier
          .weight(1f, fill = false)
          .fillMaxWidth()
          .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Tap items from Top 10 Quick-Tap to build a kitchen pass ticket.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f, fill = false),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(draft.lines, key = { it.draftId }) { line ->
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = line.itemName,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${line.spiceLevel.wireValue} • ${line.sauceOption.wireValue}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.primary
                )
                if (line.itemNote.isNotBlank()) {
                  Text(
                    text = "Note: ${line.itemNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              IconButton(
                onClick = { onEditLine(line) },
                modifier = Modifier.size(48.dp)
              ) {
                Icon(Icons.Default.Tune, contentDescription = "Edit modifiers for ${line.itemName}")
              }
              IconButton(
                onClick = { onUpdateLineQty(line.draftId, -1L) },
                modifier = Modifier.size(48.dp)
              ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
              }
              Text(
                text = "${line.quantity}",
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
              )
              IconButton(
                onClick = { onUpdateLineQty(line.draftId, 1L) },
                modifier = Modifier.size(48.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = "Increase quantity")
              }
            }
          }
        }
      }
    }

    Button(
      onClick = onSubmit,
      enabled = draft.lines.isNotEmpty() && !draft.isSubmitting,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("send_to_kitchen_button")
    ) {
      if (draft.isSubmitting) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          color = MaterialTheme.colorScheme.onPrimary,
          strokeWidth = 2.dp
        )
      } else {
        Text(
          text = String.format(Locale.US, "Send to Kitchen Pass • RM %.2f", draft.totalRm),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ModifierDialogSheet(
  title: String,
  subtitle: String,
  initialSpice: SpiceLevel,
  initialSauce: SauceOption,
  initialNote: String,
  confirmLabel: String,
  onDismiss: () -> Unit,
  onConfirm: (SpiceLevel, SauceOption, String) -> Unit
) {
  var selectedSpice by remember { mutableStateOf(initialSpice) }
  var selectedSauce by remember { mutableStateOf(initialSauce) }
  var note by remember { mutableStateOf(initialNote) }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = title, style = MaterialTheme.typography.titleLarge)
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Text("Spice Level", style = MaterialTheme.typography.titleSmall)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SpiceLevel.entries.forEach { level ->
          FilterChip(
            selected = selectedSpice == level,
            onClick = { selectedSpice = level },
            label = { Text(level.wireValue) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
          )
        }
      }

      Text("Sauce & Sambal Packing", style = MaterialTheme.typography.titleSmall)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SauceOption.entries.forEach { sauce ->
          FilterChip(
            selected = selectedSauce == sauce,
            onClick = { selectedSauce = sauce },
            label = { Text(sauce.wireValue) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
          )
        }
      }

      OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("Item-specific note (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f).height(48.dp)
        ) {
          Text("Cancel")
        }
        Button(
          onClick = { onConfirm(selectedSpice, selectedSauce, note) },
          modifier = Modifier.weight(1f).height(48.dp)
        ) {
          Text(confirmLabel)
        }
      }
    }
  }
}
