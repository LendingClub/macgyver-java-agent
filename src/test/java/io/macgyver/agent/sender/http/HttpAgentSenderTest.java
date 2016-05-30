package io.macgyver.agent.sender.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.macgyver.agent.AgentException;
import io.macgyver.agent.AppEventBuilder;

import io.macgyver.agent.MacGyverAgent;
import io.macgyver.agent.sender.http.HttpAgentSender;
import okhttp3.Credentials;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class HttpAgentSenderTest {

	ObjectMapper mapper = new ObjectMapper();

	@Rule
	public MockWebServer mockServer = new MockWebServer();

	@Test
	public void testIt() throws InterruptedException, JsonProcessingException, IOException {

		MacGyverAgent agent = new MacGyverAgent();
		mockServer.enqueue(new MockResponse().setBody("{}"));
		agent.withSender(new HttpAgentSender().withBaseUrl(mockServer.url("/").toString()));

		agent.reportCheckIn();

		RecordedRequest rr = mockServer.takeRequest();

		Assertions.assertThat(rr.getRequestLine()).isEqualTo("POST /api/cmdb/checkIn HTTP/1.1");

		Assertions.assertThat(rr.getHeader("content-type")).contains("application/json");

		JsonNode n = mapper.readTree(rr.getBody().readUtf8());

		Assertions.assertThat(n.has("host")).isTrue();

	}

	@Test
	public void testAppEvent() throws InterruptedException, JsonProcessingException, IOException {

		mockServer.enqueue(new MockResponse().setBody("{}"));
		MacGyverAgent agent = new MacGyverAgent();

		HttpAgentSender sender = new HttpAgentSender().withBaseUrl(mockServer.url("/").toString());
		agent.withSender(sender);

		agent.reportAppEvent(
				new AppEventBuilder().withEventType(io.macgyver.agent.MacGyverAgent.AppEventType.DEPLOY_FAILED)
						.withHost("somehost").withMessage("my message").withAppId("myapp"));

		RecordedRequest rr = mockServer.takeRequest();

		Assertions.assertThat(rr.getRequestLine()).isEqualTo("POST /api/cmdb/app-event HTTP/1.1");

		Assertions.assertThat(rr.getHeader("content-type")).contains("application/json");

		JsonNode n = mapper.readTree(rr.getBody().readUtf8());

		Assertions.assertThat(n.has("host")).isTrue();

		Assertions.assertThat(n.path("appId").asText()).isEqualTo("myapp");
		Assertions.assertThat(n.path("eventType").asText()).isEqualTo("DEPLOY_FAILED");
	}

	@Test
	public void testThreadDump() throws InterruptedException, JsonProcessingException, IOException {

		MacGyverAgent agent = new MacGyverAgent();
		mockServer.enqueue(new MockResponse().setBody("{}"));
		HttpAgentSender sender = new HttpAgentSender().withBaseUrl(mockServer.url("/").toString());

		agent.withSender(sender);
		agent.reportThreadDump();

		RecordedRequest rr = mockServer.takeRequest();

		Assertions.assertThat(rr.getRequestLine()).isEqualTo("POST /api/monitor/thread-dump HTTP/1.1");

		Assertions.assertThat(rr.getHeader("content-type")).contains("application/json");

		JsonNode n = mapper.readTree(rr.getBody().readUtf8());

		Assertions.assertThat(n.has("host")).isTrue();

		GZIPInputStream gis = new GZIPInputStream(
				new ByteArrayInputStream(Base64.getDecoder().decode(n.get("threadDumpGzip").asText())));
		BufferedReader isr = new BufferedReader(new InputStreamReader(gis));
		String line = null;

		List<String> lines = new ArrayList<>();
		while ((line = isr.readLine()) != null) {
			lines.add(line);
		}

		Assertions.assertThat(lines.stream().anyMatch(p -> p.contains("state=RUNNABLE"))).isTrue();
	}

	@Test
	public void testConnectException() {
		try {
			HttpAgentSender sender = new HttpAgentSender().withBaseUrl("http://localhost:34223");
			sender.sendAppEvent(mapper.createObjectNode());
			Assertions.failBecauseExceptionWasNotThrown(AgentException.class);
		} catch (Exception e) {
			Assertions.assertThat(e).isInstanceOf(AgentException.class);
			Assertions.assertThat(e.getCause()).isInstanceOf(ConnectException.class);
		}

	}
	

	
	@Test
	public void test500Response() {
		try {
			mockServer.enqueue(new MockResponse().setResponseCode(500));
			HttpAgentSender sender = new HttpAgentSender().withBaseUrl(mockServer.url("/").toString());
			sender.sendAppEvent(mapper.createObjectNode());
			Assertions.failBecauseExceptionWasNotThrown(AgentException.class);
		} catch (Exception e) {
			Assertions.assertThat(e).isInstanceOf(AgentException.class).hasMessageContaining("statusCode=500").hasMessageContaining("POST http://localhost:").hasMessageContaining("app-event");
			
		}
	}
	
	@Test
	public void test403Response() {
		try {
			mockServer.enqueue(new MockResponse().setResponseCode(403));
			HttpAgentSender sender = new HttpAgentSender().withBaseUrl(mockServer.url("/").toString());
			sender.sendAppEvent(mapper.createObjectNode());
			Assertions.failBecauseExceptionWasNotThrown(AgentException.class);
		} catch (Exception e) {
			Assertions.assertThat(e).isInstanceOf(AgentException.class).hasMessageContaining("statusCode=403").hasMessageContaining("POST http://localhost:").hasMessageContaining("app-event");
			
		}
	}
}
