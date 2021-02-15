package io.websitecd.operator.config.validator;

import com.networknt.schema.ValidationMessage;
import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class InvalidConfigTest {

    private static final Logger log = Logger.getLogger(InvalidConfigTest.class);
    
    @Inject
    ValidatorService validatorService;

    @Test
    void invalidApiVersion() throws IOException {
        try (InputStream is = InvalidConfigTest.class.getResourceAsStream("/invalid-apiVersion.yaml")) {
            Set<ValidationMessage> messages = validatorService.validate(is);
            assertEquals(1, messages.size());
            ValidationMessage msg = messages.iterator().next();
            assertEquals("$.apiVersion", msg.getPath());
            assertEquals("$.apiVersion: integer found, string expected", msg.getMessage());
        }
    }

}