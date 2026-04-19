package com.selloLegitimo.gatewayRegis.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

	@Bean
	SecurityWebFilterChain securityWebFilterChain(
			ServerHttpSecurity http,
			KeycloakJWTAuthenticationConverter keycloakJwtAuthenticationConverter,
			BearerTokenLoggingWebFilter bearerTokenLoggingWebFilter,
			ServerAuthenticationEntryPoint authenticationEntryPoint,
			ServerAccessDeniedHandler accessDeniedHandler) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.addFilterBefore(bearerTokenLoggingWebFilter,
						org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION)
				.authorizeExchange(exchange -> exchange
						.pathMatchers("/actuator/health", "/actuator/info").permitAll()
						.pathMatchers(HttpMethod.OPTIONS).permitAll()
						.anyExchange().authenticated())
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.oauth2ResourceServer(oauth2 -> oauth2
						.authenticationEntryPoint(authenticationEntryPoint)
						.jwt(jwt -> jwt.jwtAuthenticationConverter(
								new ReactiveJwtAuthenticationConverterAdapter(keycloakJwtAuthenticationConverter))))
				.build();
	}

	@Bean
	ServerAuthenticationEntryPoint authenticationEntryPoint() {
		return (exchange, ex) -> {
			String authorizationHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
			LOGGER.warn(
					"JWT authentication rejected method={} path={} hasBearerToken={} message={}",
					exchange.getRequest().getMethod(),
					exchange.getRequest().getURI().getRawPath(),
					authorizationHeader != null && authorizationHeader.startsWith("Bearer "),
					ex.getMessage());
			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		};
	}

	@Bean
	ServerAccessDeniedHandler accessDeniedHandler() {
		return (exchange, ex) -> exchange.getPrincipal()
				.map(java.security.Principal::getName)
				.defaultIfEmpty("anonymous")
				.flatMap(principalName -> {
			LOGGER.warn(
					"JWT access denied method={} path={} principal={} message={}",
					exchange.getRequest().getMethod(),
					exchange.getRequest().getURI().getRawPath(),
					principalName,
					ex.getMessage());
			exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
			return exchange.getResponse().setComplete();
		});
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(List.of("*"));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(false);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}