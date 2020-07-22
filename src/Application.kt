package dev.msfjarvis

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.html.respondHtml
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.netty.EngineMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.html.*

val db = HashMap<String, Int>()

fun main(args: Array<String>) = EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    val client = HttpClient(Apache) {
        install(Logging) {
            level = LogLevel.HEADERS
        }
    }

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
        }
    }
}
