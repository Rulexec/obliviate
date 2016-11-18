package ruliov.obliviate.db

import ruliov.async.IFuture
import ruliov.async.bindToErrorFuture
import ruliov.async.catch
import ruliov.async.createFuture
import ruliov.javadb.DBConnectionPool
import ruliov.javadb.IDBConnectionPool
import ruliov.toHexString
import java.sql.Connection
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

    private fun getConnectionAndCatch(handler: (Connection) -> IFuture<Any?>): IFuture<Any?> {
        return this.pool.getConnection().bindToErrorFuture { catch { it.use { handler(it.getConnection()) } } }
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

    fun resetDb(): IFuture<Any?> = this.getConnectionAndCatch {
        val statement = it.createStatement()
        statement.execute(RESET_DB_SQL)

        createFuture<Any?>(null)
    }

    fun deleteWord(id: Long): IFuture<Any?> = this.getConnectionAndCatch {
        val ps = it.prepareCall(
            "WITH removedWord AS (DELETE FROM words WHERE id = ? RETURNING \"translationId\" AS tid)" +
            "DELETE FROM translations WHERE id IN (SELECT tid FROM removedWord)")
        ps.setLong(1, id)

        val rows = ps.executeUpdate()

        if (rows == 1) {
            // maybe we need to switch to trees, but not now
            // also, we can use binary search, if we will fetch sorted words from db
            var i = 0
            for (word in this.wordsWithTranslations) {
                if (word.wordId == id) {
                    this.wordsWithTranslations.removeAt(i)
                    this.wordById.remove(id)

                    break
                }

                i++
            }

            createFuture<Any?>(null)
        } else {
            createFuture("Nothing deleted: $rows")
        }
    }

    fun updateWord(id: Long, wordText: String, translation: String): IFuture<Any?> =
            this.getConnectionAndCatch {
        val ps = it.prepareCall(
            "UPDATE words SET word = ? WHERE id = ?;" +
            "UPDATE translations SET text = ? FROM words WHERE " +
                "translations.id = words.\"translationId\" AND words.id = ?;")

        ps.setString(1, wordText)
        ps.setLong(2, id)
        ps.setString(3, translation)
        ps.setLong(4, id)

        val rows = ps.executeUpdate()

        if (rows == 1) {
            val word = this.wordById[id]

            if (word != null) {
                word.word = wordText
                word.translation = translation
            } else {
                System.err.println("Word $id not in memory")
            }

            createFuture<Any?>(null)
        } else {
            createFuture("Nothing updated: $rows")
        }
    }

    fun getAllWords(): List<WordWithTranslation> {
        return this.wordsWithTranslations
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

                if (this.wordsWithTranslations.size < 4) {
                    // FIXME: just quickfix to prevent endless loop
                    throw Exception("It's a trap!")
                }
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