package com.sliit.ems.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Thin wrapper around jBCrypt. Add the jBCrypt jar to backend/lib
 * (see README.md) - it is a small, standard dependency, acceptable per
 * project rules for secure local password hashing.
 */
public class PasswordUtil {

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }

    public static boolean verify(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Malformed hash in DB - treat as failed login rather than crashing
            return false;
        }
    }
}
