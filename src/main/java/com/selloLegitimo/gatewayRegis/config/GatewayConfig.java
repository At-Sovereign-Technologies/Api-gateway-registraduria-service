package com.selloLegitimo.gatewayRegis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

	@Bean
	RouteLocator gatewayRoutes(
			RouteLocatorBuilder builder,
			@Value("${gateway.services.configuracion-eleccion.url}") String configuracionEleccionUrl,
			@Value("${gateway.services.gestion-pre-electoral.url}") String gestionPreElectoralUrl) {
                return builder.routes()
                                .route("configuracion-eleccion-service-root", route -> route
                                                .path("/api/configuracion-eleccion")
                                                .filters(filter -> filter.setPath("/api/elecciones"))
                                                .uri(configuracionEleccionUrl))
                                .route("configuracion-eleccion-service", route -> route
                                                .path("/api/configuracion-eleccion/**")
                                                .filters(filter -> filter.rewritePath(
                                                                "/api/configuracion-eleccion/(?<segment>.*)",
                                                                "/api/elecciones/${segment}"))
                                                .uri(configuracionEleccionUrl))
                                .route("gestion-pre-electoral-service", route -> route
                                                .path("/api/gestion-pre-electoral", "/api/gestion-pre-electoral/**")
                                                .filters(filter -> filter.stripPrefix(2))
                                                .uri(gestionPreElectoralUrl))
                                .build();
        }
}