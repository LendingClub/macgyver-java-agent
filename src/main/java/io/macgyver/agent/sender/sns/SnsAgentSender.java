package io.macgyver.agent.sender.sns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.macgyver.agent.MacGyverAgent.MessageType;

public class SnsAgentSender implements io.macgyver.agent.MacGyverAgent.Sender {

	ObjectMapper mapper = new ObjectMapper();

	AmazonSNSClient client;
	String topicArn;

	Logger logger = LoggerFactory.getLogger(SnsAgentSender.class);
	public <T extends SnsAgentSender> T withTopicArn(String arn) {
		this.topicArn = arn;
		return (T) this;
	}

	public <T extends SnsAgentSender> T withAmazonSNSClient(AmazonSNSClient client) {
		this.client = client;
		return (T) this;
	}

	void init() {
		if (client==null) {
			throw new IllegalStateException("SNS client not set");
		}
		
	}
	public String getTopicArnForMessageType(MessageType type) {
		
		if (topicArn==null) {
			throw new IllegalStateException("topicArn not set");
		}
		
	
		return topicArn;
	}
	protected void send(MessageType type, ObjectNode data) {
		init();
		PublishRequest pr = new PublishRequest();

		ObjectNode wrapper = mapper.createObjectNode();

		wrapper.put("messageType", type.toString());
		wrapper.put("ts", System.currentTimeMillis());
		wrapper.set("data", data);

		pr.setMessage(wrapper.toString());
		String topic = getTopicArnForMessageType(type);
		logger.debug("sending message type={} to topicArn={}",type,topic);
		pr.setTopicArn(topic);

		PublishResult result = client.publish(pr);
		
	}

	@Override
	public void sendCheckIn(ObjectNode status) {
		send(MessageType.APP_CHECK_IN,status);
	}

	@Override
	public void sendThreadDump(ObjectNode threadDump) {
		send(MessageType.THREAD_DUMP,threadDump);

	}

	@Override
	public void sendAppEvent(ObjectNode n) {
		send(MessageType.APP_EVENT,n);

	}

}
