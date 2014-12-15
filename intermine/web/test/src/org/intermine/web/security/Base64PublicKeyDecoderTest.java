/**
 * 
 */
package org.intermine.web.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.security.KeyPairGenerator;
import java.security.PublicKey;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;

/**
 * @author alex
 *
 */
public class Base64PublicKeyDecoderTest {

	private PublicKey key;
	private String encoded;
	private Base64PublicKeyDecoder decoder;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		Base64 encoder = new Base64();
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		this.key = keyGen.generateKeyPair().getPublic();
		this.encoded = encoder.encodeToString(key.getEncoded());
		this.decoder = new Base64PublicKeyDecoder();
	}

	@Test
	public void test() throws Exception {
		PublicKey decoded = decoder.decode(encoded);
		assertEquals(decoded, key);
	}
	
	@Test
	public void testException() throws Exception {
		try {
			decoder.decode("not a key");
			fail("Should have thrown an exception here");
		} catch (DecodingException e) {
			// all good.
		}
	}

}
