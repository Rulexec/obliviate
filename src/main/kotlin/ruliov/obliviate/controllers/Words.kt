package ruliov.obliviate.controllers

import ruliov.jetty.*
import ruliov.obliviate.db.Database
import ruliov.obliviate.json.toCompactJSON
import ruliov.obliviate.json.toJSON

fun getAllWordsController(database: Database) = createControllerRespondsJSON { request, groups ->
    val words = database.getAllWords()

    request.response.writer.write(words.toCompactJSON())
}

fun getRandomWordController(database: Database) = createControllerRespondsJSONDontCaches { request, strings ->
    val word = database.getRandomWordWith4RandomTranslations()

    request.response.writer.write(word.toJSON())
}

fun checkAndGetNextWordController(database: Database) = createControllerRespondsJSON { request, groups ->
    groups ?: throw IllegalStateException()

    val wordId = groups[0].toLong()
    val translationIdJSON = database.getWordTranslationId(wordId)?.let { "\"$it\"" } ?: "null"

    val nextWordJSON = database.getRandomWordWith4RandomTranslations().toJSON()

    request.response.writer.write("{\"correct\":$translationIdJSON,\"word\":$nextWordJSON}")
}

fun deleteWordController(database: Database) = createControllerRespondsJSON { request, groups ->
    groups ?: throw IllegalStateException()

    val wordId = groups[0].toLong()

    database.deleteWord(wordId).run {
        if (it == null) {
            request.response.writer.write("{\"error\":null}")
        } else {
            request.response.writer.write(it.toString())
        }
    }
}