package io.macgyver.agent.decorator;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.macgyver.agent.AppMetadataProvider;
import io.macgyver.agent.MacGyverAgent;

public class StandardDiscoveryDecorator implements StatusDecorator {

	MacGyverAgent agent;

	ObjectMapper mapper = new ObjectMapper();

	Logger logger = LoggerFactory.getLogger(StandardDiscoveryDecorator.class);

	public StandardDiscoveryDecorator(MacGyverAgent agent) {
		this.agent = agent;
	}

	protected void safeSet(ObjectNode n, String attr, Function<Object, Object> f) {

		try {
			Object val = f.apply(null);
			if (val == null) {
				n.set(attr, null);
			} else if (val instanceof Date) {
				n.put(attr, formatDate((Date) val));
			} else {
				n.set(attr, mapper.convertValue(val, JsonNode.class));
			}
		} catch (RuntimeException e) {
			logger.warn("could not obtain value for "+attr+": "+e.toString());
		}

	}

	@Override
	public void decorate(ObjectNode n) {

		if (agent != null) {
			Optional<AppMetadataProvider> mdp = agent.getAppMetadataProvider();

			mdp.ifPresent(md -> {
				safeSet(n, "appId", x -> md.getAppId());
				safeSet(n, "version", x -> md.getVersion());
				safeSet(n, "scmRevision", x -> md.getScmRevision());
				safeSet(n, "scmBranch", x -> md.getScmBranch());
				safeSet(n, "buildTime", x -> formatDate(md.getBuildTime()));
				safeSet(n, "deployTime", x -> formatDate(md.getBuildTime()));
				safeSet(n, "environment", x -> md.getEnvironment());
				safeSet(n, "subEnvironment", x -> md.getSubEnvironment());
				JsonNode extendedData = md.getExtendedData();
				if (extendedData != null) {

					Iterator<Entry<String, JsonNode>> t = extendedData.fields();
					while (t.hasNext()) {
						Entry<String, JsonNode> entry = t.next();
						n.set(entry.getKey(), entry.getValue());
					}
				}

			});

		}

		safeSet(n, "startTime", x -> formatDate(agent.getStartTime()));

		n.put("osName", System.getProperty("os.name"));
		n.put("osVersion", System.getProperty("os.version"));
		n.put("osArch", System.getProperty("os.arch"));
		n.put("javaVersion", System.getProperty("java.version"));
		n.put("javaHome", System.getProperty("java.home"));

	}

	protected String formatDate(Date d) {
		if (d == null) {
			return null;
		}

		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("UTC")).format(d.toInstant());

	}
}
