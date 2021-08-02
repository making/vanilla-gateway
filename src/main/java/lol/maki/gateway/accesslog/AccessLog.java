package lol.maki.gateway.accesslog;

import java.time.OffsetDateTime;
import java.util.Objects;

import brave.Span;
import brave.propagation.TraceContext;

public class AccessLog {
	private OffsetDateTime date;

	private String method;

	private String path;

	private int status;

	private String host;

	private String address;

	private double elapsed;

	private String userAgent;

	private String referer;

	private String contentType;

	public AccessLog(OffsetDateTime date, String method, String path, int status, String host,
			String address, double elapsed, String userAgent,
			String referer, String contentType) {
		this.date = date;
		this.method = method;
		this.path = path;
		this.status = status;
		this.host = host;
		this.address = address;
		this.elapsed = elapsed;
		this.userAgent = userAgent;
		this.referer = referer;
		this.contentType = contentType;
	}

	public AccessLog() {
	}

	public OffsetDateTime getDate() {
		return date;
	}

	public void setDate(OffsetDateTime date) {
		this.date = date;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public double getElapsed() {
		return elapsed;
	}

	public void setElapsed(double elapsed) {
		this.elapsed = elapsed;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getReferer() {
		return referer;
	}

	public void setReferer(String referer) {
		this.referer = referer;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@Override
	public String toString() {
		return "date:" + this.date + "\thost:" + this.host
				+ "\tmethod:" + this.method + "\tpath:" + this.path
				+ "\tstatus:" + this.status + "\taddress:"
				+ this.address + "\telapsed:" + this.elapsed + "ms\tuser-agent:"
				+ this.userAgent + "\treferer:" + this.referer;
	}

	/**
	 * @see https://docs.cloudfoundry.org/concepts/architecture/router.html#about-access-logs
	 */
	public String goRouterCompliant(String xForwardedFor, String xForwardedProto, Span span) {
		final TraceContext context = span.context();
		return String.format("%s - [%s] \"%s %s %s\" %d %d %d \"%s\" \"%s\" \"%s\" \"-\" x_forwarded_for:\"%s\" x_forwarded_proto:\"%s\" vcap_request_id:\"-\" response_time:%s gorouter_time:0.0 app_id:\"-\" app_index:\"-\" x_b3_traceid:\"%s\" x_b3_spanid:\"%s\" x_b3_parentspanid:\"%s\" b3:\"-\"",
				this.host, this.date, this.method, this.path, "HTTP/1.1" /* TODO */, this.status, 0 /* TODO */, 0 /* TODO */,
				Objects.toString(this.referer, "-"),
				this.userAgent,
				this.address,
				Objects.toString(xForwardedFor, "-"),
				Objects.toString(xForwardedProto, "-"),
				this.elapsed, context.traceIdString(),
				context.spanIdString(),
				Objects.toString(context.parentIdString(), "-"));
	}

	public String caddyCompliantLog() {
		return String.format("{"
						+ "\"ts\":%.3f,"
						+ "\"request\":{"
						+ "\"remote_addr\":\"%s\","
						+ "\"proto\":\"%s\","
						+ "\"method\":\"%s\","
						+ "\"host\":\"%s\","
						+ "\"uri\":\"%s\","
						+ "\"headers\":{"
						+ "\"User-Agent\":["
						+ "\"%s\""
						+ "],"
						+ "\"Referer\":["
						+ "\"%s\""
						+ "]"
						+ "}"
						+ "},"
						+ "\"duration\":%s,"
						+ "\"size\":%d,"
						+ "\"status\":%d,"
						+ "\"resp_headers\":{"
						+ "\"Content-Type\":["
						+ "\"%s\""
						+ "]"
						+ "}"
						+ "}",
				this.date.toInstant().toEpochMilli() / 1000.0,
				this.address,
				"HTTP/1.1" /* TODO */,
				this.method,
				this.host,
				this.path,
				this.userAgent,
				this.referer,
				this.elapsed,
				0 /* TODO */,
				this.status,
				this.contentType
		);
	}
}