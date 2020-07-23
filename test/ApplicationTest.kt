package dev.msfjarvis

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
  @Test
  fun testRoot() {
    withTestApplication({ module() }) {
      handleRequest(HttpMethod.Get, "/view").apply {
        assertEquals(HttpStatusCode.OK, response.status())
        assertEquals("No url query parameter provided", response.content)
      }
      handleRequest(HttpMethod.Get, "/view?url=https://msfjarvis.dev").apply {
        assertEquals(HttpStatusCode.OK, response.status())
        assertEquals("View recoded for https://msfjarvis.dev", response.content)
      }
      handleRequest(HttpMethod.Get, "/stats").apply {
        assertEquals(HttpStatusCode.OK, response.status())
        assertEquals(
          """
          <!DOCTYPE html>
          <html>
            <head>
              <title>Stats</title>
            </head>
            <body>
              <h1>Stats</h1>
              <table>
                <thead>URL</thead>
                <thead>Count</thead>
                <tr>
                  <td>https://msfjarvis.dev</td>
                  <td>1</td>
                </tr>
              </table>
            </body>
          </html>
          """.trimIndent(), response.content?.trimIndent()
        )
      }
    }
  }
}
