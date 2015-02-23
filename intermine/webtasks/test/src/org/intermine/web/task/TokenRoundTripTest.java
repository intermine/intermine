package org.intermine.web.task;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.BuildException;
import org.intermine.web.security.KeySigner;
import org.intermine.webservice.server.JWTVerifier;
import org.junit.Before;
import org.junit.Test;

/**
 * @author alex
 *
 */
public class TokenRoundTripTest {

    private static final String DN = "CN=unittest";
    private IssueTokenTask issuer;
    private VerifyTokenTask verifier;
    private ByteArrayOutputStream verifierMessages;
    private ByteArrayOutputStream issuerMessages;
    private String buildDir;
    private File defaultStrategy;
    private File anyAlias;
    private File badConf;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        defaultStrategy = File.createTempFile("options", "properties");
        anyAlias = File.createTempFile("options", "properties");
        badConf = File.createTempFile("options", "properties");
        Properties options = new Properties();
        options.setProperty(IssueTokenTask.PUBLICIDENTITY, "unittest");
        options.setProperty(IssueTokenTask.PRIVATEKEY_ALIAS, "SELF");
        options.setProperty(IssueTokenTask.PRIVATEKEY_PASSWORD, "intermine");
        options.setProperty("security.keystore.password", "intermine");

        // Without a strategy or an alias mapping, the config is wrong.
        writeProperties(badConf, options);
        options.setProperty("security.keystore.alias.unittest", "SELF");
        writeProperties(defaultStrategy, options);
        options.remove("security.keystore.alias.unittest");
        options.setProperty(JWTVerifier.VERIFICATION_STRATEGY, "ANY");
        writeProperties(anyAlias, options);


        // Create the key store
        File keyStoreFile = File.createTempFile("keystore", "jks");
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512);
        KeyPair keyPair = keyGen.genKeyPair();
        KeySigner selfIssuer =
                new KeySigner(keyPair.getPrivate(), DN, 365, KeySigner.DEFAULT_ALGORITHM);
        Certificate cert = selfIssuer.generateCertificate("unittest", keyPair.getPublic());
        Certificate[] chain = new Certificate[]{ cert };
        ks.setKeyEntry("SELF", keyPair.getPrivate(), "intermine".toCharArray(), chain);
        OutputStream ksf = new FileOutputStream(keyStoreFile);
        ks.store(ksf, "intermine".toCharArray());
        ksf.close();

        buildDir = System.getProperty("java.io.tmpdir");

        issuer = new IssueTokenTask();
        issuer.setBuildDir(buildDir);
        issuer.setKeystoreFile(keyStoreFile.getAbsolutePath());
        issuer.setOptions(defaultStrategy.getAbsolutePath());
        issuer.setEmail("someone@somewhere.com");
        issuer.setExpiry("100");
        issuer.setSubject("someone");

        verifier = new VerifyTokenTask();
        verifier.setKeystoreFile(keyStoreFile.getAbsolutePath());

        verifierMessages = new ByteArrayOutputStream();
        issuerMessages = new ByteArrayOutputStream();
        issuer.setOut(issuerMessages);
        verifier.setOut(verifierMessages);

    }

    private void writeProperties(File anyAlias, Properties options)
            throws FileNotFoundException, IOException {
        OutputStream propf = new FileOutputStream(anyAlias);
        options.store(propf, "Automatically generated for TokenRoundTripTest");
        propf.close();
    }

    @Test
    public void testMatchingStrategy() throws Exception {
        issuer.execute();
        assertEquals("Writing token to " + buildDir  + "/someone.jwt",
                issuerMessages.toString().trim());
        File token = new File(buildDir, "someone.jwt");
        assertTrue("The file was created correctly", token.exists());

        FileInputStream is = new FileInputStream(token);
        String jwt = IOUtils.toString(is);

        verifier.setOptions(defaultStrategy.getAbsolutePath());
        verifier.setToken(jwt);
        verifier.execute();
        assertEquals("IDENTITY = someone<someone@somewhere.com>",
                verifierMessages.toString().trim());
    }

    @Test
    public void testAnyStrategy() throws Exception {
        issuer.execute();
        assertEquals("Writing token to " + buildDir  + "/someone.jwt",
                issuerMessages.toString().trim());
        File token = new File(buildDir, "someone.jwt");
        assertTrue("The file was created correctly", token.exists());

        FileInputStream is = new FileInputStream(token);
        String jwt = IOUtils.toString(is);

        verifier.setOptions(anyAlias.getAbsolutePath());
        verifier.setToken(jwt);
        verifier.execute();
        assertEquals("IDENTITY = someone<someone@somewhere.com>",
                verifierMessages.toString().trim());
    }

    @Test
    public void testBadConfig() throws Exception {
        issuer.execute();
        assertEquals("Writing token to " + buildDir  + "/someone.jwt",
                issuerMessages.toString().trim());
        File token = new File(buildDir, "someone.jwt");
        assertTrue("The file was created correctly", token.exists());

        FileInputStream is = new FileInputStream(token);
        String jwt = IOUtils.toString(is);

        verifier.setOptions(badConf.getAbsolutePath());
        verifier.setToken(jwt);
        try {
            verifier.execute();
            fail("Should have failed");
        } catch (BuildException e) {
            // Expected behaviour.
        }
    }
}
