package me.marie.pronouns.util

import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.helpers.McClient

fun modMessage(message: String) {
    appendChatMessage(
        Component.literal("[PronounDB] ").withColor(0xFFAAAAAA.toInt())
            .append(
                Component.literal(message)
            )
    )
}

fun appendChatMessage(message: Component) {
    McClient.runOrNextTick {
        McClient.chat.addMessage(message)
    }
}