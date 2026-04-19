package biz.smt_life.android.feature.outbound.proxyshipment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import biz.smt_life.android.core.domain.model.ProxyShipmentStatus

internal const val PROXY_PREF_NAME_ORIENTATION = "p21_orientation_prefs"
internal const val PROXY_PREF_KEY_IS_PORTRAIT = "p21_is_portrait"

internal val BackgroundCream = Color.White
internal val TitleRed = Color(0xFFC0392B)
internal val AccentOrange = Color(0xFFE67E22)
internal val DividerGold = Color(0xFFF9A825)
internal val HeaderBg = Color(0xFFFDFBF2)
internal val BadgeGreen = Color(0xFF27AE60)
internal val ProgressGray = Color(0xFFE0E0E0)
internal val TimerBg = Color(0xFFF0F0F0)
internal val BodyBg = Color(0xFFF5F5F5)
internal val Amber50 = Color(0xFFFFFBEB)
internal val Amber200 = Color(0xFFFDE68A)
internal val Amber300 = Color(0xFFFCD34D)
internal val Amber600 = Color(0xFFD97706)
internal val Amber700 = Color(0xFFB45309)
internal val Neutral200 = Color(0xFFE5E5E5)
internal val Neutral300 = Color(0xFFD4D4D4)
internal val Neutral400 = Color(0xFFA3A3A3)
internal val Neutral500 = Color(0xFF737373)
internal val ReadonlyText = Color(0xFF888888)

internal data class ProxyShipmentCardColors(
    val background: Color,
    val border: Color,
    val titleColor: Color,
    val backgroundPressed: Color,
    val borderPressed: Color
)

internal data class ProxyShipmentStatusPalette(
    val container: Color,
    val content: Color
)

internal fun proxyShipmentCardColors(status: ProxyShipmentStatus): ProxyShipmentCardColors = when (status) {
    ProxyShipmentStatus.RESERVED -> ProxyShipmentCardColors(
        background = Color(0xFFFFFDE7),
        border = Color(0xFFF9A825),
        titleColor = Color(0xFFE67E22),
        backgroundPressed = Color(0xFFFFF9C4),
        borderPressed = Color(0xFFF57F17)
    )

    ProxyShipmentStatus.PICKING -> ProxyShipmentCardColors(
        background = Color(0xFFE8F5E9),
        border = Color(0xFF4CAF50),
        titleColor = Color(0xFF2E7D32),
        backgroundPressed = Color(0xFFC8E6C9),
        borderPressed = Color(0xFF388E3C)
    )

    ProxyShipmentStatus.FULFILLED -> ProxyShipmentCardColors(
        background = Color(0xFFF1F8E9),
        border = Color(0xFF7CB342),
        titleColor = Color(0xFF558B2F),
        backgroundPressed = Color(0xFFDCEDC8),
        borderPressed = Color(0xFF689F38)
    )

    ProxyShipmentStatus.SHORTAGE -> ProxyShipmentCardColors(
        background = Color(0xFFFFF3E0),
        border = Color(0xFFEF6C00),
        titleColor = Color(0xFFD84315),
        backgroundPressed = Color(0xFFFFE0B2),
        borderPressed = Color(0xFFE65100)
    )
}

internal fun proxyShipmentStatusPalette(status: ProxyShipmentStatus): ProxyShipmentStatusPalette = when (status) {
    ProxyShipmentStatus.RESERVED -> ProxyShipmentStatusPalette(
        container = Color(0xFFFFF3E0),
        content = Color(0xFFEF6C00)
    )

    ProxyShipmentStatus.PICKING -> ProxyShipmentStatusPalette(
        container = Color(0xFFE8F5E9),
        content = Color(0xFF2E7D32)
    )

    ProxyShipmentStatus.FULFILLED -> ProxyShipmentStatusPalette(
        container = Color(0xFFE8F5E9),
        content = Color(0xFF2E7D32)
    )

    ProxyShipmentStatus.SHORTAGE -> ProxyShipmentStatusPalette(
        container = Color(0xFFFFEBEE),
        content = Color(0xFFC62828)
    )
}

@Composable
internal fun ProxyShipmentStatusBadge(
    status: ProxyShipmentStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val palette = proxyShipmentStatusPalette(status)
    Surface(
        modifier = modifier,
        color = palette.container,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(if (compact) 10.dp else 20.dp),
        border = BorderStroke(1.dp, palette.content.copy(alpha = 0.2f))
    ) {
        Text(
            text = status.label,
            modifier = Modifier.padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 3.dp else 4.dp),
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            color = palette.content
        )
    }
}

internal fun formatProxyTimer(seconds: Long): String {
    val minutes = (seconds / 60).toString().padStart(2, '0')
    val remainSeconds = (seconds % 60).toString().padStart(2, '0')
    return "$minutes:$remainSeconds"
}
