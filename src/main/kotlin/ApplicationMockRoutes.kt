import org.apache.camel.builder.RouteBuilder

class ApplicationMockRoutes : RouteBuilder() {

    override fun configure() {
        rest("/application")
            .get()
            .to("direct:mock-get-request")

        from("direct:mock-get-request")
            .setHeader("Content-Type", simple("application/json"))
            .setBody(constant(mapOf("data" to "request processed ok")))
    }
}