package br.com.perpetuus.radar.data

import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private val packageJson = Json {
    ignoreUnknownKeys = true
    isLenient = false
    explicitNulls = true
    allowSpecialFloatingPointValues = false
}

data class PackagePreview(
    val rawPackage: String,
    val packageType: String,
    val previousVersion: Int,
    val dataVersion: Int,
    val recordCount: Int,
    val operationCount: Int,
    val newRecords: Int,
    val updatedRecords: Int,
    val relatedCounts: Map<String, Int>,
    val errors: List<String>,
    val warnings: List<String>
) {
    val canImport: Boolean get() = errors.isEmpty() && packageType in setOf("snapshot", "delta")
}

data class DeltaOperation(
    val table: String,
    val id: String,
    val data: JsonObject
)

object PackageEngine {
    const val APP_ID = "radar-nacional"
    const val SCHEMA_VERSION = 1
    private val allowedTables = setOf("records", "people", "institutions", "events", "sources", "monitoring")

    fun canonicalize(element: JsonElement): String = when (element) {
        is JsonObject -> element.keys.sorted().joinToString(",", "{", "}") { key ->
            JsonPrimitive(key).toString() + ":" + canonicalize(element.getValue(key))
        }
        JsonNull -> "null"
        is JsonArray -> element.joinToString(",", "[", "]") { canonicalize(it) }
        is JsonPrimitive -> when {
            element.isString -> element.toString()
            element.booleanOrNull != null -> element.content
            else -> canonicalNumber(element.content)
        }
        else -> "null"
    }

    private fun canonicalNumber(source: String): String {
        val exact = source.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Valor numérico JSON inválido.")
        // JSON integers are parsed as arbitrary-precision integers by the companion Python prompts.
        if (!source.contains('.') && !source.contains('e', true)) return exact.toBigInteger().toString()

        val value = source.toDoubleOrNull()
            ?: throw IllegalArgumentException("Valor numérico JSON fora do intervalo suportado.")
        if (!value.isFinite()) throw IllegalArgumentException("NaN e Infinity não são permitidos.")
        if (value == 0.0) return "0"
        // Match Python's canonicalizer: an integral float is emitted as an integer.
        if (value % 1.0 == 0.0) return BigDecimal(value).toBigInteger().toString()

        val shortest = BigDecimal.valueOf(value).stripTrailingZeros()
        val exponent = shortest.precision() - shortest.scale() - 1
        // Python's JSON encoder uses decimal notation for exponents -4 through 15.
        if (exponent in -4..15) return shortest.toPlainString()

        val digits = shortest.unscaledValue().abs().toString()
        val mantissa = if (digits.length == 1) digits else digits.first() + "." + digits.drop(1)
        val sign = if (shortest.signum() < 0) "-" else ""
        val exponentSign = if (exponent >= 0) "+" else "-"
        val exponentDigits = kotlin.math.abs(exponent).toString().padStart(2, '0')
        return "$sign$mantissa" + "e" + exponentSign + exponentDigits
    }

    fun sha256(element: JsonElement): String {
        val bytes = canonicalize(element).toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    fun inspect(raw: String, currentVersion: Int, existingRecordIds: Set<String>): PackagePreview {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val root = try {
            packageJson.parseToJsonElement(raw).jsonObject
        } catch (error: Exception) {
            return PackagePreview(raw, "invalid", currentVersion, currentVersion, 0, 0, 0, 0, emptyMap(), listOf("O arquivo não contém um objeto JSON válido."), emptyList())
        }

        val manifest = root["manifest"] as? JsonObject
        val payload = root["payload"] as? JsonObject
        if (manifest == null) errors += "Manifesto ausente ou inválido."
        if (payload == null) errors += "Payload ausente ou inválido."
        if (manifest == null || payload == null) {
            return PackagePreview(raw, "invalid", currentVersion, currentVersion, 0, 0, 0, 0, emptyMap(), errors, warnings)
        }

        val requiredManifestFields = listOf("app_id", "schema_version", "package_type", "previous_version", "data_version", "created_at", "checksum_algorithm", "checksum_sha256")
        requiredManifestFields.filter { manifest[it] == null || manifest[it] === JsonNull }.forEach {
            errors += "Campo obrigatório ausente no manifesto: $it."
        }
        val appId = manifest.text("app_id")
        val schemaVersion = manifest.number("schema_version")
        val packageType = manifest.text("package_type") ?: "invalid"
        val previousVersion = manifest.number("previous_version") ?: -1
        val dataVersion = manifest.number("data_version") ?: -1
        val checksumAlgorithm = manifest.text("checksum_algorithm")
        val expectedChecksum = manifest.text("checksum_sha256")
        val createdAt = manifest.text("created_at")

        if (appId != APP_ID) errors += "Este pacote pertence a outro aplicativo."
        if (schemaVersion != SCHEMA_VERSION) errors += "Versão de schema incompatível (esperada: $SCHEMA_VERSION)."
        if (packageType !in setOf("snapshot", "delta", "workset")) errors += "Tipo de pacote desconhecido."
        if (checksumAlgorithm != "SHA-256") errors += "Algoritmo de integridade ausente ou incompatível."
        if (createdAt.isNullOrBlank()) errors += "Data de geração ausente no manifesto."
        if (expectedChecksum.isNullOrBlank() || !expectedChecksum.matches(Regex("[a-fA-F0-9]{64}"))) errors += "Checksum SHA-256 ausente ou malformado."
        else {
            val actual = runCatching { sha256(payload) }.getOrNull()
            if (actual == null || !actual.equals(expectedChecksum, ignoreCase = true)) {
                errors += "O checksum não confere. O banco não será alterado."
            }
        }
        if (previousVersion < 0 || dataVersion < 0) errors += "As versões do pacote não podem ser negativas."
        if (dataVersion < previousVersion) errors += "A versão de destino é anterior à versão de origem."

        var recordCount = 0
        var operationCount = 0
        var newRecords = 0
        var updatedRecords = 0
        val related = linkedMapOf<String, Int>()

        when (packageType) {
            "snapshot" -> {
                val names = listOf("records", "people", "institutions", "events", "sources", "monitoring")
                names.forEach { name ->
                    val array = payload[name] as? JsonArray
                    if (array == null) errors += "A coleção '$name' está ausente ou não é uma lista."
                    else {
                        related[name] = array.size
                        if (name == "records") {
                            recordCount = array.size
                            array.forEachIndexed { index, element ->
                                val item = element as? JsonObject
                                if (item == null) errors += "Registro $index não é um objeto."
                                else validateRecord(item, "records[$index]", errors)
                            }
                            duplicateIds(array, "records", errors)
                        } else {
                            validateSimpleCollection(name, array, errors)
                        }
                    }
                }
                val monitoring = payload["monitoring"] as? JsonArray
                if (monitoring != null && (monitoring.size != 1 || (monitoring.firstOrNull() as? JsonObject)?.text("id") != "state")) {
                    errors += "A coleção monitoring deve conter exatamente um registro principal com id = state."
                }
                val recordIds = (payload["records"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.text("id") }.toSet()
                (payload["events"] as? JsonArray).orEmpty().forEachIndexed { index, item ->
                    val event = item as? JsonObject ?: return@forEachIndexed
                    val recordId = event.text("record_id")
                    if (!recordId.isNullOrBlank() && recordId !in recordIds) errors += "events[$index] referencia um registro que não existe no snapshot."
                }
                if (currentVersion > 0) {
                    warnings += "Snapshot completo: o acervo local será substituído após backup automático."
                }
            }
            "delta" -> {
                if (previousVersion != currentVersion) errors += "Base divergente: o delta parte da versão $previousVersion, mas o aparelho está na versão $currentVersion. Importe um snapshot ou os deltas intermediários."
                if (dataVersion != previousVersion + 1) errors += "A versão do delta deve avançar exatamente uma etapa."
                val operations = payload["operations"] as? JsonArray
                if (operations == null) errors += "A lista de operações do delta está ausente."
                else {
                    operationCount = operations.size
                    operations.forEachIndexed { index, element ->
                        val operation = element as? JsonObject
                        if (operation == null) {
                            errors += "Operação $index não é um objeto."
                        } else {
                            val op = operation.text("op")
                            val table = operation.text("table")
                            val id = operation.text("id")
                            val data = operation["data"] as? JsonObject
                            if (op != "upsert") errors += "Operação $index inválida: somente upsert é aceito; exclusões não são permitidas."
                            if (table == null || table !in allowedTables) errors += "Operação $index aponta para uma tabela desconhecida."
                            if (id.isNullOrBlank() || data == null) errors += "Operação $index não contém ID e objeto data válidos."
                            else if (data.text("id") != id) errors += "Operação $index: o ID da operação difere de data.id."
                            if (table == "records" && data != null) validateRecord(data, "operations[$index].data", errors)
                        }
                    }
                    val deltaRecordIds = operations.mapNotNull { element ->
                        val op = element as? JsonObject ?: return@mapNotNull null
                        if (op.text("table") == "records") op.text("id") else null
                    }.toSet()
                    val availableRecordIds = existingRecordIds + deltaRecordIds
                    operations.forEachIndexed { index, element ->
                        val operation = element as? JsonObject ?: return@forEachIndexed
                        val table = operation.text("table") ?: return@forEachIndexed
                        val id = operation.text("id") ?: return@forEachIndexed
                        val data = operation["data"] as? JsonObject ?: return@forEachIndexed
                        if (table in setOf("people", "institutions", "sources") && data.text("name").isNullOrBlank()) {
                            errors += "operations[$index].data: o campo name é obrigatório para $table."
                        }
                        if (table == "events") {
                            if (data.text("label").isNullOrBlank()) errors += "operations[$index].data: label é obrigatório para events."
                            if (data.text("date").isNullOrBlank()) errors += "operations[$index].data: date é obrigatório para events."
                            val recordId = data.text("record_id")
                            if (recordId.isNullOrBlank()) errors += "operations[$index].data: record_id é obrigatório para events."
                            else if (recordId !in availableRecordIds) errors += "operations[$index].data: o evento $id referencia um registro desconhecido ($recordId)."
                        }
                        if (table == "monitoring") {
                            if (id != "state") errors += "operations[$index]: monitoring deve usar o ID state."
                            listOf("source_checkpoints", "record_states", "coverage_gaps", "pending_review").forEach { field ->
                                if (data[field] !is JsonArray) errors += "operations[$index].data: $field deve ser uma lista em monitoring."
                            }
                        }
                    }
                    val operationKeys = operations.mapNotNull { item ->
                        val op = item as? JsonObject ?: return@mapNotNull null
                        val table = op.text("table") ?: return@mapNotNull null
                        val id = op.text("id") ?: return@mapNotNull null
                        "$table:$id"
                    }
                    if (operationKeys.size != operationKeys.distinct().size) errors += "Há operações repetidas para a mesma tabela e ID no delta."
                    val recordOps = operations.mapNotNull { (it as? JsonObject)?.takeIf { op -> op.text("table") == "records" } }
                    val uniqueRecordOps = recordOps.mapNotNull { it.text("id") }.distinct()
                    recordCount = recordOps.size
                    newRecords = uniqueRecordOps.count { it !in existingRecordIds }
                    updatedRecords = uniqueRecordOps.size - newRecords
                    recordOps.groupBy { it.text("table") ?: "" }.forEach { (name, items) -> related[name] = items.size }
                }
            }
            "workset" -> errors += "Pacote de trabalho é apenas contexto para pesquisa. Não pode ser importado como atualização."
        }

        return PackagePreview(
            rawPackage = raw,
            packageType = packageType,
            previousVersion = previousVersion,
            dataVersion = dataVersion,
            recordCount = recordCount,
            operationCount = operationCount,
            newRecords = newRecords,
            updatedRecords = updatedRecords,
            relatedCounts = related,
            errors = errors.distinct(),
            warnings = warnings
        )
    }

    private fun validateRecord(item: JsonObject, path: String, errors: MutableList<String>) {
        if (item.text("id").isNullOrBlank()) errors += "$path: ID ausente."
        if (item.text("type").isNullOrBlank()) errors += "$path: tipo ausente."
        if (item.text("title").isNullOrBlank()) errors += "$path: título ausente."
        if (item.text("summary").isNullOrBlank()) errors += "$path: resumo ausente."
        val adherence = item.decimal("adherence")
        if (adherence != null && adherence !in 0.0..10.0) errors += "$path: aderência deve estar entre 0 e 10."
        val factualStatus = item.text("factual_status")
        if (factualStatus != null && factualStatus !in setOf("confirmed", "pending_review", "disputed")) {
            errors += "$path: factual_status deve ser confirmed, pending_review ou disputed."
        }
        listOf("impact", "severity", "media_opportunity").forEach { metric ->
            val value = item.text(metric)
            if (value != null && value !in setOf("low", "medium", "high")) errors += "$path: $metric deve ser low, medium ou high."
        }
        val horizon = item.text("horizon")
        if (horizon != null && horizon !in setOf("short", "medium", "long", "curto_prazo", "medio_prazo", "longo_prazo")) {
            errors += "$path: horizon deve ser short, medium ou long."
        }
        val archivedValue = item["is_archived"]
        if (archivedValue != null && (archivedValue !is JsonPrimitive || archivedValue.booleanOrNull == null)) {
            errors += "$path: is_archived precisa ser booleano."
        }
        val urgency = item["urgency"]
        if (urgency != null) {
            val urgencyObject = urgency as? JsonObject
            if (urgencyObject == null) errors += "$path: urgency deve ser um objeto."
            else listOf("temporal", "moral", "institutional", "media").forEach { dimension ->
                val value = urgencyObject.text(dimension)
                if (value != null && value !in setOf("low", "medium", "high")) errors += "$path: urgency.$dimension deve ser low, medium ou high."
            }
        }
        val axes = item["axes"]
        if (axes != null && axes !is JsonArray) errors += "$path: axes deve ser uma lista."
        (axes as? JsonArray)?.forEachIndexed { index, element ->
            val axis = element as? JsonObject
            if (axis == null) errors += "$path.axes[$index] não é um objeto."
            else {
                val axisId = axis.number("axis_id")
                val incidence = axis.text("incidence")
                val score = axis.decimal("score")
                if (axisId == null || axisId !in 1..10) errors += "$path.axes[$index]: axis_id deve estar entre 1 e 10."
                if (incidence == null || incidence !in setOf("direct", "indirect", "mention")) errors += "$path.axes[$index]: incidência inválida."
                if (score == null || score !in 0.0..10.0) errors += "$path.axes[$index]: score deve estar entre 0 e 10."
                if (axis.text("rationale").isNullOrBlank()) errors += "$path.axes[$index]: justificativa ausente."
                if (axis.text("evidence").isNullOrBlank()) errors += "$path.axes[$index]: evidência ausente."
            }
        }
    }

    private fun validateSimpleCollection(name: String, array: JsonArray, errors: MutableList<String>) {
        val required = when (name) {
            "people", "institutions", "sources" -> "name"
            "events" -> "label"
            else -> null
        }
        array.forEachIndexed { index, element ->
            val item = element as? JsonObject
            if (item == null) errors += "$name[$index] não é um objeto."
            else {
                if (item.text("id").isNullOrBlank()) errors += "$name[$index]: ID ausente."
                if (required != null && item.text(required).isNullOrBlank()) errors += "$name[$index]: campo $required ausente."
                if (name == "events") {
                    if (item.text("record_id").isNullOrBlank()) errors += "$name[$index]: record_id ausente."
                    if (item.text("date").isNullOrBlank()) errors += "$name[$index]: date ausente."
                }
                if (name == "monitoring") {
                    listOf("source_checkpoints", "record_states", "coverage_gaps", "pending_review").forEach { field ->
                        if (item[field] !is JsonArray) errors += "$name[$index]: $field deve ser uma lista."
                    }
                }
            }
        }
        duplicateIds(array, name, errors)
    }

    private fun duplicateIds(array: JsonArray, table: String, errors: MutableList<String>) {
        val ids = array.mapNotNull { (it as? JsonObject)?.text("id") }
        if (ids.size != ids.distinct().size) errors += "A coleção '$table' contém IDs duplicados."
    }

    fun deltaOperations(payload: JsonObject): List<DeltaOperation> {
        val operations = payload["operations"]?.jsonArray ?: return emptyList()
        return operations.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val table = obj.text("table") ?: return@mapNotNull null
            val id = obj.text("id") ?: return@mapNotNull null
            val data = obj["data"] as? JsonObject ?: return@mapNotNull null
            DeltaOperation(table, id, data)
        }
    }

    fun recordFromJson(item: JsonObject, saved: Boolean = false): RecordEntity {
        val id = item.text("id") ?: error("Registro sem ID.")
        val status = item.text("status")
        return RecordEntity(
            id = id,
            type = item.text("type") ?: "other",
            title = item.text("title") ?: "Sem título",
            summary = item.text("summary") ?: "",
            officialId = item.text("official_id"),
            jurisdiction = item.text("jurisdiction"),
            institution = item.text("institution") ?: item.text("institution_name"),
            status = status,
            phase = item.text("phase"),
            horizon = item.text("horizon"),
            adherence = item.decimal("adherence"),
            impact = item.text("impact"),
            severity = item.text("severity"),
            mediaOpportunity = item.text("media_opportunity"),
            factualStatus = item.text("factual_status"),
            lastChecked = item.text("last_checked"),
            updatedAt = item.text("updated_at"),
            createdAt = item.text("created_at"),
            urgencyJson = item["urgency"]?.toString() ?: "{}",
            axesJson = item["axes"]?.toString() ?: "[]",
            actorsJson = item["actors"]?.toString() ?: "[]",
            sourcesJson = item["sources"]?.toString() ?: "[]",
            eventsJson = item["events"]?.toString() ?: "[]",
            observations = item.text("observations") ?: item.text("notes"),
            isArchived = (item["is_archived"] as? JsonPrimitive)?.booleanOrNull ?: (status.equals("archived", true) || status.equals("arquivado", true)),
            isSaved = saved,
            rawJson = item.toString()
        )
    }

    fun personFromJson(item: JsonObject) = PersonEntity(
        id = item.text("id") ?: error("Pessoa sem ID."),
        name = item.text("name") ?: "Sem nome",
        party = item.text("party"),
        institution = item.text("institution"),
        description = item.text("description"),
        contactUrl = item.text("contact_url"),
        checkedAt = item.text("checked_at"),
        rawJson = item.toString()
    )

    fun institutionFromJson(item: JsonObject) = InstitutionEntity(
        id = item.text("id") ?: error("Instituição sem ID."),
        name = item.text("name") ?: "Sem nome",
        kind = item.text("kind"),
        jurisdiction = item.text("jurisdiction"),
        url = item.text("url"),
        rawJson = item.toString()
    )

    fun eventFromJson(item: JsonObject) = EventEntity(
        id = item.text("id") ?: error("Evento sem ID."),
        recordId = item.text("record_id") ?: "",
        date = item.text("date") ?: "",
        label = item.text("label") ?: "Movimentação",
        description = item.text("description"),
        institution = item.text("institution"),
        sourceUrl = item.text("source_url") ?: item.text("url"),
        rawJson = item.toString()
    )

    fun sourceFromJson(item: JsonObject) = SourceEntity(
        id = item.text("id") ?: error("Fonte sem ID."),
        name = item.text("name") ?: "Sem nome",
        kind = item.text("kind"),
        organization = item.text("organization"),
        url = item.text("url"),
        publishedAt = item.text("published_at"),
        checkedAt = item.text("checked_at"),
        rawJson = item.toString()
    )

    fun monitoringFromJson(item: JsonObject) = MonitoringEntity(
        id = item.text("id") ?: "state",
        lastRunAt = item.text("last_run_at"),
        sourceCheckpointsJson = item["source_checkpoints"]?.toString() ?: "[]",
        recordStatesJson = item["record_states"]?.toString() ?: "[]",
        coverageGapsJson = item["coverage_gaps"]?.toString() ?: "[]",
        pendingReviewJson = item["pending_review"]?.toString() ?: "[]",
        note = item.text("note"),
        rawJson = item.toString()
    )

    fun buildPackage(packageType: String, previousVersion: Int, dataVersion: Int, payload: JsonObject): String {
        val manifest = buildJsonObject {
            put("app_id", APP_ID)
            put("schema_version", SCHEMA_VERSION)
            put("package_type", packageType)
            put("previous_version", previousVersion)
            put("data_version", dataVersion)
            put("created_at", Instant.now().toString())
            put("checksum_algorithm", "SHA-256")
            put("checksum_sha256", sha256(payload))
            put("description", "Pacote gerado pelo Radar Nacional. O checksum detecta corrupção, mas não comprova autoria.")
        }
        return buildJsonObject {
            put("manifest", manifest)
            put("payload", payload)
        }.toString()
    }

    fun snapshotPayload(
        records: List<RecordEntity>,
        people: List<PersonEntity>,
        institutions: List<InstitutionEntity>,
        events: List<EventEntity>,
        sources: List<SourceEntity>,
        monitoring: MonitoringEntity?
    ): JsonObject = buildJsonObject {
        put("records", JsonArray(records.map { safeJson(it.rawJson) }))
        put("people", JsonArray(people.map { safeJson(it.rawJson) }))
        put("institutions", JsonArray(institutions.map { safeJson(it.rawJson) }))
        put("events", JsonArray(events.map { safeJson(it.rawJson) }))
        put("sources", JsonArray(sources.map { safeJson(it.rawJson) }))
        val monitoringObject = monitoring?.let { safeJson(it.rawJson) } ?: buildJsonObject {
            put("id", "state")
            put("last_run_at", JsonNull)
            put("source_checkpoints", buildJsonArray {})
            put("record_states", buildJsonArray {})
            put("coverage_gaps", buildJsonArray {})
            put("pending_review", buildJsonArray {})
            put("note", "Nenhuma rodada de monitoramento foi registrada ainda.")
        }
        put("monitoring", JsonArray(listOf(monitoringObject)))
    }

    fun worksetPayload(
        records: List<RecordEntity>,
        people: List<PersonEntity>,
        institutions: List<InstitutionEntity>,
        sources: List<SourceEntity>,
        monitoring: MonitoringEntity?
    ): JsonObject {
        val pendingIds = runCatching {
            (packageJson.parseToJsonElement(monitoring?.pendingReviewJson ?: "[]") as? JsonArray)
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet().orEmpty()
        }.getOrDefault(emptySet())
        // Keep the full index for deduplication/history, but use the review batch for active items.
        val selected = records.asSequence().filterNot { it.isArchived }.sortedWith(
            compareByDescending<RecordEntity> { it.id in pendingIds }
                .thenBy { it.lastChecked ?: "" }
                .thenByDescending { it.updatedAt ?: "" }
        ).take(20).toList()
        val recordIndex = records.map { record ->
            buildJsonObject {
                put("id", record.id)
                put("type", record.type)
                put("title", record.title)
                put("official_id", record.officialId ?: "")
                put("status", record.status ?: "")
                put("last_checked", record.lastChecked ?: "")
                put("horizon", record.horizon ?: "")
                put("jurisdiction", record.jurisdiction ?: "")
                put("institution", record.institution ?: "")
                put("is_archived", record.isArchived)
                put("fingerprint", sha256(safeJson(record.rawJson)))
            }
        }
        val state = monitoring?.let { safeJson(it.rawJson) as? JsonObject } ?: buildJsonObject {
            put("id", "state")
            put("last_run_at", JsonNull)
            put("source_checkpoints", buildJsonArray {})
            put("record_states", buildJsonArray {})
            put("coverage_gaps", buildJsonArray {})
            put("pending_review", buildJsonArray {})
        }
        return buildJsonObject {
            put("monitoring_state", state)
            put("records_to_review", JsonArray(selected.map { safeJson(it.rawJson) }))
            put("record_index", JsonArray(recordIndex))
            put("people_index", JsonArray(people.map { safeJson(it.rawJson) }))
            put("institutions_index", JsonArray(institutions.map { safeJson(it.rawJson) }))
            put("source_index", JsonArray(sources.map { safeJson(it.rawJson) }))
            put("batch", buildJsonObject {
                put("max_records", 20)
                put("selected_count", selected.size)
                put("due_count", selected.size)
                put("exported_at", Instant.now().toString())
                put("selection_rule", "Pendências de verificação primeiro; depois registros sem verificação recente.")
            })
        }
    }

    private fun safeJson(raw: String): JsonElement = runCatching { packageJson.parseToJsonElement(raw) }.getOrDefault(JsonNull)
}

private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.number(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
private fun JsonObject.decimal(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
