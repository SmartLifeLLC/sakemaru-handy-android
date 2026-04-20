# Android Plan: P10-Redesign-0223

- **ID**: P10-Redesign-0223
- **作成日**: 2026-02-23
- **最終更新**: 2026-02-23
- **ステータス**: 完了
- **ディレクトリ**: prompts/P10-Redesign-0223/

## セッション再開手順

1. このファイルを読む（boot.md）
2. プロジェクト仕様を読む: `prompts/api.md`, `prompts/design.md`, `prompts/pages.md`
3. plan.md を読む（作業計画の全体像）
4. 下記「進捗」テーブルで現在のPhaseを確認
5. 「Phase完了記録」セクションで完了済みPhaseの実績を確認
6. 「作業中コンテキスト」セクションで途中データを確認
7. 未完了の最初のPhaseから plan.md の該当セクションを読んで作業再開

---

## 概要

P10〜P14（入荷処理画面群）のUIを**緑テーマ**にリデザイン。
ヘッダー・背景・カード・バッジ・ボタン等の色を緑系統に統一する。ロジック・API・ViewModel・ナビゲーション変更禁止。

---

## 使用API

なし（純粋なUIリデザイン）

---

## 重要な設計制約

- 画面解像度: 1080 x 2400, 420dpi, portrait/landscape対応
- API Level 33
- テーマ: NoActionBar + Compose TopAppBar
- **ViewModel・ロジック・ナビゲーション変更禁止**
- UIファイルのみ変更対象（5ファイル）
- 未実装の要素は追加しない（spec の Step 0 チェックリスト準拠）

---

## 緑テーマ色定数

```kotlin
private val AccentGreen  = Color(0xFF27AE60)  // メイン強調色
private val DarkGreen    = Color(0xFF1A7A4A)  // ロケーション・ダーク
private val PressGreen   = Color(0xFF1E8449)  // プレス時
private val BodyBg       = Color(0xFFF0FFF4)  // 画面背景（ミントグリーン）
private val HeaderBg     = Color.White         // ヘッダー背景
private val DividerGreen = Color(0xFFD5F5E3)  // ヘッダー下線
private val CardBorder   = Color(0xFFB2DFDB)  // カード枠（ミント）
private val HistCardBg   = Color(0xFFF0FFF4)  // 履歴カード背景
private val HistCardBdr  = Color(0xFFA5D6A7)  // 履歴カード枠
private val TextPrimary  = Color(0xFF212529)  // 主テキスト
private val TextSecond   = Color(0xFF555555)  // 補助テキスト
private val ReadonlyText = Color(0xFF888888)  // 空状態テキスト
private val DeleteRed    = Color(0xFFE74C3C)  // 削除・エラー
private val BadgeRed     = Color(0xFFE74C3C)  // 履歴件数バッジ
```

---

## 対象ファイル

### 既存変更（5ファイル）
- `feature/inbound/src/main/java/biz/smt_life/android/feature/inbound/incoming/WarehouseSelectionScreen.kt` (P10)
- `feature/inbound/src/main/java/biz/smt_life/android/feature/inbound/incoming/ProductListScreen.kt` (P11)
- `feature/inbound/src/main/java/biz/smt_life/android/feature/inbound/incoming/ScheduleListScreen.kt` (P12)
- `feature/inbound/src/main/java/biz/smt_life/android/feature/inbound/incoming/IncomingInputScreen.kt` (P13)
- `feature/inbound/src/main/java/biz/smt_life/android/feature/inbound/incoming/HistoryScreen.kt` (P14)

### 参照のみ（変更禁止）
- `feature/inbound/src/main/java/biz/smt_life/android/feature/inbound/incoming/IncomingViewModel.kt`
- `feature/inbound/src/main/java/biz/smt_life/android/feature/inbound/incoming/IncomingState.kt`

---

## Step 0 確認結果（実装済み要素）

| 要素 | 実装 | 対象画面 |
|------|------|---------|
| 戻るボタン | ✓ | 全画面 |
| タイトル | ✓ | 全画面 |
| 創高名 | ✓ | P11〜P14（TopAppBar title に含む） |
| 設定アイコン | ✗ | なし |
| 日付バッジ | ✗ | なし（P13 に "入荷日" テキストあり） |
| バーコード入力 | ✗ | なし（DEVのみ） |
| 入荷タイプドロップダウン | ✗ | なし |
| ロケーションサジェスト | ✓ | P13 |
| 賞味期限入力 | ✓ | P13 |
| 空状態 | ✓ | P11、P14 |
| 履歴パネル | ✓ | P14（HistoryScreen） |
| 同期ボタン | ✗ | なし |
| 一致バッジ | ✗ | なし |
| 修正・削除ボタン | ✗ | なし |

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: WarehouseSelectionScreen リデザイン | 完了 | 2026-02-23 | P10 |
| P2: ProductListScreen リデザイン | 完了 | 2026-02-23 | P11 |
| P3: ScheduleListScreen リデザイン | 完了 | 2026-02-23 | P12 |
| P4: IncomingInputScreen リデザイン | 完了 | 2026-02-23 | P13 |
| P5: HistoryScreen リデザイン | 完了 | 2026-02-23 | P14 |
| P6: ビルド確認 | 完了 | 2026-02-23 | BUILD SUCCESSFUL |

---

## 作業中コンテキスト

### Git ブランチ
- 作業ブランチ: `feature/design-upgrade`
- ベースブランチ: `master`

---

## Phase完了記録

### P1〜P6: 全Phase完了
- 完了日: 2026-02-23
- 実績:
  - WarehouseSelectionScreen.kt: 緑テーマ適用（ヘッダー・カード・ボタン）
  - ProductListScreen.kt: 緑テーマ適用（ヘッダー・検索・カード・バッジ）
  - ScheduleListScreen.kt: 緑テーマ適用（ヘッダー・カード・バッジ）
  - IncomingInputScreen.kt: 緑テーマ適用（ヘッダー・フォーム・成功メッセージ）
  - HistoryScreen.kt: 緑テーマ適用（ヘッダー・カード・ステータスバッジ）
  - feature/inbound/build.gradle.kts: material.icons.extended 追加（Inventory2 アイコン対応）
  - assembleDebug: BUILD SUCCESSFUL
