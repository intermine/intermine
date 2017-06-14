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

import static org.junit.Assert.assertEquals;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Alex Kalderimis
 *
 */
public class KeySignerTest {

	private static final String ALIAS = "foo";
	private PrivateKey signingKey;
	private PublicKey trustedKey;
	private KeySigner signer;
	private KeyStore store;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
	    keyGen.initialize(512); // short keys means faster tests.
	    
	    // For the purposes of these tests it is very important that the
	    // signing key and the signed key are not related, since that is how
	    // it would be used in the wild.
	    KeyPair keyPair = keyGen.generateKeyPair();
	    this.signingKey = keyPair.getPrivate();
	    
	    KeyPair keyPair2 = keyGen.generateKeyPair();
	    this.trustedKey = keyPair2.getPublic();
	    String dn = "CN=Test, L=London, C=GB";
	    
	    this.signer = new KeySigner(signingKey, dn, 1, KeySigner.DEFAULT_ALGORITHM);
	    this.store = KeyStore.getInstance(KeyStore.getDefaultType());
	    this.store.load(null, null);
	}

	@Test
	public void testPutYourCertificateInPullYourCertificateOut() throws Exception {
		X509Certificate cert = signer.generateCertificate("trusted", trustedKey);
		store.setCertificateEntry(ALIAS, cert);
		Certificate retrieved = store.getCertificate(ALIAS);
		PublicKey fromStore = retrieved.getPublicKey();
		assertEquals(fromStore, trustedKey);
	}

}
