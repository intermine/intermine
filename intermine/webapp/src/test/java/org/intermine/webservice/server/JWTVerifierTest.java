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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.InvalidBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.util.DevNullMap;
import org.intermine.web.security.PublicKeySource;
import org.intermine.webservice.server.JWTVerifier.VerificationError;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JWTVerifierTest
{

	private static PrivateKey testingKey;
	private static PrivateKey wso2Key;
    private static PublicKeySource ks;

    private Properties options = null;
    private static Properties defaultOptions = new Properties();
    private String token, wso2Token, expired, wrongSig, unknown;
	private JWTVerifier verifier;

    @BeforeClass
    public static void createKeySource() throws Exception {
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    	keyGen.initialize(512);
    	Map<String, PublicKey> keys = new HashMap<String, PublicKey>();
    	
    	KeyPair testingKeyPair = keyGen.generateKeyPair();
    	testingKey = testingKeyPair.getPrivate();
    	keys.put("testing", testingKeyPair.getPublic());
    	defaultOptions.setProperty("security.keystore.alias.testing issuer", "testing");
    	
    	KeyPair wso2KeyPair = keyGen.generateKeyPair();
    	wso2Key = wso2KeyPair.getPrivate();
    	keys.put("wso2", wso2KeyPair.getPublic());
    	defaultOptions.setProperty("security.keystore.alias.wso2 issuer", "wso2");
    	
    	ks = new MapPublicKeySource(keys);
    }

	@Before
    public void setup() throws Exception {
        options = new Properties(defaultOptions);
        // Normally "wso2.org/products/am" => "http://wso2.org/claims/emailaddress"
        options.setProperty("jwt.key.sub.wso2 issuer", "http://wso2.org/claims/emailaddress");
        Profile profile = new FakeProfile("Mr Somebody", "somebody@somewhere.org");
        int expiry = 60 * 60;

        //System.out.println("Expires at: " + expirationTime);
        JWTBuilder testBuilder = new JWTBuilder(testingKey, "testing issuer");
        JWTBuilder wso2Builder = new JWTBuilder(wso2Key, "wso2 issuer");
        JWTBuilder unknownBuilder = new JWTBuilder(testingKey, "unknown issuer");
        JWTBuilder mismatchBuilder = new JWTBuilder(wso2Key, "testing issuer");

        token = testBuilder.issueToken(profile, expiry);
        wso2Token = wso2Builder.issueToken(profile, expiry);
        unknown = unknownBuilder.issueToken(profile, expiry);
        wrongSig = mismatchBuilder.issueToken(profile, expiry);
        expired = testBuilder.issueToken(profile.getUsername(), profile.getEmailAddress(), -1L);
        
        this.verifier = new JWTVerifier(ks, options);
    }

    @Test
    public void testVerify() throws Exception {
        JWTVerifier.Verification resp = verifier.verify(token);
        assertEquals(resp.getIssuer(), "testing issuer");
        assertEquals(resp.getIdentity(), "Mr Somebody");
    }

    @Test
    public void testVerifyWSO2() throws Exception {
        JWTVerifier.Verification resp = verifier.verify(wso2Token);
        assertEquals("wso2 issuer", resp.getIssuer());
        assertEquals("somebody@somewhere.org", resp.getIdentity());
    }

    @Test
    public void testNullToken() throws Exception {
        try {
            verifier.verify(null);
            fail();
        } catch (VerificationError e) {
            // This is expected.
        }
    }

    @Test
    public void testBadToken() throws Exception {
        try {
            verifier.verify(wrongSig);
            fail();
        } catch (VerificationError e) {
            // This is expected.
        }
    }

    @Test
    public void testUnknownIssuer() throws Exception {
        try {
            verifier.verify(unknown);
            fail();
        } catch (VerificationError e) {
            // This is expected.
        }
    }

    @Test
    public void testExpiredToken() throws Exception {
        try {
            verifier.verify(expired);
            fail();
        } catch (VerificationError e) {
            // This is expected.
        }
    }

    @Test
    public void testBlankToken() throws Exception {
        try {
            verifier.verify("");
            fail();
        } catch (VerificationError e) {
            // This is expected.
        }
    }

    // Implementation for issuing tokens to users by name.
    private class FakeProfile extends Profile
    {

        private String username, email;

        public FakeProfile(String username, String email) {
            super(null, null, null, null,
                    new HashMap<String, SavedQuery>(), new HashMap<String, InterMineBag>(),
                    new HashMap<String, ApiTemplate>(), null, true, false);
            savedQueries = new DevNullMap<String, SavedQuery>();
            savedBags = new DevNullMap<String, InterMineBag>();
            savedTemplates = new DevNullMap<String, ApiTemplate>();
            savedInvalidBags = new DevNullMap<String, InvalidBag>();
            queryHistory = new DevNullMap<String, SavedQuery>();
            savingDisabled = true;

            this.username = username;
            this.email = email;
        }

        @Override
        public String getUsername() {
            return this.username;
        }

        @Override
        public String getEmailAddress() {
            return this.email;
        }
    }

}
