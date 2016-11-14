package ruliov.obliviate

import java.util.*

object Words {
    data class WordWith4TranslationVariants(
            val wordId: Int, val word: String, val variants: Array<Pair<Int, String>>)

    private val random = Random()

    val pairs: Array<Pair<String, String>> = arrayOf(
        Pair("grapple", "захват / сцепиться"),
        Pair("refuse", "отказаться"),
        Pair("struggle", "борьба (усилия)"),
        Pair("fuse", "плавить / предохранитель"),
        Pair("ominous", "угрожающий / зловещий")
    )

    fun getRandomWordWith4RandomTranslations(): WordWith4TranslationVariants {
        val wordId = this.random.nextInt(this.pairs.size)

        val usedVariants = HashSet<Int>()
        usedVariants.add(wordId)

        val correctVariantIndex = this.random.nextInt(4)

        val variants = Array(4, { i ->
            if (i == correctVariantIndex) return@Array Pair(wordId + 1, this.pairs[wordId].second)

            var rand: Int

            do {
                rand = this.random.nextInt(this.pairs.size)
            } while (rand in usedVariants)

            usedVariants.add(rand)

            Pair(rand + 1, this.pairs[rand].second)
        })

        return WordWith4TranslationVariants(wordId + 1, this.pairs[wordId].first, variants)
    }
}