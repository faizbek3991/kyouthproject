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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.QuickMenuItem
import com.example.data.model.SauceOption
import com.example.data.model.SpiceLevel
import com.example.ui.UiState
import com.example.ui.theme.MonoFontFamily
import java.util.Locale

@Composable
fun MenuManageScreen(
  userEmail: String,
  menuItemsState: UiState<List<QuickMenuItem>>,
  onSaveItem: (QuickMenuItem) -> Unit,
  onDeleteItem: (QuickMenuItem) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier
) {
  var editingItem by remember { mutableStateOf<QuickMenuItem?>(null) }
  var isCreatingNew by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.size(48.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Storefront,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Selangor Family Kitchen",
                style = MaterialTheme.typography.titleMedium
              )
              Text(
                text = userEmail.ifBlank { "Google Account Connected" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            OutlinedButton(
              onClick = onSignOut,
              modifier = Modifier.testTag("sign_out_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.size(6.dp))
              Text(stringResource(R.string.sign_out))
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Quick-Tap Menu Items",
            style = MaterialTheme.typography.titleMedium
          )
          Text(
            text = "Top 10 appear on Counter",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      when (menuItemsState) {
        is UiState.Loading -> {
          item {
            Box(
              modifier = Modifier.fillMaxWidth().padding(32.dp),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator()
            }
          }
        }
        is UiState.Error -> {
          item {
            Text(text = menuItemsState.message, color = MaterialTheme.colorScheme.error)
          }
        }
        is UiState.Success -> {
          items(menuItemsState.data, key = { it.id }) { item ->
            MenuManageCard(
              item = item,
              onEdit = {
                isCreatingNew = false
                editingItem = item
              },
              onDelete = { onDeleteItem(item) }
            )
          }
        }
      }
    }

    FloatingActionButton(
      onClick = {
        val nextSort = ((menuItemsState as? UiState.Success)?.data?.size ?: 0) + 1L
        isCreatingNew = true
        editingItem = QuickMenuItem(
          id = "",
          name = "",
          category = "Mains",
          priceRm = 10.0,
          defaultSpice = SpiceLevel.STANDARD.wireValue,
          defaultSauce = SauceOption.STANDARD.wireValue,
          sortOrder = nextSort,
          isTopTen = true
        )
      },
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
        .testTag("add_menu_item_fab")
    ) {
      Icon(imageVector = Icons.Default.Add, contentDescription = "Add quick-tap dish")
    }
  }

  editingItem?.let { item ->
    MenuItemEditorSheet(
      initialItem = item,
      isNew = isCreatingNew,
      onDismiss = { editingItem = null },
      onSave = { updated ->
        onSaveItem(updated)
        editingItem = null
      }
    )
  }
}

@Composable
private fun MenuManageCard(
  item: QuickMenuItem,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(text = item.name, style = MaterialTheme.typography.titleSmall)
          if (item.isTopTen) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "TOP 10",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
        Text(
          text = String.format(
            Locale.US,
            "RM %.2f • %s • %s / %s",
            item.priceRm,
            item.category,
            item.defaultSpice,
            item.defaultSauce
          ),
          fontFamily = MonoFontFamily,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Default.Edit, contentDescription = "Edit ${item.name}")
      }
      IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
        Icon(
          Icons.Default.DeleteOutline,
          contentDescription = "Delete ${item.name}",
          tint = MaterialTheme.colorScheme.error
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MenuItemEditorSheet(
  initialItem: QuickMenuItem,
  isNew: Boolean,
  onDismiss: () -> Unit,
  onSave: (QuickMenuItem) -> Unit
) {
  var name by remember { mutableStateOf(initialItem.name) }
  var priceText by remember { mutableStateOf(String.format(Locale.US, "%.2f", initialItem.priceRm)) }
  var category by remember { mutableStateOf(initialItem.category) }
  var defaultSpice by remember { mutableStateOf(initialItem.defaultSpice) }
  var defaultSauce by remember { mutableStateOf(initialItem.defaultSauce) }
  var isTopTen by remember { mutableStateOf(initialItem.isTopTen) }

  val categories = listOf("Mains", "Noodles", "Sides", "Drinks")

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = if (isNew) "Add Quick-Tap Dish" else "Edit ${initialItem.name}",
        style = MaterialTheme.typography.titleLarge
      )

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Dish Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )

      OutlinedTextField(
        value = priceText,
        onValueChange = { priceText = it },
        label = { Text("Price (RM)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
      )

      Text("Category", style = MaterialTheme.typography.titleSmall)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { cat ->
          FilterChip(
            selected = category == cat,
            onClick = { category = cat },
            label = { Text(cat) }
          )
        }
      }

      Text("Default Spice", style = MaterialTheme.typography.titleSmall)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SpiceLevel.entries.forEach { spice ->
          FilterChip(
            selected = defaultSpice == spice.wireValue,
            onClick = { defaultSpice = spice.wireValue },
            label = { Text(spice.wireValue) }
          )
        }
      }

      Text("Default Sauce", style = MaterialTheme.typography.titleSmall)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SauceOption.entries.forEach { sauce ->
          FilterChip(
            selected = defaultSauce == sauce.wireValue,
            onClick = { defaultSauce = sauce.wireValue },
            label = { Text(sauce.wireValue) }
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Pin to Counter Top 10", style = MaterialTheme.typography.titleSmall)
        Switch(checked = isTopTen, onCheckedChange = { isTopTen = it })
      }

      Button(
        onClick = {
          val parsedPrice = priceText.toDoubleOrNull()?.coerceIn(0.0, 5000.0) ?: initialItem.priceRm
          if (name.trim().isNotEmpty()) {
            onSave(
              initialItem.copy(
                name = name.trim(),
                category = category,
                priceRm = parsedPrice,
                defaultSpice = defaultSpice,
                defaultSauce = defaultSauce,
                isTopTen = isTopTen
              )
            )
          }
        },
        enabled = name.trim().isNotEmpty(),
        modifier = Modifier.fillMaxWidth().height(50.dp)
      ) {
        Text("Save Dish")
      }
    }
  }
}
