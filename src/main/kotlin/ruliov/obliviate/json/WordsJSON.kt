package ruliov.obliviate.json

import ruliov.obliviate.db.WordWith4TranslationVariants

fun WordWith4TranslationVariants.toJSON(): String {
    // {"wordId": 42, "word": "word", "choices": [{"id": 1, "value": "кошка"}, ... 3 more]}

    val choices = java.lang.String.join(
            ",",
            this.variants.map { "{\"id\":\"${it.first}\",\"value\":\"${it.second}\"}" })

    return "{\"wordId\":${this.wordId},\"word\":\"${this.word}\",\"choices\":[$choices]}"
}