# Android Plan: P22-Redesign-0223

- **ID**: P22-Redesign-0223
- **作成日**: 2026-02-23
- **最終更新**: 2026-02-23
- **ステータス**: 完了
- **ディレクトリ**: prompts/P22-Redesign-0223/

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

P22（出庫履歴画面 = PickingHistoryScreen）のUIを P21（出庫データ入力画面）のデザインに統一する。
ヘッダーは変更禁止。ロジック・API・ViewModel・ナビゲーションは一切変更しない。UIファイルのみ変更。

---

## 使用API

なし（純粋なUIリデザイン）

---

## 重要な設計制約

- 画面解像度: 1080 x 2400, 420dpi, portrait/landscape対応
- API Level 33
- テーマ: NoActionBar + Compose TopAppBar
- **ヘッダー（TopAppBar "出庫履歴" + 戻るボタン）変更禁止**
- **ViewModel・ロジック・ナビゲーション変更禁止**
- UIファイル（PickingHistoryScreen.kt）のみ変更対象

---

## P21 色定数（適用先で使用する値）

```kotlin
private val TitleRed     = Color(0xFFC0392B)  // タイトル文字・Shortage/Pending バッジ
private val AccentOrange = Color(0xFFE67E22)  // 強調・ボタン・PICKING バッジ
private val DividerGold  = Color(0xFFF9A825)  // (ヘッダー区切り線。本画面では使用しない)
private val BodyBg       = Color.White         // 画面背景
private val TextPrimary  = Color(0xFF212529)  // 主テキスト
private val TextSecond   = Color(0xFF555555)  // 補助テキスト
private val BorderGray   = Color(0xFFCCCCCC)  // カード枠
private val ReadonlyBg   = Color(0xFFF5F5F5)  // (本画面では使用しない)
private val ReadonlyText = Color(0xFF888888)  // 空状態テキスト
private val BadgeGreen   = Color(0xFF27AE60)  // COMPLETED バッジ・確定済みバッジ
```

---

## Step 0 — 実装確認結果

### 対象ファイル

- UI（変更対象）: `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/PickingHistoryScreen.kt`
- State/ViewModel: 変更禁止

### 変更箇所一覧

| コンポーネント | P22現状 | P21スタイルへの変更 |
|--------------|---------|------------------|
| `Scaffold containerColor` | なし（デフォルト） | `BodyBg = Color.White` |
| `HistoryItemCard` カード | `Card(elevation=2dp)` | `OutlinedCard(border=BorderGray 1dp, radius=12dp)` |
| `HistoryItemCard` padding | `padding(16.dp)` | `padding(12.dp)` |
| `HistoryItemCard` spacing | `spacedBy(8.dp)` | `spacedBy(6.dp)` |
| `HorizontalDivider` | デフォルト色 | `color = Color(0xFFEEEEEE)` |
| コース情報カード（課題なし） | `Card` | `OutlinedCard(border=AccentOrange 2dp, radius=12dp)` |
| コース名テキスト | `titleMedium + Bold` | `16sp, Bold, color=TitleRed` |
| エリア名テキスト | `onSurfaceVariant` | `14sp, color=TextSecond` |
| 確定済バッジ | `primaryContainer` 色 | `BadgeGreen + Color.White` |
| `InfoRow` ラベル | `bodyMedium + onSurfaceVariant` | `13sp + TextSecond` |
| `InfoRow` 値 | `bodyMedium + デフォルト` | `13sp + TextPrimary` |
| `StatusBadge` PICKING | `tertiary` | `AccentOrange` |
| `StatusBadge` COMPLETED | `primary` | `BadgeGreen` |
| `StatusBadge` SHORTAGE/PENDING | `error` | `TitleRed` |
| `HistoryBottomBar` 確定ボタン | `Button(デフォルトBlue)` | `Button(containerColor=AccentOrange)` |
| 削除ボタン | `OutlinedButton(デフォルト)` | `OutlinedButton(borderColor=AccentOrange)` |
| ReadOnly アイコン tint | `primary` | `AccentOrange` |
| ReadOnly テキスト色 | `onSurfaceVariant` | `TextSecond` |
| 空状態テキスト色 | `onSurfaceVariant` | `ReadonlyText` |

---

## 対象ファイル

### 既存変更（1ファイルのみ）
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/PickingHistoryScreen.kt`

### 参照のみ（変更禁止）
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/PickingHistoryViewModel.kt`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/PickingHistoryState.kt`

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: UIリデザイン実装 | 未着手 | 2026-02-23 | PickingHistoryScreen.kt 書き換え |
| P2: ビルド確認 | 未着手 | 2026-02-23 | assembleDebug |

---

## 作業中コンテキスト

### Git ブランチ
- 作業ブランチ: `feature/design-upgrade`
- ベースブランチ: `master`

---

## Phase完了記録

（完了後に記入）
