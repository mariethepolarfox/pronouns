package me.marie.pronouns.handler

import com.mojang.authlib.GameProfile
import me.marie.pronouns.impl.PronounDbImpl
import me.marie.pronouns.util.currentInstant
import me.marie.pronouns.util.modMessage
import me.marie.pronouns.util.passedSince
import me.owdding.ktmodules.Module
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.hypixel.ServerChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent
import tech.thatgravyboat.skyblockapi.api.events.render.RenderHudEvent
import tech.thatgravyboat.skyblockapi.api.events.time.TickEvent
import tech.thatgravyboat.skyblockapi.helpers.McFont
import tech.thatgravyboat.skyblockapi.helpers.McLevel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@Module
@OptIn(ExperimentalUuidApi::class)
object PlayerHandler {

    private fun queueUncachedPlayers(profiles: List<GameProfile>) {
        val uncachedPlayers = profiles.asSequence()
            .map { it.id.toKotlinUuid() }
            .filter { uuid ->
                uuid.toJavaUuid().version() == 4 && !PronounDbImpl.isCached(uuid)
            }
            .toList()

        if (uncachedPlayers.isNotEmpty()) {
            PronounDbImpl.playersToRequest.addAll(uncachedPlayers)
        }
    }

    fun onTablistUpdate(profiles: List<GameProfile>) {
        queueUncachedPlayers(profiles)
    }

    @Subscription(TickEvent::class)
    fun onTick() {
        queueUncachedPlayers(McLevel.players.map { it.gameProfile })
    }

    @Subscription
    fun onWorldJoin(event: ServerChangeEvent) {
        PronounDbImpl.lastRequest = currentInstant()
    }

    /*@Subscription
    fun onRenderHud(event: RenderHudEvent) {
        val graphics = event.graphics

        val displayLines = listOf(
            "Loaded players: ${McLevel.players.size}",
            "Players to request: ${PronounDbImpl.playersToRequest.size}",
            "Cached players: ${PronounDbImpl.cache.size}",
            "Last request: ${PronounDbImpl.lastRequest.passedSince().inWholeMilliseconds}ms ago"
        )

        val x = graphics.guiWidth() / 2
        var y = 10
        displayLines.forEach {
            graphics.drawCenteredString(McFont.self, it, x, y, -1)
            y += McFont.height + 2
        }
    }*/

    @Subscription
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        event.register("pronouns") {
            thenCallback("printcache") {
                PronounDbImpl.cache.forEach { (uuid, pair) ->
                    println("UUID: $uuid, Pronouns: ${pair.first.joinToString(", ")}, Cached for: ${pair.second.passedSince().inWholeSeconds} seconds")
                }
            }
            thenCallback("invalidateCache") {
                val size = PronounDbImpl.cache.size
                PronounDbImpl.cache.clear()
                modMessage("Cleared $size entries from the pronouns cache.")
            }
        }
    }
}