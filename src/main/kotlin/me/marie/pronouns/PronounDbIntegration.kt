package me.marie.pronouns

import me.marie.pronouns.generated.PronounsModules
import me.owdding.ktmodules.Module
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey
import net.minecraft.world.entity.Entity
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import kotlin.uuid.ExperimentalUuidApi

@Module
@OptIn(ExperimentalUuidApi::class)
object PronounDbIntegration : ModInitializer {
	val logger: Logger = LogManager.getLogger("PronounDbIntegration")
	var debug = false

	override fun onInitialize() {
		logger.info("Meow :3")

		PronounsModules.init {
			SkyBlockAPI.eventBus.register(it)
		}
	}

	@JvmField
	val ENTITY_DATA_KEY: RenderStateDataKey<Entity> = RenderStateDataKey.create { "pronounsdbimpl:entity_data_key" }
}