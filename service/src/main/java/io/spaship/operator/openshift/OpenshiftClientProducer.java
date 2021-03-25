package io.spaship.operator.openshift;

import io.fabric8.openshift.client.DefaultOpenShiftClient;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

@Singleton
public class OpenshiftClientProducer {

    @Produces
    public DefaultOpenShiftClient openshiftClient() {
        return new DefaultOpenShiftClient();
    }
}
