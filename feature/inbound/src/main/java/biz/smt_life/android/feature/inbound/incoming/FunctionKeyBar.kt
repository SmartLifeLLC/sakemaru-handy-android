package biz.smt_life.android.feature.inbound.incoming

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AccentGreen = Color(0xFF27AE60)

data class FunctionKeyAction(
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun FunctionKeyBar(
    f1: FunctionKeyAction? = null,
    f2: FunctionKeyAction? = null,
    f3: FunctionKeyAction? = null,
    f4: FunctionKeyAction? = null,
    centerAligned: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        if (centerAligned) {
            // ボタン幅を SpaceEvenly の1スロット幅（全体 ÷ 4）に固定して中央揃え
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val slotWidth = (maxWidth - 16.dp) / 4
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(f1, f2, f3, f4).forEach { action ->
                        if (action != null) {
                            FunctionKeyButton(
                                action = action,
                                modifier = Modifier.width(slotWidth)
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FunctionKeyButton(action = f1, modifier = Modifier.weight(1f))
                FunctionKeyButton(action = f2, modifier = Modifier.weight(1f))
                FunctionKeyButton(action = f3, modifier = Modifier.weight(1f))
                FunctionKeyButton(action = f4, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FunctionKeyButton(
    action: FunctionKeyAction?,
    modifier: Modifier = Modifier
) {
    if (action != null) {
        Button(
            onClick = action.onClick,
            modifier = modifier
                .padding(horizontal = 2.dp)
                .height(40.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
        ) {
            Text(
                text = action.label,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    } else {
        Spacer(modifier = modifier.padding(horizontal = 2.dp))
    }
}
