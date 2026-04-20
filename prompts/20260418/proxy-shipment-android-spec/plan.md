# 横持ち出荷 Android 実装計画

- 作成日: 2026-04-19
- 前提: `proxy-shipment-android-investigation.md`, `proxy-shipment-android-design.md`

## P1. Domain / Network 追加

- 追加:
  - `ProxyShipmentApi`
  - `ProxyShipmentModels`
  - `ProxyShipmentRepository`
  - `ProxyShipmentRepositoryImpl`
  - status / quantity / detail model
- 検証:
  - `:core:network` と関連モジュールの compile が通る
  - 一覧 / 詳細 / start / update / complete の interface が API 仕様と一致する
- ロールバック:
  - 新規ファイル削除
- リスク:
  - 2026-04-19 API 仕様との差分取り込み漏れ

### ハネス適合チェック

- `Kotlinx Serialization` を使っているか
- `NetworkException` に正規化できる構造か

## P2. Navigation / Main 入口追加

- 変更:
  - `Routes.kt`
  - `HandyNavHost.kt`
  - `MainScreen.kt`
- 検証:
  - P03 から横持ち出荷一覧へ遷移できる
  - 戻る操作で P03 に復帰できる
- ロールバック:
  - route 追加を戻す
- リスク:
  - 既存の `移動処理` 導線との用語衝突

### ハネス適合チェック

- 既存 UI パターンを壊していないか
- 画面回転でルートが壊れないか

## P3. 一覧画面実装

- 追加:
  - `ProxyShipmentListScreen`
  - `ProxyShipmentListViewModel`
  - `ProxyShipmentListState`
- 実装:
  - 倉庫表示
  - 日付フィルタ
  - 配送コースフィルタ
  - `RESERVED` / `PICKING` タブ
  - start 後の遷移
- 検証:
  - 初回一覧取得
  - `meta.business_date` の反映
  - コース絞り込み
- ロールバック:
  - P30 画面ごと外す
- リスク:
  - 日付初期化仕様の解釈差

### ハネス適合チェック

- `ViewModel + StateFlow` を守っているか
- Portrait / Landscape Preview があるか

## P4. ピッキング画面実装

- 追加:
  - `ProxyShipmentPickingScreen`
  - `ProxyShipmentPickingViewModel`
  - `ProxyShipmentPickingState`
- 実装:
  - 詳細取得
  - JAN 照合
  - 候補ロケーション表示
  - `update` / `complete`
  - 未保存確認ダイアログ
- 検証:
  - `0 <= picked_qty <= assign_qty`
  - `CARTON` 表示
  - update 後の picked / remaining 反映
- ロールバック:
  - P31 画面ごと外す
- リスク:
  - `JanCodeScannerDialog` の複数 JAN 対応

### ハネス適合チェック

- エラー時に UI が固まらないか
- 画面回転時に入力状態が保持されるか

## P5. 結果画面実装

- 追加:
  - `ProxyShipmentResultScreen`
  - `ProxyShipmentResultViewModel`
  - `ProxyShipmentResultState`
- 実装:
  - `FULFILLED` / `SHORTAGE` 表示
  - `stock_transfer_queue_id` 表示
  - 一覧戻り後 refresh
- 検証:
  - `picked_qty = 0` 完了
  - queue id あり / なし
- ロールバック:
  - P32 画面ごと外す
- リスク:
  - complete 後の再読込タイミング

### ハネス適合チェック

- 成功 / 欠品の両状態を Preview で再現できるか

## P6. 文書・試験更新

- 更新:
  - `prompts/pages.md`
  - API 疎通メモ
  - 必要なら `boot.md` 追記
- 検証:
  - `curl` またはテストで 5 API を確認
  - 仕様書と実装差分がない
- ロールバック:
  - 文書差分を戻す
- リスク:
  - 実 API の business date / query 仕様差分

### ハネス適合チェック

- 画面追加内容が `pages.md` に反映されているか
- API 試験結果を残しているか
