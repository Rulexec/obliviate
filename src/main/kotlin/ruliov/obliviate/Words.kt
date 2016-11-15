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
        Pair("ominous", "угрожающий / зловещий"),
        Pair("gurgle", "бульканье"),
        Pair("miserable", "несчастный"),
        Pair("irritate", "разражать / нервировать"),
        Pair("interfere", "вмешиваться / мешать"),
        Pair("obvious", "очевидный"),
        Pair("deflate", "выкачивать / спускать"),
        Pair("reluctant", "неохотный / вынужденный"),
        Pair("refute", "опровергать"),
        Pair("allegation", "голословное заявление"),
        Pair("obsess", "преследовать"),
        Pair("grieve", "горевать"),
        Pair("accuse", "обвинять"),
        Pair("droopy", "упавший духом"),
        Pair("wither", "увядать / сохнуть"),
        Pair("disguised", "замаскированный"),
        Pair("burp", "отрыжка"),
        Pair("residue", "остаток"),
        Pair("rack", "стеллаж / рама"),
        Pair("furious", "яростный"),
        Pair("profound", "глубокий / проникновенный"),
        Pair("sole", "единственный / подошва"),
        Pair("ponder", "обдумывать"),
        Pair("slip", "скользить"),
        Pair("kerfuffle", "суматоха"),
        Pair("firmly", "твёрдо"),
        Pair("dwell", "обитать / жить"),
        Pair("hip", "бедро"),
        Pair("insist", "настаивать"),
        Pair("relent", "смягчаться"),
        Pair("stink", "вонь"),
        Pair("pellet", "гранула"),
        Pair("puckish", "плутовской"),
        Pair("affectionate", "любящий"),
        Pair("elude", "ускользать"),
        Pair("effusively", "экспансивно"),
        Pair("entwined", "переплетены"),
        Pair("fright", "испуг"),
        Pair("chase", "гнаться / преследовать"),
        Pair("carriage", "вагон / экипаж"),
        Pair("contrary", "противоположность"),
        Pair("peppermint", "мятный"),
        Pair("fair", "справедливый"),
        Pair("mild", "мягкий / умеренный"),
        Pair("affront", "оскорбление"),
        Pair("desperate", "отчаянный"),
        Pair("heir", "наследник"),
        Pair("fierce", "свирепый / лютый"),
        Pair("quite", "довольно / вполне"),
        Pair("twist", "поворот / кручение"),
        Pair("bit", "немного / кусочек"),
        Pair("delight", "восторг / наслаждение"),
        Pair("discombobulate", "смущать/сбивать с толку"),
        Pair("amuse", "забавлять / развлекать"),
        Pair("proffer", "предложение / сделка"),
        Pair("dithering", "дрожать / трепетать"),
        Pair("concern", "забота / интерес"),
        Pair("relentless", "безжалостный / неумолимый"),
        Pair("pretense", "притворство"),
        Pair("melting", "плавление / таяние")
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