# 入荷処理画面群 緑テーマ リデザイン 作業計画

## 前提

### 参照仕様
- API仕様: `prompts/api.md`
- デザイン仕様: `prompts/design.md`
- 画面一覧: `prompts/pages.md`
- 色仕様: `prompts/P10-Redesign-0223/P10-Redesign-0223.md`

### 完了済みの作業・現在の状況
- P20〜P22（出荷）の緑・オレンジテーマリデザイン完了済み
- P10〜P14（入荷）は全画面が `MaterialTheme.colorScheme.*` のデフォルト色のまま

---

## 色定数（全ファイル共通）

```kotlin
private val AccentGreen  = Color(0xFF27AE60)  // メイン強調色
private val DarkGreen    = Color(0xFF1A7A4A)  // ロケーション・ダーク
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
```

---

## Phase 一覧

| # | Phase | 対象ファイル | 完了条件 |
|---|-------|------------|---------|
| P1 | WarehouseSelectionScreen リデザイン | WarehouseSelectionScreen.kt | ビルドエラーなし |
| P2 | ProductListScreen リデザイン | ProductListScreen.kt | ビルドエラーなし |
| P3 | ScheduleListScreen リデザイン | ScheduleListScreen.kt | ビルドエラーなし |
| P4 | IncomingInputScreen リデザイン | IncomingInputScreen.kt | ビルドエラーなし |
| P5 | HistoryScreen リデザイン | HistoryScreen.kt | ビルドエラーなし |
| P6 | ビルド確認 | - | assembleDebug SUCCESS |

---

## P1: WarehouseSelectionScreen リデザイン（P10）

### 変更箇所一覧

| 箇所 | 現状 | 変更後 |
|------|------|--------|
| `Scaffold containerColor` | デフォルト | `BodyBg` |
| `TopAppBar containerColor` | デフォルト | `HeaderBg` |
| TopAppBar title | `Text("倉庫選択")` | アイコン + "倉庫選択" テキスト |
| タイトル色 | デフォルト | `AccentGreen`, 18sp, Bold |
| Inventory2 アイコン | なし | `AccentGreen`, 22dp |
| 戻るボタン色 | デフォルト | `AccentGreen` |
| ヘッダー下線 | なし | `HorizontalDivider(1dp, DividerGreen)` |
| WarehouseCard | `Card(elevation=2dp)` | `OutlinedCard(border=CardBorder 1dp, radius=12dp)` |
| 倉庫名 | デフォルト色 | `TextPrimary`, 18sp, Bold |
| コードテキスト | `onSurfaceVariant` | `TextSecond` |
| 再試行 Button | デフォルト | `containerColor = AccentGreen` |

### 必要なインポート追加
```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
```

### 完了条件
- WarehouseSelectionScreen.kt がビルドエラーなく変更完了

---

## P2: ProductListScreen リデザイン（P11）

### 変更箇所一覧

| 箇所 | 現状 | 変更後 |
|------|------|--------|
| `Scaffold containerColor` | デフォルト | `BodyBg` |
| `TopAppBar containerColor` | デフォルト | `HeaderBg` |
| TopAppBar title | `Text(倉庫名 + 入荷処理)` | アイコン + 同テキスト（緑） |
| タイトル色 | デフォルト | `AccentGreen`, 18sp, Bold |
| Inventory2 アイコン | なし | `AccentGreen`, 22dp |
| 戻るボタン色 | デフォルト | `AccentGreen` |
| ヘッダー下線 | なし | `HorizontalDivider(1dp, DividerGreen)` |
| SearchField leadingIcon 色 | デフォルト | `AccentGreen` |
| ProductCard | `Card(elevation=1dp)` | `OutlinedCard(border=CardBorder 1dp, radius=12dp)` |
| 商品名 | デフォルト | `TextPrimary` |
| コード・規格テキスト | `onSurfaceVariant` | `TextSecond` |
| 残バッジ | `primaryContainer` | `AccentGreen` + `Color.White` |
| 済バッジ | `secondaryContainer` | `Color(0xFF66BB6A)` (薄緑) + `Color.White` |
| 作業中バッジ | `tertiaryContainer` | `AccentGreen` + `Color.White` |
| 空状態テキスト | `onSurfaceVariant` | `ReadonlyText` |

### 完了条件
- ProductListScreen.kt がビルドエラーなく変更完了

---

## P3: ScheduleListScreen リデザイン（P12）

### 変更箇所一覧

| 箇所 | 現状 | 変更後 |
|------|------|--------|
| `Scaffold containerColor` | デフォルト | `BodyBg` |
| `TopAppBar containerColor` | デフォルト | `HeaderBg` |
| TopAppBar title | 倉庫名 + 入荷処理 | アイコン + 同テキスト（緑） |
| 戻るボタン色 | デフォルト | `AccentGreen` |
| ヘッダー下線 | なし | `HorizontalDivider(1dp, DividerGreen)` |
| 商品サマリーCard | `CardDefaults.cardColors(surfaceVariant)` | `OutlinedCard(border=CardBorder 1dp)` |
| 商品名 | デフォルト | `TextPrimary` |
| JAN・規格テキスト | `onSurfaceVariant` | `TextSecond` |
| 合計数量Card | `Card` | `OutlinedCard(border=CardBorder 1dp)` |
| 残 数値色 | `primary` | `AccentGreen` |
| ScheduleCard（選択可） | `Card(elevation=2dp)` | `OutlinedCard(border=AccentGreen 2dp, radius=10dp)` |
| ScheduleCard（選択不可） | `Card(elevation=0dp)` | `OutlinedCard(border=CardBorder 1dp, radius=10dp, alpha=0.5)` |
| 残数量 Surface（選択可） | `primary` | `AccentGreen` |
| 残数量 Surface（選択不可） | `surfaceVariant` | `Color(0xFFEEEEEE)` |
| ScheduleStatusBadge CONFIRMED | `primary` 色 | `AccentGreen` |
| ScheduleStatusBadge TRANSMITTED | `secondary` 色 | `DarkGreen` |
| ScheduleStatusBadge CANCELLED | `error` 色 | `DeleteRed` |
| 倉庫名テキスト | デフォルト | `TextPrimary` |
| 日付テキスト | `onSurfaceVariant` | `TextSecond` |

### 完了条件
- ScheduleListScreen.kt がビルドエラーなく変更完了

---

## P4: IncomingInputScreen リデザイン（P13）

### 変更箇所一覧

| 箇所 | 現状 | 変更後 |
|------|------|--------|
| `Scaffold containerColor` | デフォルト | `BodyBg` |
| `TopAppBar containerColor` | デフォルト | `HeaderBg` |
| TopAppBar title | `倉庫名 入荷処理` | アイコン + 同テキスト（緑） |
| 戻るボタン色 | デフォルト | `AccentGreen` |
| ヘッダー下線 | なし | `HorizontalDivider(1dp, DividerGreen)` |
| 商品情報Card | `CardDefaults.cardColors(surfaceVariant)` | `OutlinedCard(border=CardBorder 1dp, radius=12dp)` |
| 商品名 | デフォルト | `TextPrimary`, Bold |
| JAN/Code テキスト | `onSurfaceVariant` | `TextSecond` |
| 入荷日テキスト | デフォルト | `TextSecond` |
| 入荷数量ラベル | デフォルト | `TextPrimary` |
| 賞味期限ラベル | デフォルト | `TextPrimary` |
| カレンダーアイコン | デフォルト | `AccentGreen` |
| ロケーションラベル | デフォルト | `TextPrimary` |
| ロケーションサジェストCard | `Card(elevation=4dp)` | `OutlinedCard(border=CardBorder 1dp, radius=8dp)` |
| サジェストアイテムテキスト | デフォルト | `DarkGreen` |
| 成功メッセージ Surface | `primaryContainer` | `AccentGreen` + `Color.White` |
| 成功メッセージテキスト | `onPrimaryContainer` | `Color.White` |

### 完了条件
- IncomingInputScreen.kt がビルドエラーなく変更完了

---

## P5: HistoryScreen リデザイン（P14）

### 変更箇所一覧

| 箇所 | 現状 | 変更後 |
|------|------|--------|
| `Scaffold containerColor` | なし（デフォルト） | `BodyBg` |
| `TopAppBar containerColor` | なし | `HeaderBg` |
| TopAppBar title | `倉庫名 入荷処理` | アイコン + 同テキスト（緑） |
| 戻るボタン色 | デフォルト | `AccentGreen` |
| ヘッダー下線 | なし | `HorizontalDivider(1dp, DividerGreen)` |
| "本日の入荷履歴" テキスト | デフォルト | `AccentGreen`, Bold |
| HistoryCard | `Card(elevation=2dp)` | `OutlinedCard(bg=HistCardBg, border=HistCardBdr 1dp, radius=10dp)` |
| コード/JAN テキスト | `onSurfaceVariant` | `TextSecond` |
| 商品名 | デフォルト | `TextPrimary`, Bold |
| 倉庫名テキスト | `onSurfaceVariant` | `TextSecond` |
| 入荷日テキスト | `onSurfaceVariant` | `TextSecond` |
| 数量テキスト | デフォルト | `TextPrimary` |
| WorkStatusBadge WORKING | `tertiary.copy(alpha=0.15)` + tertiary | `AccentGreen` + `Color.White` |
| WorkStatusBadge COMPLETED | `primary.copy(alpha=0.15)` + primary | `DarkGreen` + `Color.White` |
| WorkStatusBadge CANCELLED | `error.copy(alpha=0.15)` + error | `DeleteRed` + `Color.White` |
| 空状態テキスト | `onSurfaceVariant` | `ReadonlyText` |

### 完了条件
- HistoryScreen.kt がビルドエラーなく変更完了

---

## P6: ビルド確認

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :feature:inbound:assembleDebug
```

### 完了条件
- `BUILD SUCCESSFUL` であること

---

## 制約（厳守）

- ViewModel・ロジック・ナビゲーション変更禁止
- 未実装の UI 要素（バーコード入力、同期ボタン等）を追加しない
- `local.properties` の認証情報はコミットしない
- 全 Phase 完了後に boot.md の進捗テーブルを更新する

## 全体完了条件

- 5ファイル全ての変更が完了
- `assembleDebug` が SUCCESS
- boot.md の全Phase が「完了」
