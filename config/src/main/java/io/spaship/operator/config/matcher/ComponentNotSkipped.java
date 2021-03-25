package io.spaship.operator.config.matcher;

import io.spaship.operator.config.model.ComponentConfig;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Matcher to check if component is not skipped (enabled) for target environment and context to skip
 */
public class ComponentNotSkipped implements Predicate<ComponentConfig> {

    Set<String> skipContexts;

    public ComponentNotSkipped(Set<String> skipContexts) {
        this.skipContexts = skipContexts;
    }

    @Override
    public boolean test(ComponentConfig component) {
        if (skipContexts != null && skipContexts.contains(component.getContext())) {
            return false;
        }
        return true;
    }

}
