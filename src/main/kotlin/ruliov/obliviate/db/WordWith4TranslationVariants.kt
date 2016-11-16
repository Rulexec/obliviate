package ruliov.obliviate.db

data class WordWith4TranslationVariants(
    val wordId: Long,
    val word: String,
    val variants: Array<Pair<String, String>> // id:translation
)