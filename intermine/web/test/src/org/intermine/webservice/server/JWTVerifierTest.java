package org.intermine.webservice.server;

import static org.junit.Assert.assertEquals;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.intermine.webservice.server.JWTVerifier.VerificationError;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class JWTVerifierTest {

    private static KeyPair testingKeyPair;
    private static KeyPair wso2KeyPair;
    private static KeyStore ks = null;
    
    private Properties options = null;
    private static Properties defaultOptions = new Properties();
    private String token, wso2Token, expired, wrongSig, unknown;

    @BeforeClass
    public static void setupOnce() throws Exception {
        ks = KeyStore.getInstance("JKS");
        ks.load(null, null); // srsly - this is necessary to initialize the keystore.
        testingKeyPair = generateKeyPair("testing");
        wso2KeyPair = generateKeyPair("wso2");
    }

    @Before
    public void setup() throws Exception {
        options = new Properties(defaultOptions);
        // Normally "wso2.org/products/am" => "http://wso2.org/claims/emailaddress"
        options.setProperty("jwt.key.sub.wso2 issuer", "http://wso2.org/claims/emailaddress"); 

        long expirationTime = System.currentTimeMillis() + 1000L * 60 * 60;
        //System.out.println("Expires at: " + expirationTime);

        token = generateToken(testingKeyPair, expirationTime, "testing issuer");
        wso2Token = generateToken(wso2KeyPair, expirationTime, "wso2 issuer");
        unknown = generateToken(testingKeyPair, expirationTime, "unknown issuer");
        wrongSig = generateToken(wso2KeyPair, expirationTime, "testing issuer");
        expired = generateToken(wso2KeyPair, 0L, "testing issuer");
    }

    private String generateToken(KeyPair keyPair, long expirationTime, String issuer)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Map<String, Object> header = new HashMap<String, Object>();
        Map<String, Object> claims = new HashMap<String, Object>();

        header.put("alg", "SHA256withRSA");
        header.put("typ", "JWT");
        claims.put("sub", "Mr Somebody");
        claims.put("iss", issuer);
        claims.put("exp", expirationTime);
        claims.put("iat", System.currentTimeMillis());
        claims.put("http://wso2.org/claims/emailaddress", "somebody@somewhere.org");

        String toSign = String.format("%s.%s",
                Base64.encodeBase64URLSafeString(new JSONObject(header).toString().getBytes()),
                Base64.encodeBase64URLSafeString(new JSONObject(claims).toString().getBytes()));

        Signature signing = Signature.getInstance("SHA256withRSA");

        signing.initSign(keyPair.getPrivate());
        signing.update(toSign.getBytes());

        byte[] signature = signing.sign();
        return toSign + "." + Base64.encodeBase64URLSafeString(signature);
    }

    private static KeyPair generateKeyPair(String alias)
            throws NoSuchAlgorithmException, Exception, KeyStoreException {
        defaultOptions.setProperty("security.keystore.alias." + alias + " issuer", alias);
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
    public void testUnknownIssuer() throws Exception {
        JWTVerifier verifier = new JWTVerifier(ks, options);
        try {
            verifier.verify(unknown);
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

    // Yes it is deprecated. It also generates self-signed certificates. So not exactly an example
    // of great things to do in production.
    private static X509Certificate generateCertificate(KeyPair keyPair) throws Exception {
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
