# 横持ち出荷 Android 設計ブート

- 作成日: 2026-04-19
- 対象: `sakemaru-handy-android` の横持ち出荷 Android 実装設計
- 目的: 現行出荷ロジックを調査し、横持ち出荷の Android 側実装方針と仕様を `proxy-shipment-android-spec` に整理する

## 出力物

1. `proxy-shipment-android-investigation.md`
2. `proxy-shipment-android-design.md`

## 調査対象

- `prompts/api.md`
- `prompts/design.md`
- `prompts/harness.md`
- `prompts/local-server-setup.md`
- `prompts/pages.md`
- `prompts/setup-skills.md`
- `prompts/skill-android-plan.md`
- `prompts/20260418/proxy-shipment-api-specification.md`
- `prompts/20260418/20260418-proxy-shipment-api-handy-v2-design.md`
- `prompts/20260418/shipping-and-proxy-shipment-spec.md`
- `feature/outbound/*`
- `core/domain/repository/PickingTaskRepository.kt`
- `core/network/api/PickingApi.kt`
- `core/network/repository/PickingTaskRepositoryImpl.kt`
- `app/.../navigation/Routes.kt`
- `app/.../navigation/HandyNavHost.kt`
- `feature/main/MainViewModel.kt`
- `feature/main/MainScreen.kt`
- `core/ui/TokenManager.kt`

## 仕様優先順位

優先順位は以下で固定する。

1. 2026-04-19 作成の `proxy-shipment-api-specification.md`
2. 2026-04-18 作成の `20260418-proxy-shipment-api-handy-v2-design.md`
3. 2026-04-18 作成の `shipping-and-proxy-shipment-spec.md`

差分がある場合は 2026-04-19 の仕様を正とする。

### 既に見つかった差分

| 項目 | 古い記述 | 最新記述 | 採用 |
| --- | --- | --- | --- |
| 公開対象ステータス | `PENDING` / `PICKING` | `RESERVED` / `PICKING` | 最新記述 |
| 一覧クエリの日付キー | `date` | `shipment_date` | 最新記述 |
| 初期日付 | 端末側で送る前提 | `meta.business_date` を参照 | 最新記述 |

## 重要な設計制約

- UI は Jetpack Compose + Material 3
- 状態管理は `ViewModel` + `StateFlow`
- 通信は `Retrofit` + `Kotlinx Serialization`
- 例外は `NetworkException` に正規化する
- Portrait / Landscape の両 Preview を持つ
- 画面追加時は `prompts/pages.md` 更新が必要

## 現時点の主要結論

1. 現行出荷は `feature/outbound` と `PickingTaskRepository.tasksFlow` を中心とした 3 画面構成（P20/P21/P22）で成立している。
2. 横持ち出荷は API もエンティティ粒度も異なるため、通常出荷画面へ条件分岐を増やすより、別ルート・別 State として切り出した方が安全である。
3. ただし UI トーン、画面回転制御、スキャンダイアログ、エラーハンドリング、Repository の単一真実源パターンは再利用できる。
4. 横持ち出荷 API は `1 allocation = 1 商品` であり、通常出荷の「1 task = 複数商品」の前提を直接は流用できない。
5. `MainViewModel` / `TokenManager` の `shippingDate` は現在 `yyyy/MM/dd` 文字列で保持されているが、横持ち出荷 API は `yyyy-MM-dd` を要求するため、同じ状態をそのまま共有しない方がよい。
