package com.teacher.productivitylauncher.presentation.widgets

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WidgetType {
    CLOCK, CLASS_ROUTINE, QUICK_NOTES, WEATHER, CALENDAR
}

enum class WidgetSize {
    SMALL, MEDIUM, LARGE
}

data class WidgetConfig(
    val id: String,
    val type: WidgetType,
    val size: WidgetSize = WidgetSize.MEDIUM,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val isVisible: Boolean = true,
    val transparency: Float = 1f,
    val showBorder: Boolean = false,
    val customTitle: String = "",
    val order: Int = 0,
    val showOnHome: Boolean = true
)

fun WidgetSize.toHeight(): Dp = when (this) {
    WidgetSize.SMALL -> 80.dp
    WidgetSize.MEDIUM -> 160.dp
    WidgetSize.LARGE -> 240.dp
}

fun WidgetSize.toLabel(): String = when (this) {
    WidgetSize.SMALL -> "Small"
    WidgetSize.MEDIUM -> "Medium"
    WidgetSize.LARGE -> "Large"
}

fun WidgetType.toLabel(): String = when (this) {
    WidgetType.CLOCK -> "🕐 Clock"
    WidgetType.CLASS_ROUTINE -> "📚 Class Routine"
    WidgetType.QUICK_NOTES -> "📝 Quick Notes"
    WidgetType.WEATHER -> "🌤️ Weather"
    WidgetType.CALENDAR -> "📅 Calendar"
}