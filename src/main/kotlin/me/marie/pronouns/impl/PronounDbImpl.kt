package me.marie.pronouns.impl

import com.google.gson.JsonObject
import me.marie.pronouns.util.*
import me.owdding.ktmodules.Module
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.time.TickEvent
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

@Module
@OptIn(ExperimentalUuidApi::class)
object PronounDbImpl {
    val CACHE_TIMEOUT = 60.minutes
    val cache = mutableMapOf<Uuid, Pair<List<Pronouns>, Instant>>()

    val REQUEST_INTERVAL = 5.seconds
    var lastRequest: Instant = Instant.DISTANT_PAST

    val playersToRequest = mutableSetOf<Uuid>()

    @Subscription(TickEvent::class)
    fun onTick() {
        if (playersToRequest.isEmpty()) return
        if (lastRequest.passedSince() < REQUEST_INTERVAL) return

        lastRequest = currentInstant()

        val uuidsToRequest = playersToRequest.take(50)
        playersToRequest.removeAll(uuidsToRequest.toSet())
        getData(uuidsToRequest)
    }

    fun getPronounExtensionComponent(uuid: UUID) = getPronounExtensionComponent(uuid.toKotlinUuid())

    fun getPronounExtensionComponent(uuid: Uuid): Component {
        var pronouns = getPronouns(uuid)
        if (pronouns.isEmpty()) pronouns = listOf(Pronouns.ASK)

        val displayNames = pronouns.joinToString(", ") { it.displayName }
        return Component.literal(" ($displayNames)").withColor(0xFFAAAAAA.toInt())
    }

    fun getPronouns(uuid: UUID) = getPronouns(uuid.toKotlinUuid())

    fun getPronouns(uuid: Uuid): List<Pronouns> {
        val cached = cache[uuid] ?: return listOf(Pronouns.ASK)
        if (cached.second.passedSince() >= CACHE_TIMEOUT) {
            cache.remove(uuid)
            return listOf(Pronouns.ASK)
        }

        return cached.first
    }

    fun isCached(uuid: UUID) = isCached(uuid.toKotlinUuid())

    fun isCached(uuid: Uuid): Boolean {
        val cached = cache[uuid] ?: return false
        return cached.second.passedSince() < CACHE_TIMEOUT
    }

    fun getData(uuids: List<Uuid>) {
        Multithreading.launch {
            val result = RequestUtil.fetchJson<JsonObject>(buildRequestUrl(uuids))
            val data = result.getOrNull() ?: run {
                modMessage("Failed to fetch pronoun data for ${uuids.size} UUID(s) (see log for details)")
                return@launch
            }

            val now = currentInstant()
            val receivedUuids = mutableSetOf<Uuid>()

            data.entrySet().forEach { entry ->
                val uuid = try {
                    Uuid.parse(entry.key)
                } catch (e: Exception) {
                    println("Failed to parse UUID ${entry.key} from PronounDB response: ${e.message}")
                    return@forEach
                }

                val pronouns = try {
                    entry.value.asJsonObject
                        .getAsJsonObject("sets")
                        ?.getAsJsonArray("en")
                        ?.mapNotNull { pronoun ->
                            val pronounStr = pronoun.asString
                            Pronouns.entries.find { it.name.equals(pronounStr, ignoreCase = true) }
                        } ?: emptyList()
                } catch (e: Exception) {
                    println("Failed to parse pronouns for UUID $uuid: ${e.message}")
                    emptyList()
                }

                cache[uuid] = Pair(pronouns.ifEmpty { listOf(Pronouns.ASK) }, now)
                receivedUuids.add(uuid)
            }

            val missingUuids = uuids.filterNot { it in receivedUuids }
            missingUuids.forEach { uuid ->
                cache[uuid] = Pair(listOf(Pronouns.ASK), now)
            }

            val foundCount = receivedUuids.size
            val missingCount = missingUuids.size
            val totalCount = uuids.size

            println("Fetched pronoun data for $foundCount/$totalCount UUID(s) from PronounDB")
            if (missingCount > 0) {
                println("Cached $missingCount UUID(s) as ASK (not found in PronounDB)")
            }

            val message = if (missingCount > 0) {
                "Fetched $foundCount/$totalCount pronouns ($missingCount not found)"
            } else {
                "Fetched $foundCount pronoun(s)"
            }
            println(message)
        }
    }

    private fun buildRequestUrl(uuids: List<Uuid>, platform: String = "minecraft"): String {
        return "https://pronoundb.org/api/v2/lookup?platform=$platform&ids=${uuids.joinToString(",") { it.toString() }}"
    }

    enum class Pronouns(val displayName: String) {
        SHE("she/her"),
        HE("he/him"),
        IT("it/its"),
        THEY("they/them"),
        ANY("any"),
        ASK("ask"),
        AVOID("name"),
        OTHER("other"),
        ;
    }

}

/**
 * Looks up the data saved in PronounDB for one or more (up to 50) account for a given platform.
 *
 * The response is a map of IDs to the corresponding data. If an ID is not in our database, it will not be present in the response.
 *
 * It is strongly recommended to fetch IDs in bulk when possible and applicable, to help prevent hitting and potential rate limits.
 * Request types
 *
 * type RequestParams = {
 * 	// See below for a list of supported platforms
 * 	platform: string
 * 	// Items are separated by ",". Example: 13,27,31
 * 	ids: string[]
 * }
 *
 * type ResponseBody = {
 * 	[userId: string]: {
 * 		// See below for a list of supported sets
 * 		sets: {
 * 			// See below for a list of supported locales
 * 			[locale: string]: string[]
 * 		}
 * 	}
 * }
 *
 * Example request
 *
 * GET /api/v2/lookup?platform=discord&ids=94762492923748352,246652610747039745
 *
 * Example response
 *
 * HTTP/2 200 OK
 * Content-Type: application/json
 *
 * {
 * 	"94762492923748352": {
 * 		"sets": {
 * 			"en": [
 * 				"she",
 * 				"it"
 * 			]
 * 		}
 * 	}
 * }
 *
 * en locale
 *     Nominative
 *         he: he/him
 *         it: it/its
 *         she: she/her
 *         they: they/them
 *     Meta sets
 *         any: Any pronouns
 *         ask: Ask me my pronouns
 *         avoid: Avoid pronouns, use my name
 *         other: Other pronouns
 *
 *
 */