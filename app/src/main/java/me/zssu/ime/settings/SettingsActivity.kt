package me.zssu.ime.settings

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import me.zssu.ime.keyboard.KeyboardStyle
import me.zssu.ime.keyboard.LayoutRepository
import me.zssu.ime.mozc.MozcEngine
import me.zssu.ime.mozc.UserDictionary
import me.zssu.ime.theme.ZinnaTheme

/**
 * Setup and status screen.
 *
 * Deliberately thin for now: enabling the IME and confirming the native engine actually loaded are
 * the two things a user needs on first run, and the second one is the difference between "the
 * keyboard is broken" and "libmozc.so is missing for this ABI".
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZinnaTheme {
                Scaffold { padding ->
                    SettingsScreen(Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { LayoutRepository(context) }
    val engineStatus = remember { describeEngine(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Komorebi IME", style = MaterialTheme.typography.headlineMedium)
        Text("言葉をそっと照らす、日本語入力", style = MaterialTheme.typography.titleMedium)

        InfoCard(title = "変換エンジン", body = engineStatus)
        InfoCard(title = "追加辞書", body = describeDictionaries(context))
        CustomizationCard(repository)

        UserDictionaryCard()

        MeaningDictionaryCard()

        LearningCard()

        KeyboardCard()

        AppProfilesCard()

        AppearanceCard()

        AboutCard()

        TestInputField()

        Button(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("キーボードを有効化") }

        Button(
            onClick = {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("キーボードを選択") }
    }
}

@Composable
private fun MeaningDictionaryCard() {
    val context = LocalContext.current
    val count = me.zssu.ime.dictionary.MeaningDictionaryRepository(context)
        .dictionaries().sumOf { it.entries.size }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("意味辞書", style = MaterialTheme.typography.titleMedium)
            Text("$count 語の意味を候補長押しで表示できます")
            Button(
                onClick = {
                    context.startActivity(Intent(context, MeaningDictionaryActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("意味辞書を追加・管理") }
        }
    }
}

@Composable
private fun AppProfilesCard() {
    val context = LocalContext.current
    val count = AppProfileStore(context).profiles().size
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("アプリ別入力プロファイル", style = MaterialTheme.typography.titleMedium)
            Text(if (count == 0) "アプリ別設定はありません" else "$count アプリに設定済み")
            Text(
                "入力方式・片手モード・高さ・シークレットモードをアプリごとに切り替えます",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = {
                    context.startActivity(Intent(context, AppProfileActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("アプリ別設定を管理") }
        }
    }
}

@Composable
private fun CustomizationCard(repository: LayoutRepository) {
    val context = LocalContext.current
    val settings = remember { ImeSettings(context) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("配列とテーマ", style = MaterialTheme.typography.titleMedium)
            Text(
                "配列 ${repository.availableLayoutIds().size} 個 / テーマ " +
                    "${repository.availableThemeIds().size} 個",
            )
            Text(
                "使用中: ${settings.activeLayoutId ?: "入力方式の標準"} / ${settings.activeThemeId}",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = {
                    context.startActivity(Intent(context, CustomizationActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("プリセット選択・自作・共有") }
        }
    }
}

/**
 * Attribution is deliberately visible in the installed app, not only in the source repository.
 * The packaged LICENSE/NOTICE files provide the complete legal text; this card gives users the
 * identity, origin, and support route they need without unpacking the APK.
 */
@Composable
private fun AboutCard() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("このアプリについて", style = MaterialTheme.typography.titleMedium)
            Text(
                "ZenSky Project が soichi11208/Zinna-IME をフォークし、変更・配布している版です。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "ライセンス: Apache License 2.0\n" +
                    "パッケージ: me.zssu.ime\n" +
                    "連絡先: support@zslink.xyz",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "原作者および Mozc・同梱辞書の権利は各権利者に帰属します。完全な表示は" +
                    "「オープンソースライセンス」から確認できます。",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = {
                    context.startActivity(Intent(context, LegalActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("オープンソースライセンス") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        uriHandler.openUri("https://github.com/soichi11208/Zinna-IME")
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("フォーク元") }
                OutlinedButton(
                    onClick = { uriHandler.openUri("mailto:support@zslink.xyz") },
                    modifier = Modifier.weight(1f),
                ) { Text("問い合わせ") }
            }
        }
    }
}

@Composable
private fun UserDictionaryCard() {
    val context = LocalContext.current
    // Recomputed on every composition rather than remembered: returning from the editor
    // recomposes this screen, and a cached count would show the pre-edit number.
    val count = UserDictionary(context).entries().size

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ユーザー辞書", style = MaterialTheme.typography.titleMedium)
            Text(
                if (count == 0) "登録された単語はありません" else "$count 語を登録済み",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = {
                    context.startActivity(Intent(context, UserDictionaryActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("単語を編集") }
        }
    }
}

/**
 * How the keyboard is laid out and how big it is — choices about the input surface rather than
 * about how it looks, which is why they are not in [AppearanceCard].
 */
@Composable
private fun KeyboardCard() {
    val context = LocalContext.current
    val settings = remember { ImeSettings(context) }

    var style by remember { mutableStateOf(settings.keyboardStyle) }
    var heightScale by remember { mutableStateOf(settings.keyHeightScale) }
    var oneHandMode by remember { mutableStateOf(settings.oneHandMode) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("キーボード", style = MaterialTheme.typography.titleMedium)

            Text("入力方式", style = MaterialTheme.typography.bodyLarge)
            Text(
                "かなと英字それぞれの配列。キーボード上のキーで面を切り替えたときも、この選択に従います",
                style = MaterialTheme.typography.bodySmall,
            )
            for ((option, label) in listOf(
                KeyboardStyle.FLICK to "フリック",
                KeyboardStyle.QWERTY to "QWERTY",
                KeyboardStyle.MIXED to "かなフリック + 英字QWERTY",
            )) {
                val onSelect = {
                    style = option
                    settings.keyboardStyle = option
                }
                if (style == option) {
                    Button(onClick = onSelect, modifier = Modifier.fillMaxWidth()) { Text(label) }
                } else {
                    OutlinedButton(
                        onClick = onSelect,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(label) }
                }
            }

            HorizontalDivider()

            Text(
                "キーボードの高さ  ${(heightScale * 100).toInt()}%",
                style = MaterialTheme.typography.bodyLarge,
            )
            Slider(
                value = heightScale,
                onValueChange = { heightScale = it },
                onValueChangeFinished = { settings.keyHeightScale = heightScale },
                valueRange = ImeSettings.MIN_KEY_HEIGHT_SCALE..ImeSettings.MAX_KEY_HEIGHT_SCALE,
            )

            HorizontalDivider()
            Text("片手モード", style = MaterialTheme.typography.bodyLarge)
            Text(
                "候補がないときのツールバーからも左右を切り替えられます",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((option, label) in listOf(
                    ImeSettings.OneHandMode.LEFT to "左寄せ",
                    ImeSettings.OneHandMode.OFF to "全幅",
                    ImeSettings.OneHandMode.RIGHT to "右寄せ",
                )) {
                    val select = {
                        oneHandMode = option
                        settings.oneHandMode = option
                    }
                    if (oneHandMode == option) {
                        Button(onClick = select, modifier = Modifier.weight(1f)) { Text(label) }
                    } else {
                        OutlinedButton(onClick = select, modifier = Modifier.weight(1f)) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningCard() {
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("変換学習", style = MaterialTheme.typography.titleMedium)
            Text(
                "候補を長押しすると意味を確認でき、削除可能な予測候補では学習削除も選べます。",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("変換学習をすべてリセット") }
            result?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("変換学習をリセットしますか？") },
            text = { Text("ユーザー辞書は残り、変換履歴と予測履歴だけが削除されます。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = MozcEngine.get(context)?.clearLearning() == true
                        result = if (ok) "変換学習をリセットしました" else "リセットに失敗しました"
                        confirmReset = false
                    },
                ) { Text("リセット") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("キャンセル") }
            },
        )
    }
}

/**
 * Appearance controls that belong to this device rather than to a shareable theme file.
 *
 * The keyboard picks changes up the next time it opens — it lives in another process and only
 * re-reads settings when its input view starts.
 */
@Composable
private fun AppearanceCard() {
    val context = LocalContext.current
    val settings = remember { ImeSettings(context) }

    var pureBlack by remember { mutableStateOf(settings.pureBlack) }
    var opacity by remember { mutableStateOf(settings.backgroundOpacity) }
    var hasImage by remember { mutableStateOf(settings.backgroundImage != null) }
    var selectedPreset by remember { mutableStateOf(settings.backgroundPresetId) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Read it out now rather than holding the URI: the IME runs in a process that never got
        // this grant, and the picked file can be deleted or its volume unmounted later.
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes != null) {
            settings.setBackgroundImage(bytes)
            hasImage = true
            selectedPreset = null
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("外観", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("ピュアブラック", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "有機EL向け。背景を完全な黒にして画素を消灯させます",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = pureBlack,
                    onCheckedChange = {
                        pureBlack = it
                        settings.pureBlack = it
                    },
                )
            }

            HorizontalDivider()

            Text("キーボードの背景画像", style = MaterialTheme.typography.bodyLarge)
            Text("生成アート・プリセット", style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ImeSettings.BACKGROUND_PRESETS.forEach { preset ->
                    val thumbnail = remember(preset.id) {
                        context.assets.open(preset.assetPath).use {
                            requireNotNull(
                                BitmapFactory.decodeStream(
                                    it,
                                    null,
                                    BitmapFactory.Options().apply { inSampleSize = 4 },
                                )
                            ) { "背景プリセットを読み込めません: ${preset.assetPath}" }
                                .asImageBitmap()
                        }
                    }
                    Card(
                        onClick = {
                            if (settings.setBackgroundPreset(preset.id)) {
                                selectedPreset = preset.id
                                hasImage = true
                            }
                        },
                        modifier = Modifier.width(144.dp),
                    ) {
                        Image(
                            bitmap = thumbnail,
                            contentDescription = "${preset.label} 背景",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                        )
                        Text(
                            if (selectedPreset == preset.id) "${preset.label}・選択中"
                            else preset.label,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }

            Text("自分の画像", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { pickImage.launch(arrayOf("image/*")) },
                    modifier = Modifier.weight(1f),
                ) { Text(if (hasImage) "変更" else "選択") }
                OutlinedButton(
                    onClick = {
                        settings.clearBackgroundImage()
                        hasImage = false
                        selectedPreset = null
                    },
                    enabled = hasImage,
                    modifier = Modifier.weight(1f),
                ) { Text("解除") }
            }

            if (hasImage) {
                Text(
                    "画像の濃さ  ${(opacity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                )
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    onValueChangeFinished = { settings.backgroundOpacity = opacity },
                    valueRange = 0f..1f,
                )
            }
        }
    }
}

/**
 * Somewhere to actually try the keyboard without leaving the app.
 *
 * A multi-line field on purpose: it is the only way to see what the Enter key does when the editor
 * has no action attached, which is exactly the case that is easy to get wrong.
 */
@Composable
private fun TestInputField() {
    var text by remember { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("試し入力", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = { Text("ここで入力を試せます") },
                singleLine = false,
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * The bundled dictionaries import in the background on first run, so this doubles as the way to
 * tell "not imported yet" from "imported and empty" without digging through logcat.
 */
private fun describeDictionaries(context: Context): String {
    val engine = MozcEngine.get(context) ?: return "—"
    val installed = engine.installedDictionaries(context)
    if (installed.isEmpty()) return "読み込み中… (初回起動時にバックグラウンドで取り込みます)"
    return installed.joinToString("\n") { "${it.name}  ${"%,d".format(it.entryCount)} 語" }
}

private fun describeEngine(context: Context): String {
    val engine = MozcEngine.get(context)
        ?: return "読み込み失敗 — この ABI 用の libmozc.so がありません (scripts/build_mozc.sh を実行)"
    val version = engine.dataVersion.ifEmpty { "unknown" }
    return "オフライン動作中 / 辞書バージョン $version"
}
