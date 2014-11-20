package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class to encapsulate the logic for verifying identity claims as represented as JWTs
 * (JSON Web Tokens).
 *
 * @author Alex Kalderimis
 *
 */
public class JWTVerifier
{
    private static final String EMAIL_CLAIM = "http://wso2.org/claims/emailaddress";
    private final Properties options;
    private final KeyStore keyStore;

    /**
     * Construct a verifier.
     * @param keyStore All our trusted keys.
     * @param options Configurable options.
     */
    public JWTVerifier(KeyStore keyStore, Properties options) {
        this.keyStore = keyStore;
        this.options = options;
    }

    /**
     * Verify this token and return a verification result.
     *
     * @param rawString The string representation of the token.
     * @return The verification result.
     * @throws VerificationError if this claim cannot be verified.
     */
    public Verification verify(final String rawString) throws VerificationError {
        Base64 decoder = new Base64();
        if (StringUtils.isBlank(rawString)) {
            throw new VerificationError("token is blank");
        }
        String[] pieces = rawString.split("\\.");
        if (pieces.length != 3) {
            throw new VerificationError("Illegal JWT token.");
        }
        JSONObject header, claims;
        long expiry;
        String issuer;
        try {
            header = new JSONObject(new String(decoder.decode(pieces[0])));
            claims = new JSONObject(new String(decoder.decode(pieces[1])));
            expiry = claims.getLong("exp");
            issuer = claims.getString("iss");
        } catch (JSONException e) {
            throw new VerificationError("Could not parse token: " + e.getMessage());
        }

        // expiry is, as per spec, the number of seconds since the epoch.
        long secondsSinceExpiry = (System.currentTimeMillis() / 1000L) - expiry;
        if (secondsSinceExpiry >= 0) {
            throw new VerificationError(
                    String.format("This token expired %d seconds ago", secondsSinceExpiry));
        }

        if (!verifySignature(
                header,
                claims,
                pieces[0] + "." + pieces[1],
                decoder.decode(pieces[2]))) {
            throw new VerificationError("Could not verify signature.");
        }
        return new Verification(issuer, getPrincipal(claims), getEmail(claims));
    }

    /* Private API */

    private String getKeyAlias(String issuer) {
        return options.getProperty("security.keystore.alias." + issuer);
    }

    private String getPrincipal(JSONObject claims) throws VerificationError {
        try {
            String issuer = claims.getString("iss");
            if (options.containsKey("jwt.key.sub." + issuer)) {
                return claims.getString(options.getProperty("jwt.key.sub." + issuer));
            } else {
                return claims.getString("sub");
            }
        } catch (JSONException e) {
            throw new VerificationError("Could not read principal: " + e.getMessage());
        }
    }

    private String getEmail(JSONObject claims) throws VerificationError {
        try {
            String issuer = claims.getString("iss");
            String emailClaim = options.getProperty("jwt.key.email." + issuer, EMAIL_CLAIM);
            if (claims.has(emailClaim)) {
                return claims.getString(emailClaim);
            }
            return null;
        } catch (JSONException e) {
            throw new VerificationError("Could not read email: " + e.getMessage());
        }
    }

    private KeyStore getKeyStore() throws VerificationError {
        return keyStore;
    }

    /* A veritable cornucopia of trying and catching */
    private boolean verifySignature(
            JSONObject header,
            JSONObject claims,
            String signed,
            byte[] toVerify)
        throws VerificationError {

        if (toVerify == null || toVerify.length == 0) {
            throw new VerificationError("Cannot verify an unsigned token");
        }

        String issuer, algorithm;
        try {
            issuer = claims.getString("iss");
            algorithm = header.getString("alg");
        } catch (JSONException e) {
            throw new VerificationError("Missing required property.");
        }

        KeyStore ks = getKeyStore();

        // algorithm should be something like "SHA256withRSA"
        Signature signature;
        try {
            signature = Signature.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new VerificationError(e.getMessage());
        }
        String keyAlias = getKeyAlias(issuer);
        if (StringUtils.isBlank(keyAlias)) {
            throw new VerificationError("Unknown identity issuer: " + issuer);
        }

        if (algorithm.endsWith("withRSA")) {
            try {
                Certificate cert = ks.getCertificate(keyAlias);
                PublicKey key = cert.getPublicKey();
                signature.initVerify(key);
            } catch (InvalidKeyException e) {
                throw new VerificationError("Key is invalid. " + e.getMessage());
            } catch (KeyStoreException e) {
                throw new VerificationError("Could not retrieve key. " + e.getMessage());
            }
        } else {
            throw new VerificationError("Unsupported signing algorithm: " + algorithm);
        }

        try {
            signature.update(signed.getBytes());
        } catch (SignatureException e) {
            throw new VerificationError(e.getMessage());
        }

        try {
            return signature.verify(toVerify);
        } catch (SignatureException e) {
            throw new VerificationError(e.getMessage());
        }
    }

    /**
     * The result of a successful verification.
     * @author Alex Kalderimis
     *
     */
    public static final class Verification
    {
        private final String identity, issuer, email;

        private Verification(String issuer, String identity, String email) {
            this.issuer = issuer;
            this.identity = identity;
            this.email = email;
        }

        /** @return the name of the issuer **/
        public String getIssuer() {
            return issuer;
        }

        /** @return the identity of the claimant **/
        public String getIdentity() {
            return identity;
        }

        /** @return the email of the claimant **/
        public String getEmail() {
            return email;
        }
    }

    /**
     * An error that declares that something has gone wrong when trying to
     * verify a JWT.
     * @author Alex Kalderimis
     *
     */
    public static final class VerificationError extends Exception
    {
        private static final long serialVersionUID = 1215260310118002737L;

        /**
         * Initialise a new verification error with a message detailing the problem.
         * @param problem The problem we are reporting.
         */
        public VerificationError(String problem) {
            super(problem);
        }
    }
}
