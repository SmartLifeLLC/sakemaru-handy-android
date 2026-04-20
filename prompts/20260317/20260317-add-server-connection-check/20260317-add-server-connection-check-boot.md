# Work Plan: add-server-connection-check

- **ID**: add-server-connection-check
- **作成日**: 2026-03-17
- **最終更新**: 2026-03-17
- **ステータス**: 完了
- **ディレクトリ**: /Users/jungsinyu/Projects/sakemaru-handy-android/prompts/20260317/20260317-add-server-connection-check/

## セッション再開手順

コンテキストがクリアされた場合、以下を読んで作業を再開する:

1. このファイルを読む（20260317-add-server-connection-check-boot.md）
2. 20260317-add-server-connection-check-plan.md を読む（作業計画の全体像）
3. 下記「進捗」テーブルで現在のPhaseを確認
4. 「Phase完了記録」セクションで完了済みPhaseの実績を確認
5. 「作業中コンテキスト」セクションで途中データを確認
6. 未完了の最初のPhaseから plan.md の該当セクションを読んで作業再開

## 概要

P01ログイン画面のフッターに「接続確認」ボタンを追加。`/api/auth/login` への空POSTでサーバ到達性を確認し、結果を5秒間テキスト表示する。

## 重要な設計制約

- ログインボタンの動作・位置に影響を与えないこと
- 接続確認は認証不要（API Keyのみ）で `POST /api/auth/login` を使用
- 接続確認中もUI操作をブロックしないこと
- 既存のRetrofit/OkHttpクライアント（API Keyインターセプター付き）を使用
- 結果表示はテキストボタン + テキスト表示（5秒後に自動消去）

## 対象ファイル

### 既存変更
- `feature/login/src/main/java/biz/smt_life/android/feature/login/LoginState.kt`
- `feature/login/src/main/java/biz/smt_life/android/feature/login/LoginViewModel.kt`
- `feature/login/src/main/java/biz/smt_life/android/feature/login/LoginScreen.kt`
- `core/domain/src/main/java/biz/smt_life/android/core/domain/repository/AuthRepository.kt`
- `core/network/src/main/java/biz/smt_life/android/core/network/repository/AuthRepositoryImpl.kt`

### 参照のみ（変更禁止）
- `core/network/src/main/java/biz/smt_life/android/core/network/api/AuthService.kt`
- `core/network/src/main/java/biz/smt_life/android/core/network/NetworkResult.kt`
- `core/ui/src/main/java/biz/smt_life/android/core/ui/HostPreferences.kt`

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: AuthRepository に checkConnection 追加 | 完了 | 2026-03-17 | Domain層+Network層+Fake |
| P2: LoginState・LoginViewModel 変更 | 完了 | 2026-03-17 | ConnectionResult enum追加 |
| P3: LoginScreen UI 追加 | 完了 | 2026-03-17 | フッターに接続確認ボタン+5秒自動消去 |
| P4: ビルド確認・動作テスト | 完了 | 2026-03-17 | assembleDebug成功 |

---

## 作業中コンテキスト

> Phase作業中に蓄積される中間データ。セッション再開時に必ず確認。

### 確認済み仕様判断
- 接続確認エンドポイント: `/api/auth/login` への空POST（4xx応答 = 接続OK）
- UI表示: テキストボタン + 結果テキスト（接続OK/NG）
- 自動消去: 5秒

### Git ブランチ
- 作業ブランチ: feature/design-upgrade（現在のブランチ）
- ベースブランチ: master

---

## Phase完了記録

### P1: AuthRepository に checkConnection 追加
- 完了日: 2026-03-17
- 実績:
  - AuthRepository.kt に `checkConnection(): Result<Unit>` 追加
  - AuthRepositoryImpl.kt に実装追加（4xx=接続OK、IOException=接続NG）
  - FakeAuthRepository.kt にも実装追加

### P2: LoginState・LoginViewModel 変更
- 完了日: 2026-03-17
- 実績:
  - LoginState に `isCheckingConnection`, `connectionResult` フィールド追加
  - `ConnectionResult` enum 追加（SUCCESS / FAILURE）
  - LoginViewModel に `checkConnection()`, `clearConnectionResult()` 追加

### P3: LoginScreen UI 追加
- 完了日: 2026-03-17
- 実績:
  - フッターに「接続確認」TextButton + 結果テキスト（OK緑/NG赤）追加
  - 確認中はCircularProgressIndicator表示
  - LaunchedEffectで5秒後に結果自動消去
  - 全4つのPreview関数にパラメータ追加

### P4: ビルド確認・動作テスト
- 完了日: 2026-03-17
- 実績:
  - `./gradlew assembleDebug` BUILD SUCCESSFUL
