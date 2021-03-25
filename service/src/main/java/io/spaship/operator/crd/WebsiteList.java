package io.spaship.operator.crd;

import io.fabric8.kubernetes.client.CustomResourceList;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class WebsiteList extends CustomResourceList<Website> {

}