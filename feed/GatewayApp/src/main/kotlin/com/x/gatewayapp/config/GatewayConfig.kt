package com.x.gatewayapp.config

import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GatewayConfig {

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            // UserApp routes - port 8083
            .route("user-service") { r ->
                r.path("/api/v1/users/**")
                    .uri("http://localhost:8083")
            }
            // FeedApp routes - port 8084
            .route("feed-service") { r ->
                r.path("/api/v1/feeds/**")
                    .uri("http://localhost:8084")
            }
            .route("newsfeed-service") { r ->
                r.path("/api/v1/newsfeed/**")
                    .uri("http://localhost:8084")
            }
            .build()
    }
}
