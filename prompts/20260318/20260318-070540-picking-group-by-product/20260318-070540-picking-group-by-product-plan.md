# P21 同一商品グルーピング & 一括数量入力 作業計画

## 前提

- P21 OutboundPickingScreen は Portrait/Landscape 切り替え対応済み
- `ProductInfoSection` / `QuantityInputSection` にコンポーネント分離済み
- 現在は `pendingItems`（フラットリスト）の `currentIndex` で1アイテムずつ表示・登録
- API は `POST /api/picking/tasks/{wms_picking_item_result_id}/update` で1件ずつ登録
- 同一商品でも得意先ごとに別エントリとして返却される

---

## Phase 一覧

| # | Phase | 概要 | 完了条件 |
|---|-------|------|---------|
| P1 | データモデル & State 変更 | GroupedPickingItem, CustomerEntry, State 再設計 | 新データクラスが定義され、State のフィールドが更新される。ビルド成功 |
| P2 | ViewModel グルーピング & 振り分けロジック | グルーピング、合計↔個別連動、一括登録 | initialize でグルーピング、振り分け計算、registerGroupedItem が実装される。ビルド成功 |
| P3 | UI — ProductInfoSection 変更 | 得意先名削除、得意先数表示 | 左ペインから得意先名が消え、「N 得意先」が表示される |
| P4 | UI — GroupedQuantitySection | 合計入力 + 得意先リスト + Portrait 対応 | 右ペインに合計入力エリア + 得意先リスト（LazyColumn）が表示される |
| P5 | カウンター & ナビゲーション調整 | グループ数ベースのカウンター、CompletionCard | ヘッダーのカウンターがグループ数ベース。全グループ登録後に CompletionCard 表示 |
| P6 | Preview & ビルド確認 | サンプルデータ、ビルド成功 | グルーピング版の Preview が表示、ビルド成功 |

---

## P1: データモデル & State 変更

### 目的

同一商品の得意先エントリをまとめるためのデータクラスを定義し、`OutboundPickingState` を新しい構造に変更する。

### 修正対象ファイル

- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingState.kt`

### 修正方針

#### 1. 新データクラスを追加

```kotlin
/**
 * 同一商品（itemId）でグルーピングされたピッキングアイテム。
 * 1つの GroupedPickingItem = 1画面で表示される単位。
 */
data class GroupedPickingItem(
    val itemId: Int,
    val itemName: String,
    val janCode: String?,
    val volume: String?,
    val capacityCase: Int?,
    val locationCode: String?,
    val images: List<String>,
    val walkingOrder: Int,
    val customerEntries: List<CustomerEntry>
)

/**
 * 得意先ごとのエントリ。API 登録時の単位。
 * 同一得意先でケースとバラの受注が混在する場合、1つの CustomerEntry にまとめる。
 */
data class CustomerEntry(
    val caseEntry: CustomerEntryDetail?,   // ケース受注（null = ケース受注なし）
    val pieceEntry: CustomerEntryDetail?,  // バラ受注（null = バラ受注なし）
    val customerName: String,
    val slipNumbers: List<Int>             // 関連する伝票番号（ケース・バラで異なる場合あり）
)

data class CustomerEntryDetail(
    val pickingItemResultId: Int,
    val plannedQty: Double,
    val pickedQtyInput: String             // ユーザー入力値
)
```

**注意**: 同一得意先のケース・バラは1行にまとめるため、`CustomerEntry` はケース/バラ両方のフィールドを持つ。
グルーピングは `itemId` → さらに `customerName` でサブグルーピングする。

#### 2. State フィールド変更

```kotlin
data class OutboundPickingState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val originalTask: PickingTask? = null,
    // --- 旧フィールド（削除） ---
    // val pendingItems: List<PickingTaskItem> = emptyList()
    // val currentIndex: Int = 0
    // val pickedQtyInput: String = ""
    // --- 新フィールド ---
    val groupedItems: List<GroupedPickingItem> = emptyList(),
    val currentGroupIndex: Int = 0,
    val totalCaseInput: String = "",
    val totalPieceInput: String = "",
    // --- 既存維持 ---
    val isUpdating: Boolean = false,
    val showCompletionDialog: Boolean = false,
    val isCompleting: Boolean = false,
    val showImageDialog: Boolean = false,
    val warehouseId: Int = 0,
    val warehouseName: String = ""
)
```

#### 3. 計算プロパティ変更

旧: `currentItem`, `canMovePrev`, `canMoveNext`, `totalCount`, `registeredCount`, `quantityTypeLabel`, `canRegister`, `hasImages`

新:
```kotlin
val currentGroup: GroupedPickingItem?
    get() = groupedItems.getOrNull(currentGroupIndex)

val totalGroupCount: Int
    get() = /* originalTask から PENDING items を groupBy(itemId) した数 + 登録済みグループ数 */

val registeredGroupCount: Int
    get() = /* totalGroupCount - groupedItems.size */

val canMovePrev: Boolean
    get() = currentGroupIndex > 0

val canMoveNext: Boolean
    get() = currentGroupIndex < (groupedItems.size - 1)

val canRegister: Boolean
    get() = !isUpdating && currentGroup != null

val hasImages: Boolean
    get() = currentGroup?.images?.isNotEmpty() == true

// ケースエントリの合計受注数
val totalCasePlanned: Double
    get() = currentGroup?.customerEntries
        ?.mapNotNull { it.caseEntry }
        ?.sumOf { it.plannedQty } ?: 0.0

// バラエントリの合計受注数
val totalPiecePlanned: Double
    get() = currentGroup?.customerEntries
        ?.mapNotNull { it.pieceEntry }
        ?.sumOf { it.plannedQty } ?: 0.0
```

#### 4. 旧プロパティの削除/非推奨化

`pendingItems`, `currentIndex`, `pickedQtyInput`, `currentItem`, `isLastItem`, `quantityTypeLabel`, `formatQuantity`, `task` を削除。

### 完了条件

- `GroupedPickingItem`, `CustomerEntry`, `CustomerEntryDetail` が定義される
- `OutboundPickingState` が新フィールドで定義される
- 旧フィールド・計算プロパティが削除される
- ビルドエラーの修正は P2 以降で実施（この時点では ViewModel/Screen でエラーが出る想定）

---

## P2: ViewModel グルーピング & 振り分けロジック

### 目的

ViewModel でグルーピング処理、合計↔個別連動の振り分け計算、一括登録フローを実装する。

### 修正対象ファイル

- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingViewModel.kt`

### 修正方針

#### 1. グルーピング関数

```kotlin
private fun groupPendingItems(items: List<PickingTaskItem>): List<GroupedPickingItem> {
    val pendingItems = items.filter { it.status == ItemStatus.PENDING }
    return pendingItems
        .groupBy { it.itemId }
        .map { (itemId, itemGroup) ->
            val representative = itemGroup.first()
            // 同一得意先のケース・バラをまとめる
            val customerGroups = itemGroup.groupBy { it.customerName ?: "" }
            val customerEntries = customerGroups.map { (customerName, customerItems) ->
                val caseItem = customerItems.find { it.plannedQtyType == QuantityType.CASE }
                val pieceItem = customerItems.find { it.plannedQtyType == QuantityType.PIECE }
                CustomerEntry(
                    customerName = customerName,
                    caseEntry = caseItem?.let {
                        CustomerEntryDetail(
                            pickingItemResultId = it.id,
                            plannedQty = it.plannedQty,
                            pickedQtyInput = String.format("%.0f", it.plannedQty)
                        )
                    },
                    pieceEntry = pieceItem?.let {
                        CustomerEntryDetail(
                            pickingItemResultId = it.id,
                            plannedQty = it.plannedQty,
                            pickedQtyInput = String.format("%.0f", it.plannedQty)
                        )
                    },
                    slipNumbers = customerItems.map { it.slipNumber }
                )
            }
            GroupedPickingItem(
                itemId = itemId,
                itemName = representative.itemName,
                janCode = representative.janCode,
                volume = representative.volume,
                capacityCase = representative.capacityCase,
                locationCode = representative.locationCode,
                images = representative.images,
                walkingOrder = itemGroup.minOf { it.walkingOrder },
                customerEntries = customerEntries
            )
        }
        .sortedBy { it.walkingOrder }
}
```

#### 2. initialize 変更

```kotlin
fun initialize(task: PickingTask) {
    val grouped = groupPendingItems(task.items)
    val firstGroup = grouped.firstOrNull()
    _state.update {
        it.copy(
            originalTask = task,
            groupedItems = grouped,
            currentGroupIndex = 0,
            totalCaseInput = firstGroup?.let { g ->
                String.format("%.0f", g.customerEntries.mapNotNull { it.caseEntry }.sumOf { it.plannedQty })
            } ?: "",
            totalPieceInput = firstGroup?.let { g ->
                String.format("%.0f", g.customerEntries.mapNotNull { it.pieceEntry }.sumOf { it.plannedQty })
            } ?: "",
            warehouseId = tokenManager.getDefaultWarehouseId()
        )
    }
    // warehouse 名を読み込み
    loadWarehouseName()
}
```

#### 3. 合計入力ハンドラ（自動振り分け）

```kotlin
fun onTotalCaseInputChange(value: String) {
    val totalInput = value.toDoubleOrNull() ?: return
    val group = _state.value.currentGroup ?: return
    val maxTotal = _state.value.totalCasePlanned
    val clamped = totalInput.coerceIn(0.0, maxTotal)

    // 上から順に割り当て
    var remaining = clamped
    val updatedEntries = group.customerEntries.map { entry ->
        if (entry.caseEntry != null) {
            val allocated = minOf(remaining, entry.caseEntry.plannedQty)
            remaining -= allocated
            entry.copy(caseEntry = entry.caseEntry.copy(
                pickedQtyInput = String.format("%.0f", allocated)
            ))
        } else entry
    }
    updateCurrentGroupEntries(updatedEntries)
    _state.update { it.copy(totalCaseInput = value) }
}

fun onTotalPieceInputChange(value: String) {
    // 同様のロジック（pieceEntry 版）
}
```

#### 4. 個別入力ハンドラ

```kotlin
fun onCustomerCaseQtyChange(customerIndex: Int, value: String) {
    val group = _state.value.currentGroup ?: return
    val entry = group.customerEntries.getOrNull(customerIndex) ?: return
    val caseEntry = entry.caseEntry ?: return
    // 受注数を超えないようクランプ
    val updatedEntries = group.customerEntries.toMutableList()
    updatedEntries[customerIndex] = entry.copy(
        caseEntry = caseEntry.copy(pickedQtyInput = value)
    )
    updateCurrentGroupEntries(updatedEntries)
    // 合計を再計算
    recalculateTotalCase()
}

fun onCustomerPieceQtyChange(customerIndex: Int, value: String) {
    // 同様
}
```

#### 5. 一括登録

```kotlin
fun registerGroupedItem() {
    viewModelScope.launch {
        _state.update { it.copy(isUpdating = true) }
        val group = _state.value.currentGroup ?: return@launch

        try {
            // 全得意先エントリをまとめて登録
            for (entry in group.customerEntries) {
                entry.caseEntry?.let { caseDetail ->
                    val qty = caseDetail.pickedQtyInput.toDoubleOrNull() ?: 0.0
                    pickingTaskRepository.updatePickingItem(
                        resultId = caseDetail.pickingItemResultId,
                        pickedQty = qty,
                        pickedQtyType = QuantityType.CASE.name
                    )
                }
                entry.pieceEntry?.let { pieceDetail ->
                    val qty = pieceDetail.pickedQtyInput.toDoubleOrNull() ?: 0.0
                    pickingTaskRepository.updatePickingItem(
                        resultId = pieceDetail.pickingItemResultId,
                        pickedQty = qty,
                        pickedQtyType = QuantityType.PIECE.name
                    )
                }
            }
            // 成功 → リフレッシュ
            refreshTaskFromServer(_state.value.originalTask!!.taskId)
            moveToNextGroupOrComplete()
        } catch (e: Exception) {
            _state.update { it.copy(errorMessage = mapErrorMessage(e)) }
        } finally {
            _state.update { it.copy(isUpdating = false) }
        }
    }
}
```

#### 6. ナビゲーション

```kotlin
fun moveToPrevGroup() {
    if (_state.value.canMovePrev) {
        val newIndex = _state.value.currentGroupIndex - 1
        _state.update { it.copy(currentGroupIndex = newIndex) }
        loadGroupTotals(newIndex)
    }
}

fun moveToNextGroup() {
    if (_state.value.canMoveNext) {
        val newIndex = _state.value.currentGroupIndex + 1
        _state.update { it.copy(currentGroupIndex = newIndex) }
        loadGroupTotals(newIndex)
    }
}

private fun moveToNextGroupOrComplete() {
    val grouped = groupPendingItems(_state.value.originalTask!!.items)
    if (grouped.isEmpty()) {
        // 全グループ完了
        _state.update { it.copy(groupedItems = emptyList(), currentGroupIndex = 0) }
    } else {
        val newIndex = 0  // リフレッシュ後は先頭に戻る
        _state.update { it.copy(groupedItems = grouped, currentGroupIndex = newIndex) }
        loadGroupTotals(newIndex)
    }
}
```

### 完了条件

- `initialize` でグルーピングが動作
- `onTotalCaseInputChange`/`onTotalPieceInputChange` で自動振り分けが動作
- `onCustomerCaseQtyChange`/`onCustomerPieceQtyChange` で個別入力→合計再計算が動作
- `registerGroupedItem` で全エントリ一括登録が動作
- ビルド成功

---

## P3: UI — ProductInfoSection 変更

### 目的

左ペイン（`ProductInfoSection`）から得意先名を削除し、代わりに得意先数を表示する。データソースを `GroupedPickingItem` に変更。

### 修正対象ファイル

- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingScreen.kt`

### 修正方針

```kotlin
@Composable
private fun ProductInfoSection(
    group: GroupedPickingItem,       // 変更: PickingTaskItem → GroupedPickingItem
    hasImages: Boolean,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 商品名、JAN/容量/入数 → group から取得
    // 得意先名 → 削除。代わりに「${group.customerEntries.size} 得意先」を表示
    // ロケーション → group.locationCode
    // 伝票番号 → 複数あるので表示しない or 先頭の伝票番号
}
```

### 完了条件

- 得意先名が表示されない
- 「N 得意先」が表示される
- ロケーション/商品情報は従来通り表示

---

## P4: UI — GroupedQuantitySection

### 目的

右ペイン（旧 `QuantityInputSection`）を `GroupedQuantitySection` に変更。合計入力エリア + 得意先別リストを表示する。

### 修正対象ファイル

- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingScreen.kt`

### 修正方針

```kotlin
@Composable
private fun GroupedQuantitySection(
    state: OutboundPickingState,
    onTotalCaseInputChange: (String) -> Unit,
    onTotalPieceInputChange: (String) -> Unit,
    onCustomerCaseQtyChange: (Int, String) -> Unit,
    onCustomerPieceQtyChange: (Int, String) -> Unit,
    onRegisterClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(10.dp)) {
        // === 合計入力エリア ===
        // ケース合計: OutlinedTextField + "/ ${totalCasePlanned}"
        // バラ合計: OutlinedTextField + "/ ${totalPiecePlanned}"
        // (ケースまたはバラが0の場合は非表示)

        Spacer(Modifier.height(8.dp))

        // === 得意先別リスト ===
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(state.currentGroup?.customerEntries ?: emptyList()) { index, entry ->
                CustomerEntryRow(
                    entry = entry,
                    onCaseQtyChange = { onCustomerCaseQtyChange(index, it) },
                    onPieceQtyChange = { onCustomerPieceQtyChange(index, it) },
                    isUpdating = state.isUpdating
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // === 登録・履歴ボタン ===
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRegisterClick, ...) { Text("登録") }
            Button(onClick = onHistoryClick, ...) { Text("履歴") }
        }
    }
}
```

**CustomerEntryRow**: 1行に得意先名 + ケース入力(あれば) + バラ入力(あれば) を表示。

```kotlin
@Composable
private fun CustomerEntryRow(
    entry: CustomerEntry,
    onCaseQtyChange: (String) -> Unit,
    onPieceQtyChange: (String) -> Unit,
    isUpdating: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 得意先名（固定幅）
        Text(
            text = entry.customerName,
            modifier = Modifier.width(100.dp),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // ケース入力（あれば）
        entry.caseEntry?.let { caseDetail ->
            Text("ケース", fontSize = 11.sp, color = Neutral500)
            OutlinedTextField(
                value = caseDetail.pickedQtyInput,
                onValueChange = onCaseQtyChange,
                enabled = !isUpdating,
                modifier = Modifier.width(60.dp).height(36.dp),
                ...
            )
            Text("/${String.format("%.0f", caseDetail.plannedQty)}", fontSize = 12.sp)
        }
        // バラ入力（あれば）
        entry.pieceEntry?.let { pieceDetail ->
            Text("バラ", fontSize = 11.sp, color = Neutral500)
            OutlinedTextField(
                value = pieceDetail.pickedQtyInput,
                onValueChange = onPieceQtyChange,
                enabled = !isUpdating,
                modifier = Modifier.width(60.dp).height(36.dp),
                ...
            )
            Text("/${String.format("%.0f", pieceDetail.plannedQty)}", fontSize = 12.sp)
        }
    }
}
```

### Portrait 対応

- Portrait 時も同じ `GroupedQuantitySection` を使用
- 横幅が広いため得意先リストが見やすくなる
- 既存の `isPortrait` 分岐（`OutboundPickingBody`）はそのまま維持

### 完了条件

- 合計入力エリア（ケース/バラ）が表示される
- 得意先リストが LazyColumn で表示される
- 各行でケース/バラの個別入力が可能
- 登録・履歴ボタンが下部に表示される
- Portrait / Landscape 両方で正常表示

---

## P5: カウンター & ナビゲーション調整

### 目的

ヘッダーのカウンターをグループ数ベースに変更し、`OutboundPickingBody` の呼び出しを新しい State に合わせる。CompletionCard も調整。

### 修正対象ファイル

- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingScreen.kt`

### 修正方針

#### ヘッダーカウンター

```kotlin
// 旧: "${state.registeredCount} / ${state.totalCount}"
// 新: "${state.registeredGroupCount} / ${state.totalGroupCount}"
Text(
    text = "${state.registeredGroupCount} / ${state.totalGroupCount}",
    ...
)
```

#### OutboundPickingScreen の分岐

```kotlin
when {
    state.isLoading -> { /* ローディング */ }
    state.currentGroup != null && state.originalTask != null -> {
        // 旧: state.currentItem != null
        OutboundPickingBody(...)
    }
    else -> {
        // CompletionCard（変更なし — isFullyProcessed 等のロジックはそのまま）
    }
}
```

#### OutboundPickingBody の引数変更

旧:
```kotlin
OutboundPickingBody(state, isPortrait, onPickedQtyChange, onImageClick, onRegisterClick, onHistoryClick)
```

新:
```kotlin
OutboundPickingBody(
    state = state,
    isPortrait = isPortrait,
    onTotalCaseInputChange = viewModel::onTotalCaseInputChange,
    onTotalPieceInputChange = viewModel::onTotalPieceInputChange,
    onCustomerCaseQtyChange = viewModel::onCustomerCaseQtyChange,
    onCustomerPieceQtyChange = viewModel::onCustomerPieceQtyChange,
    onImageClick = { viewModel.showImageDialog() },
    onRegisterClick = viewModel::registerGroupedItem,
    onHistoryClick = onNavigateToHistory,
    modifier = Modifier.padding(padding)
)
```

### 完了条件

- ヘッダーカウンターがグループ数ベース
- `currentGroup` が null の場合に CompletionCard が表示される
- Body の引数が新しいハンドラに対応
- ビルド成功

---

## P6: Preview & ビルド確認

### 目的

グルーピング版のサンプルデータで Preview を作成し、全体のビルド成功を確認する。

### 修正対象ファイル

- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingScreen.kt`

### 修正方針

#### サンプルデータ

```kotlin
// 同一 itemId=300 で得意先A（ケース5）、得意先B（ケース5+バラ3）、得意先C（バラ5）
val groupedItem = GroupedPickingItem(
    itemId = 300,
    itemName = "サッポロ生ビール黒ラベル 500ml缶",
    janCode = "4901773000001",
    volume = "500ml",
    capacityCase = 24,
    locationCode = "A-01-03",
    images = emptyList(),
    walkingOrder = 3000,
    customerEntries = listOf(
        CustomerEntry(
            customerName = "居酒屋A",
            caseEntry = CustomerEntryDetail(1, 5.0, "5"),
            pieceEntry = null,
            slipNumbers = listOf(2023121700)
        ),
        CustomerEntry(
            customerName = "レストランB",
            caseEntry = CustomerEntryDetail(2, 5.0, "5"),
            pieceEntry = CustomerEntryDetail(3, 3.0, "3"),
            slipNumbers = listOf(2023121701, 2023121702)
        ),
        CustomerEntry(
            customerName = "ホテルC",
            caseEntry = null,
            pieceEntry = CustomerEntryDetail(4, 5.0, "5"),
            slipNumbers = listOf(2023121703)
        )
    )
)
```

#### Preview

Landscape + Portrait 両方の Preview を更新。

#### ビルド確認

```bash
./gradlew :feature:outbound:compileDebugKotlin
```

### 完了条件

- Landscape Preview で左右ペイン + 得意先リストが表示される
- Portrait Preview で上下セクション + 得意先リストが表示される
- `./gradlew :feature:outbound:compileDebugKotlin` がビルド成功

---

## 制約（厳守）

1. **API 変更なし**: 既存の `updatePickingItem` API を得意先ごとに順次呼ぶ
2. **domain モデル変更なし**: `PickingTask`, `PickingTaskItem` は変更しない
3. **network 層変更なし**: `PickingTaskRepositoryImpl` は変更しない
4. **一括登録**: 全得意先エントリをまとめて API 呼び出し（途中キャンセル不可）
5. **Portrait 互換**: 既存の Portrait/Landscape 切り替え機能を維持
6. **合計制約**: 合計入力値 ≤ 各得意先の受注数合算
7. **個別制約**: 各得意先の入力値 ≤ その得意先の受注数
8. **合計→個別上書き**: 合計変更時は個別修正を上書き（上から順に充足）

## 全体完了条件

- 同一商品が1画面にグルーピングされる
- 合計入力 → 上から自動振り分けが動作する
- 個別入力 → 合計再計算が動作する
- 一括登録で全得意先が API 登録される
- カウンターがグループ数ベースで表示される
- Portrait / Landscape 両方で正常表示
- ビルド成功
