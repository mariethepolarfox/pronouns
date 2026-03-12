package me.marie.pronouns.impl

import com.google.gson.JsonObject
import me.marie.pronouns.PronounDbIntegration
import me.marie.pronouns.util.Multithreading
import me.marie.pronouns.util.RequestUtil
import me.marie.pronouns.util.sendPrefixed
import me.owdding.ktmodules.Module
import me.owdding.lib.rendering.text.builtin.GradientTextShader
import me.owdding.lib.rendering.text.textShader
import net.minecraft.network.chat.Component
import org.apache.commons.lang3.time.DurationUtils.since
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.time.TickEvent
import tech.thatgravyboat.skyblockapi.utils.extentions.currentInstant
import tech.thatgravyboat.skyblockapi.utils.extentions.since
import tech.thatgravyboat.skyblockapi.utils.json.getPath
import tech.thatgravyboat.skyblockapi.utils.text.CommonText
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.Text.send
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

@Module
object PronounDbImpl {
    val CACHE_TIMEOUT = 60.minutes
    val cache = mutableMapOf<Uuid, PronounData>()

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
        val decoration = getDecoration(uuid) ?: Decoration.NONE
        val gradient = GradientTextShader(decoration.colors)

        val displayNames = pronouns.map {
            val debug = if (PronounDbIntegration.debug) Text.of(" (${cache[uuid]?.since()?.inWholeSeconds ?: "N/A"}s old)") else CommonText.EMPTY
            Text.of(it.displayName) {
                textShader = gradient
                append(debug)
            }
        }
        return Text.join(displayNames, separator = Text.of(", ", 0xFFAAAAAA.toInt()))
    }

    fun getPronouns(uuid: UUID) = getPronouns(uuid.toKotlinUuid())
    fun getPronouns(uuid: Uuid): List<Pronouns>? {
        val cached = cache[uuid] ?: return null
        if (cached.since() >= CACHE_TIMEOUT) {
            cache.remove(uuid)
            return null
        }

        return cached.pronouns
    }

    fun getDecoration(uuid: UUID) = getDecoration(uuid.toKotlinUuid())
    fun getDecoration(uuid: Uuid): Decoration? {
        val cached = cache[uuid] ?: return null
        if (cached.since() >= CACHE_TIMEOUT) {
            cache.remove(uuid)
            return null
        }

        return cached.decoration
    }

    fun isCached(uuid: UUID) = isCached(uuid.toKotlinUuid())

    fun isCached(uuid: Uuid): Boolean {
        val cached = cache[uuid] ?: return false
        return cached.since() < CACHE_TIMEOUT
    }

    fun getData(uuids: List<Uuid>) {
        Multithreading.launch {
            val result = RequestUtil.fetchJson<JsonObject>(buildRequestUrl(uuids))
            val data = result.getOrNull() ?: run {
                "Failed to fetch pronoun data for ${uuids.size} UUID(s) (see log for details)".sendPrefixed()
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

                val pronounData = try {
                    PronounData.fromJson(entry.value.asJsonObject)
                } catch (e: Exception) {
                    println("Failed to parse pronoun data for UUID $uuid from PronounDB response: ${e.message}")
                    return@forEach
                }

                cache[uuid] = pronounData
                receivedUuids.add(uuid)
            }

            val missingUuids = uuids.filterNot { it in receivedUuids }
            missingUuids.forEach { uuid ->
                cache[uuid] = PronounData(listOf(Pronouns.UNKNOWN), Decoration.NONE, now)
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

    data class PronounData(val pronouns: List<Pronouns>, val decoration: Decoration, val lastUpdated: Instant) {
        fun since(): Duration = lastUpdated.since()

        companion object {
            fun fromJson(json: JsonObject): PronounData {
                val pronouns = json.getPath("sets.en")?.asJsonArray?.mapNotNull { pronoun ->
                    val pronounStr = pronoun.asString
                    Pronouns.entries.find { it.name.equals(pronounStr, ignoreCase = true) }
                } ?: emptyList()

                val decoration = json.getPath("decoration")?.asString?.let { deco ->
                    Decoration.entries.find { it.name.equals(deco, ignoreCase = true) }
                } ?: Decoration.NONE

                return PronounData(pronouns.ifEmpty { listOf(Pronouns.UNKNOWN) }, decoration, currentInstant())
            }
        }
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

    enum class Decoration(vararg colors: Int) {
        COGS(0xc3591d),
        COOKIE(0xda9f83),
        DAYTIME(0xffac33),
        NIGHTTIME(0x66757f),
        PRIDE(0xF47C7C, 0xFFC268, 0xF7F48B, 0xA1DE93, 0x70A1D7, 0x957DAD, 0xF47C7C),
        PRIDE_BI(0xD872AC, 0xD872AC, 0x957DAD, 0x6AA9ED, 0x6AA9ED, 0xD872AC),
        PRIDE_LESBIAN(0xEB765A, 0xFBAB74, 0xFFFFFF, 0xF295CC, 0xA36088, 0xEB765A),
        PRIDE_PAN(0xFF82B1, 0xF7F48B, 0x8BD1F9, 0xFF82B1),
        PRIDE_TRANS(0x55CDFC, 0xF7A8B8, 0xFFFFFF, 0xF7A8B8, 0x55CDFC),
        CATGIRL_CHIEF(0xF49898),
        DF_KANIN(0xE08C73),
        DF_PLUME(0xBAD9B5),
        DONATOR_AURORA(0x18F89A, 0xC243EE, 0x18F89A),
        DONATOR_BLOSSOM(0xF4ABBA),
        DONATOR_RIBBON(0xDD2E44),
        DONATOR_STAR(0xFDD264),
        DONATOR_STRAWBERRY(0xBE1931),
        DONATOR_WARMTH(0xFDD264, 0xEB5353, 0xFDD264),
        NONE(0xAAAAAA);

        val colors = colors.toList()
        val id = name.lowercase()
    }

}
