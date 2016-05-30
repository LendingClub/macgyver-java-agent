package io.macgyver.agent;

import java.util.Date;

public interface AppMetadataProvider {

	String getRevision();
	String getVersion();
	String getAppId();
	Date getBuildTime();
	Date getDeployTime();
}
