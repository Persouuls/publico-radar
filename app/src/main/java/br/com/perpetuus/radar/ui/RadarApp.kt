package br.com.perpetuus.radar.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.perpetuus.radar.R
import br.com.perpetuus.radar.data.EventEntity
import br.com.perpetuus.radar.data.PackagePreview
import br.com.perpetuus.radar.data.RadarRepository
import br.com.perpetuus.radar.data.RecordEntity
import br.com.perpetuus.radar.data.PersonEntity
import br.com.perpetuus.radar.data.InstitutionEntity
import br.com.perpetuus.radar.data.SourceEntity
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private sealed interface IconAsset {
    data class Vector(val imageVector: ImageVector) : IconAsset
    data class Resource(@DrawableRes val id: Int) : IconAsset
}

@Composable
private fun AppIcon(
    icon: IconAsset,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    when (icon) {
        is IconAsset.Vector -> Icon(icon.imageVector, contentDescription, modifier, tint)
        is IconAsset.Resource -> Icon(painterResource(icon.id), contentDescription, modifier, tint)
    }
}

private enum class Destination(val label: String, val icon: IconAsset) {
    HOME("Início", IconAsset.Vector(Icons.Filled.Home)),
    RADAR("Radar", IconAsset.Resource(R.drawable.ic_radar)),
    SAVED("Salvos", IconAsset.Resource(R.drawable.ic_bookmark_border)),
    AGENDA("Agenda", IconAsset.Resource(R.drawable.ic_calendar_month)),
    DATA("Dados", IconAsset.Resource(R.drawable.ic_storage))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarApp(repository: RadarRepository) {
    val vm: RadarViewModel = viewModel(factory = remember(repository) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RadarViewModel(repository) as T
        }
    })
    val records by vm.records.collectAsStateWithLifecycle()
    val allRecords by vm.allRecords.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val people by vm.people.collectAsStateWithLifecycle()
    val institutions by vm.institutions.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()
    val meta by vm.meta.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()

    var destination by rememberSaveable { mutableStateOf(Destination.HOME.name) }
    var selectedRecordId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf("") }
    val context = LocalContext.current
    val snackbars = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val openImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                            ?: error("Não foi possível abrir o arquivo selecionado.")
                    }
                }.onSuccess { vm.inspectPackage(it) }
                    .onFailure { snackbars.showSnackbar(it.message ?: "Falha ao ler arquivo.") }
            }
        }
    }
    val saveExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(pendingExport) }
                            ?: error("Não foi possível gravar o arquivo selecionado.")
                    }
                }.onSuccess { snackbars.showSnackbar("Arquivo salvo.") }
                    .onFailure { snackbars.showSnackbar(it.message ?: "Falha ao salvar arquivo.") }
            }
        }
    }

    LaunchedEffect(notice) {
        notice?.let {
            snackbars.showSnackbar(it)
            vm.clearNotice()
        }
    }

    val selectedRecord = selectedRecordId?.let { id -> allRecords.firstOrNull { it.id == id } }
    val currentDestination = Destination.entries.firstOrNull { it.name == destination } ?: Destination.HOME

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(if (selectedRecord != null) "Registro" else currentDestination.label, fontWeight = FontWeight.SemiBold)
                        if (selectedRecord == null) {
                            Text("RADAR NACIONAL  /  ACERVO LOCAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    if (selectedRecord != null) IconButton(onClick = { selectedRecordId = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (selectedRecord == null && currentDestination != Destination.DATA) {
                        IconButton(onClick = { destination = Destination.DATA.name }) {
                            AppIcon(IconAsset.Resource(R.drawable.ic_storage), contentDescription = "Dados e atualizações")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (selectedRecord == null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    Destination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination == item,
                            onClick = { destination = item.name },
                            icon = { AppIcon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, maxLines = 1) }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbars) }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                selectedRecord != null -> RecordDetailScreen(
                    record = selectedRecord,
                    onSave = { vm.setSaved(selectedRecord.id, !selectedRecord.isSaved) }
                )
                currentDestination == Destination.HOME -> HomeScreen(
                    records = records,
                    events = events,
                    savedCount = saved.size,
                    version = meta?.dataVersion ?: 0,
                    onOpenRadar = { destination = Destination.RADAR.name },
                    onOpenData = { destination = Destination.DATA.name },
                    onOpenRecord = { selectedRecordId = it.id },
                    onSaveRecord = { record -> vm.setSaved(record.id, !record.isSaved) }
                )
                currentDestination == Destination.RADAR -> RadarScreen(
                    records = allRecords,
                    onOpen = { selectedRecordId = it.id },
                    onSave = { record -> vm.setSaved(record.id, !record.isSaved) }
                )
                currentDestination == Destination.SAVED -> SavedScreen(
                    records = saved,
                    onOpen = { selectedRecordId = it.id },
                    onSave = { vm.setSaved(it.id, false) },
                    onExplore = { destination = Destination.RADAR.name }
                )
                currentDestination == Destination.AGENDA -> AgendaScreen(events = events)
                currentDestination == Destination.DATA -> DataScreen(
                    recordCount = records.size,
                    archivedCount = allRecords.count { it.isArchived },
                    people = people,
                    institutions = institutions,
                    eventCount = events.size,
                    sources = sources,
                    version = meta?.dataVersion ?: 0,
                    lastImported = meta?.lastImportedAt,
                    logs = logs,
                    busy = busy,
                    onImport = { openImport.launch(arrayOf("application/json", "text/json", "*/*")) },
                    onExport = { kind -> vm.export(kind) { content, filename ->
                        pendingExport = content
                        saveExport.launch(filename)
                    } }
                )
            }
            if (busy) {
                Surface(color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f), modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                            Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("Processando…", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    preview?.let { item ->
        ImportPreviewDialog(
            preview = item,
            busy = busy,
            onDismiss = vm::dismissPreview,
            onConfirm = vm::applyPackage
        )
    }
}

@Composable
private fun HomeScreen(
    records: List<RecordEntity>, events: List<EventEntity>, savedCount: Int, version: Int,
    onOpenRadar: () -> Unit, onOpenData: () -> Unit,
    onOpenRecord: (RecordEntity) -> Unit, onSaveRecord: (RecordEntity) -> Unit
) {
    val upcoming = events.count { event ->
        runCatching { LocalDate.parse(event.date).isAfter(LocalDate.now().minusDays(1)) }.getOrDefault(false)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Visão geral", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text("Uma leitura organizada do que está em acompanhamento.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item { RadarOverviewCard(recordCount = records.size, version = version, onOpen = onOpenRadar) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Registros", records.size.toString(), IconAsset.Resource(R.drawable.ic_description), Modifier.weight(1f))
                MetricTile("Salvos", savedCount.toString(), IconAsset.Resource(R.drawable.ic_bookmark_border), Modifier.weight(1f))
                MetricTile("Marcos", upcoming.toString(), IconAsset.Resource(R.drawable.ic_calendar_month), Modifier.weight(1f))
            }
        }
        item {
            SectionHeading("Janela de ação", "Registros com horizonte mais próximo")
            val priorityList = records.sortedWith(
                compareBy<RecordEntity> { when (it.horizon?.lowercase()) { "short", "curto", "curto_prazo" -> 0; "medium", "médio", "medio" -> 1; "long", "longo", "longo_prazo" -> 2; else -> 3 } }
                    .thenByDescending { it.updatedAt ?: "" }
            ).take(3)
            if (priorityList.isEmpty()) EmptyPriorityCard(onOpenRadar)
            else Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                priorityList.forEach { record -> RecordCard(record, onOpen = { onOpenRecord(record) }, onSave = { onSaveRecord(record) }) }
                Text("Ordem de exibição: horizonte declarado e atualização. Não é uma pontuação composta.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionHeading("Movimentações", "Alterações recebidas no acervo")
            ActivitySection(events = events)
        }
        item {
            OutlinedCard(onClick = onOpenData, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(IconAsset.Resource(R.drawable.ic_storage), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Acervo e atualizações", fontWeight = FontWeight.SemiBold)
                        Text("Importe a primeira base quando estiver pronta.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Abrir", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun RadarOverviewCard(recordCount: Int, version: Int, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ACERVO NACIONAL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                Text(if (recordCount == 0) "Pronto para receber sua primeira carga" else "$recordCount itens no radar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Banco local · versão $version", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Abrir radar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("→", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            RadarGlyph(Modifier.size(112.dp), subtle = false)
        }
    }
}

@Composable
private fun RadarGlyph(modifier: Modifier = Modifier, subtle: Boolean = true) {
    val tint = MaterialTheme.colorScheme.primary.copy(alpha = if (subtle) 0.26f else 0.7f)
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val max = size.minDimension * 0.43f
        listOf(0.32f, 0.62f, 0.93f).forEach { factor ->
            drawCircle(tint, max * factor, c, style = Stroke(width = 1.4.dp.toPx()))
        }
        drawLine(tint, Offset(c.x, c.y - max), Offset(c.x, c.y + max), 1.dp.toPx())
        drawLine(tint, Offset(c.x - max, c.y), Offset(c.x + max, c.y), 1.dp.toPx())
        drawLine(tint.copy(alpha = 0.75f), c, Offset(c.x + max * 0.78f, c.y - max * 0.55f), 2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(tint, 3.dp.toPx(), c)
        drawArc(tint.copy(alpha = 0.25f), -42f, 90f, false, topLeft = Offset(c.x - max, c.y - max), size = Size(max * 2, max * 2), style = Stroke(1.3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))) )
    }
}

@Composable
private fun MetricTile(label: String, value: String, icon: IconAsset, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppIcon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyPriorityCard(onOpen: () -> Unit) {
    OutlinedCard(onClick = onOpen, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(34.dp)) {
                    Box(contentAlignment = Alignment.Center) { AppIcon(IconAsset.Resource(R.drawable.ic_filter_list), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                }
                Column(Modifier.weight(1f)) {
                    Text("Nenhuma prioridade cadastrada", fontWeight = FontWeight.Medium)
                    Text("A lista será organizada após a carga inicial.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PlaceholderLine(Modifier.fillMaxWidth(0.94f))
            PlaceholderLine(Modifier.fillMaxWidth(0.73f))
            PlaceholderLine(Modifier.fillMaxWidth(0.84f))
            Text("Configurar filtros →", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ActivitySection(events: List<EventEntity>) {
    val recent = events.sortedByDescending { it.date }.take(3)
    if (recent.isEmpty()) {
        OutlinedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(IconAsset.Resource(R.drawable.ic_timeline), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text("Sem movimentações por enquanto", fontWeight = FontWeight.Medium)
                }
                repeat(2) { index ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape, modifier = Modifier.size(34.dp)) {}
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PlaceholderLine(Modifier.fillMaxWidth(if (index == 0) 0.65f else 0.78f), height = 9.dp)
                            PlaceholderLine(Modifier.fillMaxWidth(if (index == 0) 0.88f else 0.55f), height = 7.dp)
                        }
                        Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("Os espaços são reservados para atualizações reais; nenhuma atividade simulada foi inserida.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recent.forEach { AgendaEventCard(it) }
        }
    }
}

@Composable
private fun PlaceholderLine(modifier: Modifier, height: androidx.compose.ui.unit.Dp = 8.dp) {
    Box(modifier.height(height).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
}

@Composable
private fun RadarScreen(records: List<RecordEntity>, onOpen: (RecordEntity) -> Unit, onSave: (RecordEntity) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var axis by rememberSaveable { mutableStateOf(0) }
    var horizon by rememberSaveable { mutableStateOf("Todos") }
    var showMoreFilters by rememberSaveable { mutableStateOf(false) }
    var type by rememberSaveable { mutableStateOf("Todos") }
    var jurisdiction by rememberSaveable { mutableStateOf("Todos") }
    var status by rememberSaveable { mutableStateOf("Todos") }
    var effect by rememberSaveable { mutableStateOf("Todos") }
    var urgency by rememberSaveable { mutableStateOf("Todos") }
    var archiveFilter by rememberSaveable { mutableStateOf("Ativos") }
    var sortBy by rememberSaveable { mutableStateOf("Atualização") }

    val axes = listOf("Todos") + (1..10).map { "Eixo $it" }
    val types = remember(records) { listOf("Todos") + records.map { it.type }.filter { it.isNotBlank() }.distinct().sorted() }
    val jurisdictions = remember(records) { listOf("Todos") + records.mapNotNull { it.jurisdiction?.takeIf { value -> value.isNotBlank() } }.distinct().sorted() }
    val statuses = remember(records) { listOf("Todos") + records.mapNotNull { it.status?.takeIf { value -> value.isNotBlank() } }.distinct().sorted() }
    val effects = listOf("Todos", "FAVORECE", "RESTRINGE", "MISTO", "NEUTRO", "INDETERMINADO")
    val urgencies = listOf("Todos", "high", "medium", "low")
    val hasExtraFilters = type != "Todos" || jurisdiction != "Todos" || status != "Todos" || effect != "Todos" || urgency != "Todos" || archiveFilter != "Ativos"

    val filtered = remember(records, query, axis, horizon, type, jurisdiction, status, effect, urgency, archiveFilter, sortBy) {
        val matches = records.filter { record ->
            val q = query.trim()
            val searchable = listOfNotNull(
                record.title, record.summary, record.officialId, record.status, record.institution,
                record.jurisdiction, record.actorsJson, record.sourcesJson
            )
            val matchesQuery = q.isBlank() || searchable.any { it.contains(q, ignoreCase = true) }
            val matchesAxis = axis == 0 || recordAxisMatches(record.axesJson, axis, "Todos")
            val matchesHorizon = horizon == "Todos" || horizonMatches(record.horizon, horizon)
            val matchesType = type == "Todos" || record.type.equals(type, ignoreCase = true)
            val matchesJurisdiction = jurisdiction == "Todos" || record.jurisdiction == jurisdiction
            val matchesStatus = status == "Todos" || record.status == status
            val matchesEffect = effect == "Todos" || recordAxisMatches(record.axesJson, 0, effect)
            val matchesUrgency = urgency == "Todos" || recordUrgencyMatches(record.urgencyJson, urgency)
            val matchesArchive = when (archiveFilter) { "Arquivados" -> record.isArchived; "Todos" -> true; else -> !record.isArchived }
            matchesQuery && matchesAxis && matchesHorizon && matchesType && matchesJurisdiction && matchesStatus && matchesEffect && matchesUrgency && matchesArchive
        }
        when (sortBy) {
            "Horizonte" -> matches.sortedWith(
                compareBy<RecordEntity> { horizonRank(it.horizon) }.thenByDescending { it.updatedAt ?: "" }
            )
            "Aderência" -> matches.sortedWith(compareByDescending<RecordEntity> { it.adherence ?: -1.0 }.thenByDescending { it.updatedAt ?: "" })
            else -> matches.sortedByDescending { it.updatedAt ?: "" }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Encontre uma pauta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Pesquise por título, número, órgão, pessoa ou trecho.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar no acervo") }, leadingIcon = { AppIcon(IconAsset.Resource(R.drawable.ic_manage_search), contentDescription = null) },
                singleLine = true, shape = RoundedCornerShape(16.dp)
            )
        }
        item {
            ChoiceChips("EIXO NABOTE", axes, if (axis == 0) "Todos" else "Eixo $axis", { selected -> axis = if (selected == "Todos") 0 else selected.substringAfter(" ").toIntOrNull() ?: 0 })
        }
        item {
            ChoiceChips("HORIZONTE", listOf("Todos", "short", "medium", "long"), horizon, { horizon = it }, mapLabel = { when (it) { "short" -> "Curto"; "medium" -> "Médio"; "long" -> "Longo"; else -> it } })
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = showMoreFilters || hasExtraFilters,
                    onClick = { showMoreFilters = !showMoreFilters },
                    label = { Text(if (showMoreFilters) "Ocultar filtros" else "Mais filtros") },
                    leadingIcon = { AppIcon(IconAsset.Resource(R.drawable.ic_filter_list), contentDescription = null, modifier = Modifier.size(17.dp)) }
                )
                if (hasExtraFilters) Text("Filtros ativos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                if (query.isNotBlank() || axis != 0 || horizon != "Todos" || hasExtraFilters) {
                    TextButton(onClick = {
                        query = ""; axis = 0; horizon = "Todos"; type = "Todos"; jurisdiction = "Todos"
                        status = "Todos"; effect = "Todos"; urgency = "Todos"; archiveFilter = "Ativos"
                    }) { Text("Limpar") }
                }
            }
        }
        if (showMoreFilters) {
            item { ChoiceChips("TIPO", types, type, { type = it }, mapLabel = ::typeLabel) }
            item { ChoiceChips("ESFERA / JURISDIÇÃO", jurisdictions, jurisdiction, { jurisdiction = it }) }
            item { ChoiceChips("SITUAÇÃO", statuses, status, { status = it }) }
            item { ChoiceChips("EFEITO INTERPRETADO", effects, effect, { effect = it }, mapLabel = ::effectLabel) }
            item { ChoiceChips("URGÊNCIA", urgencies, urgency, { urgency = it }, mapLabel = { when (it) { "high" -> "Alta"; "medium" -> "Média"; "low" -> "Baixa"; else -> it } }) }
            item { ChoiceChips("ARQUIVO", listOf("Ativos", "Arquivados", "Todos"), archiveFilter, { archiveFilter = it }) }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Resultados", fontWeight = FontWeight.SemiBold)
                    Text("${filtered.size} de ${records.size} registros", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ordenar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf("Atualização", "Horizonte", "Aderência").forEach { option ->
                        FilterChip(selected = sortBy == option, onClick = { sortBy = option }, label = { Text(option) })
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            item { if (records.isEmpty()) EmptyRadarState() else EmptySearchState() }
        } else {
            items(filtered, key = { it.id }) { record -> RecordCard(record, onOpen = { onOpen(record) }, onSave = { onSave(record) }) }
        }
    }
}

@Composable
private fun ChoiceChips(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    mapLabel: (String) -> String = { it }
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            options.forEach { option ->
                FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(mapLabel(option)) })
            }
        }
    }
}

private fun horizonMatches(value: String?, selected: String): Boolean = when (selected) {
    "short" -> value.equals("short", true) || value.equals("curto", true) || value.equals("curto_prazo", true)
    "medium" -> value.equals("medium", true) || value.equals("médio", true) || value.equals("medio", true) || value.equals("medio_prazo", true)
    "long" -> value.equals("long", true) || value.equals("longo", true) || value.equals("longo_prazo", true)
    else -> true
}

private fun horizonRank(value: String?): Int = when {
    value.equals("short", true) || value.equals("curto", true) || value.equals("curto_prazo", true) -> 0
    value.equals("medium", true) || value.equals("médio", true) || value.equals("medio", true) || value.equals("medio_prazo", true) -> 1
    value.equals("long", true) || value.equals("longo", true) || value.equals("longo_prazo", true) -> 2
    else -> 3
}

private fun recordAxisMatches(raw: String, axisId: Int, effect: String): Boolean {
    val axes = runCatching { Json.parseToJsonElement(raw).jsonArray }.getOrNull() ?: return false
    return axes.any { element ->
        val item = element as? JsonObject ?: return@any false
        val itemAxis = item.value("axis_id")?.toIntOrNull()
        val itemEffect = item.value("effect") ?: item.value("direction")
        (axisId == 0 || itemAxis == axisId) && (effect == "Todos" || itemEffect.equals(effect, true))
    }
}

private fun recordUrgencyMatches(raw: String, selected: String): Boolean {
    val urgency = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return false
    return urgency.values.any { value -> value.jsonPrimitive.contentOrNull.equals(selected, true) }
}

@Composable
private fun EmptyRadarState() {
    OutlinedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 26.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RadarGlyph(Modifier.size(120.dp))
            Text("O radar está pronto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Ainda não há pautas no banco local. Quando você importar o primeiro snapshot, os registros aparecerão aqui com busca e filtros.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            repeat(3) { index ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlaceholderLine(Modifier.size(30.dp).aspectRatio(1f))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PlaceholderLine(Modifier.fillMaxWidth(if (index == 1) 0.6f else 0.82f))
                        PlaceholderLine(Modifier.fillMaxWidth(if (index == 0) 0.9f else 0.72f), height = 6.dp)
                    }
                    Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Nenhum conteúdo de demonstração será misturado ao acervo.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EmptySearchState() {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppIcon(IconAsset.Resource(R.drawable.ic_search_off), contentDescription = null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Nenhum resultado encontrado", fontWeight = FontWeight.SemiBold)
        Text("Tente outro termo ou ajuste os filtros.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RecordCard(record: RecordEntity, onOpen: () -> Unit, onSave: () -> Unit) {
    OutlinedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.clickable(onClick = onOpen).padding(start = 15.dp, top = 14.dp, end = 8.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(typeLabel(record.type).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onSave) {
                    AppIcon(IconAsset.Resource(if (record.isSaved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border), contentDescription = if (record.isSaved) "Remover dos salvos" else "Salvar pauta", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(record.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(record.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                if (record.isArchived) StatusPill("Arquivado") else record.status?.let { StatusPill(it) }
                record.officialId?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
                Spacer(Modifier.weight(1f))
                Text("Detalhes →", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val label = statusLabel(status)
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50)) {
        Text(label, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun statusLabel(value: String): String = when (value.lowercase()) {
    "pending_review" -> "Pendente de revisão"
    "confirmed" -> "Confirmado"
    "disputed" -> "Contestado"
    "archived" -> "Arquivado"
    else -> value.replace('_', ' ')
}

private fun typeLabel(value: String): String = when (value.lowercase()) {
    "proposition", "legislative_proposal" -> "Proposição"
    "law" -> "Lei"
    "decree" -> "Decreto"
    "regulation" -> "Regulamento"
    "judicial_decision" -> "Decisão judicial"
    "administrative_act" -> "Ato administrativo"
    "bill" -> "Projeto de lei"
    "event" -> "Evento institucional"
    else -> value.replace('_', ' ')
}

private fun metricLabel(value: String?): String = when (value?.lowercase()) {
    "high" -> "Alta"
    "medium" -> "Média"
    "low" -> "Baixa"
    else -> value ?: "—"
}

private fun horizonLabel(value: String?): String = when (value?.lowercase()) {
    "short", "curto", "curto_prazo" -> "Curto prazo"
    "medium", "médio", "medio", "medio_prazo" -> "Médio prazo"
    "long", "longo", "longo_prazo" -> "Longo prazo"
    else -> value ?: "—"
}

@Composable
private fun SavedScreen(records: List<RecordEntity>, onOpen: (RecordEntity) -> Unit, onSave: (RecordEntity) -> Unit, onExplore: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Sua seleção", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Pautas que você marcou para consultar novamente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (records.isEmpty()) item {
            OutlinedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppIcon(IconAsset.Resource(R.drawable.ic_bookmark_border), contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Ainda sem itens salvos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Ao encontrar uma pauta importante, use o marcador para mantê-la nesta área.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onExplore) { Text("Explorar o radar") }
                    repeat(2) { PlaceholderLine(Modifier.fillMaxWidth(if (it == 0) 0.9f else 0.65f)) }
                }
            }
        } else items(records, key = { it.id }) { RecordCard(it, { onOpen(it) }, { onSave(it) }) }
    }
}

@Composable
private fun AgendaScreen(events: List<EventEntity>) {
    val sorted = events.sortedBy { it.date }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Agenda", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Audiências, votações, decisões e outros marcos documentados.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (sorted.isEmpty()) item {
            OutlinedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(IconAsset.Resource(R.drawable.ic_calendar_month), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Nenhum marco registrado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Quando o acervo trouxer datas oficiais, esta agenda organizará os próximos marcos e o histórico em ordem cronológica.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    repeat(3) { PlaceholderLine(Modifier.fillMaxWidth(if (it == 0) 0.92f else 0.68f), height = 9.dp) }
                }
            }
        } else items(sorted, key = { it.id }) { event -> AgendaEventCard(event) }
    }
}

@Composable
private fun AgendaEventCard(event: EventEntity) {
    OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(event.date.take(10), modifier = Modifier.padding(9.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(event.label, fontWeight = FontWeight.SemiBold)
                event.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                event.institution?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun RecordDetailScreen(record: RecordEntity, onSave: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(typeLabel(record.type))
                record.factualStatus?.let { StatusPill(it) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSave) { AppIcon(IconAsset.Resource(if (record.isSaved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border), contentDescription = "Alternar salvo") }
            }
            Text(record.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            record.officialId?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
        }
        item { DetailSection("Síntese documental", record.summary) }
        item {
            DetailSection("Situação", listOfNotNull(record.status, record.phase, record.jurisdiction, record.institution).joinToString(" · ").ifBlank { "Aguardando dados" })
        }
        item { DetailSection("Notas documentais", record.observations ?: "Nenhuma observação documental adicional foi registrada.") }
        item { JsonDetailSection("Avaliação por eixo NABOTE", record.axesJson, "axis") }
        item {
            DetailSection("Indicadores", buildString {
                append("Aderência: ").append(record.adherence?.toString() ?: "—")
                append("   ·   Impacto: ").append(metricLabel(record.impact))
                append("\nGravidade: ").append(metricLabel(record.severity))
                append("   ·   Oportunidade midiática: ").append(metricLabel(record.mediaOpportunity))
                append("\nHorizonte: ").append(horizonLabel(record.horizon))
            })
        }
        item { UrgencySection(record.urgencyJson) }
        item { JsonDetailSection("Pessoas e instituições", record.actorsJson, "actor") }
        item { JsonDetailSection("Fontes e evidências", record.sourcesJson, "source") }
        item { JsonDetailSection("Histórico", record.eventsJson, "event") }
        item {
            Text("Classificação documental: ${record.factualStatus?.let(::statusLabel) ?: "não informada"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            record.lastChecked?.let { Text("Última verificação: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun UrgencySection(rawJson: String) {
    val values = remember(rawJson) {
        runCatching { Json.parseToJsonElement(rawJson).jsonObject.mapValues { it.value.jsonPrimitive.contentOrNull ?: "—" } }
            .getOrDefault(emptyMap())
    }
    OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Urgência por dimensão", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val dimensions = listOf("temporal" to "Temporal", "moral" to "Moral", "institutional" to "Institucional", "media" to "Midiática")
            dimensions.forEach { (key, label) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    StatusPill(values[key]?.let(::urgencyLabel) ?: "não informada")
                }
            }
            Text("As dimensões permanecem independentes; não são somadas numa nota única.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun urgencyLabel(value: String) = when (value.lowercase()) {
    "high" -> "Alta"
    "medium" -> "Média"
    "low" -> "Baixa"
    else -> value
}

@Composable
private fun DetailSection(title: String, body: String) {
    OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val naboteTitles = listOf(
    "Proteção da vida desde a concepção",
    "Liberdade de expressão e oposição à censura",
    "Propriedade privada e segurança jurídica",
    "Liberdade econômica, contratual e desburocratização",
    "Direito às armas e legítima defesa",
    "Primazia dos pais e liberdade educacional",
    "Liberdade religiosa, de proselitismo e de crítica",
    "Consciência médica e consentimento informado",
    "Segurança pública e proteção da vítima",
    "Igualdade perante a lei"
)

@Composable
private fun JsonDetailSection(title: String, rawJson: String, kind: String) {
    val uriHandler = LocalUriHandler.current
    val items = remember(rawJson) {
        runCatching { Json.parseToJsonElement(rawJson).jsonArray.map { it.jsonObject } }.getOrDefault(emptyList())
    }
    OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (items.isEmpty()) {
                Text(
                    when (kind) {
                        "axis" -> "Ainda não há avaliação por eixo neste registro."
                        "actor" -> "Nenhuma pessoa ou instituição vinculada."
                        "source" -> "Nenhuma fonte ou evidência cadastrada."
                        else -> "Nenhuma movimentação registrada."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else items.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                val primary = when (kind) {
                    "axis" -> item.value("axis_id")?.toIntOrNull()?.let { naboteTitles.getOrNull(it - 1) } ?: "Eixo não identificado"
                    "actor" -> item.value("name") ?: item.value("person_id") ?: item.value("institution") ?: "Ator relacionado"
                    "source" -> item.value("title") ?: item.value("name") ?: "Fonte"
                    else -> item.value("label") ?: "Movimentação"
                }
                val secondary = when (kind) {
                    "axis" -> listOfNotNull(item.value("incidence")?.let(::incidenceLabel), item.value("score")?.let { "Nota $it/10" }, item.value("effect")?.let(::effectLabel)).joinToString(" · ")
                    "actor" -> listOfNotNull(item.value("role"), item.value("party"), item.value("institution")).joinToString(" · ")
                    "source" -> listOfNotNull(item.value("organization"), item.value("checked_at")?.let { "Verificada em $it" }).joinToString(" · ")
                    else -> listOfNotNull(item.value("date"), item.value("institution")).joinToString(" · ")
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(primary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (secondary.isNotBlank()) Text(secondary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    val description = when (kind) {
                        "axis" -> item.value("rationale")
                        "actor" -> item.value("description")
                        "source" -> item.value("document") ?: item.value("description")
                        else -> item.value("description")
                    }
                    description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    val evidence = if (kind == "axis") item.value("evidence") else item.value("document") ?: item.value("description")
                    evidence?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4, overflow = TextOverflow.Ellipsis) }
                    val url = item.value("url") ?: item.value("source_url")
                    url?.takeIf { it.startsWith("https://", true) || it.startsWith("http://", true) }?.let { link ->
                        Text(
                            link,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { runCatching { uriHandler.openUri(link) } }
                        )
                    }
                }
            }
        }
    }
}

private fun JsonObject.value(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun incidenceLabel(value: String) = when (value) { "direct" -> "Incidência direta"; "indirect" -> "Incidência indireta"; "mention" -> "Mera menção"; else -> value }
private fun effectLabel(value: String) = when (value.uppercase()) { "FAVORECE" -> "Favorece"; "RESTRINGE" -> "Restringe"; "MISTO" -> "Efeito misto"; "NEUTRO" -> "Neutro"; "INDETERMINADO" -> "Indeterminado"; else -> value }

@Composable
private fun DirectoryPanel(people: List<PersonEntity>, institutions: List<InstitutionEntity>, sources: List<SourceEntity>) {
    var selected by rememberSaveable { mutableStateOf("Pessoas") }
    val total = when (selected) { "Instituições" -> institutions.size; "Fontes" -> sources.size; else -> people.size }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        SectionHeading("Cadastros", "Pessoas, órgãos e fontes vinculados às pautas.")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Pessoas", "Instituições", "Fontes").forEach { item ->
                FilterChip(selected = selected == item, onClick = { selected = item }, label = { Text(item) })
            }
        }
        if (total == 0) {
            OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nenhum cadastro em $selected", fontWeight = FontWeight.Medium)
                    Text("Os perfis serão montados a partir dos dados importados, evitando duplicações.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PlaceholderLine(Modifier.fillMaxWidth(0.8f))
                    PlaceholderLine(Modifier.fillMaxWidth(0.58f))
                }
            }
        } else {
            val labels = when (selected) {
                "Instituições" -> institutions.map { it.name to listOfNotNull(it.kind, it.jurisdiction).joinToString(" · ") }
                "Fontes" -> sources.map { it.name to listOfNotNull(it.organization, it.kind, it.checkedAt?.let { date -> "Verificada $date" }).joinToString(" · ") }
                else -> people.map { it.name to listOfNotNull(it.party, it.institution, it.description).joinToString(" · ") }
            }
            labels.take(8).forEach { (name, sub) ->
                OutlinedCard(shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                        if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (total > 8) Text("+ ${total - 8} cadastros", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DataScreen(
    recordCount: Int, archivedCount: Int, people: List<PersonEntity>, institutions: List<InstitutionEntity>, eventCount: Int, sources: List<SourceEntity>,
    version: Int, lastImported: String?, logs: List<br.com.perpetuus.radar.data.OperationLogEntity>,
    busy: Boolean, onImport: () -> Unit, onExport: (String) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Dados e atualizações", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("O acervo fica neste aparelho. Os arquivos são preparados e revisados fora do aplicativo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(IconAsset.Resource(R.drawable.ic_storage), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(9.dp))
                        Text("Estado do acervo", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        StatusPill("Local")
                    }
                    Text("Versão $version", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text("$recordCount ativos · $archivedCount arquivados", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text("${people.size} pessoas · ${institutions.size} instituições", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text("$eventCount marcos · ${sources.size} fontes", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text("Última importação: ${lastImported ?: "nenhuma"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SectionHeading("Importar", "Escolha um arquivo JSON recebido após pesquisa e auditoria.")
            OutlinedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppIcon(IconAsset.Resource(R.drawable.ic_upload_file), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("Snapshot ou delta", fontWeight = FontWeight.SemiBold)
                            Text("A prévia mostra versões, hash e quantidade de alterações antes de gravar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(onClick = onImport, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        AppIcon(IconAsset.Resource(R.drawable.ic_cloud_upload), contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Selecionar arquivo JSON")
                    }
                }
            }
        }
        item {
            SectionHeading("Exportar", "Arquivos para backup, pesquisa e validação do próximo lote.")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportRow("Pacote de trabalho", "Contexto compacto para a próxima rodada do ChatGPT.", IconAsset.Resource(R.drawable.ic_menu_book)) { onExport("workset") }
                ExportRow("Snapshot completo", "Backup portátil de todo o acervo e sua versão.", IconAsset.Resource(R.drawable.ic_inventory_2)) { onExport("snapshot") }
                ExportRow("Schema JSON", "Contrato de dados para preparar os arquivos de carga.", IconAsset.Resource(R.drawable.ic_description)) { onExport("schema") }
            }
        }
        item {
            DirectoryPanel(people = people, institutions = institutions, sources = sources)
        }
        item {
            SectionHeading("Histórico de operações", "Importações registradas neste aparelho.")
            if (logs.isEmpty()) {
                OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Nenhuma operação registrada", fontWeight = FontWeight.Medium)
                        PlaceholderLine(Modifier.fillMaxWidth(0.9f))
                        PlaceholderLine(Modifier.fillMaxWidth(0.58f))
                    }
                }
            } else logs.take(8).forEach { log ->
                OutlinedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(log.operation.replace('_', ' '), fontWeight = FontWeight.Medium)
                            Text(log.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("v${log.versionAfter}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        item {
            Text("Privacidade e integridade", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Não há conta, servidor, coleta automática ou permissão de internet no aplicativo. O SHA-256 verifica a integridade do arquivo, mas não comprova quem o produziu. Revogações e mudanças devem preservar o histórico.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExportRow(title: String, description: String, icon: IconAsset, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIcon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Exportar", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ImportPreviewDialog(preview: PackagePreview, busy: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { AppIcon(if (preview.canImport) IconAsset.Vector(Icons.Filled.CheckCircle) else IconAsset.Resource(R.drawable.ic_error_outline), contentDescription = null, tint = if (preview.canImport) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) },
        title = { Text(if (preview.canImport) "Conferir atualização" else "Arquivo não aprovado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tipo: ${preview.packageType} · versão ${preview.previousVersion} → ${preview.dataVersion}")
                if (preview.packageType == "snapshot") {
                    Text("Registros no acervo completo: ${preview.recordCount}")
                } else {
                    Text("Registros alterados: ${preview.recordCount} · novos: ${preview.newRecords} · já existentes: ${preview.updatedRecords}")
                    Text("Operações incrementais: ${preview.operationCount}")
                }
                val related = preview.relatedCounts.filterKeys { it != "records" && it != "monitoring" }
                if (related.isNotEmpty()) Text("Cadastros: " + related.entries.joinToString(" · ") { "${it.value} ${it.key}" })
                preview.warnings.forEach { Text("Atenção: $it", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall) }
                preview.errors.forEach { Text("• $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (preview.canImport) Text("Um backup local será criado antes da importação. O banco só será alterado após esta confirmação.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            if (preview.canImport) Button(onClick = onConfirm, enabled = !busy) { Text("Aplicar atualização") }
            else TextButton(onClick = onDismiss) { Text("Fechar") }
        },
        dismissButton = { if (preview.canImport) TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } }
    )
}
