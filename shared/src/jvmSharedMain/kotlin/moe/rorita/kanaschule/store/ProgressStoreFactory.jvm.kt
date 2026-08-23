package moe.rorita.kanaschule.store

actual fun defaultProgressStore(): ProgressStore = FileProgressStore()
