package io.websitecd.operator.config.validator;

import com.networknt.schema.ValidationMessage;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
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
    Optional<String> path;

    @Inject
    ValidatorService validatorService;

    @Override
    public int run(String... args) throws Exception {
        return checkAndValidate(path, args);
    }

    public int checkAndValidate(Optional<String> filePath, String... args) throws Exception {
        if (filePath.isEmpty() && args == null) {
            System.out.println("ERROR: No input path specified. Either as parameter or sys env APP_FILE_PATH");
            System.out.println("Usage: java -jar operator-config-validator-1.0.1-SNAPSHOT-runner.jar website.yaml");
            return 1;
        }

        int returnStatus = 0;
        if (filePath.isPresent()) {
            returnStatus = validate(filePath.get());
        }

        if (args != null) {
            for (String arg : args) {
                returnStatus += validate(arg);
            }
        }
        return returnStatus;
    }

    public int validate(String path) throws Exception {
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
            System.out.println(path + " is invalid. Errors:");
            for (ValidationMessage msg : validateMsg) {
                System.out.println(msg);
            }
            return 1;
        }
    }

}
