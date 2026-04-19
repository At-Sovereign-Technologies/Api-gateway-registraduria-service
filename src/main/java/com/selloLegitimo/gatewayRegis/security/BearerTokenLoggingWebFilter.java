package com.selloLegitimo.gatewayRegis.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class BearerTokenLoggingWebFilter implements WebFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(BearerTokenLoggingWebFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		String authorizationHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
		boolean hasBearerToken = authorizationHeader != null && authorizationHeader.startsWith("Bearer ");

		LOGGER.info(
				"Incoming request method={} path={} hasBearerToken={} tokenPreview={}",
				exchange.getRequest().getMethod(),
				exchange.getRequest().getURI().getRawPath(),
				hasBearerToken,
				maskBearerToken(authorizationHeader));

		return chain.filter(exchange);
	}

	private String maskBearerToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			return "none";
		}

		String token = authorizationHeader.substring("Bearer ".length());
		if (token.length() <= 12) {
			return "len=" + token.length();
		}

		return token.substring(0, 12) + "... len=" + token.length();
	}
}