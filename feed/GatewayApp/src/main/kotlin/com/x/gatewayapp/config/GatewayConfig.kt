package com.x.gatewayapp.config

import com.x.gatewayapp.filter.SecurityContextToJwtGatewayFilterFactory
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GatewayConfig(
    private val securityContextToJwtFilterFactory: SecurityContextToJwtGatewayFilterFactory
) {

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            .route("user-login/out") { r ->
                r.path("/api/v1/users/login", "/api/v1/users/logout")
                    .uri("http://localhost:8083")
            }
            .route("user-service") { r ->
                r.path("/api/v1/users/**")
                    .filters { f ->
                        f.filter(securityContextToJwtFilterFactory.apply(SecurityContextToJwtGatewayFilterFactory.Config()))
                    }
                    .uri("http://localhost:8083")
            }
            // FeedApp routes - port 8084
            .route("feed-service") { r ->
                r.path("/api/v1/feeds/**")
                    .filters { f ->
                        f.filter(securityContextToJwtFilterFactory.apply(SecurityContextToJwtGatewayFilterFactory.Config()))
                    }
                    .uri("http://localhost:8084")
            }
            .route("newsfeed-service") { r ->
                r.path("/api/v1/newsfeed/**")
                    .filters { f ->
                        f.filter(securityContextToJwtFilterFactory.apply(SecurityContextToJwtGatewayFilterFactory.Config()))
                    }
                    .uri("http://localhost:8084")
            }
            .build()
    }
}
