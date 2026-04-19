package biz.smt_life.android.feature.inbound.incoming

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val IncomingBodyBg = Color(0xFFF5F5F5)
internal val IncomingHeaderBg = Color(0xFFFDFBF2)
internal val IncomingTitleRed = Color(0xFFC0392B)
internal val IncomingAccentOrange = Color(0xFFE67E22)
internal val IncomingDividerGold = Color(0xFFF9A825)
internal val IncomingBadgeGreen = Color(0xFF27AE60)
internal val IncomingWarningOrange = Color(0xFFF39C12)
internal val IncomingWarningRed = Color(0xFFE74C3C)
internal val IncomingPanelBg = Color.White
internal val IncomingNeutral200 = Color(0xFFE5E5E5)
internal val IncomingNeutral300 = Color(0xFFD4D4D4)
internal val IncomingNeutral400 = Color(0xFFA3A3A3)
internal val IncomingNeutral500 = Color(0xFF737373)
internal val IncomingTextPrimary = Color(0xFF212529)
internal val IncomingReadonlyText = Color(0xFF888888)
internal val IncomingAmber50 = Color(0xFFFFFBEB)
internal val IncomingAmber200 = Color(0xFFFDE68A)
internal val IncomingAmber300 = Color(0xFFFCD34D)
internal val IncomingAmber600 = Color(0xFFD97706)
internal val IncomingAmber700 = Color(0xFFB45309)

@Composable
internal fun IncomingHeaderActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    tint: Color,
    background: Color,
    border: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(8.dp),
        color = background,
        border = BorderStroke(1.dp, border),
        enabled = enabled,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}

@Composable
internal fun IncomingCompactChip(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = Color.White,
    border: Color = IncomingNeutral200,
    contentColor: Color = IncomingNeutral500
) {
    Surface(
        modifier = modifier,
        color = background,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
internal fun IncomingMetricBadge(
    label: String,
    value: Int,
    background: Color,
    contentColor: Color
) {
    Surface(
        color = background,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "$label $value",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}
