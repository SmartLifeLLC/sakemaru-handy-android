# Android Plan: P21-Redesign-0223

- **ID**: P21-Redesign-0223
- **作成日**: 2026-02-23
- **最終更新**: 2026-02-23
- **ステータス**: 完了
- **ディレクトリ**: prompts/P21-Redesign-0223/

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

P21（出庫データ入力画面 = OutboundPickingScreen）のUIリデザイン。
P20（配送コース選択）と同一の色・ヘッダースタイルを適用。
**ロジック・API・ViewModel・ナビゲーションは一切変更しない。UIファイルのみ変更。**

---

## 使用API

なし（純粋なUIリデザイン）

---

## 重要な設計制約

- 画面解像度: 1080 x 2400, 420dpi, portrait/landscape対応
- API Level 33
- テーマ: NoActionBar + Compose TopAppBar
- **ViewModel・ロジック・ナビゲーション変更禁止**
- UIファイル（OutboundPickingScreen.kt）のみ変更対象

---

## Step 0 — 実装確認結果（完了）

### UI技術
- Compose（XMLなし）

### 対象ファイル
- UI: `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingScreen.kt`
- State: `OutboundPickingState.kt` ← 変更禁止
- ViewModel: `OutboundPickingViewModel.kt` ← 変更禁止

### 実装済み / 未実装チェックリスト

#### ヘッダー
| 項目 | 実装 |
|------|------|
| 뒤로가기 버튼 | ✅ onNavigateBack |
| 코스명 뱃지 | ❌ なし |
| 날짜 표시 | ❌ なし |
| 창고명 표시 | ❌ なし |
| ホームアイコン | ✅ Icons.Default.Home / onNavigateToMain |
| 설정 아이콘 (⚙️) | ❌ なし |

#### 入力エリア
| 項目 | 実装 |
|------|------|
| バーコード入力フィールド | ❌ なし |
| バーコードスキャンボタン | ❌ なし |
| 거래처 드롭다운 | ❌ なし |
| 상품명 표시 | ✅ currentItem.itemName |
| 상품 규격 (容量・入数・JAN・伝票番号) | ✅ ItemInformationCard |
| ロケーション表示 | ❌ なし |
| 출庫数 입력（単一入力） | ✅ pickedQtyInput |
| 받注数 표시（plannedQty） | ✅ plannedQty 読み取り専用 |
| 取消ボタン | ❌ なし |
| 登録（確定）ボタン | ✅ BottomBarの「登録(F1)」 |

#### 履歴パネル
| 項目 | 実装 |
|------|------|
| 履歴パネル | ❌ なし |
| 履歴カード・バッジ等 | ❌ すべてなし |

### レイアウト方針
- 履歴パネルなし → **パターンB（単一パネル）**
- 既存の2カラム（コース情報左 / 数量入力右）構造は維持し、デザインのみ変更

### 既存BottomBar（変更なし）
- 商品の画像 / コース変更 / 履歴 / 前へ / 登録 / 次へ

### ViewModel State フィールド（利用可能）
- `state.originalTask?.courseName` - コース名
- `state.originalTask?.pickingAreaName` - エリア名
- `state.registeredCount`, `state.totalCount` - 進捗
- `state.currentItem?.itemName` - 商品名
- `state.currentItem?.volume`, `.capacityCase`, `.janCode`, `.slipNumber`
- `state.pickedQtyInput` - 入力数量
- `state.quantityTypeLabel` - "ケース" / "バラ"
- `state.isUpdating`, `state.canRegister`, `state.hasImages`

---

## 対象ファイル

### 既存変更（1ファイルのみ）
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingScreen.kt`

### 参照のみ（変更禁止）
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingState.kt`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingViewModel.kt`

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: UIリデザイン実装 | 完了 | 2026-02-23 | OutboundPickingScreen.kt 書き換え完了 |
| P2: ビルド確認 | 完了 | 2026-02-23 | BUILD SUCCESSFUL in 5s |

---

## 作業中コンテキスト

### Git ブランチ
- 作業ブランチ: `feature/design-upgrade`
- ベースブランチ: `master`

---

## Phase完了記録

### P1: UIリデザイン実装
- 完了日: 2026-02-23
- 実績:
  - `OutboundPickingScreen.kt` 全面書き換え（1ファイルのみ）
  - ヘッダー: クリーム背景 (#FDFBF2)、📦アイコン (#E67E22)、「出庫」タイトル (#C0392B 太字18sp)
  - ヘッダー下区切り線: 2dp #F9A825
  - ボディ背景: Color.White
  - CourseHeaderCard: OutlinedCard オレンジ枠2dp、コース名赤太字、進捗バッジオレンジ
  - ItemInformationCard: OutlinedCard グレー枠1dp、商品名太字
  - QuantityInputCard: OutlinedCard オレンジ枠2dp、受注数グレー読取専用、出庫数オレンジ強調入力
  - BottomBar 登録ボタン: AccentOrange (#E67E22)
  - state.task（deprecated）→ state.originalTask に変更

### P2: ビルド確認
- 完了日: 2026-02-23
- 実績:
  - `./gradlew :feature:outbound:assembleDebug` → BUILD SUCCESSFUL in 5s
  - エラーなし（hiltViewModel deprecation警告は既存コード由来）
