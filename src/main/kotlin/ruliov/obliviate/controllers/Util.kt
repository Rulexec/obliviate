package ruliov.obliviate.controllers

import org.eclipse.jetty.server.Request
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

fun parseJSONObjectOrRespond400(request: Request): JSONObject? {
    try {
        val tokener = JSONTokener(request.inputStream)
        val jsonObject = JSONObject(tokener)

        return jsonObject
    } catch (e: JSONException) {
        respond400(request, "parse")
        return null
    }
}

fun getStringFromJSONOrRespond400(request: Request, jsonObject: JSONObject, key: String): String? {
    val value = jsonObject.get(key)

    if (value == null) {
        respond400(request, "parse")
        return null
    }

    if (value is String) {
        return value
    } else {
        return null
    }
}

fun respond400(request: Request, reason: String = "not-specified") {
    request.response.status = 400
    request.response.writer.write("""{"error":"$reason"}""")
    request.response.closeOutput()
}
fun respond500(request: Request) {
    request.response.status = 500
    request.response.writer.write("""{"error":"500"}""")
    request.response.closeOutput()
}