package org.intermine.web.task;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.web.logic.ResourceOpener;
import org.intermine.web.security.KeyStoreBuilder;

/**
 * Base task for tasks that require a key store.
 *
 * Tasks that inherit from this class inherit two required properties:
 * <code>keystore</code> and <code>options</code>.
 *
 * @author Alex Kalderimis
 *
 */
public abstract class KeyStoreTask extends Task
{
    private static final String KEY_STORE_ERR = "Error creating key-store: ";
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
        KeyStoreBuilder builder = new KeyStoreBuilder(getOptions(), getOpener());
        try {
            return builder.buildKeyStore();
        } catch (KeyStoreException e) {
            throw new BuildException(KEY_STORE_ERR + e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new BuildException(KEY_STORE_ERR + e.getMessage(), e);
        } catch (CertificateException e) {
            throw new BuildException(KEY_STORE_ERR + e.getMessage(), e);
        } catch (IOException e) {
            throw new BuildException(KEY_STORE_ERR + e.getMessage(), e);
        }
    }

    private ResourceOpener getOpener() {
        // An opener that only opens one resource - the keystore.
        return new ResourceOpener() {

            @Override
            public InputStream openResource(String resourceName) {
                return readKeystore();
            }
        };
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

    private PrintStream out = System.out;
    private PrintStream err = System.err;

    /**
     * Set the output handler.
     * @param os Where to send output.
     */
    public void setOut(OutputStream os) {
        out = new PrintStream(os);
    }

    /**
     * Set the error handler
     * @param os Where to send error messages.
     */
    public void setErr(OutputStream os) {
        err = new PrintStream(os);
    }

    /**
     * Log a message to standard out.
     * @param message The message to print.
     */
    protected void logMessage(String message) {
        out.println(message);
    }

    /**
     * Log a message to standard error.
     * @param message The message to print.
     */
    protected void logError(String message) {
        err.println(message);
    }

}
