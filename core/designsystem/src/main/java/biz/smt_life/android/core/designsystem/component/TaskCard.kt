package biz.smt_life.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import biz.smt_life.android.core.designsystem.theme.HandyTheme

data class TaskItem(
    val clientAndPerson: String,
    val area: String,
    val current: Int,
    val total: Int,
    val startTime: String,
    val endTime: String,
    val status: String
)

@Composable
fun TaskCard(
    item: TaskItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isCompleted = item.status == "完了"
    val progress = if (item.total > 0) item.current.toFloat() / item.total else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column {
            // ── Header ──
            TaskCardHeader(item = item, isCompleted = isCompleted)

            // ── Body ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Area tag
                AreaTag(area = item.area)

                // Progress
                ProgressSection(current = item.current, total = item.total, progress = progress, isCompleted = isCompleted)

                // Timeline
                TimelineFooter(startTime = item.startTime, endTime = item.endTime)
            }
        }
    }
}

// ── Header ──

@Composable
private fun TaskCardHeader(item: TaskItem, isCompleted: Boolean) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: icon + client/person
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Truck icon with blue background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalShipping,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Parse clientAndPerson: "[荷主名]担当者名"
            val (client, person) = parseClientAndPerson(item.clientAndPerson)
            Column {
                if (client.isNotBlank()) {
                    Text(
                        text = client,
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
                Text(
                    text = person,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }
        }

        // Right: status badge
        StatusBadge(status = item.status, isCompleted = isCompleted)
    }
}

@Composable
private fun StatusBadge(status: String, isCompleted: Boolean) {
    val badgeBg = if (isCompleted) {
        Color(0xFF4CAF50).copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
    val badgeText = if (isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
    val badgeIcon = if (isCompleted) Icons.Rounded.CheckCircle else null

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(badgeBg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (badgeIcon != null) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = badgeText,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = status,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = badgeText
        )
    }
}

// ── Body parts ──

@Composable
private fun AreaTag(area: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "エリア: $area",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProgressSection(current: Int, total: Int, progress: Float, isCompleted: Boolean) {
    val progressColor = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Label row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Inventory2,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "検品",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)) {
                        append("$current")
                    }
                    append(" / $total 件")
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(progressColor)
            )
        }
    }
}

@Composable
private fun TimelineFooter(startTime: String, endTime: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Start
        Column {
            Text(
                text = "START",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Text(
                text = startTime,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Divider line
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        )

        // End
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "END",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Text(
                text = endTime,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Utility ──

private fun parseClientAndPerson(text: String): Pair<String, String> {
    val closeBracket = text.indexOf(']')
    return if (closeBracket > 0) {
        val client = text.substring(0, closeBracket + 1) // e.g. "[華むすびの蔵]"
        val person = text.substring(closeBracket + 1).trim()
        client to person
    } else {
        "" to text
    }
}

// ── Previews ──

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun PreviewTaskCardInProgress() {
    HandyTheme(darkTheme = false) {
        TaskCard(
            item = TaskItem(
                clientAndPerson = "[華むすびの蔵]横山 正和",
                area = "常温",
                current = 55,
                total = 100,
                startTime = "03/24 19:58",
                endTime = "03/26 13:59",
                status = "作業中"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun PreviewTaskCardCompleted() {
    HandyTheme(darkTheme = false) {
        TaskCard(
            item = TaskItem(
                clientAndPerson = "[華むすびの蔵]横山 正和",
                area = "冷凍",
                current = 1,
                total = 1,
                startTime = "03/24 19:58",
                endTime = "03/26 13:59",
                status = "完了"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
