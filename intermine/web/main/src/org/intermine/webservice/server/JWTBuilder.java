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
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.intermine.api.profile.Profile;
import org.json.JSONObject;

/**
 * A class that encapsulates the logic required to generate JWT tokens
 * for users.
 *
 * @author Alex Kalderimis
 */
public final class JWTBuilder
{

    private static final String TO_STR_FMT =
            "JWTBuilder(algorithm = %s, key = %s, issuer = %s)";
    private static final String BAD_VALIDITY =
            "minimum validity is 1 second - you requested ";
    private static final String ALGORITHM_NOT_SUPPORTED =
            "This algorithm (%s) is not supported by this JVM.";
    private static final String BAD_PROFILE =
            "Profile must exist and be for a registered user, you provided ";

    /**
     * The signing algorithms this builder supports.
     * @author Alex Kalderimis
     *
     */
    public enum Algorithm {
        /** SHA256withRSA **/
        SHA256withRSA,
        /** SHA384withRSA **/
        SHA384withRSA,
        /** SHA512withRSA **/
        SHA512withRSA;

        /**
         * @return A signature, ready to start performing validation.
         */
        public Signature createSignature() {
            try {
                return Signature.getInstance(name());
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(String.format(ALGORITHM_NOT_SUPPORTED, name()));
            }
        }
    }

    private final Algorithm algorithm;
    private final PrivateKey key;
    private final String issuer;

    /**
     * Constructor
     * @param key The private key to sign the tokens with.
     * @param issuer The identity of this builder, reported in the 'iss' header claim.
     */
    public JWTBuilder(PrivateKey key, String issuer) {
        this.algorithm = Algorithm.SHA256withRSA;
        this.key = key;
        this.issuer = issuer;
        verify();
    }

    /**
     * Constructor
     * @param algorithm The signing algorithm
     * @param key The key used to sign the token.
     * @param issuer The identity of this builder, reported in the 'iss' header claim.
     */
    public JWTBuilder(Algorithm algorithm, PrivateKey key, String issuer) {
        this.algorithm = algorithm;
        this.key = key;
        this.issuer = issuer;
        verify();
    }

    private void verify() {
        if (algorithm == null || key == null || issuer == null) {
            throw new NullPointerException("No argument can be null for " + toString());
        }
    }

    /**
     * Issue a new token for the given profile.
     * @param profile The profile to issue a token for.
     * @param validForSeconds The number of seconds the token should be valid for.
     * @return The JWT token
     * @throws InvalidKeyException If the private key is not valid.
     * @throws SignatureException If we cannot sign the token.
     */
    public String issueToken(Profile profile, int validForSeconds)
        throws InvalidKeyException, SignatureException {

        if (validForSeconds < 1) {
            throw new IllegalArgumentException(BAD_VALIDITY + validForSeconds);
        }
        if (profile == null || !profile.isLoggedIn()) {
            throw new IllegalArgumentException(BAD_PROFILE + profile);
        }

        String sub = profile.getUsername();
        String email = profile.getEmailAddress();
        long absoluteExpiry = System.currentTimeMillis() + (validForSeconds * 1000L);
        return issueToken(sub, email, absoluteExpiry);
    }

    /**
     * Issue a token for the given subject and email address.
     *
     * This method has restricted visibility to recognise the fact that it can issue
     * inconsistent tokens (bad user names and email addresses) as well as the fact that
     * it is capable of issuing expired tokens. Do not use this method in production code - use
     * <code>issueToken(Profile, int)</code>
     *
     * @param subject The subject of the token
     * @param email The subject's email
     * @param absoluteExpiry The absolute timestamp to use as the 'exp' claim in milliseconds.
     * @return A signed JWT
     * @throws InvalidKeyException If the private key is not valid.
     * @throws SignatureException If we cannot sign the token.
     */
    String issueToken(String subject, String email, long absoluteExpiry)
        throws InvalidKeyException, SignatureException {

        Map<String, Object> header = new HashMap<String, Object>();
        header.put("alg", algorithm.name());
        header.put("typ", "JWT");

        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("sub", subject);
        claims.put("iss", issuer);
        claims.put("exp", (absoluteExpiry / 1000));
        claims.put("iat", (System.currentTimeMillis() / 1000));
        claims.put("http://wso2.org/claims/emailaddress", email);

        String toSign = encodeContent(header, claims);

        byte[] signature = sign(toSign);
        return toSign + "." + Base64.encodeBase64URLSafeString(signature);
    }

    private String encodeContent(Map<String, Object> header,
            Map<String, Object> claims) {
        String toSign = String.format("%s.%s",
                Base64.encodeBase64URLSafeString(new JSONObject(header).toString().getBytes()),
                Base64.encodeBase64URLSafeString(new JSONObject(claims).toString().getBytes()));
        return toSign;
    }

    private byte[] sign(String toSign) throws InvalidKeyException, SignatureException {
        Signature signing = algorithm.createSignature();
        signing.initSign(key);
        signing.update(toSign.getBytes());

        byte[] signature = signing.sign();
        return signature;
    }

    // Object contract methods.

    @Override
    public String toString() {
        return String.format(TO_STR_FMT, algorithm, key, issuer);
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder();
        hcb.append(algorithm).append(key).append(issuer);
        return hcb.toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof JWTBuilder) {
            JWTBuilder jwtb = (JWTBuilder) other;
            return algorithm.equals(jwtb.algorithm)
                    && key.equals(jwtb.key)
                    && issuer.equals(jwtb.issuer);
        }
        return false;
    }
}
