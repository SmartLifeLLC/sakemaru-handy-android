# 入荷処理機能 実装 作業計画

## 前提

### 参照仕様
- API仕様: `prompts/api.md`
- デザイン仕様: `prompts/design.md`
- 画面一覧: `prompts/pages.md`
- 機能仕様: `prompts/incoming/incoming-spec.md`

### 完了済みの作業・現在の状況
- 出荷処理（P20〜P22）は実装済み・動作中
- 入荷処理は旧プレースホルダー実装あり（InboundScreen/InboundViewModel/InboundState）
  - 旧APIは `/items`, `/inbound/entries` 等のダミーエンドポイント
  - FakeInboundRepository でモックデータ利用
- 新仕様では5画面構成（P10〜P14）に全面刷新
- `/api/incoming/*` エンドポイント群を使用

---

## Phase 一覧

| # | Phase | 概要 | 完了条件 |
|---|-------|------|---------|
| P1 | APIテスト | 入荷系10エンドポイントの疎通確認 | 全APIの正常レスポンス確認 |
| P2 | データ層実装 | ドメインモデル・API・Repository | ビルド成功・Repository単体動作 |
| P3 | P10 倉庫選択画面 | WarehouseSelectionScreen | 倉庫一覧表示・選択遷移動作 |
| P4 | P11 商品リスト画面 | ProductListScreen | 商品一覧表示・検索・遷移動作 |
| P5 | P12 スケジュールリスト画面 | ScheduleListScreen | スケジュール一覧表示・選択動作 |
| P6 | P13 入荷入力画面 | IncomingInputScreen | 入荷登録フロー完動 |
| P7 | P14 入荷履歴画面 | HistoryScreen | 履歴表示・編集遷移動作 |
| P8 | ナビゲーション・結合 | Routes/NavHost/DI登録 | 全画面遷移フロー動作 |
| P9 | 旧コード削除 | 不要ファイル削除 | ビルド成功・旧コード残存なし |
| P10 | pages.md 更新 | 画面一覧の更新 | P10〜P14が正しく記載 |

---

## P1: APIテスト

### 目的
使用する全APIエンドポイントの疎通確認とレスポンス構造の検証

### テスト手順
1. `local.properties` からテストユーザー情報を取得
2. `POST /api/auth/login` でトークンを取得
3. 各エンドポイントを順番にcurlでテスト
4. レスポンスを `boot.md` の作業中コンテキストに記録

### テスト対象API

| # | メソッド | エンドポイント | パラメータ | 期待結果 |
|---|----------|---------------|-----------|---------|
| 1 | POST | `/api/auth/login` | code, password | トークン取得 |
| 2 | GET | `/api/master/warehouses` | - | 倉庫リスト（id, code, name） |
| 3 | GET | `/api/incoming/schedules` | warehouse_id | 入荷予定リスト |
| 4 | GET | `/api/incoming/schedules` | warehouse_id, search=テスト | 検索結果 |
| 5 | GET | `/api/incoming/schedules/{id}` | id=テスト#3の結果 | 入荷予定詳細 |
| 6 | GET | `/api/incoming/work-items` | warehouse_id, status=all | 作業データリスト |
| 7 | POST | `/api/incoming/work-items` | incoming_schedule_id, picker_id, warehouse_id | 作業データ作成 |
| 8 | PUT | `/api/incoming/work-items/{id}` | work_quantity, work_arrival_date | 作業データ更新 |
| 9 | POST | `/api/incoming/work-items/{id}/complete` | - | 作業完了 |
| 10 | GET | `/api/incoming/locations` | warehouse_id | ロケーション一覧 |

### エラー記録ルール
- サーバ側エラー（4xx/5xx）が発生した場合:
  - `prompts/incoming/error.log` にタイムスタンプ・エンドポイント・ステータスコード・レスポンスボディを追記
  - boot.md の作業中コンテキストにも概要を記入

### 完了条件
- 全APIが正常レスポンスを返すこと
- レスポンス構造が `prompts/api.md` の仕様と一致すること
- エラーが発生した場合は error.log に記録済みであること

---

## P2: データ層実装

### 目的
入荷処理で使用するドメインモデル、APIインターフェース、Repository実装を作成する

### 2-1: ドメインモデル作成

以下のファイルを `core/domain/src/main/java/biz/smt_life/android/core/domain/model/` に作成:

#### IncomingWarehouse.kt
```kotlin
data class IncomingWarehouse(
    val id: Int,
    val code: String,
    val name: String,
    val kanaName: String,
    val outOfStockOption: String
)
```

#### IncomingProduct.kt
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

data class IncomingWarehouseSummary(
    val warehouseId: Int,
    val warehouseCode: String,
    val warehouseName: String,
    val expectedQuantity: Int,
    val receivedQuantity: Int,
    val remainingQuantity: Int
)
```

#### IncomingSchedule.kt
```kotlin
data class IncomingSchedule(
    val id: Int,
    val warehouseId: Int,
    val warehouseName: String,
    val expectedQuantity: Int,
    val receivedQuantity: Int,
    val remainingQuantity: Int,
    val quantityType: String,        // "PIECE" or "CASE"
    val expectedArrivalDate: String,
    val status: IncomingScheduleStatus
)

enum class IncomingScheduleStatus {
    PENDING, PARTIAL, CONFIRMED, TRANSMITTED, CANCELLED;
    companion object {
        fun fromString(value: String): IncomingScheduleStatus =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: PENDING
    }
}
```

#### IncomingWorkItem.kt
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
    val status: IncomingWorkStatus,
    val startedAt: String,
    val schedule: WorkItemSchedule?
)

data class WorkItemSchedule(
    val id: Int,
    val itemId: Int,
    val itemCode: String,
    val itemName: String,
    val warehouseId: Int,
    val warehouseName: String,
    val expectedQuantity: Int,
    val receivedQuantity: Int,
    val remainingQuantity: Int,
    val quantityType: String
)

enum class IncomingWorkStatus {
    WORKING, COMPLETED, CANCELLED;
    companion object {
        fun fromString(value: String): IncomingWorkStatus =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: WORKING
    }
}
```

#### Location.kt
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

### 2-2: APIインターフェース作成

`core/network/src/main/java/biz/smt_life/android/core/network/api/IncomingApi.kt`:

```kotlin
interface IncomingApi {
    @GET("/api/incoming/schedules")
    suspend fun getSchedules(
        @Query("warehouse_id") warehouseId: Int,
        @Query("search") search: String? = null
    ): ApiEnvelope<List<IncomingScheduleResponse>>

    @GET("/api/incoming/schedules/{id}")
    suspend fun getScheduleDetail(
        @Path("id") id: Int
    ): ApiEnvelope<IncomingScheduleDetailResponse>

    @GET("/api/incoming/work-items")
    suspend fun getWorkItems(
        @Query("warehouse_id") warehouseId: Int,
        @Query("picker_id") pickerId: Int? = null,
        @Query("status") status: String? = null,
        @Query("from_date") fromDate: String? = null,
        @Query("to_date") toDate: String? = null,
        @Query("limit") limit: Int? = null
    ): ApiEnvelope<List<IncomingWorkItemResponse>>

    @POST("/api/incoming/work-items")
    suspend fun startWork(
        @Body request: StartWorkRequest
    ): ApiEnvelope<IncomingWorkItemResponse>

    @PUT("/api/incoming/work-items/{id}")
    suspend fun updateWork(
        @Path("id") id: Int,
        @Body request: UpdateWorkRequest
    ): ApiEnvelope<IncomingWorkItemResponse>

    @HTTP(method = "DELETE", path = "/api/incoming/work-items/{id}")
    suspend fun cancelWork(
        @Path("id") id: Int
    ): ApiEnvelope<Any?>

    @POST("/api/incoming/work-items/{id}/complete")
    suspend fun completeWork(
        @Path("id") id: Int
    ): ApiEnvelope<Any?>

    @GET("/api/incoming/locations")
    suspend fun getLocations(
        @Query("warehouse_id") warehouseId: Int,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int? = null
    ): ApiEnvelope<List<LocationResponse>>
}
```

### 2-3: APIモデル作成

`core/network/src/main/java/biz/smt_life/android/core/network/model/IncomingModels.kt`:

リクエスト:
- `StartWorkRequest(incoming_schedule_id, picker_id, warehouse_id)`
- `UpdateWorkRequest(work_quantity, work_arrival_date, work_expiration_date, location_id)`

レスポンス:
- `IncomingScheduleResponse` — P1のAPIテスト結果に基づいて定義
- `IncomingWorkItemResponse` — 同上
- `LocationResponse` — 同上

### 2-4: Repository作成

`core/domain/.../repository/IncomingRepository.kt` (インターフェース):
```kotlin
interface IncomingRepository {
    suspend fun getSchedules(warehouseId: Int, search: String? = null): Result<List<IncomingProduct>>
    suspend fun getScheduleDetail(id: Int): Result<IncomingSchedule>
    suspend fun getWorkItems(warehouseId: Int, pickerId: Int?, status: String?, fromDate: String?): Result<List<IncomingWorkItem>>
    suspend fun startWork(scheduleId: Int, pickerId: Int, warehouseId: Int): Result<IncomingWorkItem>
    suspend fun updateWork(id: Int, quantity: Int, arrivalDate: String, expirationDate: String?, locationId: Int?): Result<IncomingWorkItem>
    suspend fun cancelWork(id: Int): Result<Unit>
    suspend fun completeWork(id: Int): Result<Unit>
    suspend fun getLocations(warehouseId: Int, search: String?): Result<List<Location>>
}
```

`core/network/.../repository/IncomingRepositoryImpl.kt` (実装):
- PickingTaskRepositoryImpl パターンを踏襲
- ErrorMapper でエラーハンドリング
- APIレスポンスをドメインモデルに変換

### 2-5: DI登録

`NetworkModule.kt` に以下を追加:
- `IncomingApi` の Retrofit provide
- `IncomingRepositoryImpl` の bind

### 完了条件
- 全ファイルが作成済み
- ビルドが成功すること
- 旧InboundApiと新IncomingApiが並存しても問題ないこと

---

## P3: P10 倉庫選択画面

### 目的
入荷処理の起点画面。倉庫マスタ一覧を表示し、作業対象倉庫を選択する。

### 実装内容

#### ファイル
- `feature/inbound/src/main/java/.../incoming/WarehouseSelectionScreen.kt`

#### API
- `GET /api/master/warehouses` — 既存WarehouseApi（またはMasterApi）を利用

#### UI仕様（incoming-spec.md P10参照）
- TopAppBar: `← 倉庫選択`
- コンテンツ: 倉庫カードリスト（LazyColumn）
  - カード形式で倉庫名を大きく表示（広い画面を活かす）
- FunctionKeyBar: F2:戻る, F4:ログアウト

#### 操作
- 倉庫タップ → IncomingViewModel に選択倉庫をセット → P11へ遷移
- F2 → P03 メインに戻る
- F4 → ログアウト処理

#### 状態管理
IncomingViewModel に以下を追加:
```kotlin
val warehouses: List<IncomingWarehouse>
val selectedWarehouse: IncomingWarehouse?
val isLoadingWarehouses: Boolean
```

### 完了条件
- 倉庫一覧がAPI取得・表示されること
- 倉庫タップでselectedWarehouseが更新されること
- ローディング・エラー状態が適切に表示されること

---

## P4: P11 商品リスト画面

### 目的
選択倉庫の入荷予定商品一覧を表示。検索・バーコードスキャン対応。

### 実装内容

#### ファイル
- `feature/inbound/src/main/java/.../incoming/ProductListScreen.kt`

#### API
- `GET /api/incoming/schedules?warehouse_id={id}&search={query}`
- `GET /api/incoming/work-items?warehouse_id={id}&picker_id={pickerId}&status=WORKING` — 作業中判定

#### UI仕様（incoming-spec.md P11参照）
- TopAppBar: `← {倉庫名} 入荷処理`
- 検索バー: 300msデバウンス、バーコードスキャン対応
- 商品カードリスト（LazyColumn）:
  - JANコード、商品コード、商品名
  - 容量、温度帯（1行）
  - 残数量バッジ、済数量バッジ
  - 作業中インジケータ（workingScheduleIdsで判定）
- FunctionKeyBar: F1:検索フォーカス, F2:戻る, F3:履歴

#### 操作
- 商品タップ → selectedProduct セット → P12へ遷移
- F1 → 検索バーにフォーカス
- F2 → P10に戻る
- F3 → P14 履歴画面へ

#### 状態管理
```kotlin
val products: List<IncomingProduct>
val searchQuery: String
val isSearching: Boolean
val workingScheduleIds: Set<Int>
```

### 完了条件
- 商品一覧がAPI取得・表示されること
- 検索で絞り込みが動作すること
- 作業中インジケータが正しく表示されること

---

## P5: P12 スケジュールリスト画面

### 目的
選択商品の入荷スケジュール一覧を表示。倉庫別の予定数量・残数量を確認する。

### 実装内容

#### ファイル
- `feature/inbound/src/main/java/.../incoming/ScheduleListScreen.kt`

#### API
- なし（P11で取得済みデータを使用）

#### UI仕様（incoming-spec.md P12参照）
- TopAppBar: `← {倉庫名} 入荷処理`
- 商品サマリーヘッダー（名前・JAN・容量・画像）
- 合計数量バー（合計予定数・入荷済数）
- スケジュールリスト（LazyColumn）:
  - 倉庫名・予定日・ロケーション（1行）
  - ステータスバッジ（確定済/連携済/キャンセル → タップ不可・薄暗い背景）
  - 残数量ボタン
- FunctionKeyBar: F2:戻る, F3:履歴

#### スケジュール選択可否
| ステータス | 選択可否 |
|-----------|---------|
| PENDING / PARTIAL | 可 |
| CONFIRMED / TRANSMITTED / CANCELLED | 不可（薄暗い背景） |

#### 操作
- スケジュールタップ（選択可の場合）→ selectedSchedule セット → P13へ遷移
- F2 → P11に戻る
- F3 → P14 履歴画面へ

### 完了条件
- スケジュール一覧が表示されること
- 確定済みスケジュールがタップ不可であること
- 商品サマリーが正しく表示されること

---

## P6: P13 入荷入力画面

### 目的
入荷数量・賞味期限・ロケーションを入力して登録する。

### 実装内容

#### ファイル
- `feature/inbound/src/main/java/.../incoming/IncomingInputScreen.kt`

#### API
- `POST /api/incoming/work-items` — 作業開始
- `PUT /api/incoming/work-items/{id}` — 数量・日付・ロケーション更新
- `POST /api/incoming/work-items/{id}/complete` — 作業完了
- `GET /api/incoming/locations?warehouse_id={id}&search={query}` — ロケーション検索

#### UI仕様（incoming-spec.md P13参照）
- TopAppBar: `← {倉庫名} 入荷処理`
- 商品情報ヘッダー（名前・JAN・商品コード）
- 入荷日表示（本日）
- 入荷数量入力（予定数表示付き、バリデーション: > 0 ≤ 残数量）
- 賞味期限入力（YYYY-MM-DD、カレンダーピッカー付き）
- ロケーション検索（オートコンプリート、300msデバウンス）
- FunctionKeyBar: F1:賞味期限カレンダー, F2:戻る, F3:登録

#### 登録フロー（新規）
```
1. POST /api/incoming/work-items → 作業データ取得
2. PUT /api/incoming/work-items/{id} → 数量・日付・ロケーション更新
3. POST /api/incoming/work-items/{id}/complete → 作業完了
4. 成功メッセージ表示（1.5秒）
5. P12 に戻る（数量更新反映のためschedules再取得）
```

#### 編集フロー（履歴から）
```
1. currentWorkItem が既にある → startWork スキップ
2. PUT /api/incoming/work-items/{id} → 更新
3. completeWork スキップ（既に完了済み）
4. P12 に戻る
```

#### 入力フィールド仕様
| フィールド | バリデーション | 備考 |
|-----------|-------------|------|
| 入荷数量 | > 0 ≤ 残数量 | フォーカス時全選択 |
| 賞味期限 | YYYY-MM-DD形式 | 任意、カレンダーピッカー有 |
| ロケーション | なし | オートコンプリート |

### 完了条件
- 新規入荷登録が正常に完了すること（3ステップAPI）
- 編集フローが正常に動作すること
- ロケーション検索・選択が動作すること
- バリデーションエラーが適切に表示されること

---

## P7: P14 入荷履歴画面

### 目的
本日の入荷作業履歴を表示し、編集可能なアイテムを選択してP13へ遷移する。

### 実装内容

#### ファイル
- `feature/inbound/src/main/java/.../incoming/HistoryScreen.kt`

#### API
- `GET /api/incoming/work-items?warehouse_id={id}&picker_id={pickerId}&status=all&from_date={today}`

#### UI仕様（incoming-spec.md P14参照）
- TopAppBar: `← {倉庫名} 入荷処理`
- 「本日の入荷履歴」ヘッダー
- 履歴カードリスト（LazyColumn）:
  - JANコード、商品コード、商品名
  - 倉庫名
  - 予定日、入荷日
  - ステータスバッジ（カラーコーディング: 作業中=tertiary, 完了=primary, キャンセル=error）
  - 数量（右端大きく）
- FunctionKeyBar: F2:戻る, F3:リスト（P11へ）

#### 編集可否判定
| WorkItem ステータス | Schedule ステータス | 編集可否 |
|-------------------|-------------------|---------|
| WORKING | PENDING / PARTIAL | 可 |
| COMPLETED | PENDING / PARTIAL | 可 |
| CANCELLED | * | 不可 |
| * | CONFIRMED / TRANSMITTED | 不可 |

#### 操作
- 履歴アイテムタップ（編集可の場合）→ currentWorkItem + isFromHistory=true セット → P13へ遷移
- F2 → 前画面に戻る
- F3 → P11 商品リストへ

### 完了条件
- 本日の履歴がAPI取得・表示されること
- ステータスバッジのカラーコーディングが正しいこと
- 編集可否判定が正しく動作すること
- 編集タップでP13にデータがプリフィルされること

---

## P8: ナビゲーション・結合

### 目的
全画面のナビゲーションを登録し、全遷移フローが動作することを確認する。

### 実装内容

#### Routes.kt 変更
```kotlin
// 既存の object Inbound を以下に置換
object IncomingWarehouseSelection : Routes("incoming_warehouse_selection")
object IncomingProductList : Routes("incoming_product_list")
object IncomingScheduleList : Routes("incoming_schedule_list")
object IncomingInput : Routes("incoming_input")
object IncomingHistory : Routes("incoming_history")
```

#### HandyNavHost.kt 変更
5件のcomposable登録。全画面で `IncomingViewModel` を共有（parentEntry scope）。

```kotlin
// P10
composable(Routes.IncomingWarehouseSelection.route) { ... }
// P11
composable(Routes.IncomingProductList.route) { ... }
// P12
composable(Routes.IncomingScheduleList.route) { ... }
// P13
composable(Routes.IncomingInput.route) { ... }
// P14
composable(Routes.IncomingHistory.route) { ... }
```

#### MainScreen 変更
- 「入荷処理」ボタンの遷移先を `Routes.Inbound` → `Routes.IncomingWarehouseSelection` に変更

#### 結合テスト手順
1. P03 → P10: 入荷処理ボタン → 倉庫一覧表示
2. P10 → P11: 倉庫選択 → 商品一覧表示
3. P11 → P12: 商品選択 → スケジュール一覧表示
4. P12 → P13: スケジュール選択 → 入荷入力表示
5. P13 → P12: 登録成功 → スケジュール一覧（数量更新）
6. P11 → P14: F3 → 履歴表示
7. P14 → P13: 編集タップ → 入荷入力（プリフィル）
8. 各画面 F2: 戻る → 前画面

### 完了条件
- 全遷移パスが正常に動作すること
- 共有ViewModelのデータが各画面で正しく共有されること
- 戻るナビゲーションが正しく動作すること

---

## P9: 旧コード削除・クリーンアップ

### 目的
不要になった旧プレースホルダー実装を削除する。

### 削除対象ファイル
- `feature/inbound/.../InboundScreen.kt`
- `feature/inbound/.../InboundViewModel.kt`
- `feature/inbound/.../InboundState.kt`
- `feature/inbound/.../component/ItemSearchBar.kt`
- `feature/inbound/.../component/QtyInputSection.kt`
- `feature/inbound/.../component/HistoryBottomSheet.kt`
- `core/network/.../api/InboundApi.kt`
- `core/domain/.../repository/InboundRepository.kt`
- `core/domain/.../model/InboundEntryDto.kt`
- `core/domain/.../model/ItemDto.kt` （他で未使用の場合）
- `core/network/.../fake/FakeInboundRepository.kt`

### 変更対象
- `NetworkModule.kt` — 旧 InboundRepository バインディング削除
- `Routes.kt` — 旧 `object Inbound` 削除

### 手順
1. 各削除候補ファイルの参照箇所を検索（Grep）
2. 参照が新コードに移行済みであることを確認
3. ファイル削除
4. ビルド確認

### 完了条件
- 全不要ファイルが削除されていること
- ビルドが成功すること
- 旧コードへの参照が残存しないこと

---

## P10: pages.md 更新

### 目的
実装した入荷処理画面（P10〜P14）の情報を `prompts/pages.md` に反映する。

### 更新内容

1. P10〜P14 の各セクションの **実装状態** を `未実装→実装対象` から削除（実装完了のため）
2. 各画面の **ファイル** パスを実際のパスに更新
3. ファンクションキーの割り当てを最終実装に合わせて更新
4. 画面遷移フローが正確であることを確認

### 完了条件
- pages.md のP10〜P14が実装完了の内容で正しく記載されていること
- 画面番号が既存と重複していないこと
- 遷移先が正しく記載されていること

---

## 制約（厳守）

- 画面解像度 1080x2040 / 420dpi / portrait・landscape対応 を前提にUI設計
- NoActionBar テーマ + Compose TopAppBar パターンを踏襲
- FunctionKeyBar の配置パターンを踏襲（F1〜F4）
- 既存出荷機能のアーキテクチャパターンを踏襲
  - Single Source of Truth (StateFlow)
  - ErrorMapper → NetworkException
  - Idempotency-Key ヘッダー（POST時）
  - Result<T> ラッパー
- `local.properties` の認証情報はコミットしない
- サーバ側エラーは `error.log` に記録する
- 旧コード削除前に参照箇所を必ず確認する

## 全体完了条件

- 全PhaseのAPIテストが成功
- P10〜P14 の全画面が正常動作
- 全遷移フロー（P03→P10→P11→P12→P13→P12、P11→P14→P13）が動作
- 入荷登録フロー（新規・編集）が正常完了
- 旧コードが削除済み
- ビルド成功
- pages.md が更新済み
- サーバエラーがあれば error.log に記録済み
