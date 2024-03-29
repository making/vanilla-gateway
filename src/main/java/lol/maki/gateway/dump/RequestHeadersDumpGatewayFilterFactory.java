package lol.maki.gateway.dump;


import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestHeadersDumpGatewayFilterFactory
		extends AbstractGatewayFilterFactory<Object> {
	private final Logger log = LoggerFactory.getLogger(RequestHeadersDumpGatewayFilterFactory.class);

	@Override
	public GatewayFilter apply(Object config) {
		return (exchange, chain) -> {
			if (log.isInfoEnabled()) {
				final ServerHttpRequest request = exchange.getRequest();
				log.info("Request Headers:\t{}\t    {}\t{}", request.getMethod(),  request.getURI(), new TreeMap<>(request.getHeaders()));
			}
			return chain.filter(exchange);
		};
	}
}
