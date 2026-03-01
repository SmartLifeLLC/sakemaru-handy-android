# 入庫処理（P10〜P14）検証・修正 作業計画

## 前提

### 参照仕様
- API仕様: `prompts/api.md`
- デザイン仕様: `prompts/design.md`
- 画面一覧: `prompts/pages.md`

### 完了済みの作業・現在の状況

コミット `8864103 入庫処理機能（P10〜P14）の実装` により、以下がすでに実装済み:
- P10〜P14 の全 UI スクリーン（Compose）
- `IncomingViewModel`（@HiltViewModel、全フロー実装）
- `IncomingRepository` インターフェース + `IncomingRepositoryImpl`（本番API接続）
- `IncomingApi`（Retrofit、全エンドポイント定義）
- DI 登録（NetworkModule で `IncomingRepositoryImpl` をバインド）

**今回の作業目的**: 実装済みコードが実際に正しく動作するかを検証し、問題があれば修正する。

---

## Phase 一覧

| # | Phase | 概要 | 完了条件 |
|---|-------|------|---------|
| P1 | API テスト | 全 Incoming API の疎通確認 | 全APIが正常レスポンスを返す |
| P2 | ビルド確認 | `assembleDebug` が成功するか | BUILD SUCCESSFUL |
| P3 | ナビゲーション確認 | P10〜P14 への画面遷移が正しく設定されているか | 遷移グラフに全ルートが存在 |
| P4 | 動作フロー確認・修正 | P10→P11→P12→P13→P14 のフローで問題を特定・修正 | 全フローが正常動作 |

---

## P1: API テスト

### 目的
使用する全 API エンドポイントの疎通確認とレスポンス構造の検証

### テスト手順

1. `local.properties` からテストユーザー情報を取得
2. `POST /api/auth/login` でトークンを取得（`API_TEST_USER` / `API_TEST_PW` / `WMS_API_KEY`）
3. 以下の順で各エンドポイントをテスト

### テスト対象API

| # | メソッド | エンドポイント | パラメータ | 期待結果 |
|---|----------|---------------|-----------|---------|
| 1 | GET | `/api/master/warehouses` | なし | 倉庫リスト取得（P10用） |
| 2 | GET | `/api/incoming/schedules` | `warehouse_id=<ID>` | 入庫予定商品リスト |
| 3 | GET | `/api/incoming/work-items` | `warehouse_id=<ID>&status=WORKING` | 作業中データ一覧 |
| 4 | GET | `/api/incoming/work-items` | `warehouse_id=<ID>&status=all&from_date=<today>` | 本日の履歴一覧（P14用） |
| 5 | GET | `/api/incoming/locations` | `warehouse_id=<ID>` | ロケーション一覧 |
| 6 | POST | `/api/incoming/work-items` | `{incoming_schedule_id, picker_id, warehouse_id}` | 作業開始（作業データ返却） |
| 7 | PUT | `/api/incoming/work-items/{id}` | `{work_quantity, work_arrival_date}` | 作業データ更新 |
| 8 | POST | `/api/incoming/work-items/{id}/complete` | なし | 作業完了 |

### curl テスト例

```bash
# 1. ログイン（トークン取得）
curl -s -X POST https://wms.lw-hana.net/api/auth/login \
  -H "X-API-Key: <WMS_API_KEY>" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "code=<API_TEST_USER>&password=<API_TEST_PW>" | jq .

# 2. 倉庫一覧取得（TOKEN を1で取得したものに置換）
curl -s -X GET "https://wms.lw-hana.net/api/master/warehouses" \
  -H "X-API-Key: <WMS_API_KEY>" \
  -H "Authorization: Bearer <TOKEN>" | jq .

# 3. 入庫予定取得
curl -s -X GET "https://wms.lw-hana.net/api/incoming/schedules?warehouse_id=<WAREHOUSE_ID>" \
  -H "X-API-Key: <WMS_API_KEY>" \
  -H "Authorization: Bearer <TOKEN>" | jq .
```

### エラー記録ルール
- サーバ側エラー（4xx/5xx）が発生した場合:
  - `prompts/add-P10-P14-0222/error.log` にタイムスタンプ・エンドポイント・ステータスコード・レスポンスボディを追記
  - boot.md の作業中コンテキストにも概要を記入

### 完了条件
- 全APIが正常レスポンスを返すこと
- エラーが発生した場合は error.log に記録済みであること
- レスポンスの構造が `IncomingModels.kt` のデータクラスと一致すること

---

## P2: ビルド確認

### 目的
現在のコードがコンパイルエラーなくビルドできるかを確認

### 手順

```bash
cd C:\Projects\sakemaru-handy-android
./gradlew assembleDebug 2>&1 | tail -50
```

### エラー発生時の対応方針

| エラー種別 | 対応 |
|-----------|------|
| Unresolved reference | インポート漏れ・クラス名の不一致を修正 |
| Type mismatch | DTO とドメインモデルの型不一致を修正 |
| Hilt injection error | DI モジュールのバインド設定を確認・修正 |
| Navigation error | ルート定義の不一致を修正 |

### 完了条件
- `BUILD SUCCESSFUL` が表示されること

---

## P3: ナビゲーション確認

### 目的
P10〜P14 への画面遷移がナビゲーショングラフに正しく定義されているかを確認

### 調査対象

1. `MainActivity.kt` またはナビゲーション設定ファイルで以下のルートが定義されているか確認:
   - `incoming_warehouse_selection`（P10）
   - `incoming_product_list`（P11）
   - `incoming_schedule_list`（P12）
   - `incoming_input`（P13）
   - `incoming_history`（P14）

2. `MainScreen.kt` の「入庫処理」ボタンが `incoming_warehouse_selection` に遷移するか確認

3. 各 Screen の `NavigationBar` / `FunctionKeyBar` のボタン遷移先が正しいか確認

### 確認コマンド例

```bash
# ナビゲーションルート定義の確認
grep -rn "incoming_" C:\Projects\sakemaru-handy-android\app --include="*.kt"
grep -rn "incoming_" C:\Projects\sakemaru-handy-android\feature --include="*.kt"
```

### 修正が必要な場合
- ルートが未定義 → `NavHost` に composable ブロックを追加
- ボタン遷移先が間違っている → pages.md の遷移定義に合わせて修正

### 完了条件
- 全 5 ルートが NavHost に定義されていること
- メイン画面から P10 へ遷移できること（コード上の確認）

---

## P4: 動作フロー確認・修正

### 目的
P10→P11→P12→P13→P14 の実際の動作フローを手動テストまたはログ確認で検証し、問題を修正する

### 確認フロー（pages.md の遷移定義に基づく）

```
P03 メイン → P10 倉庫選択 → P11 商品リスト → P12 スケジュールリスト → P13 入庫入力 → P12
                                    ↓
                              P14 入庫履歴 ←→ P13 入庫入力（編集）
```

### 確認チェックリスト

**P10 倉庫選択**
- [ ] 倉庫リストが API から正常ロードされる（`GET /api/master/warehouses`）
- [ ] 倉庫を選択すると P11 へ遷移する

**P11 商品リスト**
- [ ] 入庫予定商品リストが API から正常ロードされる（`GET /api/incoming/schedules`）
- [ ] 作業中インジケータが正しく表示される（`GET /api/incoming/work-items`）
- [ ] 検索バーが機能する（300ms デバウンス）
- [ ] F1: 検索フォーカス、F2: 戻る、F3: 履歴（P14）が動作する
- [ ] 商品をタップすると P12 へ遷移する

**P12 スケジュールリスト**
- [ ] 選択商品のスケジュール一覧が表示される（P11 取得済みデータ）
- [ ] F2: 戻る、F3: 履歴（P14）が動作する
- [ ] スケジュールをタップすると P13 へ遷移する

**P13 入庫入力**
- [ ] スケジュール情報がヘッダーに表示される
- [ ] 数量・賞味期限・ロケーションが入力できる
- [ ] ロケーション検索が機能する（`GET /api/incoming/locations`）
- [ ] F1: 賞味期限カレンダー、F2: 戻る、F3: 登録 が動作する
- [ ] 登録ボタンで作業開始→更新→完了のフローが動作する
- [ ] 登録成功後 P12 へ戻る

**P14 入庫履歴**
- [ ] 本日の入庫履歴が API から正常ロードされる（`GET /api/incoming/work-items?status=all&from_date=today`）
- [ ] ステータスカラーコーディングが正しい（作業中=tertiary、完了=primary、キャンセル=error）
- [ ] 編集可能なアイテムをタップすると P13（編集モード）へ遷移する
- [ ] F2: 戻る、F3: リスト（P11へ）が動作する

### よくある問題と修正方針

| 問題 | 原因 | 対応 |
|-----|------|------|
| 倉庫リストが空 | API 認証エラー | `NetworkModule` の API Key / Token 設定を確認 |
| 商品リストが空 | `warehouse_id` が正しく渡されていない | ViewModel の `loadProducts()` 引数を確認 |
| 登録後エラー | `picker_id` が null | `IncomingState.pickerId` の初期化を確認 |
| ロケーション候補が出ない | デバウンス設定 or API パラメータ | `onLocationSearchChange()` の debounce 処理を確認 |
| 履歴が空 | `from_date` パラメータが不正 | 日付フォーマット（YYYY-MM-DD）を確認 |

### 完了条件
- 全フローが正常動作すること
- ビルドエラーなし
- サーバエラーがあれば error.log に記録済みであること

---

## 制約（厳守）

- UI ファイル（Screen.kt）は変更しない（`sakemaru-handy-android` の既存 UI を維持）
- 不一致がある場合は UI ではなく、ViewModel・Repository 側を修正する
- `local.properties` の認証情報はコミットしない
- サーバ側エラーは `error.log` に記録する
- 画面解像度 1080x2400 / 420dpi / portrait 対応を維持
- NoActionBar テーマ + Compose TopAppBar パターンを踏襲
- FunctionKeyBar の配置パターンを踏襲（F1〜F4）

## 全体完了条件

- P1: 全APIの疎通確認完了（エラーは error.log に記録）
- P2: `assembleDebug` が `BUILD SUCCESSFUL`
- P3: 全ナビゲーションルートが定義済み
- P4: P10→P11→P12→P13→P14 の全フローが正常動作
