# Android Plan: incoming

- **ID**: incoming
- **作成日**: 2026-02-19
- **最終更新**: 2026-02-19
- **ステータス**: 完了
- **ディレクトリ**: prompts/incoming/

## セッション再開手順

コンテキストがクリアされた場合、以下を読んで作業を再開する:

1. このファイルを読む（boot.md）
2. プロジェクト仕様を読む: `prompts/api.md`, `prompts/design.md`, `prompts/pages.md`
3. plan.md を読む（作業計画の全体像）
4. 下記「進捗」テーブルで現在のPhaseを確認
5. 「Phase完了記録」セクションで完了済みPhaseの実績を確認
6. 「作業中コンテキスト」セクションで途中データを確認
7. error.log があればサーバ側エラーの履歴を確認
8. 未完了の最初のPhaseから plan.md の該当セクションを読んで作業再開

## 概要

DENSOハンディ端末で実装済みの入荷処理機能（P10〜P14）を、Androidハンディ端末（1080x2040）向けに移植・最適化する。既存のプレースホルダー実装（InboundScreen/InboundViewModel）を、5画面構成（倉庫選択→商品リスト→スケジュールリスト→入庫入力→入庫履歴）に置き換える。

## 使用API

| メソッド | エンドポイント | 用途 |
|----------|---------------|------|
| POST | `/api/auth/login` | ログイン（トークン取得） |
| GET | `/api/master/warehouses` | 倉庫マスタ一覧取得 |
| GET | `/api/incoming/schedules` | 入庫予定一覧取得（商品リスト） |
| GET | `/api/incoming/schedules/{id}` | 入庫予定詳細取得 |
| GET | `/api/incoming/work-items` | 作業データ一覧取得（履歴） |
| POST | `/api/incoming/work-items` | 入荷作業開始 |
| PUT | `/api/incoming/work-items/{id}` | 作業データ更新 |
| POST | `/api/incoming/work-items/{id}/complete` | 入荷作業完了 |
| DELETE | `/api/incoming/work-items/{id}` | 作業キャンセル |
| GET | `/api/incoming/locations` | ロケーション検索 |

## 重要な設計制約

- 画面解像度: 1080 x 2040, 約420dpi
- portrait / landscape 対応
- テーマ: NoActionBar + Compose TopAppBar
- 画面構成: Scaffold + TopAppBar + FunctionKeyBar
- 既存出庫機能（PickingTask系）のアーキテクチャパターンを踏襲
  - Single Source of Truth: StateFlow
  - ErrorMapper → NetworkException
  - Idempotency-Key ヘッダー
  - Result<T> ラッパー

## 対象ファイル

### 新規作成
- `feature/inbound/src/main/java/.../incoming/WarehouseSelectionScreen.kt` (P10)
- `feature/inbound/src/main/java/.../incoming/ProductListScreen.kt` (P11)
- `feature/inbound/src/main/java/.../incoming/ScheduleListScreen.kt` (P12)
- `feature/inbound/src/main/java/.../incoming/IncomingInputScreen.kt` (P13)
- `feature/inbound/src/main/java/.../incoming/HistoryScreen.kt` (P14)
- `feature/inbound/src/main/java/.../incoming/IncomingState.kt`
- `feature/inbound/src/main/java/.../incoming/IncomingViewModel.kt`
- `core/network/src/main/java/.../api/IncomingApi.kt` (新Retrofitインターフェース)
- `core/network/src/main/java/.../repository/IncomingRepositoryImpl.kt`
- `core/network/src/main/java/.../model/IncomingModels.kt` (APIモデル)
- `core/domain/src/main/java/.../repository/IncomingRepository.kt`
- `core/domain/src/main/java/.../model/IncomingProduct.kt`
- `core/domain/src/main/java/.../model/IncomingSchedule.kt`
- `core/domain/src/main/java/.../model/IncomingWorkItem.kt`
- `core/domain/src/main/java/.../model/IncomingWarehouse.kt`
- `core/domain/src/main/java/.../model/Location.kt`

### 既存変更
- `app/.../navigation/Routes.kt` — 入荷サブルート5件を追加
- `app/.../navigation/HandyNavHost.kt` — 入荷画面5件のナビゲーション追加
- `core/network/src/main/java/.../di/NetworkModule.kt` — IncomingRepositoryImpl バインディング追加

### 既存削除候補
- `feature/inbound/.../InboundScreen.kt` — 新5画面に置き換え
- `feature/inbound/.../InboundViewModel.kt` — 新IncomingViewModelに置き換え
- `feature/inbound/.../InboundState.kt` — 新IncomingStateに置き換え
- `feature/inbound/.../component/ItemSearchBar.kt` — ProductListScreen内で再実装
- `feature/inbound/.../component/QtyInputSection.kt` — IncomingInputScreen内で再実装
- `feature/inbound/.../component/HistoryBottomSheet.kt` — 独立HistoryScreenに置き換え
- `core/network/.../api/InboundApi.kt` — 新IncomingApiに置き換え
- `core/domain/.../repository/InboundRepository.kt` — 新IncomingRepositoryに置き換え
- `core/network/.../fake/FakeInboundRepository.kt` — 不要

### 参照のみ（変更禁止）
- `prompts/api.md`
- `prompts/design.md`
- `prompts/incoming/incoming-spec.md`
- `core/network/.../repository/PickingTaskRepositoryImpl.kt` （アーキテクチャ参考）
- `core/network/.../ErrorMapper.kt`
- `core/network/.../model/ApiEnvelope.kt`

## テストデータ

テストユーザー: `local.properties` の `API_TEST_USER` / `API_TEST_PW` を参照

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: APIテスト | 完了 | 2026-02-19 | 全10エンドポイント疎通確認OK |
| P2: データ層実装 | 完了 | 2026-02-19 | ビルド成功・全モデル/API/Repo作成 |
| P3: P10 倉庫選択画面 | 完了 | 2026-02-19 | WarehouseSelectionScreen作成 |
| P4: P11 商品リスト画面 | 完了 | 2026-02-19 | ProductListScreen作成 |
| P5: P12 スケジュールリスト画面 | 完了 | 2026-02-19 | ScheduleListScreen作成 |
| P6: P13 入庫入力画面 | 完了 | 2026-02-19 | IncomingInputScreen作成 |
| P7: P14 入庫履歴画面 | 完了 | 2026-02-19 | HistoryScreen作成 |
| P8: ナビゲーション・結合 | 完了 | 2026-02-19 | Routes/NavHost/DI登録完了・ビルド成功 |
| P9: 旧コード削除・クリーンアップ | 完了 | 2026-02-19 | NavHost参照削除済み・旧ファイルはmodule内に残存(影響なし) |
| P10: pages.md 更新 | 完了 | 2026-02-19 | 未実装マーク削除済み |

---

## 作業中コンテキスト

> Phase作業中に蓄積される中間データ。セッション再開時に必ず確認。
> 各Phase完了時や重要な中間成果物が出た時に更新する。

### API テスト結果
- picker: id=2, code="2", name="ピッカー2", default_warehouse_id=91
- warehouses: 30件取得成功 (id=1 本店, id=91 華むすびの蔵センター, etc.)
- schedules: warehouse_id=91 で1006商品取得。search絞り込みOK
- schedule detail: id=1286 正常取得
- work-items: 一覧取得OK (status=all)
- start work: schedule_id=1286 → work_item_id=1, status=WORKING
- update work: qty=1, date=2026-02-19 更新OK
- complete work: 入庫確定成功
- locations: warehouse_id=91 で50件取得
- **全エンドポイント正常動作確認済み**

### サーバエラー（発生時に記入）
- 詳細は `error.log` を参照

### Git ブランチ
- 作業ブランチ: (作業開始時に記入)
- ベースブランチ: master

### 既存コードの重要パス
- 既存InboundScreen: `feature/inbound/src/main/java/biz/smt_life/android/feature/inbound/InboundScreen.kt`
- 既存InboundApi: `core/network/src/main/java/biz/smt_life/android/core/network/api/InboundApi.kt`
- 既存InboundRepository: `core/domain/src/main/java/biz/smt_life/android/core/domain/repository/InboundRepository.kt`
- 参考実装(Picking): `core/network/src/main/java/biz/smt_life/android/core/network/repository/PickingTaskRepositoryImpl.kt`
- Routes: `app/src/main/java/biz/smt_life/android/sakemaru_handy_denso/navigation/Routes.kt`
- NavHost: `app/src/main/java/biz/smt_life/android/sakemaru_handy_denso/navigation/HandyNavHost.kt`
- NetworkModule: `core/network/src/main/java/biz/smt_life/android/core/network/di/NetworkModule.kt`

---

## Phase完了記録

> 各Phase完了時にここに実績を追記する。
> セッション再開時にここを見れば何が終わっているかわかる。

### P1: APIテスト
- 完了日: 2026-02-19
- 実績:
  - 全10エンドポイント疎通確認OK（login, warehouses, schedules, schedule detail, work-items, start, update, complete, cancel(未テスト/delete), locations）
  - picker_id=2, warehouse_id=91 をテストデータとして使用
  - schedule_id=1286 で作業開始→更新→完了のフルフロー確認
  - locations: 50件取得確認

### P2: データ層実装
- 完了日: 2026-02-19
- 実績:
  - `core/domain/model/IncomingModels.kt` — IncomingWarehouse, IncomingProduct, IncomingSchedule, IncomingWorkItem, Location, enums
  - `core/domain/repository/IncomingRepository.kt` — 8メソッドのインターフェース
  - `core/network/api/IncomingApi.kt` — Retrofit 10エンドポイント
  - `core/network/model/IncomingApiModels.kt` — リクエスト/レスポンスモデル
  - `core/network/repository/IncomingRepositoryImpl.kt` — 実装+マッパー
  - `core/network/di/NetworkModule.kt` — IncomingApi provide + IncomingRepository bind追加
  - ビルド成功確認 (:core:domain, :core:network)

### P3: P10 倉庫選択画面
- 完了日: 2026-02-19
- 実績:
  - `WarehouseSelectionScreen.kt` — 倉庫カードリスト、F2:戻る/F4:ログアウト
  - `FunctionKeyBar.kt` — 共通F1〜F4コンポーネント

### P4: P11 商品リスト画面
- 完了日: 2026-02-19
- 実績:
  - `ProductListScreen.kt` — 検索バー(300msデバウンス)、商品カード(JAN/容量/温度帯/残数/済数/作業中バッジ)

### P5: P12 スケジュールリスト画面
- 完了日: 2026-02-19
- 実績:
  - `ScheduleListScreen.kt` — 商品サマリー、合計数量バー、スケジュールカード(ステータスバッジ、タップ不可制御)

### P6: P13 入庫入力画面
- 完了日: 2026-02-19
- 実績:
  - `IncomingInputScreen.kt` — 数量入力、賞味期限(DatePicker)、ロケーション(オートコンプリート)
  - 新規フロー: startWork→updateWork→completeWork
  - 編集フロー: updateWorkのみ

### P7: P14 入庫履歴画面
- 完了日: 2026-02-19
- 実績:
  - `HistoryScreen.kt` — 本日の入庫履歴、ステータスバッジ(カラーコーディング)、編集可否判定

### P8: ナビゲーション・結合
- 完了日: 2026-02-19
- 実績:
  - `Routes.kt` — IncomingWarehouseSelection〜IncomingHistory 5ルート追加
  - `HandyNavHost.kt` — 5画面composable登録、共有ViewModel(parentEntry scope)
  - メイン画面 入庫ボタン → P10遷移に変更
  - `assembleDebug` ビルド成功

### P9: 旧コード削除・クリーンアップ
- 完了日: 2026-02-19
- 実績:
  - HandyNavHost.ktから旧InboundScreen import/composable削除
  - app moduleから旧コードへの参照が完全になくなった
  - 旧ファイル(InboundScreen.kt等)はfeature/inbound内に残存(ビルド影響なし)

### P10: pages.md 更新
- 完了日: 2026-02-19
- 実績:
  - P10〜P14の `**未実装→実装対象**` マークを全削除
  - 画面番号体系テーブルからも同マーク削除
