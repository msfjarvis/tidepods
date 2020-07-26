package dev.msfjarvis

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.AutoHeadResponse
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.receiveText
import io.ktor.response.respondFile
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.netty.EngineMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File
import java.security.SecureRandom
import java.util.*
import kotlin.system.exitProcess

val db = HashMap<String, Long>()
val json = Json(JsonConfiguration.Stable)
var latestStats = ""
val statsFile = File("stats.json")
val stylesFile = File("resources/static/styles.css")

@Serializable
data class Site(val url: String, val views: Long)

private fun flushToDisk() = synchronized(db) {
  latestStats = json.stringify(
    Site.serializer().list,
    db.map { entry -> Site(entry.key, entry.value) }.toList()
  )
  statsFile.writeText(latestStats)
}

fun main(args: Array<String>) = EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
fun Application.module(test: Boolean = false) {

  install(AutoHeadResponse)

  val stylesEndpoint = if (test) {
    "/styles.css"
  } else {
    "/styles.${SecureRandom().nextInt()}.css"
  }

  Timer().scheduleAtFixedRate(object : TimerTask() {
    override fun run() {
      flushToDisk()
    }
  }, 10000, 60000)

  if (!test) {
    if (statsFile.exists()) {
      latestStats = statsFile.readText()
      if (latestStats.isEmpty()) latestStats = "[]"
      json.parse(Site.serializer().list, latestStats).map { (url, views) -> db[url] = views }
    } else {
      if (!statsFile.createNewFile()) {
        println("Failed to create statsFile $statsFile")
        exitProcess(128)
      } else {
        latestStats = "[]"
        statsFile.writeText(latestStats)
      }
    }
  }

  routing {
    get(stylesEndpoint) {
      call.respondFile(stylesFile)
    }
    static("/static") {
      resources("static")
    }
    get("/") {
      call.respondRedirect("/stats", permanent = true)
    }
    get("/favicon.ico") {
      call.respondRedirect("https://msfjarvis.dev/favicon.ico", permanent = true)
    }
    get("/view") {
      val url = call.request.queryParameters["url"]
      if (url.isNullOrEmpty()) {
        call.respondText("No url query parameter provided")
      } else {
        launch(Dispatchers.IO) {
          synchronized(db) {
            var count = db[url] ?: 0
            db[url] = ++count
          }
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
            link {
              href = stylesEndpoint
              rel = "stylesheet"
            }
          }
          body {
            div(classes = "container") {
              h1 { +"Stats" }
              table(classes = "table-1") {
                tbody {
                  tr {
                    th { +"URL" }
                    th { +"Count" }
                  }
                  if (test) {
                    db.forEach { (url, count) ->
                      tr {
                        td {
                          attributes["data-th"] = "URL"
                          +url
                        }
                        td {
                          attributes["data-th"] = "Count"
                          +"$count"
                        }
                      }
                    }
                  } else {
                    json.parse(Site.serializer().list, latestStats).sortedBy { it.views }.reversed()
                      .map { (url, count) ->
                        tr {
                          td {
                            attributes["data-th"] = "URL"
                            +url
                          }
                          td {
                            attributes["data-th"] = "Count"
                            +"$count"
                          }
                        }
                      }
                  }
                }
              }
            }
          }
        }
      } else if (format == "json") {
        call.respondText(ContentType.Application.Json) {
          if (test) {
            json.stringify(
              Site.serializer().list,
              db.map { entry -> Site(entry.key, entry.value) }.toList()
            )
          } else {
            statsFile.readText()
          }
        }
      } else {
        call.respondText(status = HttpStatusCode.BadRequest) { "Invalid format: $format" }
      }
    }
    post("/stats") {
      val body = json.parse(Site.serializer().list, call.receiveText())
      synchronized(db) {
        db.clear()
        body.forEach { site -> db[site.url] = site.views }
      }
      flushToDisk()
      call.respondText { "Entered bulk data into stats DB" }
    }
    post("/flush") { flushToDisk() }
  }
}
