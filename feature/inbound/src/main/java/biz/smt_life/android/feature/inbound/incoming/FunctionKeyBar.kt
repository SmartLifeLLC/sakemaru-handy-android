package biz.smt_life.android.feature.inbound.incoming

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FunctionKeyButton(key = "F1", action = f1, modifier = Modifier.weight(1f))
            FunctionKeyButton(key = "F2", action = f2, modifier = Modifier.weight(1f))
            FunctionKeyButton(key = "F3", action = f3, modifier = Modifier.weight(1f))
            FunctionKeyButton(key = "F4", action = f4, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FunctionKeyButton(
    key: String,
    action: FunctionKeyAction?,
    modifier: Modifier = Modifier
) {
    if (action != null) {
        Button(
            onClick = action.onClick,
            modifier = modifier
                .padding(horizontal = 2.dp)
                .height(40.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Text(
                text = "${key}:${action.label}",
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    } else {
        Spacer(modifier = modifier.padding(horizontal = 2.dp))
    }
}
