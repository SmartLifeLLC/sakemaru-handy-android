package biz.smt_life.android.feature.stockdisposal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import biz.smt_life.android.core.domain.model.StockDisposalReason

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDisposalInputScreen(
    state: StockDisposalUiState,
    onBack: () -> Unit,
    onCaseQuantityChange: (String) -> Unit,
    onPieceQuantityChange: (String) -> Unit,
    onReasonSelected: (StockDisposalReason) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismissSubmitResult: () -> Unit
) {
    val item = state.selectedItem ?: return
    var reasonExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A2634))
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "戻る",
                    tint = Color.White
                )
            }
            Text(
                text = "在庫調節入力",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.itemCode,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A2634)
                        )
                        item.displayPackaging?.takeIf { it.isNotEmpty() }?.let { pkg ->
                            Text(pkg, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
                        }
                    }
                    Text(item.displayName ?: item.itemName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    ItemSpecRow(item = item)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val realStock = item.stock.actualQuantity
                        val theoreticalStock = item.stock.theoreticalQuantity
                        StockQuantityText(
                            label = "実在庫",
                            quantity = realStock,
                            modifier = Modifier.weight(1f),
                            valueColor = if (realStock != 0 || theoreticalStock != 0) Color(0xFF1976D2) else Color.Gray
                        )
                        StockQuantityText(
                            label = "理論在庫",
                            quantity = theoreticalStock,
                            modifier = Modifier.weight(1f),
                            valueColor = Color(0xFF1A2634)
                        )
                    }
                    if (item.stock.actualQuantity == 0 && item.stock.theoreticalQuantity == 0) {
                        Text("在庫なし", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.caseQuantity,
                    onValueChange = onCaseQuantityChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("ケース数量") },
                    supportingText = { Text("負数で在庫増") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.pieceQuantity,
                    onValueChange = onPieceQuantityChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("バラ数量") },
                    supportingText = { Text("負数で在庫増") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )
            }

            if (state.totalPieces > 0) {
                Text(
                    text = "総バラ数: ${state.totalPieces}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1976D2)
                )
            }

            ExposedDropdownMenuBox(
                expanded = reasonExpanded,
                onExpandedChange = { reasonExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.reason?.label ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    label = { Text("理由 *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = reasonExpanded,
                    onDismissRequest = { reasonExpanded = false }
                ) {
                    StockDisposalReason.entries.forEach { reason ->
                        DropdownMenuItem(
                            text = { Text(reason.label) },
                            onClick = {
                                onReasonSelected(reason)
                                reasonExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("備考") },
                minLines = 2,
                maxLines = 4
            )

            if (state.submitError != null) {
                Text(
                    text = state.submitError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("戻る")
                }
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f),
                    enabled = state.canSubmit
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp).width(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("確定")
                    }
                }
            }
        }
    }
}
