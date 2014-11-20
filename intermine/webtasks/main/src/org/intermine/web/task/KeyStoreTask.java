package org.intermine.web.task;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Base task for tasks that require a key store.
 *
 * Tasks that inherit from this class inherit two required properties: <code>keystore</code>
 * and <code>options</options>.
 *
 * @author Alex Kalderimis
 *
 */
public abstract class KeyStoreTask extends Task {
    private static final String KEY_STORE_ERR = "Error creating key-store: ";
    private static final String KEYSTORE = "security.keystore.password";
    private String keystoreFile;
    private String optionsFile;
    private final Properties options = new Properties();

    /**
     * Bean-style setter for keystoreFile, as per ant spec.
     * @param filename The name of the file.
     */
    public void setKeystoreFile(String filename) {
        this.keystoreFile = filename;
    }

    /**
     * Bean-style setter for optionsFile, as per ant spec.
     * @param filename The filename.
     */
    public void setOptions(String filename) {
        this.optionsFile = filename;
    }

    private InputStream readKeystore() {
        File ksf = new File(keystoreFile);
        if (!ksf.exists() || !ksf.canRead()) {
            throw new BuildException(keystoreFile + " does not exist, or cannot be read");
        }
        try {
            return new FileInputStream(ksf);
        } catch (FileNotFoundException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Create and return the configured key-store.
     * @return The key store.
     */
    protected KeyStore createKeyStore() {
        char[] password = getOptions().getProperty(KEYSTORE).toCharArray();
        KeyStore ks;
        try {
            ks = KeyStore.getInstance("JKS");
        } catch (KeyStoreException e) {
            throw new BuildException(KEY_STORE_ERR + e.getMessage(), e);
        }
        InputStream is = readKeystore();
        try {
            ks.load(is, password);
        } catch (NoSuchAlgorithmException e) {
            throw new BuildException(KEY_STORE_ERR + e.getMessage(), e);
        } catch (CertificateException e) {
            throw new BuildException(KEY_STORE_ERR + e.getMessage(), e);
        } catch (IOException e) {
            throw new BuildException(KEY_STORE_ERR + e.getMessage(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logError("Error closing keystore");
                }
            }
        }
        return ks;
    }

    /**
     * Get the options as configured for this task
     * @return The project properties.
     */
    protected Properties getOptions() {
        if (options.isEmpty()) {
            InputStream is;
            try {
                is = new FileInputStream(new File(optionsFile));
            } catch (FileNotFoundException e) {
                throw new BuildException(optionsFile + " not found");
            }
            try {
                options.load(is);
            } catch (IOException e) {
                throw new BuildException();
            }
        }
        return options;
    }

    /**
     * Log a message to standard out.
     */
    protected static void logMessage(String message) {
        PrintStream out = System.out;
        out.println(message);
    }

    /**
     * Log a message to standard error.
     */
    protected static void logError(String message) {
        PrintStream out = System.err;
        out.println(message);
    }

}
