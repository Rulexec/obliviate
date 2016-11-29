package ruliov.obliviate.db

import ruliov.async.*
import ruliov.javadb.DBConnectionPool
import ruliov.javadb.IDBConnectionPool
import ruliov.obliviate.LOG
import ruliov.obliviate.data.users.LoginedUser
import ruliov.obliviate.data.words.WordWith4TranslationVariants
import ruliov.obliviate.data.words.WordWithTranslation
import ruliov.randomHexString
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
                LOG.info("OLD-SESSIONS-AUTODELETE started")

                this@Database.getConnection {
                    it.prepareStatement(
                        """DELETE FROM sessions WHERE "expiresAt" < CAST(EXTRACT(EPOCH FROM timezone('UTC', now())) * 1000 AS BIGINT)"""
                    ).execute()

                    LOG.info("OLD-SESSIONS-AUTODELETE finished")

                    createFuture<Any?>(null)
                }.run {
                    if (it != null) LOG.error("OLD-SESSIONS-AUTODELETE failed:", it)
                }
            }
        }, 5 * 60 * 1000, 60 * 60 * 1000)
    }

    private fun getConnection(handler: (Connection) -> IFuture<Any?>): IFuture<Any?> {
        return this.pool.getConnection().bindToFuture { it.use { handler(it.getConnection()) } }
    }
    private fun <R> getConnection(handler: (Connection) -> IAsync<R, Any>): IAsync<R, Any> {
        return this.pool.getConnection().success { it.use { handler(it.getConnection()) } }
    }

    fun loadData(): IFuture<Any?> {
        return this.pool.getConnection().bindToFuture { it.use {
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

            LOG.trace("DB-LOADDATA translations received")

            resultSet = statement.executeQuery("""SELECT id, "ownerId", text FROM words ORDER BY text""")

            synchronized(this.wordsWithTranslations, {
                this.wordsWithTranslations.clear()
                this.wordById.clear()

                while (resultSet.next()) {
                    val wordId = resultSet.getLong(1)
                    val ownerId = resultSet.getLong(2)
                    val wordText = resultSet.getString(3)

                    val translation = translationsMap[wordId] ?: throw Exception("No translation for word $wordId")

                    val wordWithTranslation = WordWithTranslation(
                        wordId, ownerId, wordText, translation.id, translation.text)

                    this.wordsWithTranslations.add(wordWithTranslation)
                    this.wordById[wordId] = wordWithTranslation
                }
            })

            LOG.trace("DB-LOADDATA words received")

            createFuture<Any?>(null)
        } }
    }

    fun deleteWord(ownerId:Long, wordId: Long): IFuture<Any?> = this.getConnection {
        val ps = it.prepareCall("""DELETE FROM words WHERE id = ? AND "ownerId" = ?""")

        ps.setLong(1, wordId)
        ps.setLong(2, ownerId)

        val rows = ps.executeUpdate()

        if (rows == 0) return@getConnection createFuture<Any?>(null)

        // maybe we need to switch to trees, but not now
        // also, we can use binary search, if we will fetch sorted words from db
        synchronized(this.wordsWithTranslations, {
            var i = 0
            for (word in this.wordsWithTranslations) {
                if (word.wordId == wordId) { // no need check for ownerId, it already did DB
                    this.wordsWithTranslations.removeAt(i)
                    this.wordById.remove(wordId)

                    break
                }

                i++
            }
        })

        createFuture<Any?>(null)
    }

    fun updateWord(ownerId: Long, id: Long, wordText: String, translation: String): IFuture<Any?> =
    this.getConnection {
        if (!wordIsCorrect(wordText)) return@getConnection createFuture(WordValidationError())

        val ps = it.prepareCall(
"""WITH updated AS (UPDATE words SET text = ? WHERE id = ? AND "ownerId" = ? RETURNING id)
UPDATE translations SET text = ? WHERE "wordId" = (SELECT id FROM updated);""")

        ps.setString(1, wordText)
        ps.setLong(2, id)
        ps.setLong(3, ownerId)
        ps.setString(4, translation)

        val rows = ps.executeUpdate()

        if (rows == 1) {
            synchronized(this.wordsWithTranslations, {
                val word = this.wordById[id]

                if (word != null) {
                    word.word = wordText
                    word.translation = translation
                } else {
                    LOG.error("Word $id not in memory")
                }
            })
        }

        createFuture<Any?>(null)
    }

    private val correctWord = Pattern.compile("^[a-z'\\s]{1,32}$")
    class WordValidationError() : Throwable()

    fun wordIsCorrect(text: String): Boolean {
        return correctWord.matcher(text).matches()
    }

    fun createWord(ownerId:Long, wordText: String, translation: String): IAsync<Long, Any> {
        if (!wordIsCorrect(wordText)) return asyncError(WordValidationError())

        if (translation.isEmpty() || translation.length > 255) {
            return asyncError(WordValidationError())
        }

        return this.pool.getConnection().success { it.use {
            val connection = it.getConnection()

            val ps = connection.prepareStatement(
"""WITH insertedWord AS
    (INSERT INTO words ("ownerId", text) VALUES (?, ?) RETURNING id AS wid)
INSERT INTO translations (text, "wordId") VALUES
    (?, (SELECT wid FROM insertedWord))
RETURNING "wordId", id AS "translationId"""")

            ps.setLong(1, ownerId)
            ps.setString(2, wordText)
            ps.setString(3, translation)

            val resultSet = ps.executeQuery()
            if (!resultSet.next()) throw Exception("SQL: no result")

            val wordId = resultSet.getLong(1)
            val translationId = resultSet.getString(2)

            val word = WordWithTranslation(wordId, ownerId, wordText, translationId, translation)

            synchronized(this.wordsWithTranslations, {
                this.wordsWithTranslations.add(0, word)
                this.wordById[wordId] = word
            })

            asyncResult<Long, Any>(wordId)
        } }
    }

    fun getAllWords(ownerId: Long = 0): List<WordWithTranslation> =
    synchronized(this.wordsWithTranslations, {
        return this.wordsWithTranslations.filter { it.ownerId == ownerId }
    })

    fun getRandomWordWith4RandomTranslations(ownerId: Long = 0): WordWith4TranslationVariants? {
        var userWords: List<WordWithTranslation>? = null

        synchronized(this.wordsWithTranslations, {
            userWords = this.wordsWithTranslations.filter { it.ownerId == ownerId }
        })

        if (userWords!!.size < 4) return null

        val wordId = this.random.nextInt(userWords!!.size)
        val wordWithTranslation = userWords!![wordId]

        val usedVariants = HashSet<Int>()
        usedVariants.add(wordId)

        val correctVariantIndex = this.random.nextInt(4)

        val variants = Array(4, { i ->
            if (i == correctVariantIndex) {
                return@Array Pair(wordWithTranslation.translationId, wordWithTranslation.translation)
            }

            var rand: Int

            do {
                rand = this.random.nextInt(userWords!!.size)
            } while (rand in usedVariants)

            usedVariants.add(rand)

            Pair(userWords!![rand].translationId, userWords!![rand].translation)
        })

        return WordWith4TranslationVariants(
            userWords!![wordId].wordId,
            userWords!![wordId].word,
            variants)
    }

    fun getWordTranslationId(wordId: Long): String? = synchronized(this.wordsWithTranslations, {
        return this.wordById[wordId]?.translationId
    })

    fun vkAuthorization(vkUserId: Long, accessToken: String, vkExpiresIn: Long): IAsync<LoginedUser?, Any> =
    this.pool.getConnection().success { it.use {
        val connection = it.getConnection()

        val userId: Long
        val expiresAt: Long

        val authToken = randomHexString(16)

        try {
            connection.transactionIsolation = Connection.TRANSACTION_SERIALIZABLE
            connection.autoCommit = false

            val ps = connection.prepareStatement("""
WITH updatedVk AS (UPDATE "usersVk" SET "accessToken" = ?, "expiresAt" = CAST(EXTRACT(EPOCH FROM (timezone('UTC', now()) + ? * interval '1 second')) * 1000 AS BIGINT) WHERE "vkUserId" = ? RETURNING "userId" AS uid),
     userCreated AS (INSERT INTO users SELECT WHERE NOT EXISTS (SELECT uid FROM updatedVk) RETURNING id AS "uidNew"),
     vkCreated AS (INSERT INTO "usersVk" ("userId", "vkUserId", "accessToken", "expiresAt") SELECT "uidNew", ?, ?, CAST(EXTRACT(EPOCH FROM (timezone('UTC', now()) + ? * interval '1 second')) * 1000 AS BIGINT) FROM userCreated),
     uidTable AS ((SELECT uid FROM updatedVk) UNION (SELECT "uidNew" AS uid FROM userCreated))
INSERT INTO sessions ("id", "userId") SELECT ?, uid FROM uidTable RETURNING "userId", "expiresAt";""")

            ps.setString(1, accessToken)
            ps.setLong(2, vkExpiresIn)
            ps.setLong(3, vkUserId)
            ps.setLong(4, vkUserId)
            ps.setString(5, accessToken)
            ps.setLong(6, vkExpiresIn)
            ps.setString(7, authToken)

            val resultSet = ps.executeQuery()
            if (!resultSet.next()) throw Exception("SQL: no result")

            userId = resultSet.getLong(1)
            expiresAt = resultSet.getLong(2)

            connection.commit()
        } finally {
            connection.autoCommit = true
        }

        asyncResult<LoginedUser?, Any>(LoginedUser(
            id = userId,
            token = authToken,
            expiresAt = expiresAt
        ))
    } }

    fun logout(token: String): IFuture<Any?> = this.getConnection {
        val ps = it.prepareStatement("DELETE FROM sessions WHERE id = ?")

        ps.setString(1, token)

        val rows = ps.executeUpdate()
        if (rows == 0) LOG.info("LOGOUT 0")

        createFuture<Any?>(null)
    }

    fun getSession(token: String): IAsync<LoginedUser?, Any> = this.getConnection<LoginedUser?> {
        val ps = it.prepareStatement(
"""SELECT "userId", "expiresAt" FROM sessions WHERE id = ? AND "expiresAt" > CAST(EXTRACT(EPOCH FROM timezone('UTC', now())) * 1000 AS BIGINT)"""
        )
        ps.setString(1, token)

        val resultSet = ps.executeQuery()
        if (!resultSet.next()) return@getConnection asyncResult(null)

        val userId = resultSet.getLong(1)
        val expiresAt = resultSet.getLong(2)

        asyncResult(LoginedUser(userId, token, expiresAt))
    }

    fun saveEmail(email: String): IFuture<Any?> = this.getConnection {
        val ps = it.prepareStatement("""INSERT INTO emails (email) VALUES (?)""")
        ps.setString(1, email)

        ps.executeUpdate()

        createFuture<Any?>(null)
    }
}