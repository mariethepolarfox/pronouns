package me.marie.pronouns.handler

import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.vertex.PoseStack
import me.marie.pronouns.PronounDbIntegration
import me.marie.pronouns.impl.PronounDbImpl
import me.marie.pronouns.util.sendPrefixed
import me.owdding.ktmodules.Module
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.state.CameraRenderState
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.hypixel.ServerChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent
import tech.thatgravyboat.skyblockapi.api.events.time.TickEvent
import tech.thatgravyboat.skyblockapi.helpers.McLevel
import tech.thatgravyboat.skyblockapi.utils.extentions.currentInstant
import tech.thatgravyboat.skyblockapi.utils.extentions.since
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@Module
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

    fun renderNameTagExtension(
        renderState: EntityRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        cameraState: CameraRenderState,
    ) {
        val renderState = renderState as? AvatarRenderState ?: return
        val player = renderState.getData(PronounDbIntegration.ENTITY_DATA_KEY) as? AbstractClientPlayer ?: return
        val id = player.gameProfile.id

        if (id.version() != 4) return

        val comp = PronounDbImpl.getPronounExtensionComponent(id) ?: return

        poseStack.pushPose()
        poseStack.translate(0f, 9.0f * (if (renderState.scoreText != null) 2 else 1) * 1.15f * 0.025f, 0f)
        collector.submitNameTag(
            poseStack,
            renderState.nameTagAttachment,
            if (player.showExtraEars()) -10 else 0,
            comp,
            !renderState.isDiscrete,
            renderState.lightCoords,
            renderState.distanceToCameraSq,
            cameraState,
        )
        poseStack.popPose()
    }

    @Subscription(TickEvent::class)
    fun onTick() {
        queueUncachedPlayers(McLevel.players.map { it.gameProfile })
    }

    @Subscription
    fun onWorldJoin(event: ServerChangeEvent) {
        PronounDbImpl.lastRequest = currentInstant()
    }

    @Subscription
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        event.register("pronouns") {
            thenCallback("printcache") {
                "Cached: ${PronounDbImpl.cache.size}, last request: ${PronounDbImpl.lastRequest.since().inWholeSeconds}s ago".sendPrefixed()
                PronounDbImpl.cache.forEach { (uuid, data) ->
                    println("UUID: $uuid, Pronouns: ${data.pronouns.joinToString(", ")}, Decoration: ${data.decoration}, Cached for: ${data.since().inWholeSeconds} seconds")
                }
            }
            thenCallback("invalidateCache") {
                val size = PronounDbImpl.cache.size
                PronounDbImpl.cache.clear()
                "Cleared $size entries from the pronouns cache.".sendPrefixed()
            }
        }
    }
}
