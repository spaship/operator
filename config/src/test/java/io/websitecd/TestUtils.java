package io.websitecd;

import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.WebsiteConfig;

import java.io.IOException;
import java.io.InputStream;

public class TestUtils {

    public static WebsiteConfig loadConfig(String path) throws IOException {
        try (InputStream is = OperatorConfigUtils.class.getResourceAsStream(path)) {
            return OperatorConfigUtils.loadYaml(is);
        }
    }

}
