package lol.maki.gateway.accesslog;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import brave.Span;
import brave.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class AccessLogGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {
	private final Tracer tracer;

	private final AccessLogQueue logQueue;

	public AccessLogGatewayFilterFactory(Tracer tracer, AccessLogQueue logQueue) {
		this.tracer = tracer;
		this.logQueue = logQueue;
	}

	@Override
	public GatewayFilter apply(Object config) {
		return new RequestLoggingGatewayFilter(tracer, logQueue);
	}

	static class RequestLoggingGatewayFilter implements GatewayFilter {
		private final Logger log = LoggerFactory.getLogger("RTR");

		private final Tracer tracer;

		private final AccessLogQueue logQueue;

		RequestLoggingGatewayFilter(Tracer tracer, AccessLogQueue logQueue) {
			this.tracer = tracer;
			this.logQueue = logQueue;
		}

		@Override
		public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
			long begin = System.nanoTime();
			return chain.filter(exchange) //
					.doFinally(__ -> {
						final double elapsed = (System.nanoTime() - begin) / 1_000_000.0;
						final ServerHttpRequest request = exchange.getRequest();
						final ServerHttpResponse response = exchange.getResponse();
						final OffsetDateTime now = OffsetDateTime.now();
						final HttpMethod method = request.getMethod();
						final RequestPath path = request.getPath();
						final HttpStatus code = response.getStatusCode();
						final int statusCode = code == null ? 0 : code.value();
						final HttpHeaders headers = request.getHeaders();
						final String host = headers.getHost().getHostString();
						final String remoteAddr = request.getRemoteAddress().getHostString() + ":" + request.getRemoteAddress().getPort();
						final String userAgent = Objects.toString(headers.getFirst(HttpHeaders.USER_AGENT), "null");
						final String referer = headers.getFirst(HttpHeaders.REFERER);
						final MediaType contentType = response.getHeaders().getContentType();
						if (userAgent.startsWith("Go-http-client")) {
							return;
						}
						final AccessLog accessLog = new AccessLogBuilder()
								.setDate(now)
								.setMethod(Objects.toString(method, ""))
								.setPath(path.value()).setStatus(statusCode)
								.setHost(host).setAddress(remoteAddr).setElapsed(elapsed)
								.setUserAgent(userAgent)
								.setReferer(Objects.toString(referer, "-"))
								.setContentType(contentType == null ? "-" : contentType.toString())
								.build();
						this.logQueue.put(accessLog).subscribe();
						final List<String> xForwardedFors = headers.get("X-Forwarded-For");
						final String xForwardedFor = xForwardedFors == null ? null : String.join(", ", xForwardedFors);
						final String xForwardedProto = headers.getFirst("X-Forwarded-Proto");
						final Span span = this.tracer.currentSpan();
						span.tag("host", host);
						if (referer != null) {
							span.tag("referer", referer);
						}
						span.tag("user-agent", userAgent);
						span.tag("status.code", String.valueOf(statusCode));
						log.info("{}", accessLog.goRouterCompliant(xForwardedFor, xForwardedProto, span));
					});
		}

		@Override
		public String toString() {
			return "[RequestLogging]";
		}
	}
}