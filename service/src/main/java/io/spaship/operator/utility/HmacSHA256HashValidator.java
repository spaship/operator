package io.spaship.operator.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public class HmacSHA256HashValidator {

    private HmacSHA256HashValidator(){}


    private static final Logger LOG = LoggerFactory.getLogger(HmacSHA256HashValidator.class);

    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String HMAC_SHA_ALGORITHM = "HmacSHA256";
    private static final char[] HEX = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'a', 'b', 'c', 'd', 'e', 'f'};


    public static Function<String, Boolean> generateHash(String payload, String secret) {

        LOG.debug("secret token is {}", secret);
        //The BinaryOperator<T> is in fact a sub-interface of BiFunction<T, T, T>
        BinaryOperator<String> calculatedPayloadHash = HmacSHA256HashValidator::base16HmacSha256;
        return hashedPayload -> trimSignaturePrefix(hashedPayload).equals(calculatedPayloadHash.apply(secret, payload));

    }


    private static String trimSignaturePrefix(String input) {
        return input.replace(SIGNATURE_PREFIX, "");
    }

    public static String base16HmacSha256(String secretKey, String message) {
        byte[] hmacSha256 = calcHmacSha256(secretKey
                        .getBytes(StandardCharsets.UTF_8),
                message.getBytes(StandardCharsets.UTF_8)
        );
        String base16Encode = base16Encode(hmacSha256);
        LOG.debug("Generated base16 hmacSha256 hash  value is {}", base16Encode);
        return base16Encode;
    }

    private static byte[] calcHmacSha256(byte[] secret, byte[] message) {
        byte[] hmacSha256 = null;
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret, HMAC_SHA_ALGORITHM);
            mac.init(secretKeySpec);
            hmacSha256 = mac.doFinal(message);
        } catch (Exception e) {
            LOG.error("Failed to compute HmacSHA256 due to {}", e.getMessage());
            return new byte[1];
        }
        return hmacSha256;
    }


    private static String base16Encode(byte[] byteArray) {
        StringBuilder hexBuffer = new StringBuilder(byteArray.length * 2);
        for (byte b : byteArray)
            for (int j = 1; j >= 0; j--)
                hexBuffer.append(HEX[(b >> (j * 4)) & 0xF]);
        return hexBuffer.toString();
    }


}
