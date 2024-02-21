import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import java.util.*
import kotlin.random.Random

class TicketingServiceMockRoutes : RouteBuilder() {
    private val random = Random(Date().toInstant().epochSecond)

    override fun configure() {
        rest("/ticket-system-allowed")
            .get()
            .to("direct:mock-is-allowed")

        from("direct:mock-is-allowed")
            .setHeader("Content-Type", simple("application/json"))
            .bean(this, "checkIfAllowed")
    }

    fun checkIfAllowed(exchange: Exchange) {
        exchange.message.body = if (random.nextInt() % 3 == 0) {
            mapOf("status" to "allowed")
        } else {
            mapOf("status" to "not-allowed")
        }
    }
}