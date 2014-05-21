package org.intermine.webservice.server;

import static org.junit.Assert.*;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.oltu.oauth2.jwt.JWT;
import org.apache.oltu.oauth2.jwt.io.JWTWriter;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.intermine.webservice.server.JWTVerifier.VerificationError;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JWTVerifierTest {

    private KeyStore ks = null;
    private String token = null;
    private String wrongSig = null;
    private Properties options = null;
    private String expired = null;
    private String wso2Token = null;

    @Before
    public void setup() throws Exception {

        ks = KeyStore.getInstance("JKS");
        ks.load(null, null); // srsly - this is necessary to initialize the keystore.
        options = new Properties();
        // Normally "wso2.org/products/am" => "http://wso2.org/claims/emailaddress"
        options.setProperty("jwt.key.sub.wso2 issuer", "http://wso2.org/claims/emailaddress"); 

        KeyPair testingKeyPair = generateKeyPair("testing");
        KeyPair wso2KeyPair = generateKeyPair("wso2");

        long expirationTime = System.currentTimeMillis() + 1000L * 60 * 60;
        //System.out.println("Expires at: " + expirationTime);

        token = generateToken(testingKeyPair, expirationTime, "testing issuer");
        wso2Token = generateToken(wso2KeyPair, expirationTime, "wso2 issuer");
        wrongSig = generateToken(wso2KeyPair, expirationTime, "testing issuer");
        expired = generateToken(wso2KeyPair, 0L, "testing issuer");
    }

    private String generateToken(KeyPair keyPair,
            long expirationTime, String issuer)
            throws NoSuchAlgorithmException, InvalidKeyException,
            SignatureException {
        JWTWriter writer = new JWTWriter();
        JWT.Builder builder = new JWT.Builder();
        builder = builder.setClaimsSetIssuer(issuer)
                        .setClaimsSetExpirationTime(expirationTime)
                        .setHeaderAlgorithm("SHA256withRSA")
                        .setHeaderType("JWT")
                        .setClaimsSetCustomField("sub", "Mr Somebody")
                        .setClaimsSetCustomField("http://wso2.org/claims/emailaddress", "somebody@somewhere.org");

        JWT unsigned = builder.build();
        String toSign = writer.write(unsigned);
        String[] pieces = toSign.split("\\.");
        Signature signing = Signature.getInstance("SHA256withRSA");

        signing.initSign(keyPair.getPrivate());
        signing.update(pieces[0].getBytes());
        signing.update(".".getBytes());
        signing.update(pieces[1].getBytes());

        byte[] signature = signing.sign();
        return writer.write(builder.setSignature(Base64.encodeBase64URLSafeString(signature)).build());
    }

    private KeyPair generateKeyPair(String alias)
            throws NoSuchAlgorithmException, Exception, KeyStoreException {
        options.setProperty("security.keystore.alias." + alias + " issuer", alias);
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512);
        KeyPair keyPair = keyGen.genKeyPair();
        X509Certificate cert = generateCertificate(keyPair);
        ks.setCertificateEntry(alias, cert);
        return keyPair;
    }

    @Test
    public void testVerify() throws Exception {
        JWTVerifier verifier = new JWTVerifier(ks, options);
        JWTVerifier.Verification resp = verifier.verify(token);
        assertEquals(resp.getIssuer(), "testing issuer");
        assertEquals(resp.getIdentity(), "Mr Somebody");
    }

    @Test
    public void testVerifyWSO2() throws Exception {
        JWTVerifier verifier = new JWTVerifier(ks, options);
        JWTVerifier.Verification resp = verifier.verify(wso2Token);
        assertEquals("wso2 issuer", resp.getIssuer());
        assertEquals("somebody@somewhere.org", resp.getIdentity());
    }

    @Test
    public void testNullToken() throws Exception {
        JWTVerifier verifier = new JWTVerifier(ks, options);
        try {
            verifier.verify(null);
            fail();
        } catch (VerificationError e) {
            // This is expected.
        }
    }

    @Test
    public void testBadToken() throws Exception {
        JWTVerifier verifier = new JWTVerifier(ks, options);
        try {
            verifier.verify(wrongSig);
            fail();
        } catch (VerificationError e) {
            // This is expected.
        }
    }

    @Test
    public void testExpiredToken() throws Exception {
        JWTVerifier verifier = new JWTVerifier(ks, options);
        try {
            verifier.verify(expired);
            fail();
        } catch (VerificationError e) {
            // This is expected.
        }
    }

    @Test
    public void testBlankToken() throws Exception {
        JWTVerifier verifier = new JWTVerifier(ks, options);
        try {
            verifier.verify("");
            fail();
        } catch (VerificationError e) {
            // This is expected.
        }
    }

    private X509Certificate generateCertificate(KeyPair keyPair) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        X509V3CertificateGenerator cert = new X509V3CertificateGenerator();
        cert.setSerialNumber(BigInteger.valueOf(1));   //or generate a random number  
        cert.setSubjectDN(new X509Principal("CN=localhost"));  //see examples to add O,OU etc  
        cert.setIssuerDN(new X509Principal("CN=localhost")); //same since it is self-signed  
        cert.setPublicKey(keyPair.getPublic());
        cert.setNotBefore(new Date());  
        cert.setNotAfter(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365));  
        cert.setSignatureAlgorithm("SHA1WithRSAEncryption");

        PrivateKey signingKey = keyPair.getPrivate();
        return cert.generate(signingKey, "BC");
     }

}
