package org.intermine.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import net.sourceforge.iharder.Base64;

/**
 * Utility methods for managing hashed passwords
 *
 * @author Matthew Wakeling
 */
public final class PasswordHasher
{
    private PasswordHasher() {
        // don't
    }

    /**
     * Converts a password into a hashed password, with a salt.
     *
     * @param password the password to hash
     * @return a 88-character String containing the salt and the hash
     */
    public static String hashPassword(String password) {
        if ((password.length() == 88) && (password.charAt(43) == '=')
                && (password.charAt(87) == '=')) {
            return password;
        }
        try {
            byte[] salt = new byte[32];
            SecureRandom sr = new SecureRandom();
            sr.nextBytes(salt);
            String saltString = Base64.encodeBytes(salt);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((saltString + password).getBytes());
            byte[] digest = md.digest();
            String hashString = Base64.encodeBytes(digest);
            return saltString + hashString;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks to see if a password matches an optionally hashed password entry.
     *
     * @param password the given password
     * @param hash the hashed password entry
     * @return true if the password matches
     */
    public static boolean checkPassword(String password, String hash) {
        if (password == null && hash == null) {
            return true;
        }
        try {
            if ((hash.length() == 88) && (hash.charAt(43) == '=') && (hash.charAt(87) == '=')) {
                String saltString = hash.substring(0, 44);
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update((saltString + password).getBytes());
                byte[] digest = md.digest();
                String hashString = Base64.encodeBytes(digest);
                if (hashString.equals(hash.substring(44))) {
                    return true;
                }
            } else {
                if (password != null) {
                    return password.equals(hash);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}
