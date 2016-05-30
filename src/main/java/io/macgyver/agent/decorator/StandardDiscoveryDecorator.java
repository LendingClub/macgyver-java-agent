package io.macgyver.agent.decorator;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.macgyver.agent.AppMetadataProvider;
import io.macgyver.agent.MacGyverAgent;

public class StandardDiscoveryDecorator implements StatusDecorator {

	MacGyverAgent agent;

	public StandardDiscoveryDecorator(MacGyverAgent agent) {
		this.agent = agent;
	}


	@Override
	public void decorate(ObjectNode n) {

		
		if (agent != null) {
			Optional<AppMetadataProvider> mdp = agent.getAppMetadataProvider();
		
				mdp.ifPresent(md -> {
					n.put("appId", md.getAppId());
					n.put("revision", md.getRevision());
					n.put("version", md.getVersion());
					n.put("buildTime", formatDate(md.getBuildTime()));
					n.put("deployTime", formatDate(md.getDeployTime()));
				});
				
		}

		n.put("startTime", formatDate(agent.getStartTime()));
		n.put("os.name", System.getProperty("os.name"));
		n.put("os.version", System.getProperty("os.version"));
		n.put("os.arch", System.getProperty("os.arch"));
		n.put("java.version", System.getProperty("java.version"));
		n.put("java.home", System.getProperty("java.home"));

	}

	String formatDate(Date d) {
		if (d == null) {
			return null;
		}

		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("UTC")).format(d.toInstant());

	}
}
