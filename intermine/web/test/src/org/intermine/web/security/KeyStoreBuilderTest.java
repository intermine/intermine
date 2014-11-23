package org.intermine.web.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.intermine.web.logic.ResourceOpener;
import org.junit.Before;
import org.junit.Test;

public class KeyStoreBuilderTest
{

	public class ConstantOpener implements ResourceOpener
	{

		private InputStream value;
	
		public ConstantOpener(InputStream value) {
			this.value = value;
		}
		@Override
		public InputStream openResource(String resourceName) {
			if ("keystore.jks".equals(resourceName)) {
				return value;
			}
			return null;
		}

	}

	private ConstantOpener nullOpener;
	private PublicKey key, key2;
	private String encoded, encoded2;;

	@Before
	public void setUp() throws Exception {
		Base64 encoder = new Base64();
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		this.key = keyGen.generateKeyPair().getPublic();
		this.key2 = keyGen.generateKeyPair().getPublic();
		this.encoded = encoder.encodeToString(key.getEncoded());
		this.encoded2 = encoder.encodeToString(key2.getEncoded());
		this.nullOpener = new ConstantOpener(null);
	}

	@Test
	public void testUnconfigured() throws Exception {
		// Supplying no file and no options is legal - you get an empty key-store.
		KeyStoreBuilder builder = new KeyStoreBuilder(new Properties(), nullOpener);
		KeyStore store = builder.buildKeyStore();
		Enumeration<String> aliases = store.aliases();
		assertFalse(aliases.hasMoreElements());
	}

	@Test
	public void testKeyInProperties() throws Exception {
		Properties options = new Properties();
		options.setProperty("security.publickey.test", encoded);
		KeyStoreBuilder builder = new KeyStoreBuilder(options, nullOpener);
		KeyStore store = builder.buildKeyStore();
		Enumeration<String> aliases = store.aliases();
		assertTrue("There is at least one alias", aliases.hasMoreElements());
		assertEquals("test", aliases.nextElement());
		Certificate cert = store.getCertificate("test");
		assertNotNull("Could not find certificate", cert);
		assertEquals(key, cert.getPublicKey());
	}
	
	@Test
	public void testMultipleKeysInProperties() throws Exception {
		Properties options = new Properties();
		options.setProperty("security.publickey.test1", encoded);
		options.setProperty("security.publickey.test2", encoded2);
		KeyStoreBuilder builder = new KeyStoreBuilder(options, nullOpener);
		KeyStore store = builder.buildKeyStore();
		
		Enumeration<String> aliases = store.aliases();
		assertTrue("There is at least one alias", aliases.hasMoreElements());

		Certificate cert = store.getCertificate("test1");
		assertNotNull("Could not find certificate", cert);
		assertEquals(key, cert.getPublicKey());
		
		Certificate cert2 = store.getCertificate("test2");
		assertNotNull("Could not find certificate", cert);
		assertEquals(key2, cert2.getPublicKey());
	}
	
	@Test
	public void testKeyInPropertiesLaxDecoding() throws Exception {
		Properties options = new Properties();
		options.setProperty("security.publickey.test", "NOT A KEY");
		KeyStoreBuilder builder = new KeyStoreBuilder(options, nullOpener);
		KeyStore store = builder.buildKeyStore();
		Enumeration<String> aliases = store.aliases();
		assertFalse(aliases.hasMoreElements());
	}
	
	@Test
	public void testMixedKeysInProperties() throws Exception {
		Properties options = new Properties();
		options.setProperty("security.publickey.good", encoded);
		options.setProperty("security.publickey.bad", "NOT A KEY");
		KeyStoreBuilder builder = new KeyStoreBuilder(options, nullOpener);
		KeyStore store = builder.buildKeyStore();
		Enumeration<String> aliases = store.aliases();
		assertTrue("There is at least one alias", aliases.hasMoreElements());
		assertEquals("good", aliases.nextElement());
		assertFalse("There are no more than one aliases", aliases.hasMoreElements());
		Certificate cert = store.getCertificate("good");
		assertNotNull("Could not find certificate", cert);
		assertEquals(key, cert.getPublicKey());
		assertNull(store.getCertificate("bad"));
	}
	
	@Test
	public void testKeyInPropertiesStrictDecoding() throws Exception {
		Properties options = new Properties();
		options.setProperty("security.publickey.test", "NOT A KEY");
		options.setProperty("keystore.strictpublickeydecoding", "true");
		KeyStoreBuilder builder = new KeyStoreBuilder(options, nullOpener);
		try {
			builder.buildKeyStore();
			fail("Expected an exception");
		} catch (RuntimeException e) {
			assertEquals("Could not decode key for test", e.getMessage());
		}
	}
	
	@Test
	public void testKeyInProtectedKeyStore() throws Exception {
		String password = "pa$$w0rd";
		
		// Build a key-store from properties which we then serialise and read in.
		Properties options = new Properties();
		options.setProperty("security.publickey.test", encoded);
		KeyStoreBuilder builder = new KeyStoreBuilder(options, nullOpener);
		KeyStore source = builder.buildKeyStore();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		source.store(os, password.toCharArray());
		ResourceOpener opener = new ConstantOpener(new ByteArrayInputStream(os.toByteArray()));
		
		options = new Properties();
		options.setProperty("security.keystore.password", password);
		builder = new KeyStoreBuilder(options, opener);
		KeyStore store = builder.buildKeyStore();
		
		Certificate cert = store.getCertificate("test");
		assertNotNull("Could not find certificate", cert);
		assertEquals(key, cert.getPublicKey());
	}
	
	@Test
	public void testKeyInUnprotectedKeyStore() throws Exception {
		
		// Build a key-store from properties which we then serialise and read in.
		Properties options = new Properties();
		options.setProperty("security.publickey.test", encoded);
		KeyStoreBuilder builder = new KeyStoreBuilder(options, nullOpener);
		KeyStore source = builder.buildKeyStore();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		source.store(os, new char[0]);
		ResourceOpener opener = new ConstantOpener(new ByteArrayInputStream(os.toByteArray()));
		
		options = new Properties();
		builder = new KeyStoreBuilder(options, opener);
		KeyStore store = builder.buildKeyStore();
		
		Certificate cert = store.getCertificate("test");
		assertNotNull("Could not find certificate", cert);
		assertEquals(key, cert.getPublicKey());
	}
	
	@Test
	public void testKeyStoreAndProperties() throws Exception {
		String password = "s3kvre";
		// Build a key-store from properties which we then serialise and read in.
		Properties options = new Properties();
		options.setProperty("security.publickey.from-store", encoded);
		KeyStoreBuilder builder = new KeyStoreBuilder(options, nullOpener);
		KeyStore source = builder.buildKeyStore();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		source.store(os, password.toCharArray());
		ResourceOpener opener = new ConstantOpener(new ByteArrayInputStream(os.toByteArray()));
		
		options = new Properties();
		options.setProperty("security.keystore.password", password);
		options.setProperty("security.publickey.from-properties", encoded2);
		builder = new KeyStoreBuilder(options, opener);
		KeyStore store = builder.buildKeyStore();
		
		Certificate cert = store.getCertificate("from-store");
		assertNotNull("Could not find certificate", cert);
		assertEquals(key, cert.getPublicKey());
		
		Certificate cert2 = store.getCertificate("from-properties");
		assertNotNull("Could not find certificate", cert2);
		assertEquals(key2, cert2.getPublicKey());
	}

}
