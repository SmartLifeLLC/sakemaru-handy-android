# 入荷処理機能 実装仕様書（Android ハンディ端末）

## 概要

DENSOハンディ端末（320x534）で実装済みの入荷処理機能を、Androidハンディ端末（1080x2040）向けに移植・最適化する。
画面が広いため、一画面により多くの情報を表示でき、操作性を向上させる。

## デバイス仕様

| 項目 | DENSO（参照元） | Android（実装先） |
|------|----------------|------------------|
| 画面解像度 | 320 x 534 | 1080 x 2040 |
| 画面密度 | 160dpi | 約420dpi |
| 画面方向 | portrait固定 | portrait / landscape |
| API Level | 33 | 33 |

## 参照ドキュメント

- API仕様: `prompts/api.md`
- デザイン仕様: `prompts/design.md`
- 画面一覧: `prompts/pages.md`
- 認証情報: `local.properties`（gitignore対象）

---

## 使用API一覧

| # | メソッド | エンドポイント | 用途 |
|---|----------|---------------|------|
| 1 | POST | `/api/auth/login` | ログイン（トークン取得） |
| 2 | GET | `/api/master/warehouses` | 倉庫マスタ一覧取得 |
| 3 | GET | `/api/incoming/schedules` | 入庫予定一覧取得（商品リスト） |
| 4 | GET | `/api/incoming/schedules/{id}` | 入庫予定詳細取得 |
| 5 | GET | `/api/incoming/work-items` | 作業データ一覧取得（履歴） |
| 6 | POST | `/api/incoming/work-items` | 入荷作業開始 |
| 7 | PUT | `/api/incoming/work-items/{id}` | 作業データ更新 |
| 8 | POST | `/api/incoming/work-items/{id}/complete` | 入荷作業完了 |
| 9 | DELETE | `/api/incoming/work-items/{id}` | 作業キャンセル |
| 10 | GET | `/api/incoming/locations` | ロケーション検索 |

---

## 画面構成（5画面）

### 画面動線

```
P03 メイン画面
  └── P10 倉庫選択画面
        └── P11 商品リスト画面 ←──── P14 入庫履歴画面
              └── P12 スケジュールリスト画面    ↑
                    └── P13 入庫入力画面 ───────┘
                          ↓（登録成功）
                          P12 に戻る
```

### 画面遷移フロー詳細

```
[P10 倉庫選択]
  ├─ 倉庫を選択 → [P11 商品リスト]
  └─ 戻る → [P03 メイン]

[P11 商品リスト]
  ├─ 商品を選択 → [P12 スケジュールリスト]
  ├─ 履歴ボタン → [P14 入庫履歴]
  └─ 戻る → [P10 倉庫選択]

[P12 スケジュールリスト]
  ├─ スケジュールを選択 → [P13 入庫入力]
  ├─ 履歴ボタン → [P14 入庫履歴]
  └─ 戻る → [P11 商品リスト]

[P13 入庫入力]
  ├─ 登録成功 → [P12 スケジュールリスト]（数量更新済み）
  └─ 戻る → [P12 スケジュールリスト]

[P14 入庫履歴]
  ├─ 履歴アイテム選択（編集） → [P13 入庫入力]（プリフィル）
  ├─ リストボタン → [P11 商品リスト]
  └─ 戻る → 前の画面
```

---

## 画面詳細仕様

### P10: 倉庫選択画面 (WarehouseSelectionScreen)

| 項目 | 内容 |
|------|------|
| ルート | `incoming_warehouse_selection` |
| API | `GET /api/master/warehouses` |
| 共有ViewModel | `IncomingViewModel`（全入荷画面で共有） |

#### UI構成

```
┌──────────────────────────────────────┐
│ TopAppBar: ← 倉庫選択               │
├──────────────────────────────────────┤
│                                      │
│  ┌──────────────────────────────┐    │
│  │  酒丸本社                    │    │
│  └──────────────────────────────┘    │
│  ┌──────────────────────────────┐    │
│  │  酒丸第二倉庫                │    │
│  └──────────────────────────────┘    │
│  ┌──────────────────────────────┐    │
│  │  酒丸冷蔵倉庫                │    │
│  └──────────────────────────────┘    │
│                                      │
│  （広い画面を活かし、倉庫情報を      │
│  　カード形式で大きく表示）           │
│                                      │
├──────────────────────────────────────┤
│ [F1:---] [F2:戻る] [F3:---] [F4:ログアウト] │
└──────────────────────────────────────┘
```

#### 操作

| 操作 | 動作 |
|------|------|
| 倉庫タップ | 選択してP11へ遷移 |
| F2 | 前画面に戻る |
| F4 | ログアウトしてログイン画面へ |

#### API呼び出し

```
画面表示時 → GET /api/master/warehouses
レスポンス → state.warehouses にセット
```

---

### P11: 商品リスト画面 (ProductListScreen)

| 項目 | 内容 |
|------|------|
| ルート | `incoming_product_list` |
| API | `GET /api/incoming/schedules?warehouse_id={id}&search={query}` |

#### UI構成

```
┌──────────────────────────────────────┐
│ TopAppBar: ← {倉庫名} 入庫処理      │
├──────────────────────────────────────┤
│ ┌─────────────────────────┐ [🔍]    │
│ │ 検索（JANコード/商品名） │          │
│ └─────────────────────────┘          │
├──────────────────────────────────────┤
│ ┌──────────────────────────────────┐ │
│ │ JAN: 4901234567890  Code: 10001 │ │
│ │ 商品名AAAA                      │ │
│ │ 720ml / 常温                    │ │
│ │              残: 80  済: 20     │ │
│ │                        [作業中] │ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ JAN: 4901234567891  Code: 10002 │ │
│ │ 商品名BBBB                      │ │
│ │ 1800ml / 冷蔵                   │ │
│ │              残: 50  済: 0      │ │
│ └──────────────────────────────────┘ │
│ ...                                  │
├──────────────────────────────────────┤
│ [F1:検索] [F2:戻る] [F3:履歴] [F4:---] │
└──────────────────────────────────────┘
```

#### 広い画面を活かした表示改善

- 商品カードに **容量**（720ml）、**温度帯**（常温/冷蔵）を1行で表示
- **残数量**と**済数量**をバッジ形式で右寄せ表示
- **作業中**インジケータを目立つ位置に表示
- 商品画像がある場合はサムネイル表示も可能

#### 操作

| 操作 | 動作 |
|------|------|
| 商品タップ | 選択してP12へ遷移 |
| 検索入力 | 300msデバウンスでAPI検索 |
| バーコードスキャン | 検索クエリにセットして検索 |
| F1 | 検索バーにフォーカス |
| F2 | P10に戻る |
| F3 | P14 履歴画面へ |

#### API呼び出し

```
画面表示時 → GET /api/incoming/schedules?warehouse_id={id}
検索時    → GET /api/incoming/schedules?warehouse_id={id}&search={query}
作業中確認 → GET /api/incoming/work-items?warehouse_id={id}&picker_id={pickerId}&status=WORKING
```

#### 状態管理

```kotlin
// 商品リスト
val products: List<IncomingProduct>
val searchQuery: String
val isSearching: Boolean
val workingScheduleIds: Set<Int>  // 作業中スケジュールのID集合
```

---

### P12: スケジュールリスト画面 (ScheduleListScreen)

| 項目 | 内容 |
|------|------|
| ルート | `incoming_schedule_list` |
| API | なし（P11で取得済みデータを使用） |

#### UI構成

```
┌──────────────────────────────────────┐
│ TopAppBar: ← {倉庫名} 入庫処理      │
├──────────────────────────────────────┤
│ ┌──────────────────────────────────┐ │
│ │ 商品名AAAA                      │ │
│ │ JAN: 4901234567890              │ │
│ │ 720ml / 常温                    │ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ 合計入荷予定: 100  入庫済: 20   │ │
│ └──────────────────────────────────┘ │
├──────────────────────────────────────┤
│ ┌────────────────────────────┬─────┐ │
│ │ 酒丸本社                   │     │ │
│ │ 02月19日  📍A-1-01         │ 80  │ │
│ └────────────────────────────┴─────┘ │
│ ┌────────────────────────────┬─────┐ │
│ │ 酒丸第二倉庫     [確定済]  │     │ │
│ │ 02月20日  📍B-2-03         │ 20  │ │
│ └────────────────────────────┴─────┘ │
├──────────────────────────────────────┤
│ [F1:---] [F2:戻る] [F3:履歴] [F4:---] │
└──────────────────────────────────────┘
```

#### 広い画面を活かした表示改善

- 商品サマリーヘッダーに画像・容量・温度帯を表示
- スケジュール行に倉庫名・日付・ロケーションを1行で表示（DENSOでは2行必要だった）
- ステータスバッジ（確定済/連携済/キャンセル）を行内にインライン表示
- 残数量ボタンを大きめに表示

#### 操作

| 操作 | 動作 |
|------|------|
| スケジュールタップ | 選択してP13へ遷移（作業可能な場合のみ） |
| F2 | P11に戻る |
| F3 | P14 履歴画面へ |

#### スケジュール選択可否

| ステータス | 選択可否 | 表示 |
|-----------|---------|------|
| PENDING | 可 | 通常表示 |
| PARTIAL | 可 | 通常表示（残数量が減少） |
| CONFIRMED | 不可 | 「確定済」バッジ、薄暗い背景 |
| TRANSMITTED | 不可 | 「連携済」バッジ、薄暗い背景 |
| CANCELLED | 不可 | 「キャンセル」バッジ、薄暗い背景 |

---

### P13: 入庫入力画面 (IncomingInputScreen)

| 項目 | 内容 |
|------|------|
| ルート | `incoming_input` |
| API | `POST /api/incoming/work-items`、`PUT /api/incoming/work-items/{id}`、`POST /api/incoming/work-items/{id}/complete`、`GET /api/incoming/locations` |

#### UI構成

```
┌──────────────────────────────────────┐
│ TopAppBar: ← {倉庫名} 入庫処理      │
├──────────────────────────────────────┤
│ ┌──────────────────────────────────┐ │
│ │ 商品名AAAA                      │ │
│ │ JAN: 4901234567890  Code: 10001 │ │
│ └──────────────────────────────────┘ │
│                                      │
│ 入荷日: 2026年02月19日              │
│                                      │
│ 🛒 入庫数量  入庫予定: 80           │
│ ┌──────────────────────────────────┐ │
│ │ [                              ] │ │
│ └──────────────────────────────────┘ │
│                                      │
│ 📅 賞味期限（任意）                  │
│ ┌────────────────────────────┐ [📅] │
│ │ YYYY-MM-DD                 │      │
│ └────────────────────────────┘      │
│                                      │
│ 🔍 ロケーション                      │
│ ┌──────────────────────────────────┐ │
│ │ ロケーション検索                 │ │
│ └──────────────────────────────────┘ │
│ ┌ サジェスト ──────────────────────┐ │
│ │ A 1 01                          │ │
│ │ A 1 02                          │ │
│ │ A 2 01                          │ │
│ └──────────────────────────────────┘ │
│                                      │
├──────────────────────────────────────┤
│ [F1:賞味] [F2:戻る] [F3:登録] [F4:---] │
└──────────────────────────────────────┘
```

#### 広い画面を活かした表示改善

- 入力フィールドを縦に余裕をもって配置
- ロケーションサジェストドロップダウンを広く表示
- カレンダーピッカーボタンを賞味期限フィールドの右に配置
- 予定数量を入力フィールドのラベルに表示

#### 入力フィールド仕様

| フィールド | 型 | バリデーション | 備考 |
|-----------|-----|-------------|------|
| 入庫数量 | 数値 | > 0 かつ ≤ 残数量 | フォーカス時に全選択 |
| 賞味期限 | YYYY-MM-DD | 日付形式 | 任意、カレンダーピッカー有 |
| ロケーション | テキスト | なし | オートコンプリート、300msデバウンス |

#### 操作

| 操作 | 動作 |
|------|------|
| F1 | カレンダーピッカーを開く |
| F2 | P12に戻る |
| F3 | 入庫登録（submit） |
| Tab / ↓ | 次のフィールドへ移動 |
| ロケーションサジェスト選択 | ロケーションをセット |

#### 登録フロー（新規エントリー）

```
1. POST /api/incoming/work-items
   → body: { incoming_schedule_id, picker_id, warehouse_id }
   → 作業データ（IncomingWorkItem）を取得

2. PUT /api/incoming/work-items/{id}
   → body: { work_quantity, work_arrival_date, work_expiration_date, location_id }
   → 数量・日付・ロケーションを更新

3. POST /api/incoming/work-items/{id}/complete
   → 作業完了（入庫確定）

4. 成功メッセージ表示（1.5秒）
5. 商品リスト再読み込み（数量更新）
6. P12 スケジュールリストに戻る
```

#### 編集フロー（履歴から）

```
1. currentWorkItem が既にある（履歴画面から引き継ぎ）
   → startWork をスキップ

2. PUT /api/incoming/work-items/{id}
   → 数量・日付・ロケーションを更新

3. completeWorkItem をスキップ（既に完了済み）

4. P12 スケジュールリストに戻る
```

---

### P14: 入庫履歴画面 (HistoryScreen)

| 項目 | 内容 |
|------|------|
| ルート | `incoming_history` |
| API | `GET /api/incoming/work-items?warehouse_id={id}&picker_id={pickerId}&status=all&from_date={today}` |

#### UI構成

```
┌──────────────────────────────────────┐
│ TopAppBar: ← {倉庫名} 入庫処理      │
├──────────────────────────────────────┤
│ ┌ 本日の入庫履歴 ──────────────────┐ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ JAN: 4901234567890  Code: 10001 │ │
│ │ 商品名AAAA                      │ │
│ │ 酒丸本社                        │ │
│ │ 予定:02/19  入庫:02/19 [完了] 80│ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ JAN: 4901234567891  Code: 10002 │ │
│ │ 商品名BBBB                      │ │
│ │ 酒丸第二倉庫                    │ │
│ │ 予定:02/19  入庫:02/19 [作業中] 50│
│ └──────────────────────────────────┘ │
│                                      │
├──────────────────────────────────────┤
│ [F1:---] [F2:戻る] [F3:リスト] [F4:---] │
└──────────────────────────────────────┘
```

#### 広い画面を活かした表示改善

- 履歴カードにJANコード・商品コード・商品名・倉庫名・日付・ステータス・数量をすべて表示
- ステータスバッジをカラーコーディング（作業中=tertiary、完了=primary、キャンセル=error）
- 数量を右端に大きく表示

#### 操作

| 操作 | 動作 |
|------|------|
| 履歴アイテムタップ | 編集可能であればP13へ遷移（データプリフィル） |
| F2 | 前画面に戻る |
| F3 | P11 商品リストへ |

#### 編集可否判定

| WorkItem ステータス | Schedule ステータス | 編集可否 |
|-------------------|-------------------|---------|
| WORKING | PENDING / PARTIAL | 可 |
| COMPLETED | PENDING / PARTIAL | 可 |
| CANCELLED | * | 不可 |
| * | CONFIRMED / TRANSMITTED | 不可 |

---

## 状態管理（IncomingState）

全画面で共有するViewModelの状態:

```kotlin
data class IncomingState(
    // セッション
    val pickerId: Int?,
    val pickerName: String?,

    // P10: 倉庫選択
    val warehouses: List<IncomingWarehouse>,
    val selectedWarehouse: IncomingWarehouse?,
    val isLoadingWarehouses: Boolean,

    // P11: 商品リスト
    val products: List<IncomingProduct>,
    val searchQuery: String,
    val isSearching: Boolean,
    val workingScheduleIds: Set<Int>,

    // P12: スケジュールリスト
    val selectedProduct: IncomingProduct?,

    // P13: 入庫入力
    val selectedSchedule: IncomingSchedule?,
    val currentWorkItem: IncomingWorkItem?,
    val isFromHistory: Boolean,
    val inputQuantity: String,
    val inputExpirationDate: String,
    val inputLocationSearch: String,
    val inputLocationId: Int?,
    val inputLocation: Location?,
    val locationSuggestions: List<Location>,
    val isLoadingLocations: Boolean,
    val isSubmitting: Boolean,

    // P14: 履歴
    val historyItems: List<IncomingWorkItem>,
    val isLoadingHistory: Boolean,

    // 共通
    val errorMessage: String?,
    val successMessage: String?
)
```

---

## ドメインモデル

### IncomingProduct

```kotlin
data class IncomingProduct(
    val itemId: Int,
    val itemCode: String,
    val itemName: String,
    val searchCode: String,
    val janCodes: List<String>,
    val volume: String?,
    val temperatureType: String?,
    val images: List<String>,
    val totalExpectedQuantity: Int,
    val totalReceivedQuantity: Int,
    val totalRemainingQuantity: Int,
    val warehouses: List<IncomingWarehouseSummary>,
    val schedules: List<IncomingSchedule>
)
```

### IncomingSchedule

```kotlin
data class IncomingSchedule(
    val id: Int,
    val warehouseId: Int,
    val warehouseName: String,
    val expectedQuantity: Int,
    val receivedQuantity: Int,
    val remainingQuantity: Int,
    val quantityType: String,       // "PIECE" or "CASE"
    val expectedArrivalDate: String,
    val expirationDate: String?,
    val status: IncomingScheduleStatus,
    val location: Location?
)
```

### IncomingWorkItem

```kotlin
data class IncomingWorkItem(
    val id: Int,
    val incomingScheduleId: Int,
    val pickerId: Int,
    val warehouseId: Int,
    val locationId: Int?,
    val location: Location?,
    val workQuantity: Int,
    val workArrivalDate: String,
    val workExpirationDate: String?,
    val status: IncomingWorkStatus,  // WORKING, COMPLETED, CANCELLED
    val startedAt: String,
    val schedule: WorkItemSchedule?
)
```

### Location

```kotlin
data class Location(
    val id: Int,
    val code1: String,
    val code2: String,
    val code3: String,
    val name: String,
    val displayName: String
)
```

---

## エラーハンドリング

### APIエラーマッピング

| HTTP ステータス | エラーコード | 日本語メッセージ |
|---------------|------------|-----------------|
| 401 | UNAUTHORIZED | 認証エラー。再ログインしてください。 |
| 403 | FORBIDDEN | アクセス権限がありません。 |
| 404 | NOT_FOUND | データが見つかりません。 |
| 422 | VALIDATION_ERROR | APIから返されるエラーメッセージをそのまま表示 |
| 400 | ALREADY_WORKING | 既に作業中です（startWork時、既存workItemを使用） |
| 5xx | SERVER_ERROR | サーバーエラー。しばらくしてから再度お試しください。 |
| ネットワーク | NETWORK_ERROR | ネットワークエラー。接続を確認してください。 |

### エラー表示

- `Snackbar` でエラーメッセージを表示
- 次のユーザー操作で自動クリア

---

## 現行実装からの変更点

### 既存ファイル（変更必要）

| ファイル | 変更内容 |
|---------|---------|
| `Routes.kt` | 入荷サブルート5件を追加 |
| `HandyNavHost.kt` | 入荷画面5件のナビゲーション追加 |
| `InboundRepository.kt` | API仕様に合わせてインターフェース変更 |
| `InboundEntryDto.kt` → ドメインモデル全面刷新 | `IncomingProduct`, `IncomingSchedule`, `IncomingWorkItem` 等 |

### 新規作成ファイル

| ファイル | 役割 |
|---------|------|
| `IncomingState.kt` | 共有状態データクラス |
| `IncomingViewModel.kt` | 全画面共有ViewModel |
| `WarehouseSelectionScreen.kt` | P10画面 |
| `ProductListScreen.kt` | P11画面 |
| `ScheduleListScreen.kt` | P12画面 |
| `IncomingInputScreen.kt` | P13画面 |
| `HistoryScreen.kt` | P14画面 |
| `IncomingApi.kt` | Retrofit APIインターフェース |
| `IncomingRepositoryImpl.kt` | Repository実装 |
| `IncomingModels.kt` | ドメインモデル群 |
| `IncomingApiModels.kt` | APIレスポンス/リクエストモデル |

### 既存ファイル（削除候補）

| ファイル | 理由 |
|---------|------|
| `InboundScreen.kt` | 新しい5画面構成に置き換え |
| `InboundViewModel.kt` | 新しいIncomingViewModelに置き換え |
| `InboundState.kt` | 新しいIncomingStateに置き換え |
| `ItemSearchBar.kt` | ProductListScreen内で再実装 |
| `QtyInputSection.kt` | IncomingInputScreen内で再実装 |
| `HistoryBottomSheet.kt` | 独立したHistoryScreenに置き換え |
