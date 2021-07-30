package io.spaship.operator;


import io.spaship.operator.config.model.WebsiteConfig;
import io.spaship.operator.crd.Website;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

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
            if(args[index].contains("~")){
                object.put(args[index].split("~")[0],stringToJson(args[index].split("~")[1]));
            }else{
                object.put("attr_".concat(Integer.toString(index)),args[index]);
            }

        });
        return object.toString();
    }

    private static Object stringToJson(String input) {
        try {
            input = input.replace("\\","");
            return new JsonObject(input);
        } catch (DecodeException decodeException) {
            LOG.warn("failed to parse string into json detected {} proceeding with string format", decodeException.getMessage());
            return input;
        }
    }

}
