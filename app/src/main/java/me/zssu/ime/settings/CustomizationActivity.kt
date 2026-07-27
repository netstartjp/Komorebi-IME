package me.zssu.ime.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.zssu.ime.keyboard.LayoutRepository
import me.zssu.ime.theme.MaterialYouTheme
import me.zssu.ime.theme.ZinnaTheme

/**
 * Data-first layout/theme studio with both common visual edits and full JSON access.
 *
 * The visual editor handles spatial changes and long-press symbols. The JSON editor remains the
 * complete representation of every KeyAction, while preset selection, validation and SAF sharing
 * keep both paths interoperable.
 */
class CustomizationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZinnaTheme { Scaffold { Studio(Modifier.padding(it)) } } }
    }
}

private enum class ResourceKind { LAYOUT, THEME }

@Composable
private fun Studio(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { LayoutRepository(context) }
    val settings = remember { ImeSettings(context) }
    var kind by remember { mutableStateOf(ResourceKind.LAYOUT) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("プリセットを選ぶか、JSONをインポートしてください") }
    var refresh by remember { mutableIntStateOf(0) }
    var exportText by remember { mutableStateOf<String?>(null) }

    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("ファイルを読み込めません")
        }
        text.onSuccess {
            editor = it
            repository.importJson(it).onSuccess { imported ->
                kind = if (imported is LayoutRepository.ImportedResource.Layout) {
                    ResourceKind.LAYOUT
                } else ResourceKind.THEME
                selectedId = imported.id
                message = "${imported.id} を検証して取り込みました"
                refresh++
            }.onFailure { error -> message = "取り込み失敗: ${error.message}" }
        }.onFailure { message = "読み込み失敗: ${it.message}" }
    }
    val exportFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val text = exportText
        if (uri != null && text != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
                    ?: error("保存先を開けません")
            }.onSuccess { message = "JSONを書き出しました" }
                .onFailure { message = "書き出し失敗: ${it.message}" }
        }
        exportText = null
    }

    val ids = remember(kind, refresh) {
        if (kind == ResourceKind.LAYOUT) repository.availableLayoutIds()
        else repository.availableThemeIds()
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("配列・テーマスタジオ", style = MaterialTheme.typography.headlineSmall)
        Text("プリセットを選択し、複製して自由に編集できます。作成数に上限はありません。")
        Button(
            onClick = {
                context.startActivity(Intent(context, VisualLayoutEditorActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("視覚的な配列エディタを開く") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeButton("配列", kind == ResourceKind.LAYOUT, Modifier.weight(1f)) {
                kind = ResourceKind.LAYOUT; selectedId = null; editor = ""
            }
            ModeButton("テーマ", kind == ResourceKind.THEME, Modifier.weight(1f)) {
                kind = ResourceKind.THEME; selectedId = null; editor = ""
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("プリセット / 作成済み", style = MaterialTheme.typography.titleMedium)
                ids.forEach { id ->
                    val active = if (kind == ResourceKind.LAYOUT) settings.activeLayoutId == id
                        else settings.activeThemeId == id
                    OutlinedButton(
                        onClick = {
                            selectedId = id
                            editor = if (kind == ResourceKind.LAYOUT) {
                                repository.exportLayout(id).orEmpty()
                            } else repository.exportTheme(id).orEmpty()
                            message = if (id == MaterialYouTheme.ID) {
                                "Material You は端末色から動的生成されるため編集できません"
                            } else "$id を読み込みました"
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text((if (active) "✓ " else "") + id) }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    selectedId?.let {
                        if (kind == ResourceKind.LAYOUT) settings.activeLayoutId = it
                        else settings.activeThemeId = it
                        message = "$it を使用します。キーボードを開き直すと反映されます"
                        refresh++
                    }
                },
                enabled = selectedId != null,
                modifier = Modifier.weight(1f),
            ) { Text("使用する") }
            OutlinedButton(
                onClick = { importFile.launch(arrayOf("application/json", "text/*")) },
                modifier = Modifier.weight(1f),
            ) { Text("取り込み") }
        }

        HorizontalDivider()
        Text("JSONエディタ", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = editor,
            onValueChange = { editor = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp),
            label = { Text("id を変更すると複製として保存") },
            textStyle = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    repository.importJson(editor)
                        .onSuccess {
                            kind = if (it is LayoutRepository.ImportedResource.Layout) {
                                ResourceKind.LAYOUT
                            } else ResourceKind.THEME
                            selectedId = it.id
                            message = "${it.id} を検証して保存しました"
                            refresh++
                        }
                        .onFailure { message = "保存できません: ${it.message}" }
                },
                enabled = editor.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) { Text("検証して保存") }
            OutlinedButton(
                onClick = {
                    val id = selectedId ?: return@OutlinedButton
                    exportText = if (kind == ResourceKind.LAYOUT) repository.exportLayout(id)
                        else repository.exportTheme(id)
                    if (exportText == null) message = "この動的テーマは書き出せません"
                    else exportFile.launch("$id.json")
                },
                enabled = selectedId != null,
                modifier = Modifier.weight(1f),
            ) { Text("書き出し") }
        }
        OutlinedButton(
            onClick = {
                val id = selectedId ?: return@OutlinedButton
                val directory = if (kind == ResourceKind.LAYOUT) LayoutRepository.LAYOUTS_DIR
                    else LayoutRepository.THEMES_DIR
                if (repository.deleteOverride(directory, id)) {
                    if (kind == ResourceKind.LAYOUT && settings.activeLayoutId == id) {
                        settings.activeLayoutId = null
                    }
                    if (kind == ResourceKind.THEME && settings.activeThemeId == id) {
                        settings.activeThemeId = ImeSettings.DEFAULT_THEME_ID
                    }
                    selectedId = null
                    editor = ""
                    message = "ユーザー版を削除しました（同名プリセットがあれば既定に戻ります）"
                    refresh++
                } else message = "同梱プリセットは削除できません。複製して編集してください"
            },
            enabled = selectedId?.let {
                repository.hasUserOverride(
                    if (kind == ResourceKind.LAYOUT) LayoutRepository.LAYOUTS_DIR
                    else LayoutRepository.THEMES_DIR,
                    it,
                )
            } == true,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("ユーザー版を削除") }
        Text(message, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ModeButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    if (selected) Button(onClick, modifier) { Text(label) }
    else OutlinedButton(onClick, modifier) { Text(label) }
}
