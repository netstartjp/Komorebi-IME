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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GTranslate
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZinnaTheme {
                SettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen() {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("設定", style = MaterialTheme.typography.titleLarge) },
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            HeroSection()
            Spacer(Modifier.height(4.dp))
            QuickActionsSection()
            EngineSection()
            DictionarySection()
            LayoutThemeSection()
            InputStyleSection()
            KeyboardLayoutSection()
            AppearanceSection()
            LearningSection()
            AppProfilesSection()
            AboutSection()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ),
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Brush,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Komorebi IME",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "言葉をそっと照らす、日本語入力",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionsSection() {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(Icons.Filled.Settings, Icons.Outlined.Settings, "クイック設定")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                ) {
                    Icon(Icons.Filled.Keyboard, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("有効化")
                }
                FilledTonalButton(
                    onClick = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showInputMethodPicker()
                    },
                ) {
                    Icon(Icons.Filled.GridView, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("切り替え")
                }
            }
        }
    }
}

@Composable
private fun EngineSection() {
    val context = LocalContext.current
    val engineStatus = remember { describeEngine(context) }
    val dictStatus = remember { describeDictionaries(context) }
    val healthy = engineStatus.startsWith("オフライン")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (healthy) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (healthy) Icons.Filled.Psychology else Icons.Filled.Info,
                    contentDescription = null,
                    tint = if (healthy) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text("変換エンジン", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                engineStatus,
                style = MaterialTheme.typography.bodyMedium,
                color = if (healthy) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onErrorContainer,
            )
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(dictStatus, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictionarySection() {
    val context = LocalContext.current
    val settings = remember { ImeSettings(context) }
    var useDict by remember { mutableStateOf(settings.useProperNounDictionary) }
    var useAiDict by remember { mutableStateOf(settings.useAiTechDictionary) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "変換辞書")
            Text(
                "用途にあわせて辞書のオン・オフを切り替えられます",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SwitchRow(
                title = "有名固有名詞",
                subtitle = "企業名・サービス名・人名・地名",
                checked = useDict,
                onCheckedChange = { checked ->
                    useDict = checked
                    settings.useProperNounDictionary = checked
                    Thread {
                        try {
                            MozcEngine.get(context)?.setBundledDictionaryActive(
                                context, "proper-nouns.txt", checked
                            )
                        } catch (_: Exception) {}
                    }.apply { isDaemon = true }.start()
                },
            )
            HorizontalDivider()
            SwitchRow(
                title = "AI・テクノロジー",
                subtitle = "AI企業・技術用語・スタートアップ名",
                checked = useAiDict,
                onCheckedChange = { checked ->
                    useAiDict = checked
                    settings.useAiTechDictionary = checked
                    Thread {
                        try {
                            MozcEngine.get(context)?.setBundledDictionaryActive(
                                context, "ai-tech-nouns.txt", checked
                            )
                        } catch (_: Exception) {}
                    }.apply { isDaemon = true }.start()
                },
            )
            HorizontalDivider()
            SwitchRow(
                title = "カタカナ語→英語",
                subtitle = "カタカナ外来語の英語表記を表示",
                checked = true,
                enabled = false,
                onCheckedChange = {},
            )
        }
    }
}

@Composable
private fun LayoutThemeSection() {
    val context = LocalContext.current
    val repository = remember { LayoutRepository(context) }
    val settings = remember { ImeSettings(context) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(Icons.Filled.Palette, Icons.Outlined.Palette, "配列とテーマ")
            Text(
                "配列 ${repository.availableLayoutIds().size} 個 / テーマ ${repository.availableThemeIds().size} 個",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "使用中: ${settings.activeLayoutId ?: "入力方式の標準"} / ${settings.activeThemeId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        context.startActivity(Intent(context, CustomizationActivity::class.java))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Style, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("プリセット選択")
                }
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(context, ThemeEditorActivity::class.java))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("自作")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputStyleSection() {
    val context = LocalContext.current
    val settings = remember { ImeSettings(context) }

    var style by remember { mutableStateOf(settings.keyboardStyle) }
    var flickInputMode by remember { mutableStateOf(settings.flickInputMode) }
    var guide by remember { mutableStateOf(settings.flickGuideStyle) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(Icons.Filled.TouchApp, Icons.Outlined.TouchApp, "入力方式")

            Text("キーボード配列", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChoice(
                    selected = style == KeyboardStyle.FLICK,
                    label = "フリック",
                    onClick = {
                        style = KeyboardStyle.FLICK
                        settings.keyboardStyle = KeyboardStyle.FLICK
                    },
                )
                FilterChoice(
                    selected = style == KeyboardStyle.QWERTY,
                    label = "QWERTY",
                    onClick = {
                        style = KeyboardStyle.QWERTY
                        settings.keyboardStyle = KeyboardStyle.QWERTY
                    },
                )
                FilterChoice(
                    selected = style == KeyboardStyle.MIXED,
                    label = "混合",
                    onClick = {
                        style = KeyboardStyle.MIXED
                        settings.keyboardStyle = KeyboardStyle.MIXED
                    },
                )
            }

            HorizontalDivider()

            Text("フリック連打", style = MaterialTheme.typography.labelLarge)
            Text(
                "ケータイ打ちでは同じキー連打で文字が循環します",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChoice(
                    selected = flickInputMode == ImeSettings.FlickInputMode.FLICK_ONLY,
                    label = "フリックのみ",
                    onClick = {
                        flickInputMode = ImeSettings.FlickInputMode.FLICK_ONLY
                        settings.flickInputMode = ImeSettings.FlickInputMode.FLICK_ONLY
                    },
                )
                FilterChoice(
                    selected = flickInputMode == ImeSettings.FlickInputMode.FLICK_AND_TOGGLE,
                    label = "ケータイ併用",
                    onClick = {
                        flickInputMode = ImeSettings.FlickInputMode.FLICK_AND_TOGGLE
                        settings.flickInputMode = ImeSettings.FlickInputMode.FLICK_AND_TOGGLE
                    },
                )
            }

            HorizontalDivider()

            Text("フリックガイド", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChoice(
                    selected = guide == FlickGuideStyle.PREVIEW,
                    label = "選択文字のみ",
                    onClick = {
                        guide = FlickGuideStyle.PREVIEW
                        settings.flickGuideStyle = FlickGuideStyle.PREVIEW
                    },
                )
                FilterChoice(
                    selected = guide == FlickGuideStyle.DIRECTIONS,
                    label = "4方向表示",
                    onClick = {
                        guide = FlickGuideStyle.DIRECTIONS
                        settings.flickGuideStyle = FlickGuideStyle.DIRECTIONS
                    },
                )
            }
        }
    }
}

@Composable
private fun KeyboardLayoutSection() {
    val context = LocalContext.current
    val settings = remember { ImeSettings(context) }

    var heightScale by remember { mutableStateOf(settings.keyHeightScale) }
    var oneHandMode by remember { mutableStateOf(settings.oneHandMode) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(Icons.Filled.Keyboard, Icons.Outlined.Keyboard, "キーボードレイアウト")

            Text(
                "キーの高さ  ${(heightScale * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
            )
            Slider(
                value = heightScale,
                onValueChange = { heightScale = it },
                onValueChangeFinished = { settings.keyHeightScale = heightScale },
                valueRange = ImeSettings.MIN_KEY_HEIGHT_SCALE..ImeSettings.MAX_KEY_HEIGHT_SCALE,
            )

            HorizontalDivider()

            Text("片手モード", style = MaterialTheme.typography.labelLarge)
            Text(
                "候補がないときのツールバーからも切り替えられます",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = oneHandMode == ImeSettings.OneHandMode.LEFT,
                    onClick = {
                        oneHandMode = ImeSettings.OneHandMode.LEFT
                        settings.oneHandMode = ImeSettings.OneHandMode.LEFT
                    },
                    label = { Text("左寄せ") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = oneHandMode == ImeSettings.OneHandMode.OFF,
                    onClick = {
                        oneHandMode = ImeSettings.OneHandMode.OFF
                        settings.oneHandMode = ImeSettings.OneHandMode.OFF
                    },
                    label = { Text("全幅") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = oneHandMode == ImeSettings.OneHandMode.RIGHT,
                    onClick = {
                        oneHandMode = ImeSettings.OneHandMode.RIGHT
                        settings.oneHandMode = ImeSettings.OneHandMode.RIGHT
                    },
                    label = { Text("右寄せ") },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AppearanceSection() {
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
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes != null) {
            settings.setBackgroundImage(bytes)
            hasImage = true
            selectedPreset = null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(Icons.Filled.Image, Icons.Outlined.Image, "外観")

            SwitchRow(
                title = "ピュアブラック",
                subtitle = "有機EL画面の黒を完全に消灯",
                checked = pureBlack,
                onCheckedChange = {
                    pureBlack = it
                    settings.pureBlack = it
                },
            )

            HorizontalDivider()

            Text("キーボードの背景", style = MaterialTheme.typography.labelLarge)
            Text(
                "生成アート・プリセット",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                        shape = RoundedCornerShape(12.dp),
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
                            if (selectedPreset == preset.id) "✓ ${preset.label}"
                            else preset.label,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }

            Text("カスタム画像", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { pickImage.launch(arrayOf("image/*")) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (hasImage) "変更" else "選択")
                }
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

            AnimatedVisibility(visible = hasImage) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "背景の濃さ  ${(opacity * 100).toInt()}%",
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
}

@Composable
private fun LearningSection() {
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(Icons.Filled.School, Icons.Outlined.School, "変換学習")
            Text(
                "候補を長押しすると意味を確認でき、削除可能な予測候補では学習削除も選べます。",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Update, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("学習をリセット")
            }
            result?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
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
                        result = if (ok) "リセットしました" else "リセットに失敗しました"
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

@Composable
private fun AppProfilesSection() {
    val context = LocalContext.current
    val count = AppProfileStore(context).profiles().size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(Icons.Filled.Devices, Icons.Outlined.Devices, "アプリ別プロファイル")
            Text(
                if (count == 0) "設定されているアプリはありません" else "$count アプリに設定済み",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "入力方式・片手モード・高さ・シークレットモードをアプリごとに切り替え",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = {
                    context.startActivity(Intent(context, AppProfileActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Tune, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("アプリ別設定を管理")
            }
        }
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(Icons.Filled.Info, Icons.Outlined.Info, "このアプリについて")
            Text(
                "ZenSky Project が soichi11208/Zinna-IME をフォークし、変更・配布している版です。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "ライセンス: Apache License 2.0\nパッケージ: me.zssu.ime\n連絡先: support@zslink.xyz",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = {
                    context.startActivity(Intent(context, LegalActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Description, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("オープンソースライセンス")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        uriHandler.openUri("https://github.com/soichi11208/Zinna-IME")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("フォーク元")
                }
                OutlinedButton(
                    onClick = { uriHandler.openUri("mailto:support@zslink.xyz") },
                    modifier = Modifier.weight(1f),
                ) { Text("問い合わせ") }
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // User Dictionary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        val userContext = LocalContext.current
        val userCount = remember { UserDictionary(userContext).entries().size }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(Icons.Filled.Edit, Icons.Outlined.Edit, "ユーザー辞書")
            Text(
                if (userCount == 0) "登録された単語はありません" else "$userCount 語を登録済み",
                style = MaterialTheme.typography.bodyMedium,
            )
            FilledTonalButton(
                onClick = {
                    userContext.startActivity(Intent(userContext, UserDictionaryActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("単語を編集")
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // Meaning Dictionary (now with short code)
    val meanContext = LocalContext.current
    val meanCount = me.zssu.ime.dictionary.MeaningDictionaryRepository(meanContext)
        .dictionaries().sumOf { it.entries.size }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(Icons.Filled.GTranslate, Icons.Outlined.GTranslate, "意味辞書")
            Text(
                "$meanCount 語の意味を候補長押しで表示できます",
                style = MaterialTheme.typography.bodyMedium,
            )
            FilledTonalButton(
                onClick = {
                    meanContext.startActivity(Intent(meanContext, MeaningDictionaryActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("意味辞書を管理")
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // Test input
    var text by remember { mutableStateOf("") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(Icons.Filled.Keyboard, Icons.Outlined.Keyboard, "試し入力")
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                placeholder = { Text("ここで入力を試せます") },
                singleLine = false,
            )
        }
    }
}

// ── Reusable components ──

@Composable
private fun SectionHeader(
    filledIcon: ImageVector,
    outlinedIcon: ImageVector,
    title: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            filledIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun FilterChoice(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
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
        ?: return "読み込み失敗 — libmozc.so が見つかりません (scripts/build_mozc.sh を実行)"
    val version = engine.dataVersion.ifEmpty { "unknown" }
    return "オフライン動作中 / 辞書 v$version"
}
