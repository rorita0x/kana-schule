package moe.rorita.kanaschule.store

expect fun currentTimeMs(): Long

/**
 * Tag seit Epoch in der lokalen Zeitzone. Tagesgrenzen (neue Zeichen pro Tag,
 * Tagesaggregate, Streak) muessen sich am Kalender des Nutzers orientieren,
 * nicht an UTC.
 */
expect fun epochDayOf(millis: Long): Long

fun currentEpochDay(): Long = epochDayOf(currentTimeMs())
