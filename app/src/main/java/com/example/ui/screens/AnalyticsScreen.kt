package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MistakeCategory
import com.example.data.model.MistakeLog
import com.example.data.model.MistakeOutcome
import com.example.data.model.OrderRecord
import com.example.ui.AnalyticsSummary
import com.example.ui.UiState
import com.example.ui.theme.MonoFontFamily
import java.util.Locale

@Composable
fun AnalyticsScreen(
  ordersState: UiState<List<OrderRecord>>,
  mistakeLogsState: UiState<List<MistakeLog>>,
  computeAnalytics: (List<OrderRecord>, List<MistakeLog>) -> AnalyticsSummary,
  modifier: Modifier = Modifier
) {
  if (ordersState is UiState.Loading || mistakeLogsState is UiState.Loading) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }

  val orders = (ordersState as? UiState.Success)?.data ?: emptyList()
  val logs = (mistakeLogsState as? UiState.Success)?.data ?: emptyList()
  val summary = remember(orders, logs) { computeAnalytics(orders, logs) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      HeroSavingsCard(summary = summary)
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricTile(
          title = "7–9 PM Peak Rate",
          value = String.format(Locale.US, "%.1f%%", summary.peakMistakeRatePercent),
          subtitle = "vs 50% voice baseline",
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          modifier = Modifier.weight(1f)
        )
        MetricTile(
          title = "Direct Loss",
          value = String.format(Locale.US, "RM %.0f", summary.directMistakeLossRm),
          subtitle = "${summary.mistakesOccurred} mistake(s) @ RM 6",
          containerColor = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      ChannelSplitCard(summary = summary)
    }

    item {
      MistakeCategoryChartCard(summary = summary)
    }

    item {
      Text(
        text = "Recent Quality & Pass Audit Logs",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    if (logs.isEmpty()) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Analytics,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(32.dp)
            )
            Text(
              text = stringResource(R.string.empty_analytics_title),
              style = MaterialTheme.typography.titleSmall
            )
            Text(
              text = stringResource(R.string.empty_analytics_body),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      items(logs.take(15), key = { it.id }) { log ->
        QualityLogRow(log = log)
      }
    }
  }
}

@Composable
private fun HeroSavingsCard(summary: AnalyticsSummary) {
  val totalSaved = summary.caughtSavingsRm + summary.estimatedBaselineSavingsRm
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("hero_savings_card")
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Savings,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
          text = "RM 6 MISTAKE ELIMINATION TRACKER",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer
        )
      }

      Text(
        text = String.format(Locale.US, "RM %.2f Saved", totalSaved),
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer
      )

      Text(
        text = "${summary.caughtAtPass} caught at pass (RM ${summary.caughtSavingsRm.toInt()}) + ${summary.peakHourOrders} peak-hour tickets protected vs. 50% voice error baseline",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
      )

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "RM 1,500 Tablet Setup ROI",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
          )
          Text(
            text = String.format(Locale.US, "%.0f%%", summary.roiProgressFraction * 100f),
            fontFamily = MonoFontFamily,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
          )
        }
        LinearProgressIndicator(
          progress = { summary.roiProgressFraction },
          color = MaterialTheme.colorScheme.secondary,
          trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
          modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
        )
      }
    }
  }
}

@Composable
private fun MetricTile(
  title: String,
  value: String,
  subtitle: String,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = containerColor,
    contentColor = contentColor,
    shape = RoundedCornerShape(18.dp),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(text = title, style = MaterialTheme.typography.labelMedium)
      Text(
        text = value,
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.headlineSmall
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = contentColor.copy(alpha = 0.82f)
      )
    }
  }
}

@Composable
private fun ChannelSplitCard(summary: AnalyticsSummary) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Channel Tracking (${summary.totalOrders} Orders)",
          style = MaterialTheme.typography.titleSmall
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "${summary.peakHourOrders} in 7–9 PM",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ChannelCountPill("Takeaway", summary.takeawayCount, Modifier.weight(1f))
        ChannelCountPill("Phone Order", summary.phoneTakeawayCount, Modifier.weight(1f))
        ChannelCountPill("Dine-In", summary.dineInCount, Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun ChannelCountPill(label: String, count: Int, modifier: Modifier = Modifier) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "$count",
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun MistakeCategoryChartCard(summary: AnalyticsSummary) {
  val caughtColor = MaterialTheme.colorScheme.secondary
  val slippedColor = MaterialTheme.colorScheme.error
  val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

  val rows = listOf(
    Triple("Missing Item", summary.missingItemCaught, summary.missingItemSlipped),
    Triple("Wrong Item", summary.wrongItemCaught, summary.wrongItemSlipped),
    Triple("Spice / Sauce", summary.wrongSpiceCaught, summary.wrongSpiceSlipped)
  )
  val maxVal = rows.maxOf { maxOf(it.second + it.third, 1) }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = "Root-Cause Mistake Breakdown",
        style = MaterialTheme.typography.titleSmall
      )
      Text(
        text = "Green = Caught by checklist (+RM 6) • Red = Slipped (-RM 6)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      rows.forEach { (label, caught, slipped) ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(
              text = "$caught caught / $slipped slipped",
              fontFamily = MonoFontFamily,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Canvas(
            modifier = Modifier
              .fillMaxWidth()
              .height(14.dp)
          ) {
            drawRoundRect(
              color = trackColor,
              size = size,
              cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
            )
            val caughtWidth = (caught.toFloat() / maxVal) * size.width
            val slippedWidth = (slipped.toFloat() / maxVal) * size.width
            if (caughtWidth > 0f) {
              drawRoundRect(
                color = caughtColor,
                topLeft = Offset.Zero,
                size = Size(caughtWidth, size.height),
                cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
              )
            }
            if (slippedWidth > 0f) {
              drawRoundRect(
                color = slippedColor,
                topLeft = Offset(caughtWidth, 0f),
                size = Size(slippedWidth.coerceAtMost(size.width - caughtWidth), size.height),
                cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QualityLogRow(log: MistakeLog) {
  val isCaught = log.outcome == MistakeOutcome.CAUGHT_AT_PASS.wireValue
  val category = MistakeCategory.fromWire(log.category)

  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Icon(
        imageVector = if (isCaught) Icons.Default.CheckCircle else Icons.Default.ReportProblem,
        contentDescription = null,
        tint = if (isCaught) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${log.ticketNumber} • ${category.label}",
          style = MaterialTheme.typography.titleSmall
        )
        if (log.note.isNotBlank()) {
          Text(
            text = log.note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
      Text(
        text = if (isCaught) "+RM 6" else "-RM 6",
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall,
        color = if (isCaught) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
      )
    }
  }
}
