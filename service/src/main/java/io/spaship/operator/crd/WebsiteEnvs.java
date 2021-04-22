package io.spaship.operator.crd;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;
import java.util.Objects;

@RegisterForReflection
public class WebsiteEnvs {

    List<String> included;

    List<String> excluded;

    public WebsiteEnvs() {
    }

    public WebsiteEnvs(List<String> included, List<String> excluded) {
        this.included = included;
        this.excluded = excluded;
    }

    public List<String> getIncluded() {
        return included;
    }

    public void setIncluded(List<String> included) {
        this.included = included;
    }

    public List<String> getExcluded() {
        return excluded;
    }

    public void setExcluded(List<String> excluded) {
        this.excluded = excluded;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebsiteEnvs{");
        sb.append("included=").append(included);
        sb.append(", excluded=").append(excluded);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebsiteEnvs that = (WebsiteEnvs) o;
        return Objects.equals(included, that.included) && Objects.equals(excluded, that.excluded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(included, excluded);
    }
}
