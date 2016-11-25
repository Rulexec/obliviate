package ruliov.obliviate.db

import ruliov.async.*
import ruliov.handleError
import ruliov.javadb.DBConnectionPool
import ruliov.javadb.IDBConnectionPool
import ruliov.obliviate.data.users.LoginedUser
import ruliov.obliviate.data.words.WordWith4TranslationVariants
import ruliov.obliviate.data.words.WordWithTranslation
import ruliov.toHexString
import java.sql.Connection
import java.util.*
import java.util.regex.Pattern

class Database(dbUrl: String) {
    private val random: Random = Random()

    private val pool: IDBConnectionPool
    private val wordsWithTranslations = ArrayList<WordWithTranslation>()
    private val wordById = HashMap<Long, WordWithTranslation>()

    private val timer = Timer(true)

    init {
        this.pool = DBConnectionPool(1, dbUrl)

        timer.schedule(object : TimerTask() {
            override fun run() {
                this@Database.getConnectionAndCatch {
                    it.prepareStatement(
                        """DELETE FROM sessions WHERE "expiresAt" < timezone('UTC', now())"""
                    ).execute()

                    createFuture<Any?>(null)
                }.run {
                    if (it != null) handleError(it)
                }
            }
        }, 5 * 60 * 1000, 60 * 60 * 1000)
    }

    private fun getConnectionAndCatch(handler: (Connection) -> IFuture<Any?>): IFuture<Any?> {
        return this.pool.getConnection().bindToErrorFuture { catchFuture { it.use { handler(it.getConnection()) } } }
    }

    fun loadData(): IFuture<Any?> {
        return this.pool.getConnection().bindToErrorFuture { catchFuture { it.use {
            val connection = it.getConnection()

            data class Translation(val id: String, val text: String)
            // wordId -> Translation
            val translationsMap = HashMap<Long, Translation>()

            val statement = connection.createStatement()
            var resultSet = statement.executeQuery("SELECT id, \"wordId\", text FROM translations")

            while (resultSet.next()) {
                val translationId = resultSet.getString(1)
                val wordId = resultSet.getLong(2)
                val text = resultSet.getString(3)

                translationsMap[wordId] = Translation(translationId, text)
            }

            resultSet = statement.executeQuery("SELECT id, text FROM words ORDER BY text")

            synchronized(this.wordsWithTranslations, {
                this.wordsWithTranslations.clear()
                this.wordById.clear()

                while (resultSet.next()) {
                    val wordId = resultSet.getLong(1)
                    val wordText = resultSet.getString(2)

                    val translation = translationsMap[wordId] ?: throw Exception("No translation for word $wordId")

                    val wordWithTranslation = WordWithTranslation(
                        wordId, wordText, translation.id, translation.text)

                    this.wordsWithTranslations.add(wordWithTranslation)
                    this.wordById[wordId] = wordWithTranslation
                }
            })

            createFuture<Any?>(null)
        } } }
    }

    fun resetDb(): IFuture<Any?> = this.getConnectionAndCatch {
        val statement = it.createStatement()
        statement.execute(RESET_DB_SQL)

        this.loadData()
    }

    fun deleteWord(id: Long): IFuture<Any?> = this.getConnectionAndCatch {
        val ps = it.prepareCall("DELETE FROM words WHERE id = ?")
        ps.setLong(1, id)

        val rows = ps.executeUpdate()

        if (rows == 1) {
            // maybe we need to switch to trees, but not now
            // also, we can use binary search, if we will fetch sorted words from db
            synchronized(this.wordsWithTranslations, {
                var i = 0
                for (word in this.wordsWithTranslations) {
                    if (word.wordId == id) {
                        this.wordsWithTranslations.removeAt(i)
                        this.wordById.remove(id)

                        break
                    }

                    i++
                }
            })

            createFuture<Any?>(null)
        } else {
            createFuture("Nothing deleted: $rows")
        }
    }

    fun updateWord(id: Long, wordText: String, translation: String): IFuture<Any?> =
            this.getConnectionAndCatch {
        val ps = it.prepareCall(
            "UPDATE words SET text = ? WHERE id = ?;" +
            "UPDATE translations SET text = ? WHERE \"wordId\" = ?;")

        ps.setString(1, wordText)
        ps.setLong(2, id)
        ps.setString(3, translation)
        ps.setLong(4, id)

        val rows = ps.executeUpdate()

        if (rows == 1) {
            synchronized(this.wordsWithTranslations, {
                val word = this.wordById[id]

                if (word != null) {
                    word.word = wordText
                    word.translation = translation
                } else {
                    System.err.println("Word $id not in memory")
                }
            })

            createFuture<Any?>(null)
        } else {
            createFuture("Nothing updated: $rows")
        }
    }

    private val correctWord = Pattern.compile("^[a-z'\\s]{1,32}$")
    class WordValidationError() : Throwable()

    fun createWord(wordText: String, translation: String): IAsync<Long, Any> {
        if (!correctWord.matcher(wordText).matches()) {
            return asyncError(WordValidationError())
        }

        if (translation.isEmpty() || translation.length > 255) {
            return asyncError(WordValidationError())
        }

        return this.pool.getConnection().success { catchAsync<Long> { it.use {
            val connection = it.getConnection()

            val ps = connection.prepareStatement(
"""WITH insertedWord AS
    (INSERT INTO words (text) VALUES (?) RETURNING id AS wid)
INSERT INTO translations (text, "wordId") VALUES
    (?, (SELECT wid FROM insertedWord))
RETURNING "wordId", id AS "translationId"""")

            ps.setString(1, wordText)
            ps.setString(2, translation)

            val resultSet = ps.executeQuery()
            if (!resultSet.next()) throw Exception("SQL: no result")

            val wordId = resultSet.getLong(1)
            val translationId = resultSet.getString(2)

            val word = WordWithTranslation(wordId, wordText, translationId, translation)

            synchronized(this.wordsWithTranslations, {
                this.wordsWithTranslations.add(0, word)
                this.wordById[wordId] = word
            })

            asyncResult(wordId)
        } } }
    }

    fun getAllWords(): List<WordWithTranslation> = synchronized(this.wordsWithTranslations, {
        return ArrayList(this.wordsWithTranslations)
    })

    fun getRandomWordWith4RandomTranslations(): WordWith4TranslationVariants? =
    synchronized(this.wordsWithTranslations, {
        if (this.wordsWithTranslations.size < 4) return null

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
    })

    fun getWordTranslationId(wordId: Long): String? = synchronized(this.wordsWithTranslations, {
        return this.wordById[wordId]?.translationId
    })

    // returns session token
    fun vkAuthorization(vkUserId: Long, accessToken: String, vkExpiresIn: Long): IAsync<LoginedUser?, Any> =
    this.pool.getConnection().success { catchAsync { it.use {
        val connection = it.getConnection()

        val userId: Long
        val token: ByteArray
        val expiresAt: Long

        try {
            connection.transactionIsolation = Connection.TRANSACTION_SERIALIZABLE
            connection.autoCommit = false

            val ps = connection.prepareStatement("""
WITH updatedVk AS (UPDATE "usersVk" SET "accessToken" = ?, "expiresAt" = (timezone('UTC', now()) + ? * interval '1 second') WHERE "vkUserId" = ? RETURNING "userId" AS uid),
     userCreated AS (INSERT INTO users SELECT WHERE NOT EXISTS (SELECT uid FROM updatedVk) RETURNING id AS "uidNew"),
     vkCreated AS (INSERT INTO "usersVk" ("userId", "vkUserId", "accessToken", "expiresAt") SELECT "uidNew", ?, ?, (timezone('UTC', now()) + ? * interval '1 second') FROM userCreated),
     uidTable AS ((SELECT uid FROM updatedVk) UNION (SELECT "uidNew" AS uid FROM userCreated))
INSERT INTO sessions ("userId") SELECT uid FROM uidTable RETURNING "userId", id AS "token", CAST(EXTRACT(EPOCH FROM "expiresAt") * 1000 AS BIGINT);""")

            ps.setString(1, accessToken)
            ps.setLong(2, vkExpiresIn)
            ps.setLong(3, vkUserId)
            ps.setLong(4, vkUserId)
            ps.setString(5, accessToken)
            ps.setLong(6, vkExpiresIn)

            val resultSet = ps.executeQuery()
            if (!resultSet.next()) throw Exception("SQL: no result")

            userId = resultSet.getLong(1)
            token = resultSet.getBytes(2)
            expiresAt = resultSet.getLong(3)

            connection.commit()
        } finally {
            connection.autoCommit = true
        }

        asyncResult<LoginedUser?, Any>(LoginedUser(
            id = userId,
            token = token.toHexString(),
            expiresAt = expiresAt
        ))
    } } }
}