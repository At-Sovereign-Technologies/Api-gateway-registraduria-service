package com.selloLegitimo.gatewayRegis.config;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Component
public class GatewayRequestLoggingFilter implements GlobalFilter, Ordered {

	private static final Logger LOGGER = LoggerFactory.getLogger(GatewayRequestLoggingFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
		String routeId = route != null ? route.getId() : "unmatched";
		URI routeUri = route != null ? route.getUri() : null;

		LOGGER.info(
				"Gateway request received method={} path={} routeId={} routeUri={}",
				request.getMethod(),
				request.getURI().getRawPath(),
				routeId,
				routeUri);

		return chain.filter(exchange)
				.doOnSuccess(ignored -> logCompletion(exchange, routeId))
				.doOnError(error -> LOGGER.error(
						"Gateway request failed method={} path={} routeId={} message={}",
						request.getMethod(),
						request.getURI().getRawPath(),
						routeId,
						error.getMessage(),
						error));
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	private void logCompletion(ServerWebExchange exchange, String routeId) {
		ServerHttpRequest request = exchange.getRequest();
		URI targetUri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
		LOGGER.info(
				"Gateway request completed method={} path={} routeId={} targetUri={} status={}",
				request.getMethod(),
				request.getURI().getRawPath(),
				routeId,
				targetUri,
				exchange.getResponse().getStatusCode());
	}
}