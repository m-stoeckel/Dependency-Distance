package org.texttechnologylab.mdd.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA1 {

    private static final MessageDigest SHA_1_DIGEST;

    static {
        try {
            SHA_1_DIGEST = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not found", e);
        }
    }

    public static MessageDigest getSha1Digest() {
        return SHA_1_DIGEST;
    }
}
