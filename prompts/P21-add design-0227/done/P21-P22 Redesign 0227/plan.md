# P21 出庫データ入力 Body リデザイン 作業計画

## 前提

### 参照仕様
- API仕様: `prompts/api.md`
- デザイン仕様: `prompts/design.md`
- 画面一覧: `prompts/pages.md`
- HTMLリファレンス: `file:///C:/Users/ninpe/Downloads/ht_inventory_yoko_standalone%20(1).html` の出庫入力画面

### 完了済みの作業・現在の状況
- P21は機能完全実装済み（API連携、ナビゲーション、ViewModel全て動作）
- 現行デザイン: 左右スプリットペイン(商品情報 | 数量入力) + 6ボタンBottomBar
- ヘッダーは現行維持。Body部分のみHTMLリファレンスに合わせる

---

## Phase 一覧

| # | Phase | 概要 | 完了条件 |
|---|-------|------|---------|
| P1 | P21 Body リデザイン | 左ペイン(商品+数量+ボタン) + 右ペイン(履歴) | コンパイル成功・レイアウト変更完了 |
| P2 | ビルド確認 | app含む全モジュールビルド | BUILD SUCCESSFUL |

---

## P1: P21 Body リデザイン

### 目的
OutboundPickingScreen.kt の Body 部分（Scaffold content + bottomBar）を、HTMLリファレンスの出庫入力画面デザインに合わせてリデザインする。

### 変更方針

#### 1. レイアウト変更
```
現行:
┌──────────────────────────────────┐
│ Header (維持)                     │
├────────────────┬─────────────────┤
│ ItemInfoCard   │ QuantityInput   │
│ (商品名,伝票, │ Card            │
│  容量,JAN,ロケ)│ (受注数,出庫数) │
├────────────────┴─────────────────┤
│ BottomBar (画像/コース/履歴/前/登録/次) │
└──────────────────────────────────┘

変更後:
┌──────────────────────────────────┐
│ Header (維持)                     │
├────────────────┬─────────────────┤
│ 商品情報カード  │ "履歴" ラベル    │
│ ├商品名+画像btn│ 履歴リスト       │
│ ├ロケ/伝票(2col)│ ├amber-50カード │
│ ├受注数(readonly)│├商品名+コード  │
│ ├出庫数(editable)││出庫/受注数    │
│ └[前へ][登録][次へ]│└削除ボタン   │
│ (bottomBar廃止) │                 │
└────────────────┴─────────────────┘
```

#### 2. 左ペイン詳細設計

**a. 商品情報ヘッダー**
- 商品名: 16sp, extrabold
- JAN/容量/入数: 14sp, neutral-500, font-mono
- 画像ボタン: 44x56dp, amber-50 bg, amber-200 border, amber-600 icon
- 右寄せで画像ボタン配置

**b. ロケーション・伝票番号 (2カラムグリッド)**
- ラベル: 14sp bold neutral-500
- 値ボックス: h=28dp, amber-50 bg, amber-300 border, font-mono

**c. 受注数 (readonly)**
- neutral-50 bg, neutral-200 border, rounded-lg, p=6dp
- ラベル "受注数": 14sp bold neutral-400
- ケース行: [ケース label (44dp)] [readonly input (80dp)]
- バラ行: [バラ label (44dp)] [readonly input (80dp)]

**d. 出庫数 (editable)**
- neutral-300 border, rounded-lg, p=6dp
- ラベル "出庫数": 14sp bold neutral-500
- ケース行: [ケース label (44dp)] [editable input (80dp)]
- バラ行: [バラ label (44dp)] [editable input (80dp)]

**e. アクションボタン (mt-auto, 下部固定)**
- 3ボタン横並び, gap=8dp
- 前へ: disabled style (neutral-200 bg, neutral-300 text) — canMovePrevで制御
- 登録/更新/確定: amber-600 bg, white text, h=32dp — 状態により文言変更
- 次へ: amber-50 bg, amber-300 border, amber-700 text — canMoveNextで制御

#### 3. 右ペイン詳細設計

**a. ヘッダー**
- "履歴" テキスト: 13sp bold neutral-700

**b. 履歴リスト (白カード内)**
- 空状態: clipboard icon + "履歴はありません" (neutral-400)
- アイテムカード: p=4dp, rounded-lg, amber-50 bg, amber-200 border
  - 商品名: 16sp bold
  - コード/ロケ: 11sp neutral-600, font-mono
  - 出庫数/受注数: 11sp neutral-500
  - 削除ボタン: 28x28dp, red-50 bg, red-300 border, trash icon

**c. データソース**
- `state.originalTask?.items` から `status != PENDING` のアイテムをフィルタ
- ViewModel変更不要（originalTaskに全アイテムあり）

#### 4. 削除するコンポーネント
- `ItemInformationCard` — 左ペインに新UIとして再構築
- `QuantityInputCard` — 左ペインの受注数/出庫数セクションに統合
- `OutboundPickingBottomBar` — 左ペインのボタン行に統合
- `InfoRow` — 不要

#### 5. 維持するコンポーネント
- `OutboundPickingScreen` (メインcomposable) — ヘッダー部分はそのまま
- `CompletionConfirmationDialog` — そのまま
- `ImageViewerDialog` — そのまま

#### 6. Body背景色変更
- 現行: `BodyBg = Color.White`
- 変更: `BodyBg = Color(0xFFF5F5F5)` (neutral-100)

### 修正対象ファイル
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingScreen.kt`

### 完了条件
- 左ペインに商品情報+数量入力+ボタンが統合されていること
- 右ペインに履歴リストが表示されること
- BottomBarが削除されていること
- ヘッダー(TopAppBar)が変更されていないこと
- 既存機能（登録、前へ/次へ、画像表示、完了確認）が維持されていること
- `:feature:outbound:compileDebugKotlin` が成功すること

---

## P2: ビルド確認

### 目的
appモジュール含む全体ビルドの成功を確認する

### 手順
1. `./gradlew :app:compileDebugKotlin` を実行
2. エラーがあれば修正
3. BUILD SUCCESSFULを確認

### 完了条件
- `:app:compileDebugKotlin` BUILD SUCCESSFUL

---

## 制約（厳守）

- ヘッダー(TopAppBar)は一切変更しない
- ViewModel/State/ビジネスロジックの変更は最小限（UI層のみ）
- CompletionConfirmationDialog, ImageViewerDialogは変更しない
- 既存ナビゲーション(onNavigateBack, onNavigateToHistory等)は維持
- `local.properties` の認証情報はコミットしない

## 全体完了条件

- P21のBody部分がHTMLリファレンスと同等のレイアウトになっていること
- ビルド成功（BUILD SUCCESSFUL）
- 既存機能（登録、前へ/次へ、画像表示、完了確認、ナビゲーション）が維持されていること
