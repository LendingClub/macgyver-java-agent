package io.macgyver.agent.sender.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.macgyver.agent.AgentException;
import io.macgyver.agent.AppEventBuilder;
import io.macgyver.agent.AppMetadataProvider;
import io.macgyver.agent.MacGyverAgent;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.util.*;
import java.util.zip.GZIPInputStream;

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
	public void testAppConfigDump() throws InterruptedException, IOException {

		MacGyverAgent agent = getAgentWithMetadata();
		mockServer.enqueue(new MockResponse().setBody("{}"));
		HttpAgentSender sender = new HttpAgentSender().withBaseUrl(mockServer.url("/").toString());

		agent.withSender(sender);

		ArrayNode appConfigs = mapper.createArrayNode();

		ObjectNode config = mapper.createObjectNode();
		config.put("config", "MyConfig");
		config.put("value", "value1");
		appConfigs.add(config);

		config = mapper.createObjectNode();
		config.put("config", "MyPassword");
		config.put("value", "value2");
		appConfigs.add(config);

		agent.reportAppConfigDump(appConfigs);

		RecordedRequest rr = mockServer.takeRequest();

		Assertions.assertThat(rr.getRequestLine()).isEqualTo("POST /api/monitor/app-config-dump HTTP/1.1");

		Assertions.assertThat(rr.getHeader("content-type")).contains("application/json");

		JsonNode n = mapper.readTree(rr.getBody().readUtf8());

		Assertions.assertThat(n.has("appId")).isTrue();
		Assertions.assertThat(n.has("host")).isTrue();
		Assertions.assertThat(n.has("timestamp")).isTrue();
		Assertions.assertThat(n.has("appConfigs")).isTrue();

		JsonNode appConfig = n.get("appConfigs").get(0);
		Assertions.assertThat(appConfig.get("config").asText().equals("MyConfig"));
		Assertions.assertThat(appConfig.get("config").asText().equals("value1"));

		appConfig = n.get("appConfigs").get(1);
		Assertions.assertThat(appConfig.get("config").asText().equals("MyPassword"));
		Assertions.assertThat(appConfig.get("config").asText().equals("*****"));

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
			Assertions.assertThat(e).isInstanceOf(AgentException.class).hasMessageContaining("statusCode=500")
					.hasMessageContaining("POST http://localhost:").hasMessageContaining("app-event");

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
			Assertions.assertThat(e).isInstanceOf(AgentException.class).hasMessageContaining("statusCode=403")
					.hasMessageContaining("POST http://localhost:").hasMessageContaining("app-event");

		}
	}

	@Test
	public void testCloseOnFailure() {
		int count = 2100;
	
		HttpAgentSender sender = new HttpAgentSender().withBaseUrl(mockServer.url("/").toString());
		for (int i = 0; i < count; i++) {
			mockServer.enqueue(new MockResponse().setResponseCode(503).setBody(UUID.randomUUID().toString()));
			try {
				sender.sendAppEvent(mapper.createObjectNode());
			} catch (Exception e) {
				// ignore the exception
			}
		}

	}
	
	@Test
	public void testCloseOnSuccess() {
		int count = 2100;
	
		HttpAgentSender sender = new HttpAgentSender().withBaseUrl(mockServer.url("/").toString());
		for (int i = 0; i < count; i++) {
			mockServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
			try {
				
				sender.sendAppEvent(mapper.createObjectNode());
			} catch (Exception e) {
				// ignore the exception
			}
		}

	}

	private MacGyverAgent getAgentWithMetadata() {
		return new MacGyverAgent().withAppMetadataProvider(new AppMetadataProvider() {
			@Override
			public String getScmRevision() {
				return "123456";
			}

			@Override
			public String getScmBranch() {
				return "release/145";
			}

			@Override
			public String getVersion() {
				return "1.145.0-SNAPSHOT";
			}

			@Override
			public String getAppId() {
				return "test-app";
			}

			@Override
			public String getEnvironment() {
				return "local";
			}

			@Override
			public String getSubEnvironment() {
				return null;
			}

			@Override
			public Date getBuildTime() {
				return null;
			}

			@Override
			public Date getDeployTime() {
				return null;
			}

			@Override
			public JsonNode getExtendedData() {
				return null;
			}
		});
	}
}
