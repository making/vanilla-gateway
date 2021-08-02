package lol.maki.gateway.accesslog;

import reactor.core.publisher.Flux;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class AccessLogRSocketDrain {
	private final AccessLogQueue logQueue;

	public AccessLogRSocketDrain(AccessLogQueue logQueue) {
		this.logQueue = logQueue;
	}

	@MessageMapping("logs")
	public Flux<String> logs() {
		return this.logQueue.popTail()
				.map(AccessLog::caddyCompliantLog);
	}
}
