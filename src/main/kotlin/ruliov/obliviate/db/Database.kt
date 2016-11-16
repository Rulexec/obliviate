package ruliov.obliviate.db

import ruliov.async.IFuture
import ruliov.async.bindToErrorFuture
import ruliov.async.catch
import ruliov.async.createFuture
import ruliov.javadb.DBConnectionPool
import ruliov.javadb.IDBConnectionPool
import ruliov.toHexString
import java.util.*

class Database(jdbcDriver: String, dbUrl: String) {
    private val random: Random = Random()

    private val pool: IDBConnectionPool
    private val wordsWithTranslations = ArrayList<WordWithTranslation>()
    private val wordById = HashMap<Long, WordWithTranslation>()

    init {
        Class.forName(jdbcDriver)

        this.pool = DBConnectionPool(1, dbUrl)
    }

    fun loadData(): IFuture<Any?> {
        return this.pool.getConnection().bindToErrorFuture { catch { it.use {
            val connection = it.getConnection()

            val translationsMap = HashMap<String, String>()

            val statement = connection.createStatement()
            var resultSet = statement.executeQuery("SELECT id, text FROM translations")

            while (resultSet.next()) {
                val id = resultSet.getString(1)
                val text = resultSet.getString(2)

                translationsMap[id] = text
            }

            resultSet = statement.executeQuery("SELECT id, word, \"translationId\" FROM words")

            while (resultSet.next()) {
                val id = resultSet.getLong(1)
                val word = resultSet.getString(2)
                val translationId = resultSet.getString(3)

                val translation = translationsMap[translationId] ?: throw Exception("No translation for word $id")

                val wordWithTranslation = WordWithTranslation(id, word, translationId, translation)

                this.wordsWithTranslations.add(wordWithTranslation)
                this.wordById[id] = wordWithTranslation
            }

            createFuture<Any?>(null)
        } } }
    }

    fun getRandomWordWith4RandomTranslations(): WordWith4TranslationVariants {
        val wordId = this.random.nextInt(this.wordsWithTranslations.size)
        val wordWithTranslation = this.wordsWithTranslations[wordId]

        val usedVariants = HashSet<Int>()
        usedVariants.add(wordId)

        val correctVariantIndex = this.random.nextInt(4)

        val variants = Array(4, { i ->
            if (i == correctVariantIndex) {
                return@Array Pair(wordWithTranslation.translationId, wordWithTranslation.translation)
            }

            var rand: Int

            do {
                rand = this.random.nextInt(this.wordsWithTranslations.size)
            } while (rand in usedVariants)

            usedVariants.add(rand)

            Pair(this.wordsWithTranslations[rand].translationId, this.wordsWithTranslations[rand].translation)
        })

        return WordWith4TranslationVariants(
                this.wordsWithTranslations[wordId].wordId,
                this.wordsWithTranslations[wordId].word,
                variants)
    }

    fun getWordTranslationId(wordId: Long): String? {
        return this.wordById[wordId]?.translationId
    }
}