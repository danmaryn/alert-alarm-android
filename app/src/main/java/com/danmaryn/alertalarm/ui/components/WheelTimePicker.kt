package com.danmaryn.alertalarm.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WheelTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onTimeChanged: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp
) {
    val itemHeight = 44.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hours Wheel
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            WheelColumn(
                count = 24,
                initialValue = initialHour,
                itemHeight = itemHeight,
                onValueChanged = { h -> onTimeChanged(h, initialMinute) }
            )
        }

        Text(
            text = ":",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Minutes Wheel
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            WheelColumn(
                count = 60,
                initialValue = initialMinute,
                itemHeight = itemHeight,
                onValueChanged = { m -> onTimeChanged(initialHour, m) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelColumn(
    count: Int,
    initialValue: Int,
    itemHeight: Dp,
    onValueChanged: (Int) -> Unit
) {
    // Repeat items to allow looping feel
    val repeatMultiplier = 100
    val totalItems = count * repeatMultiplier
    val startIndex = (totalItems / 2) - ((totalItems / 2) % count) + initialValue

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val haptic = LocalHapticFeedback.current

    // Detect center item
    val centerIndex = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) 0
            else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItems.minByOrNull {
                    kotlin.math.abs((it.offset + it.size / 2) - viewportCenter)
                }?.index ?: 0
            }
        }
    }

    LaunchedEffect(centerIndex.value) {
        val selectedVal = centerIndex.value % count
        onValueChanged(selectedVal)
        try {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (e: Exception) {
            // Ignore if haptics unavailable
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight * 4.2f),
        contentAlignment = Alignment.Center
    ) {
        // Selection highlight bar (Samsung/iOS style)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(itemHeight)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    RoundedCornerShape(12.dp)
                )
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(totalItems) { index ->
                val itemValue = index % count
                val isSelected = index == centerIndex.value

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", itemValue),
                        fontSize = if (isSelected) 30.sp else 22.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color.LightGray,
                        modifier = Modifier.alpha(if (isSelected) 1f else 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Top Gradient overlay for cylindrical 3D effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * 1.5f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF16181F),
                            Color(0x0016181F)
                        )
                    )
                )
        )

        // Bottom Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * 1.5f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x0016181F),
                            Color(0xFF16181F)
                        )
                    )
                )
        )
    }
}
