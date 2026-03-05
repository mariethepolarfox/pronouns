package me.marie.pronouns.util


import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

fun currentInstant() = Clock.System.now()

fun Instant.passedSince(): Duration = currentInstant() - this