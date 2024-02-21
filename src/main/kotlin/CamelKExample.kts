rest {
    configuration {
        host = "localhost"
        port = "8080"
    }

    path("/hello") {
        get("/get") {
            produces("application/json")
            to("direct:get")
        }
    }

    path("/bye") {
        post("/post") {
            produces("application/json")
            to("direct:post")
        }
    }
}

from("direct:get")
    .process { e -> e.getIn().body = "{ 'message': 'Hello GET' }" }

from("direct:post")
    .process { e -> e.getIn().body = "{ 'message': 'Hello POST' }" }