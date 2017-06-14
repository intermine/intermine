package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.intermine.web.security.KeySourceException;
import org.intermine.web.security.PublicKeySource;
import org.jfree.util.Log;
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
    /** This is the key used to establish whether we should verify the audience. **/
    public static final String VERIFYAUDIENCE = "jwt.verifyaudience";
    /** The strategy used to verify tokens **/
    public static final String VERIFICATION_STRATEGY = "jwt.verification.strategy";
    /** This is where we get the list of aliases from if the strategy is WHITELIST **/
    public static final String WHITELIST = "jwt.alias.whitelist";

    private static final String EMAIL_CLAIM = "http://wso2.org/claims/emailaddress";
    private static final String NOT_FOR_US = "This token was issued for %s. We are %s";
    private static final String NO_PUBLIC_IDENTITY =
            "Could not verify audience - no public identity";

    private final Properties options;
    private final PublicKeySource publicKeys;
    private final String strategy;

    /**
     * Construct a verifier.
     * @param publicKeys All our trusted keys.
     * @param options Configurable options.
     */
    public JWTVerifier(PublicKeySource publicKeys, Properties options) {
        this.publicKeys = publicKeys;
        this.options = options;
        if (publicKeys == null) {
            throw new NullPointerException("publicKeys must not be null");
        }
        if (options == null) {
            throw new NullPointerException("options must not be null");
        }
        this.strategy = this.options.getProperty(VERIFICATION_STRATEGY, "NAMED_ALIAS");
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
        String audience;
        try {
            header = new JSONObject(new String(decoder.decode(pieces[0])));
            claims = new JSONObject(new String(decoder.decode(pieces[1])));
            expiry = claims.getLong("exp");
            issuer = claims.getString("iss");
            audience = claims.optString("aud");
        } catch (JSONException e) {
            throw new VerificationError("Could not parse token: " + e.getMessage());
        }

        verifyAudience(audience);

        // expiry is, as per spec, the number of seconds since the epoch.
        long secondsSinceExpiry = (System.currentTimeMillis() / 1000L) - expiry;
        if (secondsSinceExpiry >= 0) {
            throw new VerificationError(
                    String.format("This token expired %d seconds ago", secondsSinceExpiry));
        }

        if (!canVerify(
                header,
                claims,
                pieces[0] + "." + pieces[1],
                decoder.decode(pieces[2]))) {
            throw new VerificationError("Could not verify signature.");
        }
        return new Verification(issuer, getPrincipal(claims), getEmail(claims));
    }

    /* Private API */

    private void verifyAudience(String audience) throws VerificationError {
        boolean verifyAudience =
                "true".equalsIgnoreCase(options.getProperty(VERIFYAUDIENCE, "true"));
        if (verifyAudience && StringUtils.isNotBlank(audience)) {
            String self = options.getProperty("jwt.publicidentity");
            if (self == null) {
                throw new VerificationError(NO_PUBLIC_IDENTITY);
            }
            if (!self.equals(audience)) {
                throw new VerificationError(String.format(NOT_FOR_US, audience, self));
            }
        }
    }

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

    /* A veritable cornucopia of trying and catching */
    private boolean canVerify(
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
            throw new VerificationError("Missing required property: " + e.getMessage());
        }
       // algorithm should be something like "SHA256withRSA"
        if (!algorithm.endsWith("withRSA")) {
            throw new VerificationError("Unsupported signing algorithm: " + algorithm);
        }
        Log.debug("Verifying using " + strategy + " strategy");
        try {
            if ("NAMED_ALIAS".equals(strategy)) {
                return verifyNamedAlias(signed, toVerify, issuer, algorithm);
            } else if ("ANY".equals(strategy)) {
                return verifyAnyAlias(signed, toVerify, algorithm);
            } else if ("WHITELIST".equals(strategy)) {
                return verifyWhitelistedAliases(signed, toVerify, algorithm);
            } else {
                throw new VerificationError("Unknown verification strategy: " + strategy);
            }
        } catch (KeySourceException e) {
            throw new VerificationError("Could not retrieve public key");
        }
    }

    private boolean verifyWhitelistedAliases(String signed, byte[] toVerify, String algorithm)
        throws VerificationError, KeySourceException {
        String[] names = options.getProperty(WHITELIST, "").split(",");
        Log.debug("Using any of " + StringUtils.join(names, ", ") + " to verify JWT");
        for (PublicKey key: publicKeys.getSome(names)) {
            if (verifySignature(key, algorithm, signed, toVerify)) {
                return true;
            }
        }
        return false;
    }

    private boolean verifyAnyAlias(String signed, byte[] toVerify, String algorithm)
        throws VerificationError, KeySourceException {
        for (PublicKey key: publicKeys.getAll()) {
            if (verifySignature(key, algorithm, signed, toVerify)) {
                return true;
            }
        }
        return false;
    }

    private boolean verifyNamedAlias(String signed, byte[] toVerify, String issuer, String alg)
        throws VerificationError, KeySourceException {
        String keyAlias = getKeyAlias(issuer);
        if (StringUtils.isBlank(keyAlias)) {
            throw new VerificationError("Unknown identity issuer: " + issuer);
        }
        Log.debug("Using key aliased as " + keyAlias + " to verify JWT");
        return verifySignature(publicKeys.get(keyAlias), alg, signed, toVerify);
    }

    private boolean verifySignature(PublicKey key, String algorithm, String signed, byte[] toVerify)
        throws VerificationError {
        Signature signature;
        try {
            signature = Signature.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new VerificationError(e.getMessage());
        }
        try {
            signature.initVerify(key);
        } catch (InvalidKeyException e) {
            throw new VerificationError("Key is invalid. " + e.getMessage());
        }

        try {
            signature.update(signed.getBytes());
        } catch (SignatureException e) {
            throw new VerificationError("Error creating signature: " + e.getMessage());
        }

        try {
            return signature.verify(toVerify);
        } catch (SignatureException e) {
            throw new VerificationError("Error during verification: " + e.getMessage());
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
