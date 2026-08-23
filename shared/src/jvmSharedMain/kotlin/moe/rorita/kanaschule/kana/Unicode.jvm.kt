package moe.rorita.kanaschule.kana

import java.text.Normalizer

internal actual fun nfkc(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)
