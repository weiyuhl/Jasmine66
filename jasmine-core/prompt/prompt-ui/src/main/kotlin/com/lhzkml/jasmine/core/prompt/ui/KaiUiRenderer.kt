@file:OptIn(ExperimentalMaterial3Api::class)

package com.lhzkml.jasmine.core.prompt.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun KaiUiRenderer(
    node: KaiUiNode,
    isInteractive: Boolean,
    onCallback: (event: String, data: Map<String, String>) -> Unit,
    modifier: Modifier = Modifier,
    wrapInCard: Boolean = true,
) {
    val formState = remember { mutableStateMapOf<String, String>() }
    val toggleState = remember { mutableStateMapOf<String, Boolean>() }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(node) {
        try { initializeFormState(node, formState) } catch (_: Exception) { hasError = true }
    }

    if (hasError) {
        Text("Failed to render UI", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = modifier)
        return
    }

    val content: @Composable () -> Unit = {
        RenderNode(node, isInteractive, formState, toggleState, safeCallback(onCallback))
    }

    if (wrapInCard) {
        Card(modifier = modifier.fillMaxWidth().wrapContentHeight()) {
            Column(Modifier.padding(12.dp).wrapContentHeight()) { content() }
        }
    } else {
        Column(modifier = modifier.fillMaxWidth().wrapContentHeight()) { content() }
    }
}

private fun safeCallback(cb: (String, Map<String, String>) -> Unit): (String, Map<String, String>) -> Unit = { e, d -> try { cb(e, d) } catch (_: Exception) {} }

private fun collectFormData(action: CallbackAction, formState: Map<String, String>): Map<String, String> =
    if (action.collectFrom.isEmpty()) formState else formState.filterKeys { it in action.collectFrom }

private fun initializeFormState(node: KaiUiNode, formState: MutableMap<String, String>) {
    when (node) {
        is TextInputNode -> if (node.id.isNotEmpty() && node.id !in formState) formState[node.id] = node.value ?: ""
        is CheckboxNode -> if (node.id !in formState) formState[node.id] = (node.checked ?: false).toString()
        is SelectNode -> if (node.id !in formState) formState[node.id] = node.selected ?: ""
        is SwitchNode -> if (node.id !in formState) formState[node.id] = (node.checked ?: false).toString()
        is SliderNode -> if (node.id !in formState) formState[node.id] = (node.value ?: (node.min ?: 0f)).toString()
        is RadioGroupNode -> if (node.id !in formState) formState[node.id] = node.selected ?: ""
        is ColumnNode, is RowNode, is CardNode -> {
            val childrenList = when (node) {
                is ColumnNode -> node.children
                is RowNode -> node.children
                is CardNode -> node.children
                else -> emptyList()
            }
            childrenList.forEach { initializeFormState(it, formState) }
        }
        is ListNode -> node.items.forEach { initializeFormState(it, formState) }
        else -> {}
    }
}

@Composable
private fun RenderNode(
    node: KaiUiNode,
    isInteractive: Boolean,
    formState: MutableMap<String, String>,
    toggleState: MutableMap<String, Boolean>,
    onCallback: (String, Map<String, String>) -> Unit,
    depth: Int = 0,
) {
    if (depth > 10) return
    val nodeId = node.id
    if (nodeId != null && toggleState[nodeId] == false) return

    when (node) {
        is ColumnNode -> RenderColumn(node, isInteractive, formState, toggleState, onCallback, depth)
        is RowNode -> RenderRow(node, isInteractive, formState, toggleState, onCallback, depth)
        is CardNode -> RenderCard(node, isInteractive, formState, toggleState, onCallback, depth)
        is TextNode -> RenderText(node)
        is ButtonNode -> RenderButton(node, isInteractive, formState, toggleState, onCallback)
        is TextInputNode -> RenderTextInput(node, isInteractive, formState)
        is CheckboxNode -> RenderCheckbox(node, isInteractive, formState)
        is SelectNode -> RenderSelect(node, isInteractive, formState)
        is ImageNode -> RenderImage(node)
        is TableNode -> RenderTable(node)
        is ListNode -> RenderList(node, isInteractive, formState, toggleState, onCallback, depth)
        is SpacerNode -> androidx.compose.foundation.layout.Spacer(Modifier.height((node.height ?: 8).dp))
        is DividerNode -> HorizontalDivider(Modifier.padding(vertical = 4.dp))
        is SwitchNode -> RenderSwitch(node, isInteractive, formState)
        is SliderNode -> RenderSlider(node, isInteractive, formState)
        is RadioGroupNode -> RenderRadioGroup(node, isInteractive, formState)
        is ProgressNode -> RenderProgress(node)
        is CountdownNode -> RenderCountdown(node, isInteractive, formState, toggleState, onCallback)
        is AlertNode -> RenderAlert(node)
        is ChipGroupNode -> RenderChipGroup(node, isInteractive, formState)
        is ChipNode -> RenderChip(node)
        is CodeNode -> RenderCode(node)
        is QuoteNode -> RenderQuote(node)
        is BadgeNode -> RenderBadge(node)
        is StatNode -> RenderStat(node)
        is AvatarNode -> RenderAvatar(node)
        is BoxNode -> RenderBox(node, isInteractive, formState, toggleState, onCallback, depth)
        is TabsNode -> RenderTabs(node, isInteractive, formState, toggleState, onCallback, depth)
        is AccordionNode -> RenderAccordion(node, isInteractive, formState, toggleState, onCallback, depth)
        is IconNode -> Text("\uD83D\uDCCC", fontSize = (node.size ?: 24).sp)
    }
}

@Composable
private fun RenderChildren(children: List<KaiUiNode>, isInteractive: Boolean, formState: MutableMap<String, String>, toggleState: MutableMap<String, Boolean>, onCallback: (String, Map<String, String>) -> Unit, depth: Int) {
    for (child in children) RenderNode(child, isInteractive, formState, toggleState, onCallback, depth + 1)
}

@Composable
private fun RenderColumn(node: ColumnNode, isInteractive: Boolean, formState: MutableMap<String, String>, toggleState: MutableMap<String, Boolean>, onCallback: (String, Map<String, String>) -> Unit, depth: Int) {
    Column(verticalArrangement = Arrangement.spacedBy((node.spacing ?: 8).dp), modifier = Modifier.fillMaxWidth().wrapContentHeight().then(if (node.padding != null) Modifier.padding(node.padding.dp) else Modifier)) {
        RenderChildren(node.children, isInteractive, formState, toggleState, onCallback, depth)
    }
}

@Composable
private fun RenderRow(node: RowNode, isInteractive: Boolean, formState: MutableMap<String, String>, toggleState: MutableMap<String, Boolean>, onCallback: (String, Map<String, String>) -> Unit, depth: Int) {
    val spacingDp = (node.spacing ?: 8).dp
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacingDp), verticalArrangement = Arrangement.spacedBy(spacingDp), modifier = Modifier.fillMaxWidth().wrapContentHeight().then(if (node.padding != null) Modifier.padding(node.padding.dp) else Modifier)) {
        for (child in node.children) RenderNode(child, isInteractive, formState, toggleState, onCallback, depth + 1)
    }
}

@Composable
private fun RenderCard(node: CardNode, isInteractive: Boolean, formState: MutableMap<String, String>, toggleState: MutableMap<String, Boolean>, onCallback: (String, Map<String, String>) -> Unit, depth: Int) {
    Card(Modifier.fillMaxWidth().wrapContentHeight()) {
        Column(modifier = Modifier.padding((node.padding ?: 16).dp).wrapContentHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RenderChildren(node.children, isInteractive, formState, toggleState, onCallback, depth)
        }
    }
}

@Composable
private fun RenderText(node: TextNode) {
    val style = when (node.style) {
        TextNodeStyle.HEADLINE -> MaterialTheme.typography.headlineSmall
        TextNodeStyle.TITLE -> MaterialTheme.typography.titleMedium
        TextNodeStyle.BODY -> MaterialTheme.typography.bodyLarge
        TextNodeStyle.CAPTION -> MaterialTheme.typography.bodySmall
        null -> MaterialTheme.typography.bodyLarge
    }
    val color = when (node.color) {
        "primary" -> MaterialTheme.colorScheme.primary
        "secondary" -> MaterialTheme.colorScheme.secondary
        "error" -> MaterialTheme.colorScheme.error
        "success" -> Color(0xFF4CAF50)
        "warning" -> Color(0xFFFFC107)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Text(text = node.value.replace("**", ""), style = style, color = color, fontWeight = if (node.bold == true || node.value.startsWith("**")) FontWeight.Bold else null)
}

@Composable
private fun RenderButton(node: ButtonNode, isInteractive: Boolean, formState: MutableMap<String, String>, toggleState: MutableMap<String, Boolean>, onCallback: (String, Map<String, String>) -> Unit) {
    val uriHandler = LocalUriHandler.current
    val enabled = isInteractive && (node.enabled != false)
    val onClick: () -> Unit = {
        try {
            when (val action = node.action) {
                is CallbackAction -> onCallback(action.event, collectFormData(action, formState))
                is ToggleAction -> toggleState[action.targetId] = !(toggleState[action.targetId] ?: true)
                is OpenUrlAction -> uriHandler.openUri(action.url)
                null -> {}
            }
        } catch (_: Exception) {}
    }
    when (node.variant) {
        ButtonVariant.OUTLINED -> OutlinedButton(onClick = onClick, enabled = enabled) { Text(node.label) }
        ButtonVariant.TEXT -> TextButton(onClick = onClick, enabled = enabled) { Text(node.label) }
        ButtonVariant.TONAL -> FilledTonalButton(onClick = onClick, enabled = enabled) { Text(node.label) }
        ButtonVariant.FILLED, null -> Button(onClick = onClick, enabled = enabled) { Text(node.label) }
    }
}

@Composable
private fun RenderTextInput(node: TextInputNode, isInteractive: Boolean, formState: MutableMap<String, String>) {
    OutlinedTextField(value = formState[node.id] ?: "", onValueChange = { formState[node.id] = it }, label = node.label?.let { { Text(it) } }, placeholder = node.placeholder?.let { { Text(it) } }, enabled = isInteractive, singleLine = node.multiline != true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun RenderCheckbox(node: CheckboxNode, isInteractive: Boolean, formState: MutableMap<String, String>) {
    val checked = formState[node.id]?.toBooleanStrictOrNull() ?: false
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = { formState[node.id] = (!checked).toString() }, enabled = isInteractive)) {
        androidx.compose.material3.Checkbox(checked = checked, onCheckedChange = null, enabled = isInteractive)
        Text(node.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenderSelect(node: SelectNode, isInteractive: Boolean, formState: MutableMap<String, String>) {
    var expanded by remember { mutableStateOf(false) }
    val selected = formState[node.id] ?: ""
    androidx.compose.material3.ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (isInteractive) expanded = it }) {
        OutlinedTextField(value = selected, onValueChange = {}, readOnly = true, label = node.label?.let { { Text(it) } }, trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, enabled = isInteractive, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().menuAnchor())
        if (expanded) {
            androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                for (option in node.options) androidx.compose.material3.DropdownMenuItem(text = { Text(option) }, onClick = { formState[node.id] = option; expanded = false })
            }
        }
    }
}

@Composable
private fun RenderImage(node: ImageNode) {
    val modifier = Modifier.fillMaxWidth().then(if (node.height != null) Modifier.height(node.height.dp) else Modifier)
    AsyncImage(model = node.url, contentDescription = node.alt, modifier = modifier.clip(RoundedCornerShape(6.dp)))
}

@Composable
private fun RenderTable(node: TableNode) {
    val columnCount = maxOf(node.headers.size, node.rows.maxOfOrNull { it.size } ?: 0)
    if (columnCount == 0) return
    Column(Modifier.fillMaxWidth().wrapContentHeight()) {
        if (node.headers.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { for (i in 0 until columnCount) Text(text = node.headers.getOrElse(i) { "" }, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)) }
            HorizontalDivider()
        }
        for (row in node.rows) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { for (i in 0 until columnCount) Text(text = row.getOrElse(i) { "" }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun RenderList(node: ListNode, isInteractive: Boolean, formState: MutableMap<String, String>, toggleState: MutableMap<String, Boolean>, onCallback: (String, Map<String, String>) -> Unit, depth: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for ((index, item) in node.items.withIndex()) {
            Row {
                val prefix = if (node.ordered == true) "${index + 1}. " else "\u2022 "
                Text(prefix, style = MaterialTheme.typography.bodyLarge)
                Column(Modifier.weight(1f)) { RenderNode(item, isInteractive, formState, toggleState, onCallback, depth + 1) }
            }
        }
    }
}

@Composable
private fun RenderSwitch(node: SwitchNode, isInteractive: Boolean, formState: MutableMap<String, String>) {
    val checked = formState[node.id]?.toBooleanStrictOrNull() ?: false
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(onClick = { formState[node.id] = (!checked).toString() }, enabled = isInteractive)) {
        Text(node.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = null, enabled = isInteractive)
    }
}

@Composable
private fun RenderSlider(node: SliderNode, isInteractive: Boolean, formState: MutableMap<String, String>) {
    val min = node.min ?: 0f; val max = node.max ?: 100f; val step = node.step
    val currentValue = formState[node.id]?.toFloatOrNull() ?: (node.value ?: min)
    Column(Modifier.fillMaxWidth()) {
        if (node.label != null) Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(node.label, style = MaterialTheme.typography.bodyLarge); Text(text = "%.1f".format(currentValue), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
        val steps = if (step != null && step > 0) ((max - min) / step).toInt() - 1 else 0
        androidx.compose.material3.Slider(value = currentValue.coerceIn(min, max), onValueChange = { formState[node.id] = it.toString() }, valueRange = min..max, steps = steps.coerceAtLeast(0), enabled = isInteractive, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RenderRadioGroup(node: RadioGroupNode, isInteractive: Boolean, formState: MutableMap<String, String>) {
    val selected = formState[node.id] ?: node.selected ?: ""
    Column {
        if (node.label != null) Text(node.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
        for (option in node.options) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = { formState[node.id] = option }, enabled = isInteractive).padding(vertical = 4.dp)) { androidx.compose.material3.RadioButton(selected = option == selected, onClick = null, enabled = isInteractive); Text(option, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp)) }
    }
}

@Composable
private fun RenderProgress(node: ProgressNode) {
    Column(Modifier.fillMaxWidth()) {
        if (node.label != null) Text(node.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
        if (node.value != null) LinearProgressIndicator(progress = node.value.coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth()) else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RenderCountdown(node: CountdownNode, isInteractive: Boolean, formState: MutableMap<String, String>, toggleState: MutableMap<String, Boolean>, onCallback: (String, Map<String, String>) -> Unit) {
    var remaining by remember { mutableIntStateOf(node.seconds) }
    LaunchedEffect(node.seconds) { while (remaining > 0) { delay(1000); remaining-- }; node.action?.let { if (it is CallbackAction) onCallback(it.event, collectFormData(it, formState)) } }
    Text("${node.label ?: "Time remaining"}: ${remaining}s", style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun RenderAlert(node: AlertNode) {
    val bgColor = when (node.severity) { AlertSeverity.SUCCESS -> Color(0xFFE8F5E9); AlertSeverity.WARNING -> Color(0xFFFFF3E0); AlertSeverity.ERROR -> Color(0xFFFFEBEE); else -> Color(0xFFE3F2FD) }
    Surface(color = bgColor, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) { if (node.title != null) Text(node.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold); Text(node.message, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun RenderChipGroup(node: ChipGroupNode, isInteractive: Boolean, formState: MutableMap<String, String>) {
    val selected = formState[node.id] ?: ""
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (chip in node.chips) {
            val chipSelected = if (node.multiSelect == true) formState["${node.id}_${chip.value}"]?.toBooleanStrictOrNull() ?: false else chip.value == selected
            androidx.compose.material3.FilterChip(selected = chipSelected, onClick = { if (node.multiSelect == true) formState["${node.id}_${chip.value}"] = (!chipSelected).toString() else formState[node.id] = chip.value }, label = { Text(chip.label) }, enabled = isInteractive)
        }
    }
}

@Composable
private fun RenderChip(node: ChipNode) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.width(200.dp)) { Text(node.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) }
}

@Composable
private fun RenderCode(node: CodeNode) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) { Text(text = node.code, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.padding(12.dp)) }
}

@Composable
private fun RenderQuote(node: QuoteNode) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(text = node.text, style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic); if (node.source != null) Text(text = "\u2014 ${node.source}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) } }
}

@Composable
private fun RenderBadge(node: BadgeNode) {
    val color = when (node.color) { "primary" -> MaterialTheme.colorScheme.primary; "error" -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.primaryContainer }
    Surface(color = color, shape = RoundedCornerShape(12.dp)) { Text(node.value, style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) }
}

@Composable
private fun RenderStat(node: StatNode) {
    Column { Text(node.value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(node.label, style = MaterialTheme.typography.bodyMedium); if (node.description != null) Text(node.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun RenderAvatar(node: AvatarNode) {
    val sizeDp = (node.size ?: 40).dp
    if (node.imageUrl != null) AsyncImage(model = node.imageUrl, contentDescription = node.name, modifier = Modifier.height(sizeDp).width(sizeDp).clip(RoundedCornerShape(50)))
    else Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50), modifier = Modifier.height(sizeDp).width(sizeDp)) { Box(contentAlignment = Alignment.Center) { Text(text = node.name?.take(1)?.uppercase() ?: "?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer) } }
}

@Composable
private fun RenderBox(node: BoxNode, isInteractive: Boolean, formState: MutableMap<String, String>, toggleState: MutableMap<String, Boolean>, onCallback: (String, Map<String, String>) -> Unit, depth: Int) {
    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) { RenderChildren(node.children, isInteractive, formState, toggleState, onCallback, depth) }
}

@Composable
private fun RenderTabs(node: TabsNode, isInteractive: Boolean, formState: MutableMap<String, String>, toggleState: MutableMap<String, Boolean>, onCallback: (String, Map<String, String>) -> Unit, depth: Int) {
    var selectedIndex by remember { mutableIntStateOf(node.selectedIndex ?: 0) }
    Column { Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) { node.tabs.forEachIndexed { index, tab -> TextButton(onClick = { selectedIndex = index }, enabled = isInteractive) { Text(tab.label, fontWeight = if (index == selectedIndex) FontWeight.Bold else null) } } }; HorizontalDivider(); node.tabs.getOrNull(selectedIndex)?.children?.forEach { child -> RenderNode(child, isInteractive, formState, toggleState, onCallback, depth + 1) } }
}

@Composable
private fun RenderAccordion(node: AccordionNode, isInteractive: Boolean, formState: MutableMap<String, String>, toggleState: MutableMap<String, Boolean>, onCallback: (String, Map<String, String>) -> Unit, depth: Int) {
    val expanded = toggleState[node.id ?: ""] ?: (node.expanded ?: false)
    Column { Row(modifier = Modifier.fillMaxWidth().clickable(onClick = { toggleState[node.id ?: ""] = !expanded }).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(node.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold); Text(if (expanded) "\u25BC" else "\u25B6") }; androidx.compose.animation.AnimatedVisibility(visible = expanded, enter = androidx.compose.animation.expandVertically(), exit = androidx.compose.animation.shrinkVertically()) { Column(Modifier.padding(start = 16.dp)) { RenderChildren(node.children, isInteractive, formState, toggleState, onCallback, depth) } } }
}
