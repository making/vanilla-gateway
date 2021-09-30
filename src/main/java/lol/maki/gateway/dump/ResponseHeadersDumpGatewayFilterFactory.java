package lol.maki.gateway.dump;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
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
					final ServerHttpResponse response = exchange.getResponse();
					log.info("Response Headers: {}", response.getHeaders());
				});
	}
}
