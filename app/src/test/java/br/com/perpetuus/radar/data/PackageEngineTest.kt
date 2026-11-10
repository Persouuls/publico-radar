package br.com.perpetuus.radar.data

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageEngineTest {
    private fun emptySnapshotPayload() = buildJsonObject {
        put("records", buildJsonArray {})
        put("people", buildJsonArray {})
        put("institutions", buildJsonArray {})
        put("events", buildJsonArray {})
        put("sources", buildJsonArray {})
        put("monitoring", buildJsonArray {
            add(buildJsonObject {
                put("id", "state")
                put("source_checkpoints", buildJsonArray {})
                put("record_states", buildJsonArray {})
                put("coverage_gaps", buildJsonArray {})
                put("pending_review", buildJsonArray {})
            })
        })
    }

    @Test fun canonicalizationSortsObjectKeysAndPreservesArrayOrder() {
        val payload = buildJsonObject {
            put("z", 1)
            put("a", buildJsonArray { add(3); add(2) })
        }
        assertEquals("{\"a\":[3,2],\"z\":1}", PackageEngine.canonicalize(payload))
    }

    @Test fun canonicalizationNormalizesPythonCompatibleNumbers() {
        val value = kotlinx.serialization.json.Json.parseToJsonElement("""{"whole":5.0,"small":1e-5,"boundary":0.0001,"decimal":1.25}""")
        assertEquals("""{"boundary":0.0001,"decimal":1.25,"small":1e-05,"whole":5}""", PackageEngine.canonicalize(value))
    }

    @Test fun validFirstSnapshotIsAcceptedAtVersionZero() {
        val raw = PackageEngine.buildPackage("snapshot", 0, 1, emptySnapshotPayload())
        val preview = PackageEngine.inspect(raw, currentVersion = 0, existingRecordIds = emptySet())
        assertTrue(preview.errors.joinToString(), preview.canImport)
        assertEquals(1, preview.dataVersion)
        assertEquals(0, preview.recordCount)
    }

    @Test fun changingPayloadWithoutUpdatingHashIsRejected() {
        val payload = buildJsonObject {
            put("records", buildJsonArray {})
            put("people", buildJsonArray {})
            put("institutions", buildJsonArray {})
            put("events", buildJsonArray {})
            put("sources", buildJsonArray {})
            put("monitoring", buildJsonArray {
                add(buildJsonObject { put("id", "state"); put("note", "original") })
            })
        }
        val raw = PackageEngine.buildPackage("snapshot", 0, 1, payload).replace("original", "alterado")
        val preview = PackageEngine.inspect(raw, 0, emptySet())
        assertFalse(preview.canImport)
        assertTrue(preview.errors.any { it.contains("checksum", ignoreCase = true) })
    }

    @Test fun worksetIsNeverImportable() {
        val payload = buildJsonObject {
            put("monitoring_state", buildJsonObject { put("id", "state") })
            put("records_to_review", buildJsonArray {})
            put("record_index", buildJsonArray {})
            put("people_index", buildJsonArray {})
            put("institutions_index", buildJsonArray {})
            put("source_index", buildJsonArray {})
            put("batch", buildJsonObject {})
        }
        val raw = PackageEngine.buildPackage("workset", 0, 0, payload)
        val preview = PackageEngine.inspect(raw, 0, emptySet())
        assertFalse(preview.canImport)
        assertTrue(preview.errors.any { it.contains("não pode ser importado") })
    }

    @Test fun worksetKeepsArchivedRecordsInIndexButDoesNotSelectThemForReview() {
        fun record(id: String, archived: Boolean) = RecordEntity(
            id = id,
            type = "proposition",
            title = id,
            summary = "Resumo de teste",
            isArchived = archived,
            rawJson = """{"id":"$id","type":"proposition","title":"$id","summary":"Resumo de teste"}"""
        )
        val payload = PackageEngine.worksetPayload(
            records = listOf(record("ACTIVE", false), record("ARCHIVED", true)),
            people = emptyList(), institutions = emptyList(), sources = emptyList(), monitoring = null
        )
        val review = payload.getValue("records_to_review").jsonArray
        val index = payload.getValue("record_index").jsonArray
        assertEquals(1, review.size)
        assertEquals("ACTIVE", review.single().jsonObject.getValue("id").jsonPrimitive.content)
        assertEquals(2, index.size)
        assertTrue(index.any { it.jsonObject.getValue("id").jsonPrimitive.content == "ARCHIVED" })
        assertTrue(index.first { it.jsonObject.getValue("id").jsonPrimitive.content == "ARCHIVED" }
            .jsonObject.getValue("is_archived").jsonPrimitive.content.toBoolean())
    }

    @Test fun deltaFromDifferentBaseVersionIsRejected() {
        val payload = buildJsonObject { put("operations", buildJsonArray {}) }
        val raw = PackageEngine.buildPackage("delta", 2, 3, payload)
        val preview = PackageEngine.inspect(raw, 1, emptySet())
        assertFalse(preview.canImport)
        assertTrue(preview.errors.any { it.contains("Base divergente") })
    }
    @Test fun deltaAcceptsEventLinkedToRecordIncludedInSameBatch() {
        val record = buildJsonObject {
            put("id", "REC-1")
            put("type", "proposition")
            put("title", "Registro de teste")
            put("summary", "Registro sintético utilizado apenas em teste unitário.")
            put("axes", buildJsonArray {})
        }
        val event = buildJsonObject {
            put("id", "EV-1")
            put("record_id", "REC-1")
            put("date", "2026-11-08")
            put("label", "Movimentação de teste")
        }
        val payload = buildJsonObject {
            put("operations", buildJsonArray {
                add(buildJsonObject { put("op", "upsert"); put("table", "records"); put("id", "REC-1"); put("data", record) })
                add(buildJsonObject { put("op", "upsert"); put("table", "events"); put("id", "EV-1"); put("data", event) })
            })
        }
        val raw = PackageEngine.buildPackage("delta", 0, 1, payload)
        val preview = PackageEngine.inspect(raw, currentVersion = 0, existingRecordIds = emptySet())
        assertTrue(preview.errors.joinToString(), preview.canImport)
        assertEquals(1, preview.recordCount)
    }

    @Test fun deltaRejectsEventLinkedToUnknownRecord() {
        val event = buildJsonObject {
            put("id", "EV-ORPHAN")
            put("record_id", "MISSING-RECORD")
            put("date", "2026-11-08")
            put("label", "Evento órfão")
        }
        val payload = buildJsonObject {
            put("operations", buildJsonArray {
                add(buildJsonObject { put("op", "upsert"); put("table", "events"); put("id", "EV-ORPHAN"); put("data", event) })
            })
        }
        val raw = PackageEngine.buildPackage("delta", 0, 1, payload)
        val preview = PackageEngine.inspect(raw, currentVersion = 0, existingRecordIds = emptySet())
        assertFalse(preview.canImport)
        assertTrue(preview.errors.any { it.contains("registro desconhecido") })
    }

}
