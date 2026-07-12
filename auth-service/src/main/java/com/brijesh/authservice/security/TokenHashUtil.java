package com.brijesh.authservice.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class TokenHashUtil {

    private TokenHashUtil(){

    }

    /*
      SHA-256 hash of the token - stored in db instead of raw token.
      one way - cannot be reversed. used to look up tokens by their hash.
     */
    public static String hash(String token){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e){
            throw new IllegalStateException("SHA-256 algorithm not available",e);
        }
    }
}
