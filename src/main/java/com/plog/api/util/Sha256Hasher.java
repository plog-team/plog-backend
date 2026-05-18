package com.plog.api.util;

import org.apache.commons.codec.digest.DigestUtils;

public final class Sha256Hasher {

    private Sha256Hasher() {}

    public static String hex(byte[] bytes) {
        return DigestUtils.sha256Hex(bytes);
    }
}
