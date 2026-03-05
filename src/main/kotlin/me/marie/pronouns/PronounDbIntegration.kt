package me.marie.pronouns

import me.marie.pronouns.generated.PronounsModules
import net.fabricmc.api.ModInitializer
import net.minecraft.client.Minecraft
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object PronounDbIntegration : ModInitializer {
	val logger: Logger = LogManager.getLogger("PronounDbIntegration")

	override fun onInitialize() {
		logger.info("Meow :3")

		PronounsModules.init {
			SkyBlockAPI.eventBus.register(it)
		}
	}
}