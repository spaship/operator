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
class ValidConfigTest {

    private static final Logger log = Logger.getLogger(ValidConfigTest.class);

    @Inject
    ValidatorService validatorService;

    @Test
    void testSimpleConfig() throws IOException {
        try (InputStream is = ValidConfigTest.class.getResourceAsStream("/valid-simple-website.yaml")) {
            assertValid(validatorService.validate(is));
        }
    }

    @Test
    void testAdvancedConfig() throws IOException {
        try (InputStream is = ValidConfigTest.class.getResourceAsStream("/valid-advanced-website.yaml")) {
            assertValid(validatorService.validate(is));
        }
    }


    void assertValid(Set<ValidationMessage> messages) {
        if (messages.size() != 0) {
            messages.forEach(log::error);
        }
        assertEquals(0, messages.size());

    }
}