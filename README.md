# CraftDice

Kotlin Multiplatform + Compose Multiplatform によるクラフトダイスゲーム。

## 技術スタック

- **言語/基盤**: Kotlin Multiplatform (KMP)
- **UI**: Compose Multiplatform
- **アーキテクチャ**: Clean Architecture + MVI
- **DI**: Koin
- **セーブデータ**: SQLDelight
- **テスト**: Kotlin Test + Turbine

## ターゲットプラットフォーム

- Android (minSdk 24)
- iOS

## ビルド方法

```bash
./gradlew build
```

## プロジェクト構成

```
composeApp/
├── src/
│   ├── commonMain/         # 共通コード
│   │   ├── kotlin/
│   │   │   └── com/koke1024/craftdice/
│   │   │       ├── core/          # 共通ユーティリティ (Logger, Resource)
│   │   │       ├── di/            # Koin DI モジュール
│   │   │       ├── domain/        # ドメイン層
│   │   │       │   ├── model/     # モデル
│   │   │       │   ├── physics/   # 物理演算
│   │   │       │   ├── battle/    # バトル
│   │   │       │   ├── craft/     # クラフト
│   │   │       │   └── roguelike/ # ローグライク
│   │   │       ├── data/          # データ層
│   │   │       │   ├── local/     # SQLDelight ローカルデータ
│   │   │       │   └── repository/# リポジトリ実装
│   │   │       └── ui/            # プレゼンテーション層
│   │   │           ├── home/      # ホーム画面
│   │   │           ├── battle/    # バトル画面
│   │   │           ├── craft/     # クラフト画面
│   │   │           ├── dungeon/   # ダンジョン画面
│   │   │           ├── navigation/# ナビゲーション
│   │   │           └── theme/     # テーマ
│   │   ├── composeResources/      # Compose リソース
│   │   └── sqldelight/            # SQLDelight スキーマ
│   ├── commonTest/        # 共通テスト
│   ├── androidMain/       # Android 固有
│   └── iosMain/           # iOS 固有
└── build.gradle.kts
```
