package org.intermine.web.security;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;

/**
 * Issue certificates for keys we trust.
 *
 * @author Alex Kalderimis
 *
 */
@SuppressWarnings("deprecation")
public class KeySigner
{

    /**
     * Errors thrown when signing certificates.
     * @author Alex Kalderimis
     *
     */
    public final class SigningException extends Exception
    {

        private SigningException(Throwable cause) {
            super(cause);
        }

        private SigningException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    /** The default algorithm to use **/
    public static final String DEFAULT_ALGORITHM = "SHA256withRSA";

    private PrivateKey signingKey;
    private String issuer;
    private int days;
    private String algorithm;

    /**
     * Build a new signer.
     * @param signingKey Our key that we use to sign the other keys with.
     * @param dn the X.509 Distinguished Name, eg "CN=Test, L=London, C=GB"
     * @param days The number of days we issue certificates for.
     * @param algorithm The signing algorithm we use.
     */
    public KeySigner(PrivateKey signingKey, String dn, int days, String algorithm) {
        this.signingKey = signingKey;
        this.issuer = dn;
        this.days = days;
        this.algorithm = algorithm;
    }

    /**
     * Create a self-signed X.509 Certificate
     *
     * Should be eventually replaced with X509v3CertificateBuilder.
     *
     * @param subject Who we trust.
     * @param key The key we are asserting that we trust.
     * @return A certificate wrapping the key, signed by us.
     * @throws SigningException If we cannot generate the certificate for some reason.
     */
    public X509Certificate generateCertificate(String subject, PublicKey key)
        throws SigningException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        X509V3CertificateGenerator cert = new X509V3CertificateGenerator();
        cert.setSerialNumber(BigInteger.valueOf(1));   //or generate a random number
        cert.setSubjectDN(new X509Principal("CN=" + subject));  //see examples to add O,OU etc
        cert.setIssuerDN(new X509Principal(issuer)); //same since it is self-signed
        cert.setPublicKey(key);
        cert.setNotBefore(new Date());
        cert.setNotAfter(new Date(System.currentTimeMillis() + 1000L * 60L * 60L * 24L * days));
        cert.setSignatureAlgorithm(algorithm);

        try {
            return cert.generate(signingKey, "BC");
        } catch (CertificateEncodingException e) {
            throw new SigningException(e);
        } catch (InvalidKeyException e) {
            throw new SigningException(e);
        } catch (IllegalStateException e) {
            throw new SigningException(e);
        } catch (NoSuchProviderException e) {
            throw new SigningException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new SigningException("Unknown algorithm", e);
        } catch (SignatureException e) {
            throw new SigningException(e);
        }
    }
}
