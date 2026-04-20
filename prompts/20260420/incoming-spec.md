# 入荷仕様書（Android / DENSO ロジック準拠）

- 作成日: 2026-04-20
- 対象: `sakemaru-handy-android` の入荷機能
- 目的: Android の入荷処理を DENSO 端末の入荷ロジックと同等に揃え、仮想倉庫の入荷を同じ前提で処理できるようにする

---

## 1. 参照元

- DENSO ロジック説明:
  `/Users/jungsinyu/Projects/sakemaru-handy-denso/prompts/explain/incoming-logic.md`
- DENSO 実装:
  `feature/inbound/.../incoming/IncomingViewModel.kt`
  `core/network/.../repository/IncomingRepositoryImpl.kt`
- Android 現行実装:
  `feature/inbound/.../incoming/*.kt`
  `core/domain/.../repository/IncomingRepository.kt`
  `core/network/.../repository/IncomingRepositoryImpl.kt`

本仕様書は説明書だけではなく、DENSO 実コードの挙動も基準にしている。説明書と実装で差異がある箇所は、DENSO 実装準拠を優先する。

---

## 2. 今回の要求

### 2.1 必須

- Android の入荷ロジックを DENSO 端末と同じ業務ロジックに揃える
- 実倉庫で作業中に仮想倉庫の入荷を処理できるようにする
- 新規入荷と履歴編集の両フローを DENSO と同じ条件で扱う
- `ALREADY_WORKING` を含む作業開始の冪等動作を DENSO と同じにする

### 2.2 今回そろえなくてよいもの

- DENSO 端末の画面サイズ
- DENSO 端末の物理ボタン前提 UI
- `F1` `F2` `F3` `F4`、上下キー、Enter などのハードキー操作

### 2.3 Android で残すもの

- タッチ操作
- ソフトキーボード入力
- 端末がスキャナ入力を持つ場合のバーコード文字列入力

バーコード入力は「入力ソース」としては許容するが、DENSO のハードキーショートカットを前提にした導線は不要。

---

## 3. 用語定義

### 3.1 作業倉庫

ユーザーがその時点で作業している倉庫。Android 画面上で選択した倉庫であり、実倉庫を想定する。

- `selectedWarehouse.id`
- 作業開始 API の `warehouse_id`
- ロケーション検索の `warehouse_id`
- 履歴取得の `warehouse_id`

### 3.2 入荷対象倉庫

実際に入荷予定が紐づいている倉庫。スケジュール単位で持つ倉庫。

- `schedule.warehouseId`
- `schedule.warehouseName`

### 3.3 仮想倉庫入荷

以下を満たすケースを指す。

- 作業倉庫 != 入荷対象倉庫

このときでも Android は処理を拒否してはならない。DENSO と同じく、

- 取得条件や作業実績の記録は作業倉庫基準
- 表示上の入荷先はスケジュール倉庫基準

で扱う。

### 3.4 重要な前提

DENSO 実装には `is_virtual` のような明示フラグはない。したがって Android も初期実装では仮想倉庫判定を専用フラグに依存させず、

- `selectedWarehouse.id`
- `schedule.warehouseId`

の差異を許容する設計にする。

---

## 4. 画面フロー

```text
メイン
  → 倉庫選択
  → 商品一覧
  → スケジュール一覧
  → 入荷入力
  → スケジュール一覧へ戻る

商品一覧 / スケジュール一覧
  → 履歴一覧
  → 履歴編集で入荷入力へ遷移
```

### 4.1 遷移ルール

- 倉庫選択後は即座に商品一覧をロードする
- 商品一覧から履歴へ遷移可能
- スケジュール一覧から履歴へ遷移可能
- 入荷入力完了後はスケジュール一覧へ戻る
- 履歴編集後は履歴または直前画面ではなく、DENSO と同じく入荷入力完了後に再読込された一覧側へ戻す

---

## 5. 機能仕様

### 5.1 倉庫選択画面

- 初回表示時に `GET /api/master/warehouses`
- 表示項目:
  - 倉庫名
  - 倉庫コード
- 倉庫選択時の処理:
  - `selectedWarehouse` を更新
  - 商品一覧、検索条件、選択商品、選択スケジュール、入力値、履歴選択状態をクリア
  - 商品一覧取得を開始

### 5.2 商品一覧画面

- 一覧取得 API:
  - `GET /api/incoming/schedules?warehouse_id={selectedWarehouse.id}&search={optional}`
- 作業中バッジ取得:
  - DENSO と同じく商品一覧取得と並列で `WORKING` 作業を取得し、`workingScheduleIds` を作る
- 検索:
  - 300ms デバウンス
  - JAN、商品コード、商品名で検索
- 表示項目:
  - 商品名
  - JAN
  - 商品コード
  - 容量
  - 温度帯
  - 合計予定数
  - 合計入荷済数
  - 合計残数
  - 作業中バッジ
- 画面操作:
  - 履歴画面へ遷移できる通常ボタンを配置
  - F キーの代替として明示ボタンを置くが、FunctionKeyBar は前提にしない

### 5.3 スケジュール一覧画面

- 選択商品に紐づく `schedules` を表示する
- 表示項目:
  - 入荷対象倉庫名
  - 入荷予定日
  - 予定数
  - 入荷済数
  - 残数
  - ステータス
  - 既定ロケーションがある場合はロケーション
- 選択可能条件:
  - `status == PENDING`
  - `status == PARTIAL`
- 非選択状態で表示のみ許容:
  - `CONFIRMED`
  - `TRANSMITTED`
  - `CANCELLED`

#### 仮想倉庫時の表示要件

`selectedWarehouse.id != schedule.warehouseId` の場合は、ユーザーが誤認しないように以下を表示する。

- 作業倉庫
- 入荷対象倉庫

少なくともスケジュールカードまたは入荷入力ヘッダで両方を識別できること。DENSO と同じロジックを維持したまま、Android の画面幅に合わせて見せ方のみ調整してよい。

### 5.4 入荷入力画面

#### 初期値

DENSO の `prepareInputForSchedule()` と同等に初期化する。

- 新規入荷時
  - 数量: `schedule.remainingQuantity`
  - 賞味期限: `schedule.expirationDate` or 空
  - ロケーション: `schedule.location` or 空
- 履歴編集時
  - 数量: `workItem.workQuantity`
  - 賞味期限: `workItem.workExpirationDate`
  - ロケーション: `workItem.location`

#### 入力項目

- 入荷数量
- 賞味期限
- ロケーション
- 入荷日
  - 当日固定表示
  - 送信値は `LocalDate.now()` の `ISO_LOCAL_DATE`

#### 入力補助

- 賞味期限:
  - 手入力可
  - Android の日付ピッカー起動ボタンを配置
  - 入力フォーマットは `YYYY-MM-DD`
- ロケーション:
  - 300ms デバウンス検索
  - 候補一覧表示
  - 候補選択で `inputLocationId` をセット

#### ロケーション検索仕様

- DENSO と同じく、検索対象の `warehouse_id` は `selectedWarehouse.id` を使う
- つまり仮想倉庫入荷時でも、ロケーションは作業倉庫側の候補を引く

#### バリデーション

- 数量は数値のみ
- `quantity > 0`
- `quantity <= selectedSchedule.remainingQuantity`
- 不正値時は登録ボタンを無効化、または明示エラー表示

#### ボタン仕様

- 登録は Android の通常ボタンで実行
- 戻るは通常ナビゲーションで実行
- F キーラベルや FunctionKeyBar を前提にしない

### 5.5 履歴画面

- 取得 API:
  - `GET /api/incoming/work-items`
  - `warehouse_id = selectedWarehouse.id`
  - `picker_id = current picker`
  - `status = all`
  - `from_date = today`
  - `to_date = today`
  - `limit = 100`
- 表示対象:
  - 当日の入荷作業
- 表示項目:
  - 商品コード
  - 商品名
  - 入荷対象倉庫名
  - 入荷日
  - 数量
  - 作業ステータス

#### 編集可能条件

DENSO 実装準拠で以下を満たす場合のみ編集遷移を許可する。

- `workItem.status` が `WORKING` または `COMPLETED`
- かつ関連 `schedule.status` が以下のいずれか
  - `CONFIRMED`
  - `PENDING`
  - `PARTIAL`

`TRANSMITTED` と `CANCELLED` は編集不可。

補足:
説明書上は「履歴編集は CONFIRMED」と読めるが、DENSO 実コードは `canEditFromHistory || canStartWork` 判定になっているため、本仕様書では実コード基準に合わせる。

---

## 6. 登録ロジック

### 6.1 新規入荷

以下を DENSO と同じ順序で行う。

```text
1. startWork
2. updateWorkItem
3. completeWorkItem
```

#### Step 1. 作業開始

`POST /api/incoming/work-items`

送信値:

- `incoming_schedule_id = selectedSchedule.id`
- `picker_id = current picker`
- `warehouse_id = selectedWarehouse.id`

重要:

- `warehouse_id` には入荷対象倉庫ではなく作業倉庫を送る
- これが DENSO と同じ仮想倉庫対応の基点

#### Step 2. 作業更新

`PUT /api/incoming/work-items/{id}`

送信値:

- `work_quantity`
- `work_arrival_date = today`
- `work_expiration_date`
- `location_id`

#### Step 3. 作業完了

`POST /api/incoming/work-items/{id}/complete`

### 6.2 履歴編集

履歴編集では DENSO と同じく、

```text
1. startWork は行わない
2. updateWorkItem のみ行う
3. completeWorkItem は行わない
```

既存 `currentWorkItem.id` を使って更新する。

### 6.3 `ALREADY_WORKING` の扱い

DENSO と同じく、`startWork` が `ALREADY_WORKING` を返した場合はエラー扱いにしない。

- 既存 work item を取得できたものとして後続の `updateWorkItem` へ進む
- Android 現行実装のように単純失敗にはしない

### 6.4 成功後の処理

- 成功メッセージ表示
- 商品一覧再読込
- 必要に応じて履歴再読込
- 入力状態のクリア
- スケジュール一覧へ戻る

---

## 7. 仮想倉庫対応仕様

### 7.1 業務上の意味

実倉庫で作業している担当者が、仮想倉庫に紐づく入荷予定を処理できるようにする。

### 7.2 Android で保持すべきルール

- 商品一覧取得の軸は作業倉庫
- 作業開始の `warehouse_id` も作業倉庫
- ロケーション検索も作業倉庫
- ただし表示する入荷先はスケジュール倉庫
- `schedule.warehouseId` と `schedule.warehouseName` を上書きしない
- 履歴画面でも `workItem.schedule.warehouseName` を表示し続ける

### 7.3 UI 上の必須表現

仮想倉庫ケースでは、以下の 2 つを区別して見せる。

- 作業倉庫: 今いる倉庫
- 入荷対象倉庫: 実際に入荷処理される倉庫

### 7.4 今回やらないこと

- 仮想倉庫専用画面の追加
- 仮想倉庫専用 API への分岐
- `is_virtual` 前提のハードコード

必要になれば将来的にサーバーから `is_virtual` 等を受けてもよいが、今回の parity 実装では必須ではない。

---

## 8. 実装要件

### 8.1 Repository / API

Android 側の入荷 repository は DENSO と同等の責務に揃える。

- `getWorkingScheduleIds()` を持つ
- `startWork()` は `ALREADY_WORKING` を成功扱いできる
- `updateWorkItem()` / `completeWorkItem()` に責務を分ける
- `searchLocations()` は `limit` を扱える
- `getWorkItems()` は `from_date` `to_date` `limit` を扱える

### 8.2 ViewModel

- 倉庫選択時に配下状態をリセットして商品再読込
- 商品一覧取得と作業中 ID 取得を並列化
- 検索時も作業中 ID を再評価
- 入力初期化は DENSO の `prepareInputForSchedule()` に寄せる
- 履歴編集条件を DENSO 実コード準拠にする
- 成功後の再読込と画面復帰を DENSO 同等にする

### 8.3 UI

- `FunctionKeyBar` 前提を廃止
- F キー名称を UI 文言として残さない
- DENSO の狭小画面前提レイアウトをそのまま移植しない
- ただし表示情報量と業務判断に必要な情報は減らさない

---

## 9. 現行 Android 実装との差分

- `FunctionKeyBar` と F キー導線が残っているため削除対象
- `startWork()` が `ALREADY_WORKING` を成功扱いしていない
- DENSO 側の `getWorkingScheduleIds()` 相当がなく、責務が ViewModel 側に寄っている
- 履歴編集条件が DENSO 実装と一致していない
- 仮想倉庫時に「作業倉庫」と「入荷対象倉庫」を区別して明示する仕様が不足している
- `from_date` `to_date` `limit` を含む履歴取得条件が DENSO 基準で揃っていない

---

## 10. 受け入れ条件

### 10.1 通常入荷

- 作業倉庫 = 入荷対象倉庫のケースで、新規入荷が `start → update → complete` で完了する

### 10.2 仮想倉庫入荷

- 作業倉庫 != 入荷対象倉庫でも商品一覧に対象商品が出る
- スケジュール一覧で入荷対象倉庫が確認できる
- 登録時に `incoming_schedule_id` は対象スケジュール、`warehouse_id` は作業倉庫で送信される
- 完了後の履歴でも入荷対象倉庫が保持される

### 10.3 冪等開始

- 同一スケジュールで作業開始済みの場合、`ALREADY_WORKING` で続行できる

### 10.4 履歴編集

- 履歴から編集可能な item のみ入力画面へ遷移できる
- 履歴編集時は `update` のみ実行し `complete` は呼ばない

### 10.5 Android UI

- F1/F2/F3/F4 がなくても全操作ができる
- 画面サイズは Android 向けに最適化されている
- ただし DENSO と同じ業務判断情報を欠落させない

---

## 11. 実装時の優先順位

1. ロジック parity
2. 仮想倉庫対応
3. 履歴編集条件の一致
4. Android 向け UI 置き換え

UI の見た目差分より、送信値・遷移条件・編集条件・再読込条件を DENSO と一致させることを優先する。

---

## 12. 作業誤りの整理と移管メモ

### 12.1 2026-04-20 時点の整理

- この検討は `sakemaru-handy-android` 向けの内容であり、`sakemaru-delivery-handy` 側で進める作業ではない
- 今回の入荷対応は **Android ハンディアプリ側の入荷機能実装・修正** が対象
- `sakemaru-delivery-handy` の既存差分は本件の対象外として扱う

### 12.2 別プロジェクトで再開する際の前提

作業再開先:

- `/Users/jungsinyu/Projects/sakemaru-handy-android`

起点資料:

- 本ファイル `prompts/20260420/incoming-spec.md`
- `/Users/jungsinyu/Projects/sakemaru-handy-denso/prompts/explain/incoming-logic.md`
- DENSO 実装の `IncomingViewModel`, `IncomingRepository`, `IncomingRepositoryImpl`

### 12.3 実装対象として整理済みの変更内容

以下を `sakemaru-handy-android` 側で実装する。

1. `core/domain` の入荷モデルと repository I/F を DENSO 側の責務へ合わせる
2. `core/network` の Incoming API / model / repository 実装を DENSO 準拠にする
3. `feature/inbound` の `IncomingState` / `IncomingViewModel` を DENSO ロジック準拠にする
4. 入荷 UI から F キー依存を外し、通常の Android ボタン操作へ置き換える
5. 仮想倉庫ケースで「作業倉庫」と「入荷対象倉庫」を画面上で区別表示する
6. 履歴編集条件を DENSO 実コード準拠にする
7. 登録成功後の戻り先をスケジュール一覧に揃える

### 12.4 具体的な修正対象ファイル

再開時に主に見るファイル:

- `app/src/main/java/biz/smt_life/android/sakemaru_handy_denso/navigation/HandyNavHost.kt`
- `core/domain/src/main/java/biz/smt_life/android/core/domain/model/IncomingModels.kt`
- `core/domain/src/main/java/biz/smt_life/android/core/domain/repository/IncomingRepository.kt`
- `core/network/src/main/java/biz/smt_life/android/core/network/api/IncomingApi.kt`
- `core/network/src/main/java/biz/smt_life/android/core/network/model/IncomingApiModels.kt`
- `core/network/src/main/java/biz/smt_life/android/core/network/repository/IncomingRepositoryImpl.kt`
- `feature/inbound/src/main/java/biz/smt_life/android/feature/inbound/incoming/IncomingState.kt`
- `feature/inbound/src/main/java/biz/smt_life/android/feature/inbound/incoming/IncomingViewModel.kt`
- `feature/inbound/src/main/java/biz/smt_life/android/feature/inbound/incoming/WarehouseSelectionScreen.kt`
- `feature/inbound/src/main/java/biz/smt_life/android/feature/inbound/incoming/ProductListScreen.kt`
- `feature/inbound/src/main/java/biz/smt_life/android/feature/inbound/incoming/ScheduleListScreen.kt`
- `feature/inbound/src/main/java/biz/smt_life/android/feature/inbound/incoming/IncomingInputScreen.kt`
- `feature/inbound/src/main/java/biz/smt_life/android/feature/inbound/incoming/HistoryScreen.kt`

### 12.5 実装時の論点メモ

- `startWork()` は `ALREADY_WORKING` を失敗扱いしない
- 商品一覧取得時は入荷予定取得と `WORKING` 作業取得を並列化する
- 新規入荷は `start -> update -> complete`
- 履歴編集は `update` のみ
- ロケーション検索は作業倉庫の `warehouse_id` を使う
- 仮想倉庫時でも表示用の入荷対象倉庫名は `schedule.warehouseName` を維持する
- 履歴編集条件は DENSO 実コードの
  `workItem.status.canEdit && (scheduleStatus.canEditFromHistory || scheduleStatus.canStartWork)`
  に合わせる

### 12.6 再開時の確認手順

1. `sakemaru-handy-android` の現在差分を確認する
2. 上記対象ファイルに他作業の変更が入っていないか確認する
3. 本仕様書の「6. 登録ロジック」「7. 仮想倉庫対応仕様」「8. 実装要件」に従って実装する
4. 実装後は少なくとも `:app:compileDebugKotlin` と `:app:assembleDebug` を確認する

### 12.7 注意

- `sakemaru-delivery-handy` 側には本件の仕様変更を入れない
- 別スレッドまたは別作業ディレクトリで再開する場合も、作業対象は必ず `sakemaru-handy-android` とする
