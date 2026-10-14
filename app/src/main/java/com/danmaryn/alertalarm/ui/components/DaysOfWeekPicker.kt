package com.danmaryn.alertalarm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danmaryn.alertalarm.data.model.AppLanguage
import com.danmaryn.alertalarm.ui.i18n.AppStrings

@Composable
fun DaysOfWeekPicker(
    selectedDays: Set<Int>,
    onDaysChanged: (Set<Int>) -> Unit,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val dayLabels = when (lang) {
        AppLanguage.UA -> listOf(1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 7 to "Нд")
        AppLanguage.RU -> listOf(1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 7 to "Вс")
        AppLanguage.EN -> listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S")
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dayLabels.forEach { (dayIndex, label) ->
                val isSelected = selectedDays.contains(dayIndex)
                val isWeekend = dayIndex == 6 || dayIndex == 7

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color(0xFF262C38)
                        )
                        .clickable {
                            val newSet = if (isSelected) {
                                selectedDays - dayIndex
                            } else {
                                selectedDays + dayIndex
                            }
                            onDaysChanged(newSet)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isSelected -> Color.White
                            isWeekend -> Color(0xFFFF8A80)
                            else -> Color.LightGray
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick presets (Samsung One UI style)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedDays == setOf(1, 2, 3, 4, 5, 6, 7),
                onClick = {
                    if (selectedDays == setOf(1, 2, 3, 4, 5, 6, 7)) onDaysChanged(emptySet())
                    else onDaysChanged(setOf(1, 2, 3, 4, 5, 6, 7))
                },
                label = { Text(AppStrings.get("preset_daily", lang), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            FilterChip(
                selected = selectedDays == setOf(1, 2, 3, 4, 5),
                onClick = {
                    if (selectedDays == setOf(1, 2, 3, 4, 5)) onDaysChanged(emptySet())
                    else onDaysChanged(setOf(1, 2, 3, 4, 5))
                },
                label = { Text(AppStrings.get("preset_weekdays", lang), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            FilterChip(
                selected = selectedDays == setOf(6, 7),
                onClick = {
                    if (selectedDays == setOf(6, 7)) onDaysChanged(emptySet())
                    else onDaysChanged(setOf(6, 7))
                },
                label = { Text(AppStrings.get("preset_weekends", lang), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
