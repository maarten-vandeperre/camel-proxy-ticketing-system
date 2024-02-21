import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jackson.JacksonDataFormat
import org.apache.camel.model.dataformat.JsonLibrary
import org.apache.camel.model.rest.RestBindingMode

class TicketResponse {
    var status: String? = null
}

class PlatformProxyRoutes : RouteBuilder() {
    private val jacksonDataFormat = JacksonDataFormat()
    private val objectMapper = ObjectMapper()

    init {
        jacksonDataFormat.isPrettyPrint = true;
    }

    override fun configure() {
        restConfiguration().host("localhost").port(8080).bindingMode(RestBindingMode.json)

        rest("/proxy/*") //proxy mappping not required when other paths are in a separate runtime configuration
            .get()
            .to("direct:proxy")

        from("direct:proxy")
            .log("proxy hit: \${headers.CamelHttpPath} - \${headers.CamelHttpMethod} - \${headers.CamelHttpUrl}")
            .log("proxy body: \${body}")
            .setHeader("MaartenOriginalPath", header("CamelHttpPath").regexReplaceAll("proxy/", ""))
            .removeHeaders("CamelHttp*")
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setHeader("Accept", constant("application/json"))
            .log("Body before ticket system is \${body} with headers: \${headers}")
            .to("rest:get:/ticket-system-allowed")
            .log("Body after ticket system is \${body} with headers: \${headers}")
            .unmarshal(JacksonDataFormat(TicketResponse::class.java))
            .bean(this, "proxy")
            .log("Body after proxy is \${body} with headers: \${headers}")
            .setHeader(Exchange.HTTP_RESPONSE_CODE, exchangeProperty("responseCode"))
            .choice()
            .`when`(exchangeProperty("responseCode").isEqualTo(200))
            .to("direct:call-api")
            .otherwise()
            .to("direct:end")

        from("direct:call-api")
            .log("CALL API")
            .removeHeaders("CamelHttp*")
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setHeader(Exchange.HTTP_PATH, exchangeProperty("MaartenOriginalPath"))
            .setProperty("MaartenOriginalPath", exchangeProperty("MaartenOriginalPath"))
            .setHeader("Accept", constant("application/json"))
            .log("Body before api is \${body} with headers: \${headers}")
            .toD("http://localhost:8080/\${headers.MaartenOriginalPath}") //TODO exception handling
            .unmarshal()
            .json(JsonLibrary.Jackson)
            .log("Body after api is \${body} with headers: \${headers}")
            .to("direct:end")

        from("direct:end")
            .log("end request processing")
    }

    fun proxy(exchange: Exchange) {
        println(exchange.`in`.headers)
        val body = exchange.`in`.getBody(TicketResponse::class.java)
        println(body.status)
        if (body.status == "allowed") {
            exchange.setProperty("responseCode", 200)
            exchange.to("direct:call-api")
        } else {
            val cause = exchange.`in`.getBody(TicketResponse::class.java)
            exchange.`in`.body = null
            exchange.setProperty("responseCode", 422)
            exchange.message.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            exchange.message.body = objectMapper.writeValueAsString(mapOf(
                "error" to mapOf("ticket-status" to cause.status),
                "data" to null
            ))
        }
    }

}