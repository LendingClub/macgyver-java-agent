package io.macgyver.agent;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.regex.Pattern;

/**
 * Created by atsui on 6/23/17.
 */
public class AppConfigScrubber {

    private static String APP_CONFIG_SCRUB_REGEX = ".*_(password|key|token)";
    private static String SCRUBBED_VALUE = "*****";

    static void scrub(ArrayNode appConfigs, String extraScrubRegex) {
        // Determine the scrub pattern
        String scrubRegex = APP_CONFIG_SCRUB_REGEX;
        if (!StringUtils.isNullOrEmpty(extraScrubRegex)) {
            scrubRegex += "|" + extraScrubRegex;
        }
        Pattern scrubPattern = Pattern.compile(scrubRegex, Pattern.CASE_INSENSITIVE);

        for (JsonNode appConfig : appConfigs) {
            if (scrubPattern.matcher(appConfig.get("key").asText()).matches()) {
                ((ObjectNode)appConfig).put("value", SCRUBBED_VALUE);
            }
        }
    }
}
