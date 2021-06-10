package io.spaship.operator;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.spaship.operator.config.model.WebsiteConfig;
import io.spaship.operator.crd.Website;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class Utils {

    public static Map<String, String> defaultLabels(String env, Website website) {
        WebsiteConfig config = website.getConfig();
        Map<String, String> labels = new HashMap<>();
        labels.put("website", website.getMetadata().getName());
        labels.put("env", env);
        labels.put("managedBy", "spaship-operator");
        if (config.getLabels() != null) {
            labels.putAll(config.getLabels());
        }
        return labels;
    }

    public static String getWebsiteName(Website website) {
        return website.getMetadata().getName();
    }

    public static String buildEventPayload(String ... args){
        var object = new JsonObject();
        IntStream.range(0, args.length).forEach(index->{
            if(args[index].contains(":")){
                object.put(args[index].split(":")[0],args[index].split(":")[1]);
            }else{
                object.put("attr_".concat(Integer.toString(index)),args[index]);
            }

        });
        return object.toString();
    }

}
