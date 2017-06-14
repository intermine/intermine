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

import java.io.IOException;
import java.io.InputStream;
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
import org.intermine.web.security.KeySigner.SigningException;

/**
 * Build a key store from some options and possibly a keystore file.
 *
 * The following properties are read - all properties are optional:
 * <ul>
 *   <li><code>project.title</code></li>
 *   <li><code>security.keystore.password</code></li>
 *   <li><code>security.privatekey.alias</code></li>
 *   <li><code>security.privatekey.password</code></li>
 *   <li><code>security.publickey.*</code></li>
 *   <li><code>keystore.strictpublickeydecoding</code></li>
 * </ul>
 *
 * The keystore file itself is optional.
 *
 * @author Alex Kalderimis
 *
 */
public class KeyStoreBuilder
{

    private static final String CANT_GEN_CERT = "Could not generate certificate for ";

    private static final Logger LOG = Logger.getLogger(KeyStoreBuilder.class);

    private static final String DEFAULT_TITLE                = "InterMine";
    private static final String PROJECT_TITLE                = "project.title";
    private static final String SECURITY_PRIVATEKEY_ALIAS    = "security.privatekey.alias";
    private static final String SECURITY_PRIVATEKEY_PASSWORD = "security.privatekey.password";
    private static final String KS_PASSWORD                  = "security.keystore.password";
    private static final String PREFIX                       = "security.publickey";
    private static final String STRICT_DECODING              = "keystore.strictpublickeydecoding";

    private Properties options;
    private ResourceOpener opener;

    /**
     * Construct a new KeyStoreBuilder.
     * @param options The options to read. NOT NULL
     * @param opener Where I can get access to the key store file. NOT NULL
     */
    public KeyStoreBuilder(Properties options, ResourceOpener opener) {
        this.options = options;
        this.opener = opener;
        if (options == null) {
            throw new NullPointerException("options must not be null");
        }
        if (opener == null) {
            throw new NullPointerException("opener must not be null");
        }
    }

    private char[] getKeyStorePassword() {
        String password = options.getProperty(KS_PASSWORD);
        if (password != null) {
            return password.toCharArray();
        }
        return null;
    }

    /**
     * Build the key store.
     * @return A fully initialised and configured key store.
     * @throws KeyStoreException If we cannnot instantiate the keystore.
     * @throws IOException If there are system problems.
     * @throws NoSuchAlgorithmException If the algorithm specified are wrong.
     * @throws CertificateException If we cannot generate certificates.
     */
    public KeyStore buildKeyStore() throws KeyStoreException, IOException,
            NoSuchAlgorithmException, CertificateException {
        KeyStore store = KeyStore.getInstance("JKS");
        InputStream is = null;
        try {
            is = opener.openResource("keystore.jks");
            if (is == null) {
                LOG.debug("NO KEYSTORE FOUND - initialising empty keystore");
            } else {
                LOG.debug("FOUND KEYSTORE");
            }
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
            LOG.info("Found " + publicKeys.size() + " encoded keys");
            String dn = "CN=" + options.getProperty(PROJECT_TITLE, DEFAULT_TITLE);
            dn = dn.replaceAll("\\s", "");
            int oneYear = 365;
            String alias = options.getProperty(SECURITY_PRIVATEKEY_ALIAS);
            PrivateKey signingKey = getOrGeneratePrivateKey(store, alias);
            KeySigner signer = new KeySigner(signingKey, dn, oneYear, KeySigner.DEFAULT_ALGORITHM);

            for (Entry<String, PublicKey> pair: publicKeys.entrySet()) {
                try {
                    store.setCertificateEntry(pair.getKey(),
                            signer.generateCertificate(pair.getKey(), pair.getValue()));
                } catch (SigningException e) {
                    throw new CertificateException(CANT_GEN_CERT + pair.getKey());
                }
            }
        }
        int n = 0;
        for (Enumeration<String> es = store.aliases(); es.hasMoreElements(); es.nextElement()) {
            n++;
        }
        LOG.debug("Finished configuring KEYSTORE - it contains " + n + " certificates");
        return store;
    }

    private PrivateKey getOrGeneratePrivateKey(KeyStore store, String alias)
        throws KeyStoreException, NoSuchAlgorithmException {
        PrivateKey signingKey = null;
        if (alias != null && store.containsAlias(alias)) {
            String password = options.getProperty(SECURITY_PRIVATEKEY_PASSWORD);
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
        Properties ps = PropertiesUtil.stripStart(PREFIX, options);
        KeyDecoder decoder = new Base64PublicKeyDecoder();
        boolean skipBadKeys = !"true".equalsIgnoreCase(options.getProperty(STRICT_DECODING));
        Map<String, PublicKey> retVal = new HashMap<String, PublicKey>();
        for (Enumeration<?> names = ps.propertyNames(); names.hasMoreElements();) {
            String name = (String) names.nextElement();
            LOG.info("found encoded key called " + name);
            try {
                PublicKey key = decoder.decode(ps.getProperty(name));
                retVal.put(name, key);
            } catch (DecodingException e) {
                String msg = "Could not decode key for " + name;
                if (skipBadKeys) {
                    LOG.error(msg);
                    continue;
                } else {
                    throw new RuntimeException(msg, e);
                }
            }
        }

        return retVal;

    }
}
