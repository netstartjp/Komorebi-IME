# zinna-IME

Android 向けオープンソース日本語 IME。変換エンジンに [mozc](https://github.com/google/mozc) を
ネイティブ組み込みし、フリック入力・配列/テーマのカスタマイズに対応する。

名前とアイコンはヒャクニチソウ (百日草, *Zinnia*) から。アイコンは
`scripts/gen_launcher_icon.py` が花の記述一つからベクタとラスタの両方を生成する。

**完全オフライン動作**: 変換は端末内の `libmozc.so` と同梱辞書 `mozc.data` のみで完結する。
`AndroidManifest.xml` に `uses-permission` は一つも無く、ネットワークに出る手段が存在しない。

### 既知の不具合

- `onUpdateSelection` を実装していないため、アプリ側がテキストを外部から
  クリア・変更しても変換中の composition がリセットされない。
  (Chrome のアドレスバーの × を押しても mozc 側の「あか」が残り、
  続きが「あかにほん」になる)

## ビルド

### 前提

- JDK 17 以上
- Python 3.12 以上 (mozc のビルドスクリプトが要求)
- [bazelisk](https://github.com/bazelbuild/bazelisk) または bazel
- Android SDK (platform 35 / build-tools 35.0.0)

### 手順

```bash
git clone --depth 1 https://github.com/google/mozc.git third_party/mozc

# libmozc.so (4 ABI), mozc.data, protocol/*.proto を :mozc モジュールへ配置
./scripts/build_mozc.sh

# 配列 JSON を mozc のローマ字テーブルから生成
python3 scripts/gen_flick_layout.py

# 同梱する追加辞書を取得 (ビルド時のみ。アプリは実行時に通信しない)
./scripts/fetch_dictionaries.sh

echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:assembleDebug
```

`scripts/build_mozc.sh` は bazel を 2 回叩く。これは分けざるを得ない: `mozc.data` は
アーキ非依存のデータ blob だがホストツールで生成され、そのホストツールは
`--config oss_android` 下で incompatible とマークされているため、
1 コマンドで両方を要求すると analysis 段階で失敗する。

## 構成

```
app/     IME 本体 — InputMethodService, フリックキーボード View, 設定画面
mozc/    mozc ネイティブのラッパ — JNI シム, 生成 protobuf, libmozc.so, mozc.data
scripts/ ネイティブビルドと配列生成
third_party/mozc/  上流そのまま (パッチ無し)
```

### mozc との境界

`third_party/mozc` には**一切パッチを当てていない**。上流の
`android/jni/mozcjni.cc` がエクスポートするシンボルは 1 つだけで、

```
Java_com_google_android_apps_inputmethod_libs_mozc_session_MozcJNI_initialize
```

これが `RegisterNatives` で残り 3 メソッドを登録する。つまり Java 側のクラス名が
`com.google.android.apps.inputmethod.libs.mozc.session.MozcJNI` に固定される。
そこで mozc を書き換える代わりに、その名前のシムクラス
(`mozc/src/main/java/.../MozcJNI.java`) をこちら側に置いた。結果として
third_party は素の checkout のままで、いつでも上流に追従できる。

アプリコードはシムを直接触らず、`dev.oss.ime.mozc.MozcEngine`
(ライフサイクルとスレッド安全性を持つ) を経由する。

### フリック入力と mozc の契約

キーボードは「かな」を送らない。mozc の `FLICK_TO_HIRAGANA` テーブルは **ASCII キー**を
入力とする対応表で (`1`→あ, `_`→い, `*`→濁点/小文字の循環)、濁点処理も小書き文字の規則も
このテーブルが実装している。かなを直接送るとそれらを全部迂回してしまうため、
配列 JSON には**テーブルキー**を格納し、mozc に合成させる。

その対応表を手書きすると「あるフリック方向だけ無反応」という壊れ方をするので、
`scripts/gen_flick_layout.py` が上流の
`data/preedit/*.tsv` から直接生成し、値を検証している。

テーブルだけでは足りず、**変換モード**も合わせて切り替える必要がある。
テーブルは打鍵が何の文字になるかを決めるだけで、そこから変換をかけるかどうかは
`CompositionMode` が決める。英数プレーンを HIRAGANA のままにすると
"abc" に漢字候補が出てしまう。

| プレーン | テーブル | CompositionMode |
| --- | --- | --- |
| かな | `FLICK_TO_HIRAGANA` (13) | `HIRAGANA` |
| 英数 | `TOGGLE_FLICK_TO_HALFWIDTHASCII` (17) | `HALF_ASCII` |
| 記号・数字 | `TOGGLE_FLICK_TO_NUMBER` (42) | `HIRAGANA` |

### 追加辞書

[dic-nico-intersection-pixiv](https://github.com/ncaq/dic-nico-intersection-pixiv)
(ニコニコ大百科とピクシブ百科事典の共通見出し、約 10.2 万語) を標準で同梱する。
あわせて [google-ime-user-dictionary-ja-en](https://github.com/KEINOS/google-ime-user-dictionary-ja-en)
(カタカナ語→英語つづり、約 3.7 万語) も同梱する。「ぶらっく」で black が出る。
配布形態が Google IME の旧 1 万行制限に合わせて分割されているので 1 ファイルに結合し、
同じディレクトリに .docx を .txt 名で置いてあるファイルが混ざっているため
`よみ<TAB>単語<TAB>品詞` として読める行だけを通している。

どちらも `scripts/fetch_dictionaries.sh` がビルド時に取得し、APK に入る。

mozc.data に焼き込むのではなく **ユーザー辞書として取り込む**。
システム辞書に入れるには mozc の bazel 辞書ソースにパッチを当てる必要があり、
third_party を素のままにしておく方針と衝突するため。
辞書の差し替えもアセットを置き換えるだけで済み、18 MB のネイティブデータを
作り直さなくてよい。

取り込みは初回起動時にバックグラウンドで一度だけ走る。mozc 側の
`IMPORT_USER_DICTIONARY` が TSV の解析・同名辞書の置き換え・即時リロード・
プロファイルディレクトリへの保存まで面倒を見るので、こちらは投げるだけでよい。
状況は設定画面の「追加辞書」に出る。

### ユーザー辞書

設定 →「ユーザー辞書」から単語を 1 件ずつ追加・編集・削除できる。品詞は
mozc が TSV で受け付ける 45 種類から選ぶ。

この mozc には**単語単位の API が無い**。`SEND_USER_DICTIONARY_COMMAND` は
reserved になっていて、入口は辞書まるごとを名前で置き換える
`IMPORT_USER_DICTIONARY` だけ。そこで一覧の正本を
`files/user_dictionary.tsv` に持ち、編集のたびに全体を投げ直している。
個人辞書のサイズなら書き直しのコストは無視できるし、追加・編集・削除が
すべて同じ操作になる (空を投げれば辞書ごと消える)。

このファイルはそのまま mozc / Google 日本語入力の辞書ツールに読ませられる。

### 誤字修正

OSS 版 mozc に誤字修正は入っていない。上流は予測器側の配線
(`SupplementalModelInterface::CorrectComposition` を呼び、補正後の読みを辞書引きして
`TYPING_CORRECTION` を付ける処理) を残したまま、モデル本体だけを社内に置いている。
OSS ビルドが積むのは常に `nullopt` を返す `SupplementalModelStub`。

そこで `patches/0001-rule-based-typing-correction.patch` で
`TypingCorrectionModel` を追加し、スタブと差し替えている。周辺の機構には触っていない。

生成する仮説は 3 種類。

| 種類 | 例 |
| --- | --- |
| フリック方向ずれ (同一キー内の母音違い) | ありがと**お** → ありがと**う** |
| っ の脱落 | が**こ**う → が**っこ**う |
| 隣接転置 | にほん**こ** → 日本語 |

濁点・半濁点・小書きの誤りはこの補正の対象外で、
`kana_modifier_insensitive_conversion` (Request/Config 両方で有効化済み) が吸収する。
辞書引きの範囲を広げるだけなので追加コストがない。

**候補数は 6 に絞ってある。** 呼び出し側は補正候補 1 件ごとに
unigram / realtime / bigram / number の全アグリゲーションを回すため、
デスクトップ実測で 1 件あたり約 5 ms かかる。12 件にすると予測が 6 倍遅くなった。
配分は生成順ではなく「もっともらしさ順」で、直前に打ったかなの方向ずれを最優先にする
(先着順にすると後段の生成器が枯れて、が**っこ**う か ありがと**う** の
どちらかが必ず出なくなる)。

`Config.use_typing_correction` と `use_kana_modifier_insensitive_conversion` は
どちらも既定 off なので、クライアント側で `SET_CONFIG` して有効化している。

## カスタマイズ

記号は Gboard と同じく専用プレーン (`記号` キー) にまとめてある。英数プレーンの
空きフリック方向には何も入れない。プレーンの中身は mozc の
`toggle_flick-number` テーブルそのままで、Gboard の記号面と一致する
(`1` → ☆♪→、`5` → +×÷、`7` → 「」:)。

なお Gboard がこの面の `!?#` に持っている 2 ページ目の記号面は未実装で、
その位置は現状「かな」に戻るキーになっている。

配列とテーマは JSON。読み込み順は **ユーザーディレクトリ → アセット** で、
同じ id のファイルをアプリの `files/layouts/` に置けば同梱版を上書きし、
削除すれば既定に戻る。将来の GUI エディタもこの同じファイルを書くだけになる。

```
files/layouts/flick_kana.json    配列 (キー配置, フリック割り当て, 動作)
files/themes/default_dark.json   配色, キー高さ, 角丸, ハプティクス, キー塗り
```

### 外観設定 (端末ごと)

テーマが「持ち運べる見た目」なのに対し、こちらはこの端末固有の選択なので
テーマファイルではなく SharedPreferences に置く。設定画面の「外観」から操作する。

- **ピュアブラック** — 背景を `#000000` にする。有機ELでは黒画素が消灯する。
  システムがライトテーマでも配色はダーク側を使う (黒地に明色のラベルが要るため)。
- **キーボード背景画像** — 選んだ画像はアプリの files 配下に**コピー**する。
  IME は SAF の権限を持たない別プロセスで動くうえ、元ファイルは後から
  削除されうるため。濃さはスライダーで調整でき、既定は 45%。

キーは既定で**塗りを持たない**。ラベルがある以上キーごとの枠は情報を足さず、
コントラストを食って背景画像の邪魔になるだけなので。従来の塗り表示に戻すには
テーマの `flatKeys` を `false` にする。

## ライセンス

- 本体: Apache License 2.0
- `third_party/mozc`: BSD 3-Clause (Google Inc.)
- 同梱辞書 `mozc.data`: mozc OSS 辞書由来。構成要素ごとのライセンスは
  上流の `data/dictionary_oss/README.txt` を参照
