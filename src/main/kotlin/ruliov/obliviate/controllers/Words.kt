package ruliov.obliviate.controllers

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONTokener
import ruliov.data.mapR
import ruliov.jetty.createController
import ruliov.jetty.dontCaches
import ruliov.jetty.respondsJSON
import ruliov.obliviate.LOG
import ruliov.obliviate.auth.SessionProvider
import ruliov.obliviate.db.Database
import ruliov.obliviate.json.toCompactJSON
import ruliov.obliviate.json.toJSON
import java.io.InputStream

fun getAllWordsController(database: Database, sessionProvider: SessionProvider) =
createController(authRequired(sessionProvider) { request, groups, session ->
    respondsJSON(request)

    val words = database.getAllWords(session.userId)

    request.response.writer.write(words.toCompactJSON())

    request.asyncContext.complete()
})

private fun getRandomWordJSON(database: Database, ownerId: Long?): String {
    val word = database.getRandomWordWith4RandomTranslations(ownerId ?: 0)

    return word?.toJSON() ?: "{\"error\":\"no-words\"}"
}

fun getRandomWordController(database: Database, sessionProvider: SessionProvider) =
createController(tryAuth(sessionProvider) { request, groups, session ->
    respondsJSON(request)
    dontCaches(request)

    request.response.writer.write(getRandomWordJSON(database, session?.userId))
    request.asyncContext.complete()
})

fun checkAndGetNextWordController(database: Database, sessionProvider: SessionProvider) =
createController(tryAuth(sessionProvider) { request, groups, session ->
    groups ?: throw IllegalStateException()
    respondsJSON(request)

    val wordId = groups[0].toLong()
    val translationIdJSON = database.getWordTranslationId(wordId)?.let { "\"$it\"" } ?: "null"

    val nextWordJSON = getRandomWordJSON(database, session?.userId)

    request.response.writer.write("{\"correct\":$translationIdJSON,\"word\":$nextWordJSON}")
    request.asyncContext.complete()
})

fun deleteWordController(database: Database, sessionProvider: SessionProvider) =
createController(authRequired(sessionProvider) { request, groups, session ->
    groups ?: throw IllegalStateException()
    respondsJSON(request)

    val wordId = groups[0].toLong()

    database.deleteWord(session.userId, wordId).run {
        if (it == null) {
            request.response.writer.write("{\"error\":null}")
        } else {
            LOG.error("Word deletion:", it)
            respond500(request)
        }

        request.asyncContext.complete()
    }
})

fun updateWordController(database: Database, sessionProvider: SessionProvider) =
createController(authRequired(sessionProvider) { request, groups, session ->
    groups ?: throw IllegalStateException()
    respondsJSON(request)

    val wordId = groups[0].toLong()

    val maybeParsed = extractTwoStringsFromJSONArray(request.inputStream)
    if (maybeParsed == null) {
        request.response.status = 400
        return@authRequired
    }

    val word = maybeParsed.first
    val translation = maybeParsed.second

    database.updateWord(session.userId, wordId, word, translation).run {
        if (it == null) {
            request.response.writer.write("{\"error\":null}")
        } else {
            if (it is Database.WordValidationError) {
                respond400(request, "validation")
            } else {
                LOG.error("Word update:", it)
                respond500(request)
            }
        }

        request.asyncContext.complete()
    }
})

fun createWordController(database: Database, sessionProvider: SessionProvider) =
createController(authRequired(sessionProvider) { request, groups, session ->
    respondsJSON(request)

    val maybeParsed = extractTwoStringsFromJSONArray(request.inputStream)
    if (maybeParsed == null) {
        request.response.status = 400
        return@authRequired
    }

    val word = maybeParsed.first
    val translation = maybeParsed.second

    database.createWord(session.userId, word, translation).run { it.mapR({
        request.response.writer.write("{\"error\":null,\"id\":$it}")

        request.asyncContext.complete()
    }, {
        if (it is Database.WordValidationError) {
            request.response.status = 400
            request.response.writer.write("{\"error\":\"validation\"}")
        } else {
            request.response.status = 500
            request.response.writer.write("{\"error\":\"server\"}")
            LOG.error("Word creation error:", it)
        }

        request.asyncContext.complete()
    }) }
})

private fun extractTwoStringsFromJSONArray(stream: InputStream): Pair<String, String>? {
    val jsonTokener = JSONTokener(stream)

    val body: JSONArray
    try {
        body = JSONArray(jsonTokener)
    } catch (ex: JSONException) {
        return null
    }

    if (body.length() < 2) {
        return null
    }

    val word = body.getString(0)
    val translation = body.getString(1)

    return Pair(word, translation)
}