# P21 出庫ピッキング — 同一商品グルーピング & 一括数量入力

- **作成日**: 2026-03-18
- **ステータス**: ドラフト
- **ディレクトリ**: `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/`

## 背景・目的

### 現状の問題

現在のP21画面は、同じ商品でも得意先ごとに1件ずつ数量入力を行う。
例えば「サッポロ生ビール 500ml」が得意先A・B・C に配送される場合、ピッカーは同じ商品を3回ピッキングする必要がある。

これにより:
- ピッカーが同じロケーションを何度も往復する
- 作業効率が大幅に低下する
- 同一商品の合計を暗算する必要がある

### 改善後

同じ商品（itemId が同一）を1画面にまとめて表示し、合計数量を入力 → 得意先別に自動振り分けする。

## 現状の実装

### データフロー

1. API: `GET /api/picking/tasks` → `PickingTaskResponse` で `picking_list` に全アイテムが返る
2. 各アイテムは `wms_picking_item_result_id` で一意。同じ商品でも得意先ごとに別エントリ
3. ViewModel: `pendingItems = task.items.filter { it.status == PENDING }` でフラットリスト化
4. UI: `currentIndex` で1件ずつ表示、「登録」で `POST /api/picking/tasks/{id}/update` を呼ぶ

### 関連モデル

```kotlin
data class PickingTaskItem(
    val id: Int,                    // wms_picking_item_result_id（API登録キー）
    val itemId: Int,                // 商品ID（同一商品で共通）
    val itemName: String,
    val janCode: String?,
    val volume: String?,
    val capacityCase: Int?,
    val plannedQtyType: QuantityType,  // CASE or PIECE
    val plannedQty: Double,
    val pickedQty: Double,
    val status: ItemStatus,
    val slipNumber: Int,
    val customerName: String?,
    val locationCode: String?,
    val walkingOrder: Int,
    val images: List<String>,
    ...
)
```

### API

- 登録: `POST /api/picking/tasks/{wms_picking_item_result_id}/update`
  - Body: `{ picked_qty: String, picked_qty_type: String }`
  - **1アイテム（=1得意先）ごとに呼ぶ**

## 変更内容

### 概要

同じ商品（`itemId` が同一）の PENDING アイテムを1画面にグルーピングし、合計数量の入力 + 得意先別の個別入力を両立するUIに変更する。合計入力時は欠品を自動で上部の得意先から引く。

### 詳細設計

#### 画面レイアウト（Landscape）

```
┌─────────────────────────────┬──────────────────────────────────┐
│  【左ペイン: 商品情報】       │  【右ペイン: 得意先別数量リスト】   │
│                             │                                  │
│  商品名                      │  ┌─ 合計入力エリア ──────────────┐ │
│  JAN / 容量 / 入数           │  │ ケース合計: [__10__] / 15     │ │
│  ロケーション: A-01-03       │  │ バラ合計:   [__5___] / 8      │ │
│  画像ボタン                  │  └──────────────────────────────┘ │
│                             │                                  │
│                             │  ┌─ 得意先リスト ────────────────┐ │
│                             │  │ 得意先A  ケース [5]/5  バラ[-]/0│ │
│                             │  │ 得意先B  ケース [3]/5  バラ [3]/3│ │
│                             │  │ 得意先C  ケース [2]/5  バラ [2]/5│ │
│                             │  └──────────────────────────────┘ │
│                             │                                  │
│                             │  [ 登録 ]        [ 履歴 ]         │
└─────────────────────────────┴──────────────────────────────────┘
```

#### 画面レイアウト（Portrait）

```
┌──────────────────────┐
│ 【上部: 商品情報 30%】 │
│  商品名               │
│  JAN / 容量 / 入数    │
│  ロケーション         │
├──────────────────────┤
│ 【下部: 数量入力 70%】 │
│                      │
│ ケース合計: [10] / 15 │
│ バラ合計:   [5] / 8   │
│                      │
│ 得意先A ケース[5]/5   │
│ 得意先B ケース[3]/5   │
│ 得意先C ケース[2]/5   │
│                      │
│ [ 登録 ]   [ 履歴 ]   │
└──────────────────────┘
```

#### 1. グルーピングロジック（ViewModel）

PENDING アイテムを `itemId` でグルーピングし、グループ単位でナビゲーションする。

```kotlin
// 新しいデータクラス
data class GroupedPickingItem(
    val itemId: Int,
    val itemName: String,
    val janCode: String?,
    val volume: String?,
    val capacityCase: Int?,
    val locationCode: String?,
    val images: List<String>,
    val walkingOrder: Int,          // グループ内の最小 walkingOrder
    val customerEntries: List<CustomerEntry>
)

data class CustomerEntry(
    val pickingItemResultId: Int,    // API 登録用キー
    val customerName: String,
    val slipNumber: Int,
    val plannedQtyType: QuantityType,
    val plannedQty: Double,
    val pickedQtyInput: String,      // ユーザー入力値
)
```

**グルーピング処理**:

```kotlin
fun groupPendingItems(pendingItems: List<PickingTaskItem>): List<GroupedPickingItem> {
    return pendingItems
        .groupBy { it.itemId }
        .map { (itemId, items) ->
            val representative = items.first()
            GroupedPickingItem(
                itemId = itemId,
                itemName = representative.itemName,
                janCode = representative.janCode,
                volume = representative.volume,
                capacityCase = representative.capacityCase,
                locationCode = representative.locationCode,
                images = representative.images,
                walkingOrder = items.minOf { it.walkingOrder },
                customerEntries = items.map { item ->
                    CustomerEntry(
                        pickingItemResultId = item.id,
                        customerName = item.customerName ?: "",
                        slipNumber = item.slipNumber,
                        plannedQtyType = item.plannedQtyType,
                        plannedQty = item.plannedQty,
                        pickedQtyInput = String.format("%.0f", item.plannedQty)
                    )
                }
            )
        }
        .sortedBy { it.walkingOrder }
}
```

#### 2. 合計数量入力と自動振り分け

**合計入力エリア**:
- ケース合計: 全得意先のケース受注数の合算を表示（例: 15）、ユーザーが出荷合計を入力
- バラ合計: 全得意先のバラ受注数の合算を表示（例: 8）、ユーザーが出荷合計を入力

**注意**: 同一得意先でケースとバラの受注が混在する場合がある。
そのため合計入力はケース合計・バラ合計を**別々に**管理する。

**入力制約**:
- ケース合計入力値 ≤ 全得意先のケース受注数合算
- バラ合計入力値 ≤ 全得意先のバラ受注数合算

**自動振り分けロジック（欠品自動計算）**:

合計入力値が変更されたとき、得意先リストの**上から順に受注数を割り当て**、不足分は下の得意先から欠品になる。

```kotlin
fun distributeQuantity(
    totalInput: Double,
    entries: List<CustomerEntry>,  // plannedQtyType でフィルタ済み
): List<Double> {
    var remaining = totalInput
    return entries.map { entry ->
        val allocated = minOf(remaining, entry.plannedQty)
        remaining -= allocated
        allocated
    }
}
```

**例**: ケース受注 = 得意先A(5) + 得意先B(5) + 得意先C(5) = 合計15
- 合計入力 = 15 → A=5, B=5, C=5（全充足）
- 合計入力 = 12 → A=5, B=5, C=2（Cが3欠品）
- 合計入力 = 7 → A=5, B=2, C=0（B3欠品、C全欠品）

#### 3. 個別入力モード

ユーザーは得意先リストの各行の数量を直接編集可能。
個別編集時は合計値を再計算して合計入力エリアに反映する。

**個別入力制約**:
- 各得意先の入力値 ≤ その得意先の受注数
- 個別変更後、合計は自動再計算

**合計↔個別の連動**:
- 合計変更 → 個別が自動振り分け（上から充足）
- 個別変更 → 合計が再計算（個別の合算）

#### 4. 登録フロー

「登録」ボタン押下時:

```kotlin
suspend fun registerGroupedItem(group: GroupedPickingItem) {
    // 各得意先エントリを順番に API 登録
    for (entry in group.customerEntries) {
        val pickedQty = entry.pickedQtyInput.toDoubleOrNull() ?: 0.0
        pickingTaskRepository.updatePickingItem(
            resultId = entry.pickingItemResultId,
            pickedQty = pickedQty,
            pickedQtyType = entry.plannedQtyType.name
        )
    }
    // 全エントリ登録後にタスクをリフレッシュ
    refreshTaskFromServer(originalTask.taskId)
    // 次のグループに移動
    moveToNextGroupOrComplete()
}
```

**注意**: 得意先エントリごとに API を呼ぶ（既存 API を変更しない）。

#### 5. State 変更

```kotlin
data class OutboundPickingState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val originalTask: PickingTask? = null,
    val groupedItems: List<GroupedPickingItem> = emptyList(),  // NEW: グルーピング済み
    val currentGroupIndex: Int = 0,                           // NEW: グループインデックス
    val totalCaseInput: String = "",                          // NEW: ケース合計入力
    val totalPieceInput: String = "",                         // NEW: バラ合計入力
    val isUpdating: Boolean = false,
    val showCompletionDialog: Boolean = false,
    val isCompleting: Boolean = false,
    val showImageDialog: Boolean = false,
    val warehouseId: Int = 0,
    val warehouseName: String = ""
) {
    val currentGroup: GroupedPickingItem?
        get() = groupedItems.getOrNull(currentGroupIndex)

    val totalGroupCount: Int
        get() = groupedItems.size

    // ケースエントリのみ
    val caseEntries: List<CustomerEntry>
        get() = currentGroup?.customerEntries
            ?.filter { it.plannedQtyType == QuantityType.CASE } ?: emptyList()

    // バラエントリのみ
    val pieceEntries: List<CustomerEntry>
        get() = currentGroup?.customerEntries
            ?.filter { it.plannedQtyType == QuantityType.PIECE } ?: emptyList()

    val totalCasePlanned: Double
        get() = caseEntries.sumOf { it.plannedQty }

    val totalPiecePlanned: Double
        get() = pieceEntries.sumOf { it.plannedQty }

    val canRegister: Boolean
        get() = !isUpdating && currentGroup != null
}
```

#### UI変更

**左ペイン (ProductInfoSection) の変更**:
- 得意先名を**表示しない**（グループ表示のため特定の得意先が無い）
- 代わりに得意先数を表示（例:「3得意先」）

**右ペイン → `GroupedQuantitySection` に変更**:
- 上部: ケース合計入力 + バラ合計入力
- 中部: 得意先別リスト（LazyColumn）。各行に得意先名・ケース数・バラ数の入力フィールド
- 下部: 登録・履歴ボタン

### 影響範囲

| 対象 | 影響内容 |
|------|---------|
| `OutboundPickingState.kt` | `groupedItems`, `currentGroupIndex`, 合計入力フィールド追加。旧 `pendingItems`/`currentIndex`/`pickedQtyInput` を置換 |
| `OutboundPickingViewModel.kt` | グルーピングロジック、振り分けロジック、登録フロー変更 |
| `OutboundPickingScreen.kt` | 右ペインを得意先リスト表示に全面変更、左ペインから得意先名削除 |
| `PickingHistoryScreen.kt` | 影響なし（履歴は既存のまま得意先別表示） |
| API | **変更なし** — 既存の `POST /api/picking/tasks/{id}/update` を得意先ごとに呼ぶ |

## 制約

1. **API 変更なし**: 既存の `updatePickingItem` API をそのまま使用。グルーピングはクライアント側のみ
2. **登録はアトミックでない**: 複数得意先を順次 API 呼び出し。途中失敗時はリフレッシュして状態を同期
3. **walkingOrder 維持**: グループ内の最小 walkingOrder でソートし、ピッカーの歩行順序を維持
4. **既存カウンター**: `registeredCount`/`totalCount` は `originalTask` ベースで変更なし
5. **Portrait 対応**: 先に実装した Portrait レイアウトとの互換性を維持
6. **ViewModel/State 変更あり**: 今回は State とロジックの変更が必要

## 対象ファイル

### 新規作成
なし（`GroupedPickingItem` / `CustomerEntry` は `OutboundPickingState.kt` 内に定義）

### 既存変更
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingState.kt`
  - `GroupedPickingItem`, `CustomerEntry` データクラス追加
  - State フィールド変更: `groupedItems`, `currentGroupIndex`, `totalCaseInput`, `totalPieceInput`
  - 旧フィールド `pendingItems`, `currentIndex`, `pickedQtyInput` を置換
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingViewModel.kt`
  - `initialize`: グルーピングロジック追加
  - `registerCurrentItem` → `registerGroupedItem`: 複数 API 呼び出し
  - 振り分けロジック: `distributeQuantity`, `onTotalCaseInputChange`, `onTotalPieceInputChange`
  - 個別入力ハンドラ: `onCustomerQtyChange`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingScreen.kt`
  - 左ペイン: 得意先名削除、得意先数表示
  - 右ペイン: `QuantityInputSection` → `GroupedQuantitySection` に変更
  - 得意先別リスト表示（LazyColumn）
  - 合計入力フィールド追加

### 参照のみ
- `core/domain/src/main/java/biz/smt_life/android/core/domain/model/PickingTask.kt`
- `core/network/src/main/java/biz/smt_life/android/core/network/model/PickingModels.kt`
- `core/network/src/main/java/biz/smt_life/android/core/network/repository/PickingTaskRepositoryImpl.kt`

## 確認事項

1. **得意先の表示順序**: 得意先リストの並び順は何で決めるか？（伝票番号順？walkingOrder 順？API の返却順？）→ 現在の仕様では「リストの上部のものから引く」とあるので、API 返却順（= walkingOrder 順）を想定
API返却順番
2. **ケース・バラ混在時の UI**: 同一得意先がケースとバラ両方の受注を持つ場合、1行に2つの入力フィールドを表示するか、2行に分けるか？→ 本仕様では1行に「ケース [__] / バラ [__]」を表示
1行
3. **0入力の扱い**: 得意先の出荷数を 0 にした場合、API に `picked_qty=0` で送るか？それとも SHORTAGE として送るか？→ `picked_qty=0` で送り、サーバー側で SHORTAGE 判定する想定
picked_qty =0
4. **合計入力後の個別修正**: 合計で自動振り分け後、ユーザーが個別を修正 → 再度合計を変更した場合、個別修正は上書きされる。この挙動で良いか？
個別修正上書き
5. **登録途中の失敗**: 3得意先中2番目で API 失敗した場合の挙動。1番目は登録済みになる。リフレッシュして残りを再表示する想定で良いか？
まとめて登録にする。
6. **画面遷移の単位**: 現在は「次の商品へ」で1アイテムずつ移動。変更後は「次のグループへ」で移動。進捗カウンターは `totalItems`（= 全得意先の合計件数）のまま、それともグループ数に変更するか？
グループ数に変更。商品グループ単位の作業がベース。
