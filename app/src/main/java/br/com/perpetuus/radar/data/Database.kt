package br.com.perpetuus.radar.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "records", indices = [Index("updatedAt"), Index("horizon"), Index("status")])
data class RecordEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val summary: String,
    val officialId: String? = null,
    val jurisdiction: String? = null,
    val institution: String? = null,
    val status: String? = null,
    val phase: String? = null,
    val horizon: String? = null,
    val adherence: Double? = null,
    val impact: String? = null,
    val severity: String? = null,
    val mediaOpportunity: String? = null,
    val factualStatus: String? = null,
    val lastChecked: String? = null,
    val updatedAt: String? = null,
    val createdAt: String? = null,
    val urgencyJson: String = "{}",
    val axesJson: String = "[]",
    val actorsJson: String = "[]",
    val sourcesJson: String = "[]",
    val eventsJson: String = "[]",
    val observations: String? = null,
    val isArchived: Boolean = false,
    val isSaved: Boolean = false,
    val rawJson: String
)

@Entity(tableName = "people", indices = [Index("name")])
data class PersonEntity(
    @PrimaryKey val id: String,
    val name: String,
    val party: String? = null,
    val institution: String? = null,
    val description: String? = null,
    val contactUrl: String? = null,
    val checkedAt: String? = null,
    val rawJson: String
)

@Entity(tableName = "institutions", indices = [Index("name")])
data class InstitutionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String? = null,
    val jurisdiction: String? = null,
    val url: String? = null,
    val rawJson: String
)

@Entity(tableName = "events", indices = [Index("recordId"), Index("date")])
data class EventEntity(
    @PrimaryKey val id: String,
    val recordId: String,
    val date: String,
    val label: String,
    val description: String? = null,
    val institution: String? = null,
    val sourceUrl: String? = null,
    val rawJson: String
)

@Entity(tableName = "sources", indices = [Index("name")])
data class SourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String? = null,
    val organization: String? = null,
    val url: String? = null,
    val publishedAt: String? = null,
    val checkedAt: String? = null,
    val rawJson: String
)

@Entity(tableName = "monitoring")
data class MonitoringEntity(
    @PrimaryKey val id: String = "state",
    val lastRunAt: String? = null,
    val sourceCheckpointsJson: String = "[]",
    val recordStatesJson: String = "[]",
    val coverageGapsJson: String = "[]",
    val pendingReviewJson: String = "[]",
    val note: String? = null,
    val rawJson: String
)

@Entity(tableName = "collection_meta")
data class CollectionMetaEntity(
    @PrimaryKey val id: String = "main",
    val dataVersion: Int = 0,
    val schemaVersion: Int = 1,
    val lastImportedAt: String? = null
)

@Entity(tableName = "operation_log", indices = [Index("occurredAt")])
data class OperationLogEntity(
    @PrimaryKey val id: String,
    val occurredAt: String,
    val operation: String,
    val summary: String,
    val result: String,
    val versionBefore: Int,
    val versionAfter: Int
)

@Dao
interface RadarDao {
    @Query("SELECT * FROM records WHERE isArchived = 0 ORDER BY updatedAt DESC, title COLLATE NOCASE")
    fun observeRecords(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records ORDER BY updatedAt DESC, title COLLATE NOCASE")
    fun observeAllRecords(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE isSaved = 1 AND isArchived = 0 ORDER BY updatedAt DESC, title COLLATE NOCASE")
    fun observeSaved(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE isArchived = 0 ORDER BY updatedAt DESC, title COLLATE NOCASE")
    suspend fun getRecords(): List<RecordEntity>

    @Query("SELECT * FROM records ORDER BY updatedAt DESC, title COLLATE NOCASE")
    suspend fun getAllRecords(): List<RecordEntity>

    @Query("SELECT * FROM records WHERE id = :id LIMIT 1")
    suspend fun getRecord(id: String): RecordEntity?

    @Query("UPDATE records SET isSaved = :saved WHERE id = :id")
    suspend fun setSaved(id: String, saved: Boolean)

    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE")
    fun observePeople(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people")
    suspend fun getPeople(): List<PersonEntity>

    @Query("SELECT * FROM institutions ORDER BY name COLLATE NOCASE")
    fun observeInstitutions(): Flow<List<InstitutionEntity>>

    @Query("SELECT * FROM institutions")
    suspend fun getInstitutions(): List<InstitutionEntity>

    @Query("SELECT * FROM events ORDER BY date")
    fun observeEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY date")
    suspend fun getEvents(): List<EventEntity>

    @Query("SELECT * FROM sources ORDER BY name COLLATE NOCASE")
    fun observeSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources")
    suspend fun getSources(): List<SourceEntity>

    @Query("SELECT * FROM monitoring WHERE id = 'state' LIMIT 1")
    suspend fun getMonitoring(): MonitoringEntity?

    @Query("SELECT * FROM monitoring")
    fun observeMonitoring(): Flow<List<MonitoringEntity>>

    @Query("SELECT * FROM collection_meta WHERE id = 'main' LIMIT 1")
    suspend fun getMeta(): CollectionMetaEntity?

    @Query("SELECT * FROM collection_meta WHERE id = 'main' LIMIT 1")
    fun observeMeta(): Flow<CollectionMetaEntity?>

    @Query("SELECT * FROM operation_log ORDER BY occurredAt DESC LIMIT 20")
    fun observeLogs(): Flow<List<OperationLogEntity>>

    @Upsert suspend fun upsertRecords(items: List<RecordEntity>)
    @Upsert suspend fun upsertPeople(items: List<PersonEntity>)
    @Upsert suspend fun upsertInstitutions(items: List<InstitutionEntity>)
    @Upsert suspend fun upsertEvents(items: List<EventEntity>)
    @Upsert suspend fun upsertSources(items: List<SourceEntity>)
    @Upsert suspend fun upsertMonitoring(item: MonitoringEntity)
    @Upsert suspend fun upsertMeta(item: CollectionMetaEntity)
    @Upsert suspend fun upsertLog(item: OperationLogEntity)

    @Query("DELETE FROM records") suspend fun clearRecords()
    @Query("DELETE FROM people") suspend fun clearPeople()
    @Query("DELETE FROM institutions") suspend fun clearInstitutions()
    @Query("DELETE FROM events") suspend fun clearEvents()
    @Query("DELETE FROM sources") suspend fun clearSources()
    @Query("DELETE FROM monitoring") suspend fun clearMonitoring()
}

@Database(
    entities = [
        RecordEntity::class,
        PersonEntity::class,
        InstitutionEntity::class,
        EventEntity::class,
        SourceEntity::class,
        MonitoringEntity::class,
        CollectionMetaEntity::class,
        OperationLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class RadarDatabase : RoomDatabase() {
    abstract fun dao(): RadarDao

    companion object {
        @Volatile private var instance: RadarDatabase? = null

        fun get(context: Context): RadarDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                RadarDatabase::class.java,
                "radar-nacional.db"
            ).build().also { instance = it }
        }
    }
}
