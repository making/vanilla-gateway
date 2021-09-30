package lol.maki.gateway.dump;


import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class ResponseHeadersDumpGatewayFilterFactory
		extends AbstractGatewayFilterFactory<Object> {
	private final Logger log = LoggerFactory.getLogger(ResponseHeadersDumpGatewayFilterFactory.class);

	@Override
	public GatewayFilter apply(Object config) {
		return (exchange, chain) -> chain.filter(exchange)
				.doFinally(__ -> {
					if (log.isInfoEnabled()) {
						final ServerHttpResponse response = exchange.getResponse();
						final ServerHttpRequest request = exchange.getRequest();
						log.info("Response Headers:\t{}\t{} {}\t{}", request.getMethod(), response.getStatusCode().value(), request.getURI(), new TreeMap<>(response.getHeaders()));
					}
				});
	}
}
