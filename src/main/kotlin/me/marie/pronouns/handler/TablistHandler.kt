package me.marie.pronouns.handler

import me.owdding.ktmodules.Module
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.info.TabListChangeEvent
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import kotlin.uuid.ExperimentalUuidApi

@Module
@OptIn(ExperimentalUuidApi::class)
object TablistHandler {
    private val playerRegex = "\\[(?<level>\\d+)] (?<name>[\\w_-]+).*".toRegex()

    private var cachedTablist = emptyList<List<String>>()

    private fun getPlayers() {
        val playerNames = cachedTablist.asSequence()
            .flatten()
            .mapNotNull { playerRegex.matchEntire(it)?.groups?.get("name")?.value }
            .distinct()

        val playerProfiles = playerNames.mapNotNull {
            McClient.connection?.getPlayerInfo(it)?.profile
        }.toList()

        if (playerProfiles.isNotEmpty()) {
            PlayerHandler.onTablistUpdate(playerProfiles)
        }
    }

    @Subscription
    fun onTablistUpdate(event: TabListChangeEvent) {
        val new = event.new.map { section ->
            section.map { comp ->
                comp.stripped
            }
        }

        if (cachedTablist == new) return

        cachedTablist = new
        getPlayers()
    }
}