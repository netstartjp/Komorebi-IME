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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import me.zssu.ime.keyboard.KeyAction
import me.zssu.ime.keyboard.KeyOutput
import me.zssu.ime.keyboard.KeyboardLayout
import me.zssu.ime.keyboard.LayoutRepository
import me.zssu.ime.theme.ZinnaTheme

/**
 * A safe visual layer over the same KeyboardLayout model used by the JSON editor.
 *
 * It focuses on common spatial edits and long-press symbols. The JSON editor remains available for
 * specialised actions, but ordinary rearrangement no longer requires editing serialized data.
 */
class VisualLayoutEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZinnaTheme { Scaffold { VisualEditor(Modifier.padding(it)) } } }
    }
}

@Composable
private fun VisualEditor(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { LayoutRepository(context) }
    val settings = remember { ImeSettings(context) }
    var refresh by remember { mutableIntStateOf(0) }
    var layout by remember { mutableStateOf<KeyboardLayout?>(null) }
    var rowIndex by remember { mutableIntStateOf(-1) }
    var keyIndex by remember { mutableIntStateOf(-1) }
    var message by remember { mutableStateOf("配列を選択してください") }
    val ids = remember(refresh) { repository.availableLayoutIds() }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("視覚的な配列エディタ", style = MaterialTheme.typography.headlineSmall)
        Text("キーを選び、矢印で移動し、幅と長押し記号を変更できます。保存するとユーザー版になります。")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ids.forEach { id ->
                OutlinedButton(onClick = {
                    layout = repository.loadLayout(id)
                    rowIndex = -1
                    keyIndex = -1
                    message = "$id を読み込みました"
                }) { Text(id) }
            }
        }

        layout?.let { current ->
            Text(current.label, style = MaterialTheme.typography.titleMedium)
            current.rows.forEachIndexed { r, keyRow ->
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    keyRow.keys.forEachIndexed { k, key ->
                        val selected = r == rowIndex && k == keyIndex
                        if (selected) {
                            Button(
                                onClick = { rowIndex = r; keyIndex = k },
                                modifier = Modifier.width((56 * key.weight).dp),
                            ) {
                                Text(key.faceLabel)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { rowIndex = r; keyIndex = k },
                                modifier = Modifier.width((56 * key.weight).dp),
                            ) {
                                Text(key.faceLabel)
                            }
                        }
                    }
                }
            }

            val selected = current.rows.getOrNull(rowIndex)?.keys?.getOrNull(keyIndex)
            if (selected != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("選択中: ${selected.faceLabel}")
                        Text("キーを移動")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("←", "↑", "↓", "→").forEach { direction ->
                                OutlinedButton(
                                    onClick = {
                                        val moved = moveKey(
                                            current,
                                            rowIndex,
                                            keyIndex,
                                            direction,
                                        )
                                        layout = moved.layout
                                        rowIndex = moved.row
                                        keyIndex = moved.key
                                    },
                                    modifier = Modifier.weight(1f),
                                ) { Text(direction) }
                            }
                        }
                        Text("キー幅 ${(selected.weight * 100).toInt()}%")
                        Slider(
                            value = selected.weight.coerceIn(0.5f, 4f),
                            onValueChange = { weight ->
                                layout = updateSelected(current, rowIndex, keyIndex) {
                                    it.copy(weight = weight)
                                }
                            },
                            valueRange = 0.5f..4f,
                        )
                        OutlinedTextField(
                            value = selected.label.orEmpty(),
                            onValueChange = { label ->
                                layout = updateSelected(current, rowIndex, keyIndex) {
                                    it.copy(label = label.ifBlank { null })
                                }
                            },
                            label = { Text("キー表面ラベル（空欄で自動）") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = (selected.longPress?.action as? KeyAction.InsertSymbol)
                                ?.text.orEmpty(),
                            onValueChange = { symbol ->
                                layout = updateSelected(current, rowIndex, keyIndex) {
                                    it.copy(
                                        longPress = symbol.takeIf(String::isNotEmpty)?.let { value ->
                                            KeyOutput(value, KeyAction.InsertSymbol(value))
                                        },
                                    )
                                }
                            },
                            label = { Text("長押しで直接入力する記号") },
                            supportingText = { Text("空欄にすると長押し割り当てを解除") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Button(
                onClick = {
                    repository.saveLayout(current)
                    settings.activeLayoutId = current.id
                    message = "${current.id} を保存して使用します"
                    refresh++
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("ユーザー版として保存して使用") }
        }
        Text(message, style = MaterialTheme.typography.bodySmall)
    }
}

private data class MoveResult(val layout: KeyboardLayout, val row: Int, val key: Int)

private fun moveKey(
    layout: KeyboardLayout,
    row: Int,
    key: Int,
    direction: String,
): MoveResult {
    if (row !in layout.rows.indices || key !in layout.rows[row].keys.indices) {
        return MoveResult(layout, row, key)
    }
    val rows = layout.rows.map { it.copy(keys = it.keys.toMutableList()) }.toMutableList()
    val source = rows[row].keys.toMutableList()
    val value = source[key]
    return when (direction) {
        "←" -> {
            if (key == 0) MoveResult(layout, row, key)
            else {
                source[key] = source[key - 1]
                source[key - 1] = value
                rows[row] = rows[row].copy(keys = source)
                MoveResult(layout.copy(rows = rows), row, key - 1)
            }
        }
        "→" -> {
            if (key == source.lastIndex) MoveResult(layout, row, key)
            else {
                source[key] = source[key + 1]
                source[key + 1] = value
                rows[row] = rows[row].copy(keys = source)
                MoveResult(layout.copy(rows = rows), row, key + 1)
            }
        }
        "↑", "↓" -> {
            val targetRow = row + if (direction == "↑") -1 else 1
            if (targetRow !in rows.indices || source.size == 1) MoveResult(layout, row, key)
            else {
                source.removeAt(key)
                rows[row] = rows[row].copy(keys = source)
                val target = rows[targetRow].keys.toMutableList()
                val targetIndex = key.coerceAtMost(target.size)
                target.add(targetIndex, value)
                rows[targetRow] = rows[targetRow].copy(keys = target)
                MoveResult(layout.copy(rows = rows), targetRow, targetIndex)
            }
        }
        else -> MoveResult(layout, row, key)
    }
}

private fun updateSelected(
    layout: KeyboardLayout,
    row: Int,
    key: Int,
    transform: (me.zssu.ime.keyboard.KeySpec) -> me.zssu.ime.keyboard.KeySpec,
): KeyboardLayout {
    if (row !in layout.rows.indices || key !in layout.rows[row].keys.indices) return layout
    val rows = layout.rows.toMutableList()
    val keys = rows[row].keys.toMutableList()
    keys[key] = transform(keys[key])
    rows[row] = rows[row].copy(keys = keys)
    return layout.copy(rows = rows)
}
