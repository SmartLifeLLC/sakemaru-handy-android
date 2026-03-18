# Work Plan: picking-group-by-product

- **ID**: picking-group-by-product
- **作成日**: 2026-03-18
- **最終更新**: 2026-03-18
- **ステータス**: 完了
- **ディレクトリ**: `/Users/jungsinyu/Projects/sakemaru-handy-android/prompts/20260318/20260318-070540-picking-group-by-product/`

## セッション再開手順

コンテキストがクリアされた場合、以下を読んで作業を再開する:

1. このファイルを読む（20260318-070540-picking-group-by-product-boot.md）
2. 20260318-070540-picking-group-by-product-plan.md を読む（作業計画の全体像）
3. 下記「進捗」テーブルで現在のPhaseを確認
4. 「Phase完了記録」セクションで完了済みPhaseの実績を確認
5. 「作業中コンテキスト」セクションで途中データを確認
6. 未完了の最初のPhaseから plan.md の該当セクションを読んで作業再開

## 概要

P21 出庫ピッキング画面で、同一商品（itemId）の PENDING アイテムを1画面にグルーピングし、合計数量入力 + 得意先別個別入力を両立するUIに変更する。合計入力時は欠品を自動で上部の得意先から引く。

## 重要な設計制約

- API 変更なし: 既存の `POST /api/picking/tasks/{id}/update` を得意先ごとに呼ぶ
- 登録はまとめて送信（全得意先を一括で API 呼び出し）
- 進捗カウンターはグループ数ベースに変更
- Portrait レイアウトとの互換性を維持
- 得意先の並び順は API 返却順
- 同一得意先のケース・バラ混在は1行に表示
- 出荷数0は `picked_qty=0` で送信
- 合計変更時は個別修正を上書き

## 対象ファイル

### 新規作成
なし

### 既存変更
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingState.kt`
  - `GroupedPickingItem`, `CustomerEntry` データクラス追加
  - State フィールド変更
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingViewModel.kt`
  - グルーピングロジック、振り分けロジック、一括登録フロー
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingScreen.kt`
  - 左ペイン: 得意先名削除、得意先数表示
  - 右ペイン: `GroupedQuantitySection` に変更（合計入力 + 得意先リスト）

### 参照のみ（変更禁止）
- `core/domain/src/main/java/biz/smt_life/android/core/domain/model/PickingTask.kt`
- `core/network/src/main/java/biz/smt_life/android/core/network/model/PickingModels.kt`
- `core/network/src/main/java/biz/smt_life/android/core/network/repository/PickingTaskRepositoryImpl.kt`
- `app/src/main/AndroidManifest.xml`

## テストデータ

- Preview で確認: 同一 itemId で得意先 A/B/C、ケース・バラ混在のサンプルデータ
- 実機テスト: API からの実データでグルーピング動作確認

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: データモデル & State 変更 | 完了 | 2026-03-18 | GroupedPickingItem, CustomerEntry, State 再設計 |
| P2: ViewModel グルーピング & 振り分けロジック | 完了 | 2026-03-18 | グルーピング、合計↔個別連動、一括登録 |
| P3: UI — ProductInfoSection 変更 | 完了 | 2026-03-18 | 得意先名削除、得意先数表示 |
| P4: UI — GroupedQuantitySection | 完了 | 2026-03-18 | 合計入力 + 得意先リスト + Portrait対応 |
| P5: カウンター & ナビゲーション調整 | 完了 | 2026-03-18 | グループ数ベースのカウンター、CompletionCard |
| P6: Preview & ビルド確認 | 完了 | 2026-03-18 | サンプルデータ更新、ビルド成功 |

---

## 作業中コンテキスト

> Phase作業中に蓄積される中間データ。セッション再開時に必ず確認。

### 既存コード構造（参照用）
- `OutboundPickingState.kt`: 105行。`pendingItems`, `currentIndex`, `pickedQtyInput` が現在の入力管理
- `OutboundPickingViewModel.kt`: 343行。`initialize`, `registerCurrentItem`, `onPickedQtyChange`, `moveToPrev/Next`
- `OutboundPickingScreen.kt`: Portrait対応済み。`ProductInfoSection` + `QuantityInputSection` に分離済み
- API登録: `POST /api/picking/tasks/{wms_picking_item_result_id}/update` — Body: `{ picked_qty, picked_qty_type }`

### ユーザー回答（確認事項）
- 得意先並び順: API 返却順
- ケース・バラ混在: 1行に表示
- 0入力: `picked_qty=0` で送信
- 合計→個別→再合計: 個別修正は上書き
- 登録失敗: まとめて登録
- カウンター: グループ数に変更

### Git ブランチ
- 作業ブランチ: feature/design-upgrade
- ベースブランチ: master

---

## Phase完了記録

> 各Phase完了時にここに実績を追記する。

### P1: データモデル & State 変更
- 完了日: 2026-03-18
- 実績:
  - OutboundPickingState.kt を全面書き換え
  - GroupedPickingItem, CustomerEntry, CustomerEntryDetail データクラス追加
  - 旧フィールド（pendingItems, currentIndex, pickedQtyInput）削除
  - 新計算プロパティ（currentGroup, totalGroupCount, registeredGroupCount, totalCasePlanned, totalPiecePlanned）追加

### P2: ViewModel グルーピング & 振り分けロジック
- 完了日: 2026-03-18
- 実績:
  - OutboundPickingViewModel.kt を全面書き換え
  - groupPendingItems: itemId→customerNameの2段グルーピング
  - onTotalCase/PieceInputChange: 上から順に自動振り分け
  - onCustomerCase/PieceQtyChange: 個別入力→合計再計算
  - registerGroupedItem: 全得意先一括登録

### P3: UI — ProductInfoSection 変更
- 完了日: 2026-03-18
- 実績:
  - ProductInfoSection の引数を GroupedPickingItem に変更
  - 得意先名削除、「N 得意先」表示追加

### P4: UI — GroupedQuantitySection
- 完了日: 2026-03-18
- 実績:
  - GroupedQuantitySection 新規作成（合計ケース/バラ入力 + 得意先リスト）
  - CustomerEntryRow 新規作成（得意先名 + ケース/バラ個別入力）
  - OutboundPickingBody の引数を新しいハンドラに変更

### P5: カウンター & ナビゲーション調整
- 完了日: 2026-03-18
- 実績:
  - ヘッダーカウンターを registeredGroupCount / totalGroupCount に変更
  - currentGroup != null 判定に変更
  - ImageDialog を state.currentGroup?.images に変更

### P6: Preview & ビルド確認
- 完了日: 2026-03-18
- 実績:
  - Preview をグルーピング版サンプルデータに更新（居酒屋A/レストランB/ホテルC）
  - Landscape + Portrait 両方のPreview更新
  - `./gradlew :feature:outbound:compileDebugKotlin` ビルド成功
