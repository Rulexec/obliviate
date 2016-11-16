package ruliov.obliviate.db

data class WordWithTranslation(
        val wordId: Long,
        val word: String,

        val translationId: String,
        val translation: String)