package io.websitecd.operator.crd.matcher;

import io.websitecd.operator.crd.Website;
import io.websitecd.operator.crd.WebsiteEnvs;

import java.util.function.Predicate;

public class EnvIncluded implements Predicate<String> {

    Website website;

    public EnvIncluded(Website website) {
        this.website = website;
    }

    @Override
    public boolean test(String env) {
        return isEnvEnabled(website, env);
    }

    public static boolean isEnvEnabled(Website website, String env) {
        if (website.getSpec() == null || website.getSpec().getEnvs() == null) {
            return true;
        }
        WebsiteEnvs envs = website.getSpec().getEnvs();
        if (envs.getIncluded() != null) {
            for (String include : envs.getIncluded()) {
                if (env.matches(include)) {
                    return true;
                }
            }
            return false;
        }
        if (envs.getExcluded() != null) {
            for (String exclude : envs.getExcluded()) {
                if (env.matches(exclude)) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }
}
