package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusAbsentBg
import com.example.ui.theme.StatusAbsentText
import com.example.ui.theme.StatusAtRisk
import com.example.ui.theme.StatusAtRiskBg
import com.example.ui.theme.StatusExcused
import com.example.ui.theme.StatusExcusedBg
import com.example.ui.theme.StatusExcusedText
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusLateBg
import com.example.ui.theme.StatusLateText
import com.example.ui.theme.StatusPresent
import com.example.ui.theme.StatusPresentBg
import com.example.ui.theme.StatusPresentText

@Composable
fun AttendanceStatusBadge(
    status: AttendanceStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val (bg, textColor) = when (status) {
        AttendanceStatus.PRESENT -> StatusPresentBg to StatusPresentText
        AttendanceStatus.ABSENT -> StatusAbsentBg to StatusAbsentText
        AttendanceStatus.LATE -> StatusLateBg to StatusLateText
        AttendanceStatus.EXCUSED -> StatusExcusedBg to StatusExcusedText
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 4.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 6.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when (status) {
                            AttendanceStatus.PRESENT -> StatusPresent
                            AttendanceStatus.ABSENT -> StatusAbsent
                            AttendanceStatus.LATE -> StatusLate
                            AttendanceStatus.EXCUSED -> StatusExcused
                        }
                    )
            )
            Text(
                text = if (compact) status.shortLabel else status.label,
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
fun AttendanceStatusPillButton(
    status: AttendanceStatus,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = when (status) {
        AttendanceStatus.PRESENT -> StatusPresent
        AttendanceStatus.ABSENT -> StatusAbsent
        AttendanceStatus.LATE -> StatusLate
        AttendanceStatus.EXCUSED -> StatusExcused
    }

    val (bg, textColor) = if (isSelected) {
        activeColor to Color.White
    } else {
        when (status) {
            AttendanceStatus.PRESENT -> StatusPresentBg.copy(alpha = 0.5f) to StatusPresentText
            AttendanceStatus.ABSENT -> StatusAbsentBg.copy(alpha = 0.5f) to StatusAbsentText
            AttendanceStatus.LATE -> StatusLateBg.copy(alpha = 0.5f) to StatusLateText
            AttendanceStatus.EXCUSED -> StatusExcusedBg.copy(alpha = 0.5f) to StatusExcusedText
        }
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = bg,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = status.shortLabel,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun PercentageBadge(
    percentage: Float,
    targetPercent: Int = 75,
    modifier: Modifier = Modifier
) {
    val isGood = percentage >= targetPercent
    val bgColor = if (isGood) StatusPresentBg else StatusAtRiskBg
    val textColor = if (isGood) StatusPresentText else StatusAtRisk

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = String.format(java.util.Locale.US, "%.1f%%", percentage),
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
