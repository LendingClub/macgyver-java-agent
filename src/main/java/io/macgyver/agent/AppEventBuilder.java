package io.macgyver.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.macgyver.agent.MacGyverAgent.AppEventType;

public class AppEventBuilder {

	static ObjectMapper mapper = new ObjectMapper();
	
	ObjectNode data = mapper.createObjectNode();
	

	

	
	
	public AppEventBuilder withEventType(AppEventType type) {
		data.put("eventType", type.toString());
		return this;
	}
	public AppEventBuilder withAppId(String id) {
		data.put("appId", id);
		return this;
	}
	public AppEventBuilder withMessage(String message) {
		data.put("message", message);
		return this;
	}
	public AppEventBuilder withHost(String host) {
		data.put("host", host);
		return this;
	}
	public AppEventBuilder withAttribute(String key, String val) {
		data.put(key, val);
		return this;
	}
	public ObjectNode build()  {
		return data;
	}
}
