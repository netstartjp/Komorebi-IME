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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.zssu.ime.keyboard.FlickGuideStyle
import me.zssu.ime.keyboard.KeyboardStyle
import me.zssu.ime.keyboard.LayoutRepository
import me.zssu.ime.mozc.MozcEngine
import me.zssu.ime.mozc.UserDictionary
import me.zssu.ime.theme.ZinnaTheme

// ── Dashboard section keys ──
private typealias Section = String
private const val SEC_ENGINE: Section = "engine"
private const val SEC_DICT: Section = "dict"
private const val SEC_INPUT: Section = "input"
private const val SEC_LAYOUT: Section = "layoutKey"
private const val SEC_APPEARANCE: Section = "appearance"
private const val SEC_LEARNING: Section = "learning"
private const val SEC_PROFILES: Section = "profiles"
private const val SEC_DICTIONARY: Section = "userDict"
private const val SEC_MEANING: Section = "meaning"

// ── Dashboard entry model ──
private data class DashEntry(
    val key: Section,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accentColors: List<Color>,
)

private val dashEntries = listOf(
    DashEntry(SEC_ENGINE, Icons.Filled.Psychology, "変換エンジン", "辞書・動作状態", listOf(Color(0xFF0B8460), Color(0xFF4DDBA8))),
    DashEntry(SEC_DICT, Icons.Filled.AutoAwesome, "変換辞書", "固有名詞・AI用語の切替", listOf(Color(0xFF7B3FCF), Color(0xFFC8ABFF))),
    DashEntry(SEC_INPUT, Icons.Filled.TouchApp, "入力方式", "フリック・QWERTY設定", listOf(Color(0xFFE8571A), Color(0xFFFFB593))),
    DashEntry(SEC_LAYOUT, Icons.Filled.Palette, "配列とテーマ", "プリセット・自作", listOf(Color(0xFF0E7AA3), Color(0xFF7DD3F5))),
    DashEntry(SEC_APPEARANCE, Icons.Filled.Image, "外観", "背景画像・黒設定", listOf(Color(0xFFB81A6B), Color(0xFFF5A0CD))),
    DashEntry(SEC_LEARNING, Icons.Filled.School, "変換学習", "履歴リセット", listOf(Color(0xFF8A6A13), Color(0xFFFFE47A))),
    DashEntry(SEC_PROFILES, Icons.Filled.Devices, "アプリ別設定", "プロファイル管理", listOf(Color(0xFF4A4D52), Color(0xFFB0B3BB))),
    DashEntry(SEC_DICTIONARY, Icons.Filled.Edit, "ユーザー辞書", "単語の追加・編集", listOf(Color(0xFF2E8B57), Color(0xFF98FB98))),
    DashEntry(SEC_MEANING, Icons.Filled.GTranslate, "意味辞書", "候補長押しで意味表示", listOf(Color(0xFF6B3FA0), Color(0xFFD4A5FF))),
)

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZinnaTheme { SettingsScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf<Section?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Komorebi IME", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                        ))
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("言葉をそっと照らす", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(2.dp))
                    Text("Komorebi IME", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            // Quick actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Keyboard, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("キーボードを有効化")
                }
                FilledTonalButton(
                    onClick = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showInputMethodPicker()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Widgets, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("キーボードを選択")
                }
            }

            // Dashboard grid
            dashEntries.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { entry ->
                        DashboardCard(
                            entry = entry,
                            expanded = expanded == entry.key,
                            onClick = {
                                expanded = if (expanded == entry.key) null else entry.key
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // If odd number, fill with spacer
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // About row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(context, LegalActivity::class.java))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ライセンス")
                }
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(context, AppProfileActivity::class.java))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Devices, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("アプリ別設定")
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DashboardCard(
    entry: DashEntry,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .animateContentSize(tween(300))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 4.dp else 1.dp),
    ) {
        Column {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(entry.accentColors)
                    )
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        entry.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            entry.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            entry.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Expanded content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(300)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(300)) + fadeOut(tween(200)),
            ) {
                Column(Modifier.padding(12.dp)) {
                    ExpandedContent(entry.key)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandedContent(key: Section) {
    when (key) {
        SEC_ENGINE -> EngineContent()
        SEC_DICT -> DictionaryContent()
        SEC_INPUT -> InputStyleContent()
        SEC_LAYOUT -> LayoutThemeContent()
        SEC_APPEARANCE -> AppearanceContent()
        SEC_LEARNING -> LearningContent()
        SEC_PROFILES -> ProfilesContent()
        SEC_DICTIONARY -> UserDictContent()
        SEC_MEANING -> MeaningDictContent()
    }
}

// ── Engine content ──
@Composable
private fun EngineContent() {
    val context = LocalContext.current
    val engineStatus = remember { describeEngine(context) }
    val dictStatus = remember { describeDictionaries(context) }
    val healthy = engineStatus.startsWith("オフライン")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!healthy) {
            Text("⚠ 読み込み失敗", color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Text(engineStatus, style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text(dictStatus, style = MaterialTheme.typography.bodySmall)
    }
}

// ── Dictionary content ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictionaryContent() {
    val context = LocalContext.current
    val settings = remember { ImeSettings(context) }
    var useDict by remember { mutableStateOf(settings.useProperNounDictionary) }
    var useAiDict by remember { mutableStateOf(settings.useAiTechDictionary) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MiniSwitchRow("有名固有名詞", "企業名・サービス名・人名・地名", useDict) {
            useDict = it; settings.useProperNounDictionary = it
            Thread {
                try { MozcEngine.get(context)?.setBundledDictionaryActive(context, "proper-nouns.txt", it) } catch (_: Exception) {}
            }.apply { isDaemon = true }.start()
        }
        HorizontalDivider()
        MiniSwitchRow("AI・テクノロジー", "AI企業・技術用語・スタートアップ名", useAiDict) {
            useAiDict = it; settings.useAiTechDictionary = it
            Thread {
                try { MozcEngine.get(context)?.setBundledDictionaryActive(context, "ai-tech-nouns.txt", it) } catch (_: Exception) {}
            }.apply { isDaemon = true }.start()
        }
        HorizontalDivider()
        MiniSwitchRow("カタカナ語→英語", "カタカナ外来語の英語表記", checked = true, enabled = false) {}
    }
}

// ── Input style content ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputStyleContent() {
    val context = LocalContext.current
    val settings = remember { ImeSettings(context) }
    var style by remember { mutableStateOf(settings.keyboardStyle) }
    var flickInputMode by remember { mutableStateOf(settings.flickInputMode) }
    var guide by remember { mutableStateOf(settings.flickGuideStyle) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("キーボード配列", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(KeyboardStyle.FLICK to "フリック", KeyboardStyle.QWERTY to "QWERTY", KeyboardStyle.MIXED to "混合").forEach { (v, l) ->
                MiniChip(style == v, l) { style = v; settings.keyboardStyle = v }
            }
        }
        HorizontalDivider()
        Text("フリック連打", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MiniChip(flickInputMode == ImeSettings.FlickInputMode.FLICK_ONLY, "フリックのみ") {
                flickInputMode = ImeSettings.FlickInputMode.FLICK_ONLY; settings.flickInputMode = ImeSettings.FlickInputMode.FLICK_ONLY
            }
            MiniChip(flickInputMode == ImeSettings.FlickInputMode.FLICK_AND_TOGGLE, "ケータイ併用") {
                flickInputMode = ImeSettings.FlickInputMode.FLICK_AND_TOGGLE; settings.flickInputMode = ImeSettings.FlickInputMode.FLICK_AND_TOGGLE
            }
        }
        HorizontalDivider()
        Text("フリックガイド", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MiniChip(guide == FlickGuideStyle.PREVIEW, "選択文字のみ") {
                guide = FlickGuideStyle.PREVIEW; settings.flickGuideStyle = FlickGuideStyle.PREVIEW
            }
            MiniChip(guide == FlickGuideStyle.DIRECTIONS, "4方向表示") {
                guide = FlickGuideStyle.DIRECTIONS; settings.flickGuideStyle = FlickGuideStyle.DIRECTIONS
            }
        }
    }
}

// ── Layout / Theme content ──
@Composable
private fun LayoutThemeContent() {
    val context = LocalContext.current
    val repository = remember { LayoutRepository(context) }
    val settings = remember { ImeSettings(context) }
    val kind = remember { mutableStateOf("layout") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniChip(kind.value == "layout", "配列") { kind.value = "layout" }
            MiniChip(kind.value == "theme", "テーマ") { kind.value = "theme" }
        }
        Text("使用中: ${settings.activeLayoutId ?: "標準"} / ${settings.activeThemeId}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = {
                context.startActivity(Intent(context, CustomizationActivity::class.java))
            }, modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
                Text("プリセット", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(onClick = {
                context.startActivity(Intent(context, ThemeEditorActivity::class.java))
            }, modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
                Text("自作", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ── Appearance content ──
@Composable
private fun AppearanceContent() {
    val context = LocalContext.current
    val settings = remember { ImeSettings(context) }
    var pureBlack by remember { mutableStateOf(settings.pureBlack) }
    var opacity by remember { mutableStateOf(settings.backgroundOpacity) }
    var hasImage by remember { mutableStateOf(settings.backgroundImage != null) }
    var selectedPreset by remember { mutableStateOf(settings.backgroundPresetId) }
    var heightScale by remember { mutableStateOf(settings.keyHeightScale) }
    var oneHandMode by remember { mutableStateOf(settings.oneHandMode) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        if (bytes != null) { settings.setBackgroundImage(bytes); hasImage = true; selectedPreset = null }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MiniSwitchRow("ピュアブラック", "有機EL向け", pureBlack) { pureBlack = it; settings.pureBlack = it }
        HorizontalDivider()
        Text("キーの高さ  ${(heightScale * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Slider(value = heightScale, onValueChange = { heightScale = it },
            onValueChangeFinished = { settings.keyHeightScale = heightScale },
            valueRange = ImeSettings.MIN_KEY_HEIGHT_SCALE..ImeSettings.MAX_KEY_HEIGHT_SCALE)
        HorizontalDivider()
        Text("片手モード", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(ImeSettings.OneHandMode.LEFT to "左", ImeSettings.OneHandMode.OFF to "全幅", ImeSettings.OneHandMode.RIGHT to "右").forEach { (v, l) ->
                MiniChip(oneHandMode == v, l) { oneHandMode = v; settings.oneHandMode = v }
            }
        }
        HorizontalDivider()
        Text("背景プリセット", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ImeSettings.BACKGROUND_PRESETS.forEach { preset ->
                val thumbnail = remember(preset.id) {
                    context.assets.open(preset.assetPath).use {
                        requireNotNull(BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 4 }))
                    }.asImageBitmap()
                }
                Card(onClick = {
                    if (settings.setBackgroundPreset(preset.id)) { selectedPreset = preset.id; hasImage = true }
                }, modifier = Modifier.width(100.dp), shape = RoundedCornerShape(10.dp)) {
                    Image(bitmap = thumbnail, contentDescription = preset.label, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(56.dp))
                    Text(if (selectedPreset == preset.id) "✓" else preset.label, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(6.dp), maxLines = 1)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { pickImage.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
                Text(if (hasImage) "画像変更" else "画像選択", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(onClick = { settings.clearBackgroundImage(); hasImage = false; selectedPreset = null },
                enabled = hasImage, modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
                Text("解除", style = MaterialTheme.typography.labelMedium)
            }
        }
        AnimatedVisibility(visible = hasImage) {
            Column(Modifier.padding(top = 4.dp)) {
                Text("濃さ  ${(opacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                Slider(value = opacity, onValueChange = { opacity = it },
                    onValueChangeFinished = { settings.backgroundOpacity = opacity }, valueRange = 0f..1f)
            }
        }
    }
}

// ── Learning content ──
@Composable
private fun LearningContent() {
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("候補を長押しすると意味を確認でき、学習削除も選べます。", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Update, contentDescription = null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("学習をリセット", style = MaterialTheme.typography.labelMedium)
        }
        result?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("変換学習をリセットしますか？") },
            text = { Text("ユーザー辞書は残り、変換履歴と予測履歴だけが削除されます。") },
            confirmButton = { TextButton(onClick = {
                val ok = MozcEngine.get(context)?.clearLearning() == true
                result = if (ok) "リセットしました" else "リセットに失敗しました"
                confirmReset = false
            }) { Text("リセット") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("キャンセル") } },
        )
    }
}

// ── App profiles content ──
@Composable
private fun ProfilesContent() {
    val context = LocalContext.current
    val count = AppProfileStore(context).profiles().size
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (count == 0) "設定なし" else "$count アプリに設定済み", style = MaterialTheme.typography.bodySmall)
        Text("入力方式・片手モード・高さ・シークレットモードをアプリごとに切替",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { context.startActivity(Intent(context, AppProfileActivity::class.java)) },
            modifier = Modifier.fillMaxWidth()) {
            Text("管理する", style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ── User dictionary content ──
@Composable
private fun UserDictContent() {
    val context = LocalContext.current
    val count = remember { UserDictionary(context).entries().size }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (count == 0) "登録された単語はありません" else "$count 語を登録済み", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { context.startActivity(Intent(context, UserDictionaryActivity::class.java)) },
            modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Edit, contentDescription = null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("単語を編集", style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ── Meaning dictionary content ──
@Composable
private fun MeaningDictContent() {
    val context = LocalContext.current
    val count = me.zssu.ime.dictionary.MeaningDictionaryRepository(context).dictionaries().sumOf { it.entries.size }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$count 語の意味を候補長押しで表示できます", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { context.startActivity(Intent(context, MeaningDictionaryActivity::class.java)) },
            modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("管理する", style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ── Reusable micro-components ──

@Composable
private fun MiniSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun MiniChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

private fun describeDictionaries(context: Context): String {
    val engine = MozcEngine.get(context) ?: return "—"
    val installed = engine.installedDictionaries(context)
    if (installed.isEmpty()) return "読み込み中… (初回起動時にバックグラウンドで取り込みます)"
    return installed.joinToString(", ") { "${it.name} ${"%,d".format(it.entryCount)}語" }
}

private fun describeEngine(context: Context): String {
    val engine = MozcEngine.get(context)
        ?: return "読み込み失敗 — libmozc.so が見つかりません"
    val version = engine.dataVersion.ifEmpty { "unknown" }
    return "オフライン動作中 / 辞書 v$version"
}
