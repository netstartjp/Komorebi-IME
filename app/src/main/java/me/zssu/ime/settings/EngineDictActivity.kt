package me.zssu.ime.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.zssu.ime.mozc.MozcEngine
import me.zssu.ime.theme.ZinnaTheme

class EngineDictActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZinnaTheme { EngineDictScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EngineDictScreen() {
    val context = LocalContext.current
    val settings = remember { ImeSettings(context) }
    var useDict by remember { mutableStateOf(settings.useProperNounDictionary) }
    var useAiDict by remember { mutableStateOf(settings.useAiTechDictionary) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("変換エンジン・辞書") },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Engine status
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Psychology, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("変換エンジン", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(describeEngine(context), style = MaterialTheme.typography.bodyMedium)
                    Text(describeDictionaries(context), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Dictionary toggles
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("変換辞書の切り替え", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    SwitchRow("有名固有名詞", "企業名・サービス名・人名・地名", useDict) {
                        useDict = it; settings.useProperNounDictionary = it
                        Thread {
                            try { MozcEngine.get(context)?.setBundledDictionaryActive(context, "proper-nouns.txt", it) } catch (_: Exception) {}
                        }.apply { isDaemon = true }.start()
                    }
                    HorizontalDivider()
                    SwitchRow("AI・テクノロジー", "AI企業・技術用語・スタートアップ名", useAiDict) {
                        useAiDict = it; settings.useAiTechDictionary = it
                        Thread {
                            try { MozcEngine.get(context)?.setBundledDictionaryActive(context, "ai-tech-nouns.txt", it) } catch (_: Exception) {}
                        }.apply { isDaemon = true }.start()
                    }
                    HorizontalDivider()
                    SwitchRow("カタカナ語→英語", "カタカナ外来語の英語表記を表示", checked = true, enabled = false) {}
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

private fun describeDictionaries(context: Context): String {
    val engine = MozcEngine.get(context) ?: return "—"
    val installed = engine.installedDictionaries(context)
    if (installed.isEmpty()) return "読み込み中…"
    return installed.joinToString(", ") { "${it.name} ${"%,d".format(it.entryCount)}語" }
}

private fun describeEngine(context: Context): String {
    val engine = MozcEngine.get(context) ?: return "読み込み失敗"
    return "オフライン動作中 / 辞書 v${engine.dataVersion.ifEmpty { "unknown" }}"
}
