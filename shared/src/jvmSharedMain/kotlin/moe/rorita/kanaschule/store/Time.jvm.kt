package moe.rorita.kanaschule.store

import java.time.Instant
import java.time.ZoneId

actual fun currentTimeMs(): Long = System.currentTimeMillis()

actual fun epochDayOf(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
