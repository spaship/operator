package io.websitecd.operator.config.validator;

import com.networknt.schema.ValidationMessage;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

@QuarkusMain
public class ValidatorMain implements QuarkusApplication {

    @ConfigProperty(name = "app.file.path")
    Optional<String> filePath;

    @Inject
    ValidatorService validatorService;

    @Override
    public int run(String... args) throws Exception {
        String path = null;
        if (filePath.isPresent()) {
            path = filePath.get();
        }
        if (args != null && args.length > 0) {
            path = args[0];
        }

        if (StringUtils.isEmpty(path)) {
            System.out.println("ERROR: No input path specified. Either as parameter or sys env APP_FILE_PATH");
            System.out.println("Usage: java -jar operator-config-validator-1.0.1-SNAPSHOT-runner.jar website.yaml");
            return 1;
        }

        File configFile = new File(path);
        if (!configFile.exists()) {
            throw new Exception("Config file not exists. path=" + configFile.getAbsolutePath());
        }
        try (InputStream is = new FileInputStream(configFile)) {
            Set<ValidationMessage> validateMsg = validatorService.validate(is);
            if (validateMsg.isEmpty()) {
                System.out.println(path + " is valid");
                return 0;
            }
            for (ValidationMessage msg : validateMsg) {
                System.out.println(msg);
            }
            return 1;

        }
    }

}
