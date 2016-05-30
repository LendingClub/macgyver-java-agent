package io.macgyver.agent.sender.sns;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.regions.Region;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.macgyver.agent.sender.http.HttpAgentSender;
import io.macgyver.agent.sender.sns.SnsAgentSender;

public class SnsAgentSenderTest {

	ObjectMapper mapper = new ObjectMapper();
	@Test
	public void testMissingArn() {
		
		
		
		AmazonSNSClient client = new AmazonSNSClient(new DefaultAWSCredentialsProviderChain());
		client.setRegion(Region.getRegion(Regions.US_WEST_2));
		
		
		
		
		try {
			SnsAgentSender agent = new SnsAgentSender().withAmazonSNSClient(client);
			agent.sendCheckIn(mapper.createObjectNode());
		}
		catch (IllegalStateException e) {
			Assertions.assertThat(e).isInstanceOf(IllegalStateException.class).hasMessageContaining("topicArn not set");
		}
	}
	
	@Test
	public void testMissingClient() {
		

		try {
			SnsAgentSender agent = new SnsAgentSender().withTopicArn("blah");
			agent.sendCheckIn(mapper.createObjectNode());
		}
		catch (IllegalStateException e) {
			Assertions.assertThat(e).isInstanceOf(IllegalStateException.class).hasMessageContaining("SNS client not set");
		}
	}
}
