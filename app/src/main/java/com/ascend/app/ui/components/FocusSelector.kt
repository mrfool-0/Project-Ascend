package com.ascend.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ascend.app.domain.*
import com.ascend.app.ui.theme.*

@Composable
fun FocusSelector(selected: Set<FocusArea>, onChange: (Set<FocusArea>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AscendCard(Modifier.fillMaxWidth(), highlighted = FocusArea.FULL_BODY in selected, accent = EnergyCyan,
            onClick = { onChange(FocusRules.toggle(selected, FocusArea.FULL_BODY)) }) {
            Text(if (FocusArea.FULL_BODY in selected) "✓  FULL BODY SELECTED" else "FULL BODY", style = MaterialTheme.typography.titleMedium, color = EnergyCyan)
            Spacer(Modifier.height(6.dp))
            Text("Chest · back · shoulders · arms · core · glutes · legs", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Text(if (FocusArea.FULL_BODY in selected) "All seven regions included. Deselect a region to switch to targeted priorities." else "Choose your priorities, or select Full Body for balanced coverage. No four-area limit.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FocusRules.muscles.forEach { area ->
                FilterChip(selected = FocusRules.includes(selected, area), onClick = { onChange(FocusRules.toggle(selected, area)) },
                    label = { Text(area.name.lowercase().replaceFirstChar(Char::uppercase)) })
            }
        }
    }
}
