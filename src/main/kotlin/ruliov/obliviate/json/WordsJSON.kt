package ruliov.obliviate.json

import ruliov.obliviate.db.WordWith4TranslationVariants
import ruliov.obliviate.db.WordWithTranslation

fun WordWith4TranslationVariants.toJSON(): String {
    // {"wordId": 42, "word": "word", "choices": [{"id": 1, "value": "кошка"}, ... 3 more]}

    val choices = java.lang.String.join(
            ",",
            this.variants.map { "{\"id\":\"${it.first}\",\"value\":\"${it.second}\"}" })

    return "{\"wordId\":${this.wordId},\"word\":\"${this.word}\",\"choices\":[$choices]}"
}

fun WordWithTranslation.toCompactJSON(): String {
    return "[${this.wordId},\"${this.word}\",\"${this.translation}\"]"
}

fun List<WordWithTranslation>.toCompactJSON(): String {
    val sb = StringBuilder()

    sb.append('[')

    this.forEach { sb.append(it.toCompactJSON()).append(',') }

    sb.replace(sb.length - 1, sb.length, "]")

    return sb.toString()
}