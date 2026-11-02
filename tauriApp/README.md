# CraftDice Tauri 版

Tauri 2 + TypeScript/Vite のデスクトップ版。探索、物理ダイス戦闘、ダイスの面編集、職業選択、永続強化を実装しています。Android/iOS/JVM 版は従来の `composeApp` で引き続きビルドできます。

## 開発

Node.js 22.12 以降、JDK 17 以降（Gradle 8.14.2 対応版）、Rust stable が必要です。Windows は MSVC Build Tools と WebView2、macOS/Linux は [Tauri の前提環境](https://v2.tauri.app/start/prerequisites/)を準備します。Android SDK は不要です。

```sh
cd tauriApp
npm ci
npm run tauri -- dev
```

ブラウザーでの確認は `npm run dev`（`http://127.0.0.1:1420`）。Rust が PATH にない場合は `$HOME/.cargo/bin` を PATH に追加します。

## 配布ビルド

```sh
npm run tauri -- build
```

`src-tauri/target/release/bundle/` に各 OS のパッケージを生成します。対象 OS 上でビルドしてください。Windows は NSIS/MSI、macOS は app/DMG、Linux は deb/RPM/AppImage。macOS の署名・公証や Windows のコード署名は別途証明書の設定が必要です。

## テスト

```sh
npm test
npm run build
cargo test --manifest-path src-tauri/Cargo.toml
npx playwright install chromium
npx playwright test
```

Windows の配布ビルド後は `node scripts/native-smoke.mjs` で実際の WebView2 の起動・画面遷移・読み込み IPC を確認できます。セーブは変更しません。

`npm test` は共有ドメインの Kotlin/JS テストと、JS 境界から探索・戦闘・報酬・セーブを通す結合テストを実行します。元の `RunEngineTest` は JVM リフレクションを使うため JS テストから除外し、探索の結合テストで補っています。

## コードとセーブ

- `game-core` は独立した Gradle プロジェクトで、`composeApp` のドメインソースを直接コンパイルします。ルールやカタログのコピーはありません。`Game.kt` が JSON 形式の操作・表示境界です。
- 画面は `src/`、Tauri の設定と保存コマンドは `src-tauri/`。ネイティブ保存はアプリのデータディレクトリにある `progress.json` を一時ファイルから原子的に置換します。Windows は `%APPDATA%/com.koke1024.craftdice.tauri/progress.json` です。
- 保存対象は欠片・購入済み強化・選択職業です。探索途中と編集したダイスはセッション限りです。ダイス編集は無料で、次の探索に適用されます。職業変更でもダイスは初期構成に戻ります。
- 既存版の SQLDelight セーブとは独立しています。自動移行はありません。
- ブラウザー確認時のみ localStorage を使用し、デスクトップ版とは保存領域が異なります。
- 壊れたセーブの読み込み時は開始を停止します。ファイルを退避してから削除すると初期化できます。保存失敗時は操作を停止して再試行を表示します。
- ダイスの物理演算は固定 60 Hz。ドラッグで方向・強さを指定でき、キーボード利用時は「投げる」ボタンを使えます。
