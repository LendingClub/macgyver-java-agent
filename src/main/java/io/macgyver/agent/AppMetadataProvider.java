package io.macgyver.agent;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public interface AppMetadataProvider {

	String getScmRevision();
	String getScmBranch();
	String getVersion();
	String getAppId();
	String getEnvironment();
	String getSubEnvironment();
	Date getBuildTime();
	Date getDeployTime();
	JsonNode getExtendedData();
	
}
