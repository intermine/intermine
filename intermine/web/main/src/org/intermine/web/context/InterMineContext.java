package org.intermine.web.context;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.intermine.api.InterMineAPI;
import org.intermine.util.Emailer;
import org.intermine.web.logic.config.WebConfig;

public class InterMineContext {

    public static final int WORKERS = 10;
    private static InterMineAPI im;
    private static Properties webProperties;
    private static WebConfig webConfig;
    private static boolean isInitialised = false;
    private static Emailer emailer;
    private static final Map<String, Object> ATTRIBUTES = new HashMap<String, Object>();
    private static KeyStore keyStore = null;
    private static final ArrayBlockingQueue<MailAction> MAIL_QUEUE
        = new ArrayBlockingQueue<MailAction>(10000);
    private static final ExecutorService MAIL_SERVICE
        = Executors.newCachedThreadPool(new DaemonThreadFactory());

    public static void initilise(final InterMineAPI imApi, Properties webProps, WebConfig wc) {
        isInitialised = true;
        im = imApi;
        webProperties = webProps;
        webConfig = wc;
        emailer = EmailerFactory.getEmailer(webProps);
        startMailerThreads(emailer);
    }

    private static void checkInit() {
        if (!isInitialised) {
            throw new ContextNotInitialisedException(
                "Attempt to access InterMineContext before it has  been initialised");
        }
    }

    public static InterMineAPI getInterMineAPI() {
        checkInit();
        return im;
    }

    public static WebConfig getWebConfig() {
        checkInit();
        return webConfig;
    }

    public static Properties getWebProperties() {
        checkInit();
        return webProperties;
    }

    public static Object getAttribute(String name) {
        return ATTRIBUTES.get(name);
    }

    public static void setAttribute(String name, Object value) {
        ATTRIBUTES.put(name, value);
    }

    public static boolean queueMessage(MailAction action) {
        return MAIL_QUEUE.offer(action);
    }

    /** Start a number of consumer threads that poll for messages to send from the mail queue **/
    private static void startMailerThreads(Emailer emailer) {
        for (int i = 0; i < WORKERS; i++) {
            Runnable r = new MailDaemon(MAIL_QUEUE, emailer);
            MAIL_SERVICE.submit(r);
        }
    }

    public static void shutdown() {
        checkInit();
        int leftToSend = MAIL_QUEUE.size();
        // Allocate more actors to send the remaining messages.
        for (int i = 0; i < leftToSend; i++) {
            Runnable r = new MailDaemon(MAIL_QUEUE, emailer);
            MAIL_SERVICE.submit(r);
        }
        // Tell the pool to close.
        MAIL_SERVICE.shutdown();
    }


    private static char[] getKeyStorePassword() {
        return getWebProperties().getProperty("security.keystore.password").toCharArray();
    }

    public static KeyStore getKeyStore()
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        checkInit();
        if (keyStore == null ) {
            keyStore = KeyStore.getInstance("JKS");
            InputStream is = null;
            try {
                is = InterMineContext.class.getResourceAsStream("keystore.jks");
                // Must call load, even on null values, to initialise the store.
                keyStore.load(is, getKeyStorePassword());
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // who honestly cares.
                    }
                }
            }
        }
        return keyStore;
    }
}
