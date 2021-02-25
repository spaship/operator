package io.websitecd.operator.openshift;

import io.quarkus.test.junit.QuarkusTest;
import io.websitecd.operator.rest.WebhookTestCommon;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class OperatorServiceTest extends WebhookTestCommon {


    @Test
    public void testSimpleExample() throws Exception {
        registerSimpleWeb();
    }


    @Test
    public void testAdvancedExample() throws Exception {
        registerAdvancedWeb();
    }

}