[x] /Users/jungsinyu/Projects/sakemaru-handy-android/prompts/20260318/20260318-070540-picking-group-by-product/20260318-070540-picking-group-by-product-boot.md
[x] 入力フォームをテーブル形式（区分|ケース|バラ ヘッダー + 合計行 + 得意先行）に変更。weight(1f)で3列均等配置、数字切れ解消
[x] 入力フォームに数字を入れると文字が切れる → OutlinedTextFieldをBasicTextField+カスタム装飾に置換、内部パディング最小化で解消
[x] 商品情報領域: ロケーション32sp、JANコード32sp、「得意先」ラベル削除、画像ボタンをロケーション下に横幅ボタンとして配置
[x] 調査結果: APIには customer_name はあるが customer_code は存在しない。UIコードは entry.customerName を表示済み（空なら「—」）。得意先コード表示にはAPI側に customer_code 追加が必要。
[x] JANコード読み込み機能: CameraX+ML Kit導入、JanCodeScannerDialog新規作成、商品画像|JAN確認ボタン分割、一致時緑バッジ/不一致時赤バッジ+バイブ+エラー音
[x]　履歴画面の向きを入力画面と合わせてレイアウトも統一させる。
[x]  次の機能が正しく動作しない。カメラ起動後画面が映らない。＝＞JANコード読み込み機能: CameraX+ML Kit導入、JanCodeScannerDialog新規作成、商品画像|JAN確認ボタン分割、一致時緑バッジ/不一致時赤バッジ+バイブ+エラー音 （修正: ランタイム権限チェック追加、PreviewView COMPATIBLE モード、カメラクリーンアップ、エラーログ追加）
