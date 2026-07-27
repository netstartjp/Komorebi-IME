# zinna-IME — ZenSky Project版

端末の外へ入力内容を送らずに使える、Android 向けオープンソース日本語 IME です。
[Mozc](https://github.com/google/mozc) をネイティブ組み込みし、フリック入力と QWERTY
入力、ユーザー辞書、誤入力補正、キーボードの外観カスタマイズに対応しています。

> [!NOTE]
> このリポジトリは、**ZenSky Project** が
> [soichi11208/Zinna-IME](https://github.com/soichi11208/Zinna-IME) をフォークし、
> 独自に変更・配布しているものです。フォーク元の作者がこの版を公開・保証・サポート
> しているわけではありません。この版への問い合わせは
> [support@zslink.xyz](mailto:support@zslink.xyz) へお願いします。

> [!IMPORTANT]
> 現在は Open Beta（`0.1.x`）です。日常利用に必要な基本機能はありますが、パスワード
> マネージャー連携やクリップボード履歴など、一般的な商用 IME の全機能を置き換える
> 段階ではありません。

## 特長

- **完全オフライン** — 変換は同梱した `libmozc.so` と `mozc.data` だけで完結します。
  アプリは `INTERNET` を含む `uses-permission` を一つも要求しません。
- **3 種類の入力方式** — かな/英字ともフリック、かな/英字とも QWERTY、かなフリック
  + 英字 QWERTY から選べます。
- **実用的な日本語変換** — Mozc の予測変換に加え、フリック方向ずれ、促音の脱落、
  隣接文字の転置をルールベースで補正します。
- **辞書を端末内で管理** — ユーザー辞書を GUI から追加・編集・削除でき、Mozc /
  Google 日本語入力互換 TSV を端末のファイル選択画面から取り込み・書き出しできます。
  カタカナから英語綴りを引く CC BY-SA 3.0 辞書も同梱できます。
- **入力データを暗号化** — ユーザー辞書と変換学習履歴は Android Keystore の鍵を
  使って保存時に暗号化します。クラウドバックアップも無効です。
- **配列と外観を自由に作成** — Material You、ピュアブラック、キーの高さ、背景画像に
  対応。配列・テーマはプリセットから選択でき、アプリ内で JSON を複製・編集・検証・
  インポート・エクスポートできます。

名前とアイコンはヒャクニチソウ（百日草、*Zinnia*）に由来します。

## 対応環境

- Android 7.0（API 24）以上
- `arm64-v8a` / `armeabi-v7a` / `x86_64` / `x86`

## インストールと初期設定

現時点ではソースから APK をビルドしてインストールします。ビルド方法は
[開発者向けビルド](#開発者向けビルド)を参照してください。

インストール後は次の順に設定します。

1. 「zinna-IME — ZenSky Project版」を起動する。
2. 「キーボードを有効化」から zinna-IME を有効にする。
3. 「キーボードを選択」から zinna-IME を選ぶ。
4. 設定画面の「試し入力」で変換できることを確認する。

初回起動時は同梱の追加辞書をバックグラウンドで取り込みます。進捗は設定画面の
「追加辞書」で確認できます。この処理にもネットワーク接続は使いません。

application ID がフォーク元と異なるため、Android 上では別アプリとして共存します。
フォーク元や旧 `dev.oss.ime` ビルドの設定・学習履歴・ユーザー辞書は自動移行されません。

## 使える機能

### 入力と変換

- フリックかな、フリック英数、フリック記号
- ローマ字 QWERTY、英字 QWERTY、2 面の QWERTY 記号
- 予測候補の表示と候補タップによる確定
- 候補の長押しによる個別の学習削除と、設定画面からの学習一括リセット
- 濁点・半濁点・小書き文字の切り替え
- カーソル移動、Undo、連続 Backspace、変換、確定
- 入力欄側で文字列やカーソルが変更された場合の composition 同期
- パスワード・数値・URL・メール欄に合わせた入力面と候補表示の自動最適化

パスワード欄では Mozc のシークレットモードも自動で有効になり、その入力を学習せず、
既存の学習履歴も変換に利用しません。パスワード欄を離れると通常モードへ戻ります。

composition 同期により、たとえば Chrome のアドレスバーを「×」で消した後に、消す前の
読みが次の入力へ混ざることはありません。

URL・メール欄では英字 QWERTY と英語候補を表示します。パスワード・数値欄では候補領域を
非表示にし、特にパスワード欄では学習も行いません。

候補がないときは同じ領域がMaterialアイコンのツールバーになり、現在のクリップボードの
貼り付け、絵文字、片手モードの切り替え、設定画面への移動を行えます。片手モードは1つの
ボタンをタップするたびに 全幅 → 左寄せ → 右寄せ → 全幅 と循環し、端末枠内の
キーボード位置を模したインジケーターで現在のモードを表示します。
候補が出ると自動的に通常の候補一覧へ戻ります。クリップボードの内容は履歴として保存せず、
貼り付け時に現在値だけを読み取ります。

スペースキーを左右にフリックすると、カーソルを1文字ずつ移動できます。変換確定後の
予測候補が表示されているときも、カーソルを動かして入力を続けられます。

### 辞書

設定画面の「ユーザー辞書」から、読み・単語・品詞を登録できます。正本は
Mozc / Google 日本語入力の辞書ツールと互換性のある TSV として管理され、端末上では
暗号化して保存されます。

辞書画面上部の「取込」「書出」から Android のファイル選択画面を開けます。取り込みは
現在のユーザー辞書を置き換えます。取り込み前に TSV の形式とサイズを検証し、同じ
読み・単語の重複を整理してから暗号化保存します。

ビルド時に次の追加辞書を同梱できます。

- [google-ime-user-dictionary-ja-en](https://github.com/KEINOS/google-ime-user-dictionary-ja-en) —
  カタカナ語から英語綴りへの変換（例: 「ぶらっく」→ `black`）

元データを結合・検証し、翻訳語を除外して英語綴りを正規化した派生辞書を
[CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/) で再配布します。
出典、ライセンス、変更内容は生成する辞書ファイル自身にもコメントとして埋め込みます。
権利関係が明示的なライセンスだけでは判断できないスクレイピング由来辞書は、標準配布物へ
含めません。

各データのライセンスと再配布上の注意は [NOTICE](NOTICE) を確認してください。

### 外観と配列

設定画面では入力方式、キーの高さ、ピュアブラック、背景画像と濃さを変更できます。
「プリセット選択・自作・共有」から配列・テーマスタジオを開き、使用するプリセットも
選択できます。キーボードは次に開いたときに設定を反映します。

片手モードは全幅・左寄せ・右寄せから選択でき、候補がないときのツールバーの1ボタンを
タップするたびに 全幅 → 左 → 右 → 全幅 と循環して切り替わります。「かなフリック +
英字 QWERTY」では、英字面の左下を常に「かな」切り替えにして、入力方式の切り替え位置を
統一しています。

配列とテーマは JSON です。スタジオでは次の操作ができます。

- 同梱プリセットまたは作成済みデータを選んで使用
- JSON の `id` を変更して既存プリセットを複製
- JSON を直接編集し、形式と値を検証して保存
- Android のファイル選択画面から JSON を取り込み・書き出し
- ユーザー版を削除し、同名の同梱プリセットへ復元

作成数にアプリ側の固定上限はありません。同じ ID のユーザー版は同梱版より優先されます。
ID には 1〜64 文字の英数字、ハイフン、アンダースコアを使用できます。安全のため、
1ファイルが 1 MB を超える JSON、空の配列、過大な行・キー数、範囲外のサイズ値などは
保存されません。Material You は端末の配色から動的生成されるため、直接編集・書き出しの
対象外です。

## プライバシーとセキュリティ

`AndroidManifest.xml` に `uses-permission` はありません。したがって、入力内容、変換履歴、
ユーザー辞書、背景画像をアプリ自身がネットワークへ送ることはできません。

保存データは次のように扱います。

| データ | 保存方法 |
| --- | --- |
| GUI のユーザー辞書 | AES-GCM + Android Keystore |
| Mozc のユーザー辞書 | Keystore 由来の鍵で暗号化 |
| Mozc の変換学習履歴 | Keystore 由来の鍵で暗号化 |
| Android のクラウドバックアップ | 無効 |

これは Android の端末暗号化を置き換えるものではありません。Keystore の実装や安全性は
端末に依存します。設計、移行処理、検証状況は
[docs/profile-encryption.md](docs/profile-encryption.md) にまとめています。

## 既知の制約

- フリック記号面に 2 ページ目はありません。QWERTY 記号面は 2 ページあります。
- 配列・テーマの編集は JSON エディタ方式です。キーをドラッグして配置する視覚的な
  配列エディタはまだありません。
- 暗号化した Mozc ストレージは 64 MB を超えるファイルを読み込めません。
- Keystore を使う Android 固有の暗号化経路は、実機での継続的な検証が必要です。
- Open Beta 中は設定形式や内部ファイル形式が変更される可能性があります。

不具合を報告する際は、端末名、Android バージョン、入力した文字、期待した結果、
実際の結果、再現手順を添えてください。入力内容を含むログは公開前に必ず確認してください。

## 実用性を高めるロードマップ

日常利用で効果が大きく、このプロジェクトの「オフライン・最小権限」という方針を
壊さない機能を優先します。

| 優先度 | 機能案 | 実用上の効果 |
| --- | --- | --- |
| 完了 | パスワード欄・数値欄・URL・メール欄への自動最適化 | 不要な学習や候補表示を止め、入力面を自動で合わせる |
| 完了 | ユーザー辞書の SAF インポート/エクスポート | 機種変更と他 IME からの移行をオフラインで完結できる |
| 完了 | 片手モードと左右寄せ | 大画面端末でも親指で届きやすくする |
| 完了 | 候補の長押し削除・学習リセット | 誤学習を利用者自身で直せる |
| 一部完了 | クリップボード（現在値に対応、履歴は未実装） | 権限や保存リスクを増やさず貼り付けを速くする |
| 完了 | 配列・テーマの GUI インポートと検証 | JSON を手作業で内部配置する必要をなくす |
| 中 | 日本語の音声入力を別アプリへ委譲 | 本体に通信権限を追加せず、必要な人だけ利用できる |
| 一部完了 | 絵文字・顔文字パレット | 基本絵文字に対応。顔文字と最近使った項目は今後追加する |

特に上位 4 件は、見た目の追加よりも誤入力・移行・片手操作・誤学習という日常的な
離脱理由を直接減らせます。実装時はパスワード欄で学習しないこと、外部へ暗黙に通信
しないこと、保存するデータを明示することを受け入れ条件にします。

## 開発者向けビルド

### 必要なもの

- Git
- JDK 17–21（JRE のみ、および JDK 25 は不可）
- Python 3.12 以上
- Android SDK Platform 36.1 / Build Tools 36.0.0
- [Bazelisk](https://github.com/bazelbuild/bazelisk) または Bazel
- `curl`、`unzip`

Android SDK の場所は `ANDROID_HOME` に設定しておきます。

このリポジトリの Android Gradle Plugin 8.13.2 は JDK 25 に対応していません。複数の
Java が入っている環境では、`JAVA_HOME` が JDK 17 または 21 を指すことを確認してください。

### Linux / WSL が必要な工程

Gradle だけでは Mozc のネイティブエンジンは生成されません。`scripts/build_mozc.sh` は
Linux 用 Bazel ホストツールも実行するため、Linux または WSL2 上で実行してください。
Windows の Android Studio で SDK を導入して `assembleDebug` だけ実行しても、
`libmozc.so` と `mozc.data` は生成されません。

現在の Gradle 構成では、次の必須成果物がない状態で APK をパッケージしようとすると
`:mozc:verifyMozcRuntime` が失敗します。変換不能な APK を成功扱いしないための検査です。

```text
mozc/src/main/assets/mozc.data
mozc/src/main/jniLibs/arm64-v8a/libmozc.so
mozc/src/main/jniLibs/armeabi-v7a/libmozc.so
mozc/src/main/jniLibs/x86/libmozc.so
mozc/src/main/jniLibs/x86_64/libmozc.so
```

### ビルド手順

```bash
git clone <this-repository-url>
cd Zinna-IME-forktest

git clone --depth 1 https://github.com/google/mozc.git third_party/mozc

# Mozc の取得、パッチ適用、4 ABI の libmozc.so、mozc.data、proto の配置
./scripts/build_mozc.sh

# Mozc のローマ字テーブルからフリック配列と補正用データを再生成
python3 scripts/gen_flick_layout.py

# QWERTY 配列を再生成
python3 scripts/gen_qwerty_layout.py

# APK に入れる追加辞書を取得
./scripts/fetch_dictionaries.sh

printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
./gradlew :app:assembleDebug
```

Codex Linux など別のLinux環境へ引き継ぐ場合も、上記を先頭から順に実行します。
`third_party/mozc/`、`mozc/src/main/jniLibs/`、`mozc/src/main/assets/mozc.data` は
Git管理対象外なので、Windows側のcheckoutに存在しなくても異常ではありません。
ビルド後は次のコマンドでAPKへの収録を確認できます。

```bash
unzip -l app/build/outputs/apk/debug/app-universal-debug.apk \
  | grep -E 'libmozc\.so|mozc\.data'
```

4 ABI の `libmozc.so` と1つの `mozc.data` が表示されてからAPKを実機へ渡してください。

生成物は `app/build/outputs/apk/` 以下に出力されます。ABI 別 APK と universal APK を
生成します。実機と接続できる場合は、次でもインストールできます。

```bash
./gradlew :app:installDebug
```

`build_mozc.sh` が Bazel を 2 回実行するのは意図した動作です。Android 向け
`libmozc.so` と、ホストツールが生成するアーキテクチャ非依存の `mozc.data` は、Mozc
側の制約により同じ Bazel コマンドではビルドできません。

### テスト

```bash
./gradlew :app:testDebugUnitTest
```

レイアウト JSON の解析、切り替え先、QWERTY の幅、フリック方向判定、入力方式の切り替え、
editor/composition の同期方針をユニットテストしています。ネイティブ暗号化の追加検証は
[docs/profile-encryption.md](docs/profile-encryption.md) を参照してください。

## リポジトリ構成

```text
app/              InputMethodService、キーボード UI、設定画面
mozc/             JNI シム、Kotlin ラッパー、生成 protobuf、ネイティブ成果物
patches/          Mozc に適用する誤入力補正・保存時暗号化のパッチ
scripts/          Mozc ビルド、配列生成、追加辞書取得
docs/             設計と検証の詳細
third_party/mozc/ 上流 Mozc の checkout（Git 管理対象外）
```

### Mozc との境界

アプリは `MozcJNI` を直接扱わず、`MozcEngine` と `MozcSession` を通してセッションの
ライフサイクルと protobuf の入出力を管理します。上流 checkout へ加える変更は
`patches/` に分離し、`scripts/build_mozc.sh` がビルド前に適用します。

フリック配列の JSON が保持するのは表示上の「かな」ではなく、Mozc のローマ字テーブルへ
渡す ASCII キーです。`scripts/gen_flick_layout.py` は上流の `data/preedit/*.tsv` から
対応を生成・検証し、濁点や小書き文字の規則を迂回しないようにしています。

適用する独自パッチは次の 2 つです。

- `0001-rule-based-typing-correction.patch` — OSS 版 Mozc の補正フックへ軽量な
  ルールベース補正を実装
- `0002-android-keystore-profile-encryption.patch` — Mozc の履歴とユーザー辞書を
  Keystore 由来の鍵で暗号化

## バージョニング

- `0.0.1`–`0.0.9`: Open Alpha
- `0.1.0`–`0.9.x`: Open Beta
- `1.0.0` 以降: Stable（予定）

## ライセンス

本ソフトウェアは、Apache License 2.0 で提供される
[soichi11208/Zinna-IME](https://github.com/soichi11208/Zinna-IME) の派生物です。
元の著作権表示を保持したうえで、ZenSky Project による変更部分も同じ
[Apache License 2.0](LICENSE) の条件で提供します。

Android の application ID とコード namespace は、フォーク元との共存および識別のため
`me.zssu.ime` へ変更しています。Mozc は BSD 3-Clause、追加辞書にはそれぞれ別の条件が
あります。完全な帰属表示、変更の告知、再配布上の注意事項は [NOTICE](NOTICE) を参照して
ください。APKには本体の `LICENSE`、`NOTICE`、[PRIVACY](PRIVACY.md) に加えて、Mozc、
Mozc OSS辞書、Protocol Buffers、その他ランタイム依存関係のライセンス・帰属表示を同梱
します。設定画面の「オープンソースライセンス」から全文を閲覧できます。
ツールバーで使用するAndroidX Material Iconsについても、同画面の「帰属表示」と
「第三者ソフトウェア」に出典、変更内容、Apache License 2.0を表示します。

このフォークに関する問い合わせ: [support@zslink.xyz](mailto:support@zslink.xyz)
