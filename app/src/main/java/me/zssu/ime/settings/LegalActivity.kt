package me.zssu.ime.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.zssu.ime.theme.ZinnaTheme

/**
 * Human-readable access to every legal document shipped in the APK.
 *
 * Keeping the documents as assets means the exact files shown here are also available to automated
 * release audits without scraping Compose resources.
 */
class LegalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZinnaTheme {
                LegalScreen(onBack = ::finish)
            }
        }
    }
}

private data class LegalDocument(val title: String, val assetName: String)

private val DOCUMENTS = listOf(
    LegalDocument("本アプリ / Apache 2.0", "LICENSE.txt"),
    LegalDocument("帰属表示", "NOTICE.txt"),
    LegalDocument("第三者ソフトウェア", "THIRD_PARTY_NOTICES.txt"),
    LegalDocument("Mozc", "MOZC_LICENSE.txt"),
    LegalDocument("Mozc辞書", "MOZC_DICTIONARY_LICENSES.txt"),
    LegalDocument("プライバシー", "PRIVACY.txt"),
)

@Composable
private fun LegalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(DOCUMENTS.first()) }
    val body = remember(selected) {
        runCatching {
            context.assets.open("legal/${selected.assetName}")
                .bufferedReader()
                .use { it.readText() }
        }.getOrElse { "文書を読み込めませんでした: ${selected.assetName}" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onBack) { Text("戻る") }
            Text(
                "オープンソースライセンス",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DOCUMENTS.forEach { document ->
                if (document == selected) {
                    Button(onClick = { selected = document }) { Text(document.title) }
                } else {
                    OutlinedButton(onClick = { selected = document }) { Text(document.title) }
                }
            }
        }
        SelectionContainer {
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}
