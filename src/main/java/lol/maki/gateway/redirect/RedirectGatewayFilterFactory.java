package lol.maki.gateway.redirect;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import lol.maki.gateway.redirect.RedirectGatewayFilterFactory.Config;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.HttpStatusHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

@Component
public class RedirectGatewayFilterFactory
		extends AbstractGatewayFilterFactory<Config> {

	/**
	 * Status key.
	 */
	public static final String STATUS_KEY = "status";

	/**
	 * HOST key.
	 */
	public static final String HOST_KEY = "host";

	public RedirectGatewayFilterFactory() {
		super(Config.class);
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList(STATUS_KEY, HOST_KEY);
	}

	@Override
	public GatewayFilter apply(Config config) {
		return apply(config.status, config.host);
	}

	public GatewayFilter apply(String statusString, String host) {
		HttpStatusHolder httpStatus = HttpStatusHolder.parse(statusString);
		Assert.isTrue(httpStatus.is3xxRedirection(),
				"status must be a 3xx code, but was " + statusString);
		return apply(httpStatus, host);
	}

	public GatewayFilter apply(HttpStatus httpStatus, String host) {
		return apply(new HttpStatusHolder(httpStatus, null), host);
	}

	public GatewayFilter apply(HttpStatusHolder httpStatus, String host) {
		return new GatewayFilter() {
			@Override
			public Mono<Void> filter(ServerWebExchange exchange,
					GatewayFilterChain chain) {
				if (!exchange.getResponse().isCommitted()) {
					setResponseStatus(exchange, httpStatus);
					final String[] parts = host.split(":");
					final URI uri = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
							.host(parts[0]).port(parts.length > 1 ? Integer.parseInt(parts[1]) : -1).build().toUri();
					final ServerHttpResponse response = exchange.getResponse();
					response.getHeaders().set(HttpHeaders.LOCATION, uri.toString());
					return response.setComplete();
				}
				return Mono.empty();
			}

			@Override
			public String toString() {
				String status;
				if (httpStatus.getHttpStatus() != null) {
					status = String.valueOf(httpStatus.getHttpStatus().value());
				}
				else {
					status = httpStatus.getStatus().toString();
				}
				return filterToStringCreator(RedirectGatewayFilterFactory.this)
						.append(status, host).toString();
			}
		};
	}

	public static class Config {

		String status;

		String host;

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

	}

}
