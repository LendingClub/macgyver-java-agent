package io.macgyver.agent.sender.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.macgyver.agent.AgentException;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpAgentSender implements io.macgyver.agent.MacGyverAgent.Sender {

	Logger logger = LoggerFactory.getLogger(HttpAgentSender.class);

	static OkHttpClient globalClient;

	OkHttpClient okhttp;

	List<Consumer<OkHttpClient.Builder>> configurators = new ArrayList<>();

	public static final String DEFAULT_CHECK_IN_PATH = "/api/cmdb/checkIn";
	public static final String DEFAULT_APP_EVENT_PATH = "/api/cmdb/app-event";
	public static final String DEFAULT_THREAD_DUMP_PATH = "/api/monitor/thread-dump";

	String baseUrl;
	String checkInPath = DEFAULT_CHECK_IN_PATH;
	String appEventPath = DEFAULT_APP_EVENT_PATH;
	String threadDumpPath = DEFAULT_THREAD_DUMP_PATH;
	String username = null;
	String password = null;

	public HttpAgentSender withBaseUrl(String url) {
		this.baseUrl = url;
		while (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		return this;
	}

	public HttpAgentSender withOkHttpClient(OkHttpClient client) {
		this.okhttp = client;
		return this;
	}

	public HttpAgentSender withOkHttpConfigurator(Consumer<OkHttpClient.Builder> configurator) {
		configurators.add(configurator);
		return this;
	}

	public String getThreadDumpUrl() {
		return baseUrl + threadDumpPath;
	}

	public String getAppEventUrl() {
		return baseUrl + appEventPath;
	}

	public String getCheckInUrl() {
		return baseUrl + checkInPath;
	}

	public HttpAgentSender withCredentials(String username, String password) {

		return this;
	}

	private void post(String url, ObjectNode data) {

		Response response = null;
		try {
			doInit();

			if (okhttp == null) {
				throw new NullPointerException("okhttp not initialized");
			}

			Request.Builder requestBuilder = new Request.Builder().header("accept", "application/json");
			if (username != null && password != null) {
				requestBuilder = requestBuilder.addHeader("Authorization", Credentials.basic(username, password));
			}

			response = okhttp.newCall(requestBuilder
					.post(RequestBody.create(MediaType.parse("application/json"), data.toString())).url(url).build())
					.execute();
			int code = response.code();

			if (logger.isDebugEnabled()) {
				logger.debug("POST {} rc={}", url, code);
			}
			if (code != 200) {
				throw new AgentException("POST " + url + " statusCode=" + code);
			}

		} catch (IOException e) {
			throw new AgentException("POST " + url, e);
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}

	@Override
	public void sendThreadDump(ObjectNode status) {
		post(getThreadDumpUrl(), status);
	}

	@Override
	public void sendCheckIn(ObjectNode status) {

		post(getCheckInUrl(), status);
	}

	protected void doInit() {
		synchronized (HttpAgentSender.class) {
			if (globalClient == null) {
				globalClient = new OkHttpClient.Builder().build();
			}
		}
		if (this.okhttp == null) {
			Builder b = globalClient.newBuilder();
			for (Consumer<OkHttpClient.Builder> c : configurators) {
				c.accept(b);
			}
			this.okhttp = b.build();
		}

	}

	@Override
	public void sendAppEvent(ObjectNode n) {

		post(getAppEventUrl(), n);

	}

}
