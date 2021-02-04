package io.websitecd.operator.config;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ResourceRequirements;

public class KubernetesUtils {

    public static void overrideContainer(Container initial, Container override) {
        if (override == null) return;
        ResourceRequirements resources = override.getResources();
        if (resources != null) {
            if (initial.getResources() == null) {
                initial.setResources(resources);
            } else {
                if (resources.getRequests() != null && resources.getRequests().size() > 0) {
                    if (initial.getResources().getRequests() == null) {
                        initial.getResources().setRequests(resources.getRequests());
                    } else {
                        if (resources.getRequests().containsKey("cpu"))
                            initial.getResources().getRequests().put("cpu", resources.getRequests().get("cpu"));
                        if (resources.getRequests().containsKey("memory"))
                            initial.getResources().getRequests().put("memory", resources.getRequests().get("memory"));
                    }
                }
                if (resources.getLimits() != null && resources.getLimits().size() > 0) {
                    if (initial.getResources().getLimits() == null) {
                        initial.getResources().setLimits(resources.getLimits());
                    } else {
                        if (resources.getLimits().containsKey("cpu"))
                            initial.getResources().getLimits().put("cpu", resources.getLimits().get("cpu"));
                        if (resources.getLimits().containsKey("memory"))
                            initial.getResources().getLimits().put("memory", resources.getLimits().get("memory"));
                    }
                }
            }
        }
    }
}
