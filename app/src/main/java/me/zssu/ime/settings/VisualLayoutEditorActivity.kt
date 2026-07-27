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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.zssu.ime.keyboard.KeyAction
import me.zssu.ime.keyboard.KeyOutput
import me.zssu.ime.keyboard.KeySpec
import me.zssu.ime.keyboard.KeyStyle
import me.zssu.ime.keyboard.KeyboardLayout
import me.zssu.ime.keyboard.LayoutRepository
import me.zssu.ime.theme.ZinnaTheme

/** Human-readable label for each action type, used in the dropdown. */
private data class ActionChoice(val type: String, val label: String)

private val ACTION_CHOICES = listOf(
    ActionChoice("input", "文字入力 (Input)"),
    ActionChoice("backspace", "バックスペース"),
    ActionChoice("enter", "Enter（改行・確定）"),
    ActionChoice("space", "スペース"),
    ActionChoice("convert", "変換"),
    ActionChoice("cursor", "カーソル移動"),
    ActionChoice("undo", "元に戻す"),
    ActionChoice("modify", "゛小゜"),
    ActionChoice("layout", "配列切替"),
    ActionChoice("shift", "Shift"),
    ActionChoice("symbol", "記号直接入力"),
    ActionChoice("ime_picker", "IME切替"),
)

/**
 * Visual keyboard-layout editor.
 *
 * Select a key, then adjust its action type, style, weight, repeat behaviour, and flick outputs.
 * Keys can be added, removed, duplicated, and rearranged with arrow buttons. Every change is
 * applied to the live [KeyboardLayout] model; saving writes a user override.
 */
class VisualLayoutEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZinnaTheme { Scaffold { VisualEditor(Modifier.padding(it)) } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        Text("キーを選び、アクション・スタイル・幅を変更できます。追加・削除・複製・移動も可能です。")
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
                                Text(key.faceLabel, maxLines = 1)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { rowIndex = r; keyIndex = k },
                                modifier = Modifier.width((56 * key.weight).dp),
                            ) {
                                Text(key.faceLabel, maxLines = 1)
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
                        Text("選択中: ${selected.faceLabel}", style = MaterialTheme.typography.titleSmall)

                        // ── action type ──
                        Text("アクション種別", style = MaterialTheme.typography.bodyLarge)
                        var actionExpanded by remember { mutableStateOf(false) }
                        val currentType = actionType(selected.center)
                        ExposedDropdownMenuBox(
                            expanded = actionExpanded,
                            onExpandedChange = { actionExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = ACTION_CHOICES.firstOrNull { it.type == currentType }?.label
                                    ?: currentType,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodySmall,
                            )
                            ExposedDropdownMenu(
                                expanded = actionExpanded,
                                onDismissRequest = { actionExpanded = false },
                            ) {
                                ACTION_CHOICES.forEach { choice ->
                                    DropdownMenuItem(
                                        text = { Text(choice.label) },
                                        onClick = {
                                            actionExpanded = false
                                            layout = replaceAction(current, rowIndex, keyIndex, choice.type)
                                        },
                                    )
                                }
                            }
                        }

                        // ── action-specific fields ──
                        when (val action = selected.center.action) {
                            is KeyAction.Input -> {
                                OutlinedTextField(
                                    value = action.text,
                                    onValueChange = { text ->
                                        layout = updateSelected(current, rowIndex, keyIndex) {
                                            it.copy(center = it.center.copy(
                                                label = if (it.label != null || it.center.label != action.text) text else it.center.label,
                                                action = KeyAction.Input(text),
                                            ))
                                        }
                                    },
                                    label = { Text("入力テキスト（mozcテーブルキー）") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            }
                            is KeyAction.InsertSymbol -> {
                                OutlinedTextField(
                                    value = action.text,
                                    onValueChange = { text ->
                                        layout = updateSelected(current, rowIndex, keyIndex) {
                                            it.copy(center = KeyOutput(
                                                label = text.ifEmpty { "記号" },
                                                action = KeyAction.InsertSymbol(text),
                                            ))
                                        }
                                    },
                                    label = { Text("直接入力する記号") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            }
                            is KeyAction.MoveCursor -> {
                                OutlinedTextField(
                                    value = action.delta.toString(),
                                    onValueChange = { text ->
                                        val delta = text.toIntOrNull() ?: return@OutlinedTextField
                                        layout = updateSelected(current, rowIndex, keyIndex) {
                                            it.copy(center = it.center.copy(
                                                action = KeyAction.MoveCursor(delta),
                                            ))
                                        }
                                    },
                                    label = { Text("移動量（マイナスで左）") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            }
                            is KeyAction.SwitchLayout -> {
                                OutlinedTextField(
                                    value = action.layoutId,
                                    onValueChange = { id ->
                                        layout = updateSelected(current, rowIndex, keyIndex) {
                                            it.copy(center = it.center.copy(
                                                action = KeyAction.SwitchLayout(id),
                                            ))
                                        }
                                    },
                                    label = { Text("切替先の配列ID") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            }
                            else -> {}
                        }

                        // ── style ──
                        Text("キースタイル", style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            KeyStyle.entries.forEach { style ->
                                val label = when (style) {
                                    KeyStyle.CHARACTER -> "文字"
                                    KeyStyle.MODIFIER -> "修飾"
                                    KeyStyle.ACTION -> "操作"
                                }
                                val select = {
                                    layout = updateSelected(current, rowIndex, keyIndex) {
                                        it.copy(style = style)
                                    }
                                }
                                if (selected.style == style) {
                                    Button(onClick = select, modifier = Modifier.weight(1f)) { Text(label) }
                                } else {
                                    OutlinedButton(onClick = select, modifier = Modifier.weight(1f)) { Text(label) }
                                }
                            }
                        }

                        // ── repeatable ──
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Text("リピート", Modifier.weight(1f))
                            Switch(
                                checked = selected.repeatable,
                                onCheckedChange = { checked ->
                                    layout = updateSelected(current, rowIndex, keyIndex) {
                                        it.copy(repeatable = checked)
                                    }
                                },
                            )
                        }

                        // ── label ──
                        OutlinedTextField(
                            value = selected.label.orEmpty(),
                            onValueChange = { label ->
                                layout = updateSelected(current, rowIndex, keyIndex) {
                                    it.copy(label = label.ifBlank { null })
                                }
                            },
                            label = { Text("表面ラベル（空欄で自動）") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        // ── weight ──
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

                        HorizontalDivider()

                        // ── flick outputs ──
                        Text("フリック方向の出力", style = MaterialTheme.typography.bodySmall)
                        Text("各方向のテキストを設定（空欄でその方向のフリックを解除）", style = MaterialTheme.typography.bodySmall)
                        listOf<Triple<String, KeyOutput?, (KeySpec, KeyOutput?) -> KeySpec>>(
                            Triple("左", selected.left) { s, v -> s.copy(left = v) },
                            Triple("上", selected.up) { s, v -> s.copy(up = v) },
                            Triple("右", selected.right) { s, v -> s.copy(right = v) },
                            Triple("下", selected.down) { s, v -> s.copy(down = v) },
                        ).forEach { (dir, output, setter) ->
                            val currentFlickText = remember(output) {
                                (output?.action as? KeyAction.Input)?.text ?: ""
                            }
                            var flickText by remember(output) { mutableStateOf(currentFlickText) }
                            OutlinedTextField(
                                value = flickText,
                                onValueChange = { value ->
                                    flickText = value
                                    layout = updateSelected(current, rowIndex, keyIndex) { spec ->
                                        setter(spec, if (value.isBlank()) null
                                        else KeyOutput(value, KeyAction.Input(value)))
                                    }
                                },
                                label = { Text("${dir}フリックのテキスト") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall,
                            )
                        }

                        // ── long press ──
                        OutlinedTextField(
                            value = (selected.longPress?.action as? KeyAction.InsertSymbol)?.text.orEmpty(),
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
                            singleLine = true,
                        )

                        HorizontalDivider()

                        // ── move keys ──
                        Text("キーを移動")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("←", "↑", "↓", "→").forEach { direction ->
                                OutlinedButton(
                                    onClick = {
                                        val moved = moveKey(current, rowIndex, keyIndex, direction)
                                        layout = moved.layout
                                        rowIndex = moved.row
                                        keyIndex = moved.key
                                    },
                                    modifier = Modifier.weight(1f),
                                ) { Text(direction) }
                            }
                        }

                        // ── duplicate / insert / delete ──
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val result = duplicateKey(current, rowIndex, keyIndex)
                                    layout = result.layout
                                    rowIndex = result.row
                                    keyIndex = result.key
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("複製") }
                            OutlinedButton(
                                onClick = {
                                    val result = insertKey(current, rowIndex, keyIndex)
                                    layout = result.layout
                                    rowIndex = result.row
                                    keyIndex = result.key
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("追加") }
                            OutlinedButton(
                                onClick = {
                                    val result = deleteKey(current, rowIndex, keyIndex)
                                    if (result != null) {
                                        layout = result.layout
                                        rowIndex = result.row
                                        keyIndex = result.key
                                    } else {
                                        message = "最後のキーは削除できません"
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("削除") }
                        }
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

private fun actionType(output: KeyOutput): String = when (output.action) {
    is KeyAction.Input -> "input"
    is KeyAction.Backspace -> "backspace"
    is KeyAction.Enter -> "enter"
    is KeyAction.Space -> "space"
    is KeyAction.Convert -> "convert"
    is KeyAction.MoveCursor -> "cursor"
    is KeyAction.Undo -> "undo"
    is KeyAction.ModifyChar -> "modify"
    is KeyAction.SwitchLayout -> "layout"
    is KeyAction.Shift -> "shift"
    is KeyAction.InsertSymbol -> "symbol"
    is KeyAction.ShowImePicker -> "ime_picker"
}

private fun makeAction(type: String, prev: KeyAction): KeyAction = when (type) {
    "input" -> KeyAction.Input((prev as? KeyAction.Input)?.text ?: "")
    "backspace" -> KeyAction.Backspace
    "enter" -> KeyAction.Enter
    "space" -> KeyAction.Space
    "convert" -> KeyAction.Convert
    "cursor" -> KeyAction.MoveCursor((prev as? KeyAction.MoveCursor)?.delta ?: -1)
    "undo" -> KeyAction.Undo
    "modify" -> KeyAction.ModifyChar
    "layout" -> KeyAction.SwitchLayout((prev as? KeyAction.SwitchLayout)?.layoutId ?: "")
    "shift" -> KeyAction.Shift
    "symbol" -> KeyAction.InsertSymbol((prev as? KeyAction.InsertSymbol)?.text ?: "")
    "ime_picker" -> KeyAction.ShowImePicker
    else -> prev
}

private fun defaultLabelForAction(action: KeyAction): String = when (action) {
    is KeyAction.Input -> action.text.ifEmpty { "文字" }
    is KeyAction.Backspace -> "⌫"
    is KeyAction.Enter -> "↵"
    is KeyAction.Space -> "空白"
    is KeyAction.Convert -> "変換"
    is KeyAction.MoveCursor -> if (action.delta < 0) "◀" else "▶"
    is KeyAction.Undo -> "↶"
    is KeyAction.ModifyChar -> "゛小゜"
    is KeyAction.Shift -> "⬆"
    is KeyAction.ShowImePicker -> "IME"
    is KeyAction.InsertSymbol -> action.text.ifEmpty { "記号" }
    is KeyAction.SwitchLayout -> "切替"
}

private fun replaceAction(
    layout: KeyboardLayout,
    row: Int,
    key: Int,
    type: String,
): KeyboardLayout {
    if (row !in layout.rows.indices || key !in layout.rows[row].keys.indices) return layout
    val keys = layout.rows[row].keys.toMutableList()
    val spec = keys[key]
    val newAction = makeAction(type, spec.center.action)
    val newLabel = if (spec.label != null) spec.label else defaultLabelForAction(newAction)
    val newCenter = KeyOutput(newLabel, newAction)

    // Clear flick directions when switching to non-input action
    val clearsFlicks = type != "input"
    keys[key] = spec.copy(
        center = newCenter,
        left = if (clearsFlicks) null else spec.left,
        up = if (clearsFlicks) null else spec.up,
        right = if (clearsFlicks) null else spec.right,
        down = if (clearsFlicks) null else spec.down,
    )
    val rows = layout.rows.toMutableList()
    rows[row] = rows[row].copy(keys = keys)
    return layout.copy(rows = rows)
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

private fun duplicateKey(layout: KeyboardLayout, row: Int, key: Int): MoveResult {
    if (row !in layout.rows.indices || key !in layout.rows[row].keys.indices) {
        return MoveResult(layout, row, key)
    }
    val rows = layout.rows.map { it.copy(keys = it.keys.toMutableList()) }.toMutableList()
    val source = rows[row].keys.toMutableList()
    source.add(key + 1, source[key])
    rows[row] = rows[row].copy(keys = source)
    return MoveResult(layout.copy(rows = rows), row, key + 1)
}

private fun insertKey(layout: KeyboardLayout, row: Int, key: Int): MoveResult {
    if (row !in layout.rows.indices) return MoveResult(layout, row, key)
    val idx = key.coerceIn(0, layout.rows[row].keys.size - 1)
    val rows = layout.rows.map { it.copy(keys = it.keys.toMutableList()) }.toMutableList()
    val source = rows[row].keys.toMutableList()
    source.add(idx + 1, me.zssu.ime.keyboard.KeySpec(
        weight = 1f,
        center = KeyOutput("新規", KeyAction.Input("")),
        style = KeyStyle.CHARACTER,
    ))
    rows[row] = rows[row].copy(keys = source)
    return MoveResult(layout.copy(rows = rows), row, idx + 1)
}

private fun deleteKey(layout: KeyboardLayout, row: Int, key: Int): MoveResult? {
    if (row !in layout.rows.indices || key !in layout.rows[row].keys.indices) return null
    val rows = layout.rows.map { it.copy(keys = it.keys.toMutableList()) }.toMutableList()
    val source = rows[row].keys.toMutableList()
    if (source.size <= 1) return null
    source.removeAt(key)
    rows[row] = rows[row].copy(keys = source)
    val newKey = (key - 1).coerceAtLeast(0)
    return MoveResult(layout.copy(rows = rows), row, newKey)
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
