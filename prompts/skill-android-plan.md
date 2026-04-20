---
name: android-plan
description: Android機能開発・画面追加・バグ修正の作業計画を作成。LLM WIKI(api.md, design.md等)とハネス(harness.md)を動的に活用し、堅牢な計画を生成する。
argument-hint: [feature-description] or [--id=PLAN_ID feature-description]
allowed-tools: [Read, Write, Edit, Glob, Grep, Bash, AskUserQuestion]
---

# Android Plan Skill (v2.0 - Smart Wiki & Harness)

Android機能開発において、LLM WIKIを活用し、ハネス（制約）を厳守した段階的な作業計画を作成する。

## 1. LLM WIKI と動的コンテキスト読み込み

AIは計画作成時および各Phaseの実行前に、以下の「LLM WIKI」からタスクに関連するものを**選択的に**読み込まなければならない。

| WIKIファイル | 読み込みのトリガー（キーワード） |
| :--- | :--- |
| `prompts/harness.md` | **必須** (常に最初に読み込み、制約を把握する) |
| `prompts/api.md` | API, 通信, サーバー, エンドポイント, JSON, リクエスト |
| `prompts/design.md` | UI, デザイン, 色, フォント, コンポーネント, Compose |
| `prompts/pages.md` | 画面追加, 遷移, ルーティング, 画面番号(Pxx) |
| `prompts/local-server-setup.md` | 環境構築, ローカルサーバー, 接続トラブル |

## 2. 実行手順

### Step 0: ハネスとコンテキストの把握

1. `prompts/harness.md` を読み込み、プロジェクトの基本制約（解像度、色、設計原則）を頭に入れる。
2. タスク内容に関連するWIKIファイルを、上記トリガーに基づいて読み込む。
3. 既存の `boot.md` があれば、現在のセッション状態を確認する。

### Step 1: 作業ディレクトリの管理

保存先は `prompts/{PLAN_ID}/` とする。

### Step 2: 計画（plan.md）の作成ルール

`plan.md` には、各実装Phaseの最後に必ず **「ハネス適合チェック」** ステップを挿入すること。

#### plan.md 内の Phase 構成例:
```markdown
## Px: {機能名} の実装
...
### ハネス適合チェック
- [ ] 画面解像度 1080x2400 (420dpi) でレイアウトが崩れていないか
- [ ] 指定の色（TitleRed, AccentOrange等）が正しく使われているか
- [ ] ViewModel + StateFlow のパターンに従っているか
- [ ] Preview (Portrait/Landscape) が実装されているか
```

## 3. boot.md / plan.md のフォーマット

(従来のフォーマットを継承しつつ、harness.md への参照を「重要な設計制約」セクションに追加)

### boot.md の「重要な設計制約」
```markdown
## 重要な設計制約
- 詳細は `prompts/harness.md` を参照（厳守）
- 画面解像度: 1080 x 2400 (420dpi)
- 色彩: TitleRed, AccentOrange, HeaderBg, BodyBg 等の定義済みカラーを使用
```

## 4. はねす（制約）の保護

AIは、ユーザーからの指示であっても `harness.md` に抵触する場合は、その旨を報告し、ハネスを維持したまま最適解を提案しなければならない。

## 5. 完了定義 (Definition of Done)

1. `harness.md` の全項目に適合している。
2. APIテストが成功し、レスポンスが記録されている。
3. 新規画面がある場合、`pages.md` が更新されている。
4. `boot.md` の進捗と実績が最新である。
