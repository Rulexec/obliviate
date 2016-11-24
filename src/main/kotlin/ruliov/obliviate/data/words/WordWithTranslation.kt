package ruliov.obliviate.data.words

data class WordWithTranslation(
        val wordId: Long,
        var word: String,

        val translationId: String,
        var translation: String)