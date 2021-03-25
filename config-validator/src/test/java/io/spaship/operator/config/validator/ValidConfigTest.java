package io.spaship.operator.config.validator;

import com.networknt.schema.ValidationMessage;
import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class ValidConfigTest {

    private static final Logger log = Logger.getLogger(ValidConfigTest.class);

    @Inject
    ValidatorMain validatorMain;

    @Test
    void testSimpleConfig() throws Exception {
        String file = ValidConfigTest.class.getResource("/valid-simple-website.yaml").getFile();
        validatorMain.validate(file);
    }

    @Test
    void testAdvancedConfig() throws Exception {
        String file = ValidConfigTest.class.getResource("/valid-advanced-website.yaml").getFile();
        validatorMain.validate(file);
    }


    void assertValid(Set<ValidationMessage> messages) {
        if (messages.size() != 0) {
            messages.forEach(log::error);
        }
        assertEquals(0, messages.size());

    }
}