package moe.rorita.kanaschule.store

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.kana.KanaTable

/** Ergebnis des Ladens. Ein kaputter Zustand wird nie stillschweigend erraten. */
sealed interface DecodeResult {
    data class Ok(val state: AppState) : DecodeResult

    /** Datei stammt aus einer neueren App-Version. */
    data class TooNew(val version: Int) : DecodeResult

    data class Broken(val reason: String) : DecodeResult
}

internal class Migration(val from: Int, val to: Int, val apply: (JsonObject) -> JsonObject)

object ProgressCodec {

    const val CURRENT_SCHEMA = 1

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    /**
     * Migriert wird auf dem rohen JsonObject, nicht ueber mitgeschleppte alte
     * Datenklassen. Das ist das Muster, das jenseits Version 3 wartbar bleibt.
     */
    private val migrations: List<Migration> = emptyList()

    fun encode(state: AppState): String =
        json.encodeToString(state.copy(schemaVersion = CURRENT_SCHEMA))

    fun decode(text: String): DecodeResult {
        if (text.isBlank()) return DecodeResult.Broken("leere Datei")

        var element = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            return DecodeResult.Broken(e.message ?: "kein gueltiges JSON")
        }

        var version = element["schemaVersion"]?.jsonPrimitive?.intOrNull ?: 1
        if (version > CURRENT_SCHEMA) return DecodeResult.TooNew(version)

        while (version < CURRENT_SCHEMA) {
            val migration = migrations.firstOrNull { it.from == version }
                ?: return DecodeResult.Broken("keine Migration von Version $version")
            element = migration.apply(element)
            version = migration.to
        }

        val state = try {
            json.decodeFromJsonElement<AppState>(element)
        } catch (e: Exception) {
            return DecodeResult.Broken(e.message ?: "Zustand passt nicht zum Schema")
        }

        return DecodeResult.Ok(dropUnknownItems(state))
    }

    fun encodeReview(entry: ReviewEntry): String = json.encodeToString(entry)

    fun decodeReview(line: String): ReviewEntry? =
        if (line.isBlank()) {
            null
        } else {
            try {
                json.decodeFromString<ReviewEntry>(line)
            } catch (e: Exception) {
                null
            }
        }

    /** Zeichen, die es im Datensatz nicht mehr gibt, verschwinden beim Laden. */
    private fun dropUnknownItems(state: AppState): AppState =
        state.copy(items = state.items.filterKeys { KanaTable.byId.containsKey(KanaId(it)) })
}
