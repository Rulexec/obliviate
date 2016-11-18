package ruliov.obliviate.db

data class WordWithTranslation(
        val wordId: Long,
        var word: String,

        val translationId: String,
        var translation: String)