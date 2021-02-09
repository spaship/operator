package io.websitecd.operator.crd;

import java.util.List;

public class WebsiteEnvs {

    List<String> included;

    List<String> excluded;

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
}
