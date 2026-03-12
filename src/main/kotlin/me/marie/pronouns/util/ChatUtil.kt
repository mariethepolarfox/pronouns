package me.marie.pronouns.util

import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.Text.send

private val PREFIX = Text.of("[PronounDB] ", 0xFFAAAAAA.toInt())

fun String.sendPrefixed() = Text.of(this).sendPrefixed()
fun Component.sendPrefixed() = PREFIX.copy().append(this).send()
