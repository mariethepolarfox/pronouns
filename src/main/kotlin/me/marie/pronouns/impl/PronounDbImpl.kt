package me.marie.pronouns.impl

import com.google.gson.JsonObject
import me.marie.pronouns.PronounDbIntegration
import me.marie.pronouns.util.*
import me.owdding.ktmodules.Module
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.time.TickEvent
import tech.thatgravyboat.skyblockapi.utils.extentions.currentInstant
import tech.thatgravyboat.skyblockapi.utils.extentions.since
import tech.thatgravyboat.skyblockapi.utils.text.CommonText
import tech.thatgravyboat.skyblockapi.utils.text.Text
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

@Module
object PronounDbImpl {
    val CACHE_TIMEOUT = 60.minutes
    val cache = mutableMapOf<Uuid, Pair<List<Pronouns>, Instant>>()

    val REQUEST_INTERVAL = 5.seconds
    var lastRequest: Instant = Instant.DISTANT_PAST

    val playersToRequest = mutableSetOf<Uuid>()

    @Subscription(TickEvent::class)
    fun onTick() {
        if (playersToRequest.isEmpty()) return
        if (lastRequest.since() < REQUEST_INTERVAL) return

        lastRequest = currentInstant()

        val uuidsToRequest = playersToRequest.take(50)
        playersToRequest.removeAll(uuidsToRequest.toSet())
        getData(uuidsToRequest)
    }

    fun getPronounExtensionComponent(uuid: UUID) = getPronounExtensionComponent(uuid.toKotlinUuid())

    fun getPronounExtensionComponent(uuid: Uuid): Component? {
        val pronouns = getPronouns(uuid).takeUnless { it == listOf(Pronouns.UNKNOWN) } ?: return null

        val displayNames = pronouns.map {
            val debug =
                if (PronounDbIntegration.debug) Text.of(" (${cache[uuid]?.second?.since()?.inWholeSeconds ?: "N/A"}s old)") else CommonText.EMPTY
            Text.of(it.displayName, 0xFFAAAAAA.toInt()).append(debug)
        }
        return Text.join(displayNames, separator = Text.of(", ", 0xFFAAAAAA.toInt()))
    }

    fun getPronouns(uuid: UUID) = getPronouns(uuid.toKotlinUuid())

    fun getPronouns(uuid: Uuid): List<Pronouns>? {
        val cached = cache[uuid] ?: return null
        if (cached.second.since() >= CACHE_TIMEOUT) {
            cache.remove(uuid)
            return null
        }

        return cached.first
    }

    fun isCached(uuid: UUID) = isCached(uuid.toKotlinUuid())

    fun isCached(uuid: Uuid): Boolean {
        val cached = cache[uuid] ?: return false
        return cached.second.since() < CACHE_TIMEOUT
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

                cache[uuid] = Pair(pronouns.ifEmpty { listOf(Pronouns.UNKNOWN) }, now)
                receivedUuids.add(uuid)
            }

            val missingUuids = uuids.filterNot { it in receivedUuids }
            missingUuids.forEach { uuid ->
                cache[uuid] = Pair(listOf(Pronouns.UNKNOWN), now)
            }

            val foundCount = receivedUuids.size
            val missingCount = missingUuids.size
            val totalCount = uuids.size

            println("Fetched pronoun data for $foundCount/$totalCount UUID(s) from PronounDB")
            if (missingCount > 0) {
                println("Cached $missingCount UUID(s) as UNKNOWN (not found in PronounDB)")
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
        UNKNOWN(""),
        ;
    }

}
