package br.com.perpetuus.radar.data

import android.content.Context
import androidx.room.withTransaction
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class RadarRepository(private val context: Context, private val db: RadarDatabase) {
    private val dao = db.dao()
    private val parser = Json { ignoreUnknownKeys = true }

    val records = dao.observeRecords()
    val allRecords = dao.observeAllRecords()
    val saved = dao.observeSaved()
    val people = dao.observePeople()
    val institutions = dao.observeInstitutions()
    val events = dao.observeEvents()
    val sources = dao.observeSources()
    val meta = dao.observeMeta()
    val logs = dao.observeLogs()

    suspend fun setSaved(id: String, value: Boolean) = dao.setSaved(id, value)

    suspend fun preview(raw: String): PackagePreview = PackageEngine.inspect(
        raw = raw,
        currentVersion = dao.getMeta()?.dataVersion ?: 0,
        existingRecordIds = dao.getAllRecords().map { it.id }.toSet()
    )

    suspend fun importPackage(raw: String, approvedPreview: PackagePreview) {
        require(approvedPreview.canImport) { "O pacote não está válido para importação." }
        val inspected = preview(raw)
        require(inspected.canImport) { inspected.errors.joinToString("\n").ifBlank { "O pacote deixou de ser válido." } }
        require(inspected.packageType == approvedPreview.packageType && inspected.dataVersion == approvedPreview.dataVersion) {
            "O pacote mudou desde a prévia. Selecione o arquivo novamente."
        }
        val root = parser.parseToJsonElement(raw).jsonObject
        val payload = root.getValue("payload").jsonObject
        val oldVersion = dao.getMeta()?.dataVersion ?: 0
        if (inspected.packageType == "delta" && inspected.previousVersion != oldVersion) {
            error("A base mudou desde a prévia. Gere uma nova atualização para a versão instalada.")
        }
        val backupText = exportSnapshot()
        persistBackup(backupText)

        db.withTransaction {
            val versionInsideTransaction = dao.getMeta()?.dataVersion ?: 0
            require(versionInsideTransaction == oldVersion) { "A base foi alterada durante a importação. Tente novamente com o pacote atualizado." }
            when (inspected.packageType) {
                "snapshot" -> applySnapshot(payload)
                "delta" -> applyDelta(payload)
                else -> error("Tipo de pacote não importável.")
            }
            val importedAt = Instant.now().toString()
            dao.upsertMeta(
                CollectionMetaEntity(
                    dataVersion = inspected.dataVersion,
                    schemaVersion = PackageEngine.SCHEMA_VERSION,
                    lastImportedAt = importedAt
                )
            )
            dao.upsertLog(
                OperationLogEntity(
                    id = UUID.randomUUID().toString(),
                    occurredAt = importedAt,
                    operation = if (inspected.packageType == "snapshot") "snapshot_import" else "delta_import",
                    summary = "${inspected.recordCount} registro(s); ${inspected.operationCount} operação(ões).",
                    result = "success",
                    versionBefore = oldVersion,
                    versionAfter = inspected.dataVersion
                )
            )
        }
    }

    private suspend fun applySnapshot(payload: JsonObject) {
        val localSaved = dao.getAllRecords().associate { it.id to it.isSaved }
        dao.clearRecords(); dao.clearPeople(); dao.clearInstitutions(); dao.clearEvents(); dao.clearSources(); dao.clearMonitoring()
        dao.upsertRecords(arrayObjects(payload, "records").map { item ->
            val id = item["id"]?.toString()?.trim('"')
            PackageEngine.recordFromJson(item, id?.let { localSaved[it] } ?: false)
        })
        dao.upsertPeople(arrayObjects(payload, "people").map { PackageEngine.personFromJson(it) })
        dao.upsertInstitutions(arrayObjects(payload, "institutions").map { PackageEngine.institutionFromJson(it) })
        dao.upsertEvents(arrayObjects(payload, "events").map { PackageEngine.eventFromJson(it) })
        dao.upsertSources(arrayObjects(payload, "sources").map { PackageEngine.sourceFromJson(it) })
        arrayObjects(payload, "monitoring").firstOrNull()?.let { dao.upsertMonitoring(PackageEngine.monitoringFromJson(it)) }
    }

    private suspend fun applyDelta(payload: JsonObject) {
        val operations = PackageEngine.deltaOperations(payload)
        operations.groupBy { it.table }.forEach { (table, rows) ->
            when (table) {
                "records" -> dao.upsertRecords(rows.map { PackageEngine.recordFromJson(it.data, dao.getRecord(it.id)?.isSaved ?: false) })
                "people" -> dao.upsertPeople(rows.map { PackageEngine.personFromJson(it.data) })
                "institutions" -> dao.upsertInstitutions(rows.map { PackageEngine.institutionFromJson(it.data) })
                "events" -> dao.upsertEvents(rows.map { PackageEngine.eventFromJson(it.data) })
                "sources" -> dao.upsertSources(rows.map { PackageEngine.sourceFromJson(it.data) })
                "monitoring" -> rows.lastOrNull()?.let { dao.upsertMonitoring(PackageEngine.monitoringFromJson(it.data)) }
            }
        }
    }

    suspend fun exportSnapshot(): String {
        val payload = PackageEngine.snapshotPayload(
            records = dao.getAllRecords(), people = dao.getPeople(), institutions = dao.getInstitutions(),
            events = dao.getEvents(), sources = dao.getSources(), monitoring = dao.getMonitoring()
        )
        val version = dao.getMeta()?.dataVersion ?: 0
        return PackageEngine.buildPackage("snapshot", version, version, payload)
    }

    suspend fun exportWorkset(): String {
        val payload = PackageEngine.worksetPayload(
            records = dao.getAllRecords(), people = dao.getPeople(), institutions = dao.getInstitutions(),
            sources = dao.getSources(), monitoring = dao.getMonitoring()
        )
        val version = dao.getMeta()?.dataVersion ?: 0
        return PackageEngine.buildPackage("workset", version, version, payload)
    }

    suspend fun exportSchema(): String = context.assets.open("radar-package.schema.json").bufferedReader().use { it.readText() }

    private fun arrayObjects(payload: JsonObject, key: String): List<JsonObject> =
        (payload[key] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    private fun persistBackup(text: String) {
        val folder = File(context.filesDir, "backups").apply { mkdirs() }
        val file = File(folder, "radar-backup-${System.currentTimeMillis()}.json")
        file.writeText(text, Charsets.UTF_8)
        folder.listFiles()?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }
    }
}
