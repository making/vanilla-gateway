package lol.maki.gateway.accesslog;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink.OverflowStrategy;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.stereotype.Component;

@Component
public class AccessLogQueue {
	private final BlockingDeque<AccessLog> logs = new LinkedBlockingDeque<>(1_000);

	private final Logger log = LoggerFactory.getLogger(AccessLogQueue.class);

	public Mono<Void> put(AccessLog s) {
		return Mono.<Void>fromRunnable(() -> {
			try {
				while (this.logs.remainingCapacity() == 0) {
					this.logs.removeFirst();
				}
				this.logs.put(s);
			}
			catch (InterruptedException e) {
				log.info(e.getMessage(), e);
				Thread.currentThread().interrupt();
			}
		}).publishOn(Schedulers.boundedElastic());
	}

	public Flux<AccessLog> popTail() {
		return Flux.<AccessLog>create(sink -> {
			sink.onRequest(n -> {
				log.info("request({})", n);
				for (int i = 0; i < n; i++) {
					try {
						sink.next(this.logs.take());
					}
					catch (InterruptedException e) {
						sink.error(e);
						Thread.currentThread().interrupt();
						break;
					}
				}
			});
		}, OverflowStrategy.DROP)
				.subscribeOn(Schedulers.boundedElastic());
	}
}
