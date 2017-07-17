package io.macgyver.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * Created by atsui on 6/23/17.
 */
public class AppConfigScrubberTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testScrubAppConfigs() {
        ArrayNode appConfigs = mapper.createArrayNode();

        ObjectNode config = mapper.createObjectNode();
        config.put("key", "MyConfig");
        config.put("value", "value1");
        appConfigs.add(config);

        config = mapper.createObjectNode();
        config.put("key", "MyPassword");
        config.put("value", "value2");
        appConfigs.add(config);

        config = mapper.createObjectNode();
        config.put("key", "foobar");
        config.put("value", "value3");
        appConfigs.add(config);

        // Test the default scrubber
        AppConfigScrubber.scrub(appConfigs, null);

        JsonNode appConfig = appConfigs.get(0);
        Assertions.assertThat(appConfig.get("key").asText().equals("MyConfig"));
        Assertions.assertThat(appConfig.get("key").asText().equals("value1"));

        appConfig = appConfigs.get(1);
        Assertions.assertThat(appConfig.get("key").asText().equals("MyPassword"));
        Assertions.assertThat(appConfig.get("key").asText().equals("*****"));

        appConfig = appConfigs.get(2);
        Assertions.assertThat(appConfig.get("key").asText().equals("foobar"));
        Assertions.assertThat(appConfig.get("key").asText().equals("value3"));
    }

    @Test
    public void testScrubAppConfigsExtra() {
        MacGyverAgent agent = new MacGyverAgent();

        ArrayNode appConfigs = mapper.createArrayNode();

        ObjectNode config = mapper.createObjectNode();
        config.put("key", "MyConfig");
        config.put("value", "value1");
        appConfigs.add(config);

        config = mapper.createObjectNode();
        config.put("key", "MyPassword");
        config.put("value", "value2");
        appConfigs.add(config);

        config = mapper.createObjectNode();
        config.put("key", "foobar");
        config.put("value", "value3");
        appConfigs.add(config);

        // Test scrubbing with an extra pattern
        AppConfigScrubber.scrub(appConfigs, ".*bar");

        JsonNode appConfig = appConfigs.get(0);
        Assertions.assertThat(appConfig.get("key").asText().equals("MyConfig"));
        Assertions.assertThat(appConfig.get("key").asText().equals("value1"));

        appConfig = appConfigs.get(1);
        Assertions.assertThat(appConfig.get("key").asText().equals("MyPassword"));
        Assertions.assertThat(appConfig.get("key").asText().equals("*****"));

        appConfig = appConfigs.get(2);
        Assertions.assertThat(appConfig.get("key").asText().equals("foobar"));
        Assertions.assertThat(appConfig.get("key").asText().equals("*****"));
    }
}
