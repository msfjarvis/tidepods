package dev.msfjarvis

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.netty.EngineMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

val db = HashMap<String, Long>()

@Serializable
data class Site(val url: String, val views: Long)

fun main(args: Array<String>) = EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
  val json = Json(JsonConfiguration.Stable)

  routing {
    get("/view") {
      val url = call.request.queryParameters["url"]
      if (url.isNullOrEmpty()) {
        call.respondText("No url query parameter provided")
      } else {
        launch(Dispatchers.IO) {
          var count = db[url] ?: 0
          db[url] = ++count
        }
        call.respondText("View recoded for $url")
      }
    }
    get("/stats") {
      val format = call.request.queryParameters["format"]
      if (format == null || format == "html") {
        call.respondHtml {
          head {
            title { +"Stats" }
          }
          body {
            h1 { +"Stats" }
            table {
              thead { +"URL" }
              thead { +"Count" }
              db.forEach { (url, count) ->
                tr {
                  td { +url }
                  td { +"$count" }
                }
              }
            }
          }
        }
      } else if (format == "json") {
        call.respondText(ContentType.Application.Json) {
          json.stringify(Site.serializer().list, db.map { entry -> Site(entry.key, entry.value) }.toList())
        }
      } else {
        call.respondText { "Invalid format: $format" }
      }
    }
  }
}
