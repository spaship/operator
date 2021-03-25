package io.spaship;

import io.spaship.operator.config.OperatorConfigUtils;
import io.spaship.operator.config.model.WebsiteConfig;

import java.io.IOException;
import java.io.InputStream;

public class TestUtils {

    public static WebsiteConfig loadConfig(String path) throws IOException {
        try (InputStream is = OperatorConfigUtils.class.getResourceAsStream(path)) {
            return OperatorConfigUtils.loadYaml(is);
        }
    }

}
