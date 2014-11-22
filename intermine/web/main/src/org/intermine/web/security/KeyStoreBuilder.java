package org.intermine.web.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.intermine.util.PropertiesUtil;
import org.intermine.web.logic.ResourceOpener;

public class KeyStoreBuilder {

	private static final Logger LOG = Logger.getLogger(KeyStoreBuilder.class);
    private static final String KS_PASSWORD = "security.keystore.password";
    
	private Properties options;
	private ResourceOpener opener;
    
    public KeyStoreBuilder(Properties options, ResourceOpener opener) {
    	this.options = options;
    	this.opener = opener;
    }

    private char[] getKeyStorePassword() {
        String password = options.getProperty(KS_PASSWORD);
        if (password != null) {
            return password.toCharArray();
        }
        return null;
    }

	public KeyStore buildKeyStore() throws KeyStoreException, IOException,
			NoSuchAlgorithmException, CertificateException {
		KeyStore store = KeyStore.getInstance("JKS");
		InputStream is = null;
		try {
		    is = opener.openResource("keystore.jks");
		    // Must call load, even on null values, to initialise the store.
		    store.load(is, getKeyStorePassword());
		} finally {
		    if (is != null) {
		        try {
		            is.close();
		        } catch (IOException e) {
		        	LOG.error("Error closing keystore resource", e);
		        }
		    }
		}
		Map<String, PublicKey> publicKeys = getConfiguredPublicKeys();
		if (!publicKeys.isEmpty()) {
			
			String dn = options.getProperty("project.title");
			int oneYear = 365;
			String alias = options.getProperty("security.privatekey.alias");
			PrivateKey signingKey = getOrGeneratePrivateKey(store, alias);
			KeySigner signer = new KeySigner(signingKey, dn, oneYear, "SHA256withRSA");

		    for (Entry<String, PublicKey> pair: publicKeys.entrySet()) {
		    	try {
					store.setCertificateEntry(pair.getKey(), signer.generateCertificate(pair.getValue()));
				} catch (GeneralSecurityException e) {
					LOG.error("Could not generate certificate for " + pair.getKey());
				}
		    }
		}
		return store;
	}

	private PrivateKey getOrGeneratePrivateKey(KeyStore store, String alias) throws KeyStoreException, NoSuchAlgorithmException {
		PrivateKey signingKey = null;
		if (alias != null && store.containsAlias(alias)) {
			String password = options.getProperty("security.privatekey.password");
			try {
				signingKey = (PrivateKey) store.getKey(alias,
						(password != null) ? password.toCharArray() : null);
			} catch (UnrecoverableKeyException e) {
				// ignore - just generate our own.
			}
		}
		// Not configured, or unrecoverable.
		if (signingKey == null) {
			// Generate a new random key pair, sign with that.
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		    keyGen.initialize(2048);
		    KeyPair keyPair = keyGen.genKeyPair();
		    signingKey = keyPair.getPrivate();
		}
		return signingKey;
	}

	private Map<String, PublicKey> getConfiguredPublicKeys() {
		String prefix = "security.publickey.";
		Properties ps = PropertiesUtil.getPropertiesStartingWith(prefix, options);
		ps = PropertiesUtil.stripStart(prefix, ps);
		KeyDecoder decoder = new Base64PublicKeyDecoder();
		Map<String, PublicKey> retVal = new HashMap<String, PublicKey>();
		for (Enumeration<?> names = ps.propertyNames(); names.hasMoreElements();) {
			String name = (String) names.nextElement();
			try {
				PublicKey key = decoder.decode(ps.getProperty(name));
				retVal.put(name, key);
			} catch (DecodingException e) {
				LOG.error("Could not decode key for " + name);
				continue;
			}
		}
		return retVal;

	}
}
