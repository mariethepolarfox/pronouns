package me.marie.pronouns

import me.marie.pronouns.generated.PronounsModules
import me.marie.pronouns.util.sendPrefixed
import me.owdding.ktmodules.Module
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.Version
import net.minecraft.world.entity.Entity
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent

@Module
object PronounDbIntegration : ModInitializer {
    const val MODID = "pronoundb"

    val logger: Logger = LogManager.getLogger("PronounDbIntegration")
    var debug = false

    val version: Version by lazy { FabricLoader.getInstance().getModContainer(MODID).orElseThrow().metadata.version }

    override fun onInitialize() {
        logger.info("Meow :3")

        PronounsModules.init {
            SkyBlockAPI.eventBus.register(it)
        }
    }

    @Subscription
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        event.register("pronouns") {
            thenCallback("debug") {
                debug = !debug
                "Toggled debug: ${!debug} -> $debug".sendPrefixed()
            }
        }
    }

    @JvmField
    val ENTITY_DATA_KEY: RenderStateDataKey<Entity> = RenderStateDataKey.create { "$MODID:entity_data_key" }
}
