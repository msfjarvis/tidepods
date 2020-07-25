package dev.msfjarvis

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
  private val testData = arrayListOf(Site("https://msfjarvis.dev", 1))
  @Test
  fun testRoot() {
    withTestApplication({ module(true) }) {
      handleRequest(HttpMethod.Get, "/").apply {
        assertEquals(HttpStatusCode.MovedPermanently, response.status())
        assertEquals("/stats", response.headers["Location"])
      }
      handleRequest(HttpMethod.Get, "/favicon.ico").apply {
        assertEquals(HttpStatusCode.MovedPermanently, response.status())
        assertEquals("https://msfjarvis.dev/favicon.ico", response.headers["Location"])
      }
      handleRequest(HttpMethod.Get, "/view").apply {
        assertEquals(HttpStatusCode.OK, response.status())
        assertEquals("No url query parameter provided", response.content)
      }
      testData.map { (url, _) ->
        handleRequest(HttpMethod.Get, "/view?url=$url").apply {
          assertEquals(HttpStatusCode.OK, response.status())
          assertEquals("View recoded for $url", response.content)
        }
      }
      handleRequest(HttpMethod.Get, "/stats").apply {
        verifyHtmlResponse()
      }
      handleRequest(HttpMethod.Get, "/stats?format=html").apply {
        verifyHtmlResponse()
      }
      handleRequest(HttpMethod.Get, "/stats?format=badboi").apply {
        assertEquals(HttpStatusCode.BadRequest, response.status())
        assertEquals("Invalid format: badboi", response.content)
      }
      handleRequest(HttpMethod.Get, "/stats?format=json").apply {
        assertEquals(HttpStatusCode.OK, response.status())
        assertEquals(testData, json.parse(Site.serializer().list, response.content!!))
      }
      testData.add(Site("https://msfjarvis.dev/g/tidepods", 2))
      testData.sortBy { it.views }
      handleRequest(HttpMethod.Post, "/stats") {
        setBody(
          Json(JsonConfiguration.Stable).stringify(Site.serializer().list, testData)
        )
      }.apply {
        assertEquals(HttpStatusCode.OK, response.status())
        assertEquals("Entered bulk data into stats DB", response.content)
      }
      handleRequest(HttpMethod.Get, "/stats?format=json").apply {
        assertEquals(HttpStatusCode.OK, response.status())
        assertEquals(testData, json.parse(Site.serializer().list, response.content!!).sortedBy { it.views })
      }
    }
  }

  private fun TestApplicationCall.verifyHtmlResponse() {
    assertEquals(HttpStatusCode.OK, response.status())
    assertEquals(
      """
      <!DOCTYPE html>
      <html>
        <head>
          <title>Stats</title>
          <link href="/static/styles.css" rel="stylesheet">
        </head>
        <body>
          <div class="container">
            <h1>Stats</h1>
            <table class="table-1">
              <tbody>
                <tr>
                  <th>URL</th>
                  <th>Count</th>
                </tr>
                <tr>
                  <td data-th="URL">https://msfjarvis.dev</td>
                  <td data-th="Count">1</td>
                </tr>
              </tbody>
            </table>
          </div>
        </body>
      </html>
      """.trimIndent(), response.content?.trimIndent()
    )
  }
}
