package me.zssu.ime.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import me.zssu.ime.dictionary.MeaningDictionaryRepository
import me.zssu.ime.theme.ZinnaTheme

class MeaningDictionaryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZinnaTheme { Scaffold { MeaningDictionaryScreen(Modifier.padding(it)) } } }
    }
}

@Composable
private fun MeaningDictionaryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { MeaningDictionaryRepository(context) }
    var refresh by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var pendingExport by remember { mutableStateOf<String?>(null) }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("ファイルを読み込めません")
        }.mapCatching { repository.importJson(it).getOrThrow() }
            .onSuccess {
                message = "「${it.label}」を取り込みました"
                refresh++
            }
            .onFailure { message = "取り込み失敗: ${it.message}" }
    }
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val text = pendingExport
        if (uri != null && text != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
                    ?: error("保存先を開けません")
            }.onSuccess { message = "サンプルJSONを書き出しました" }
                .onFailure { message = "書き出し失敗: ${it.message}" }
        }
        pendingExport = null
    }
    val dictionaries = remember(refresh) { repository.dictionaries() }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("意味辞書", style = MaterialTheme.typography.headlineSmall)
        Text(
            "候補を長押しすると、完全一致する語の意味を表示します。JSONファイルを追加するだけで" +
                "語彙を拡張でき、変換辞書や学習履歴とは分けて保存されます。",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { importer.launch(arrayOf("application/json", "text/*")) },
                modifier = Modifier.weight(1f),
            ) { Text("JSONを取り込む") }
            OutlinedButton(
                onClick = {
                    pendingExport = repository.exampleJson()
                    exporter.launch("komorebi-meaning-dictionary-example.json")
                },
                modifier = Modifier.weight(1f),
            ) { Text("ひな形を書出") }
        }
        dictionaries.forEach { dictionary ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(dictionary.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${dictionary.id} / ${dictionary.entries.size}語",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    dictionary.entries.take(3).forEach {
                        Text("${it.term}: ${it.meanings.first()}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (repository.isUserDictionary(dictionary.id)) {
                        OutlinedButton(
                            onClick = {
                                repository.deleteUserDictionary(dictionary.id)
                                refresh++
                                message = "「${dictionary.label}」を削除しました"
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("この追加辞書を削除") }
                    }
                }
            }
        }
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
    }
}
