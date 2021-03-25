package io.spaship.operator.config.matcher;

import io.spaship.operator.config.model.ComponentConfig;

import java.util.function.Predicate;

/**
 * Matcher to test if component is given kind.
 */
public class ComponentKindMatcher implements Predicate<ComponentConfig> {

    public static ComponentKindMatcher ComponentGitMatcher = new ComponentKindMatcher(ComponentConfig.KIND_GIT);
    public static ComponentKindMatcher ComponentServiceMatcher = new ComponentKindMatcher(ComponentConfig.KIND_SERVICE);

    String kind;

    public ComponentKindMatcher(String kind) {
        this.kind = kind;
    }

    @Override
    public boolean test(ComponentConfig c) {
        return kind.equals(c.getKind());
    }
}
