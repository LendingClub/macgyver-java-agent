package io.macgyver.agent.decorator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.macgyver.agent.MacGyverAgent;

public class HostStatusDecorator implements StatusDecorator {

	@Override
	public void decorate(ObjectNode status) {

		status.put("host",MacGyverAgent.getUnqualifiedHostname());
		status.put("ip", MacGyverAgent.getHostIp());
		status.put("dnsName",MacGyverAgent.getDnsName());
	}

}
