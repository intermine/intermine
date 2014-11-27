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
import org.intermine.util.ShutdownHook;
import org.intermine.util.Shutdownable;
import org.intermine.web.logic.config.WebConfig;

/**
 * A context object that doesn't require the session.
 *
 * TODO: use injection instead.
 *
 * @author rns
 *
 */
public final class InterMineContext implements Shutdownable
{
    private InterMineContext() {
        // Hidden constructor.
    }

    private static final int WORKERS = 10;
    private static InterMineAPI im;
    private static Properties webProperties;
    private static WebConfig webConfig;
    private static volatile boolean isInitialised = false;
    private static Emailer emailer;
    private static final Map<String, Object> ATTRIBUTES = new HashMap<String, Object>();
    private static KeyStore keyStore = null;
    private static ArrayBlockingQueue<MailAction> mailQueue;
    private static ExecutorService mailService;

    /**
     * Set up the Context with everything it needs.
     * @param imApi The application state.
     * @param webProps The application properties.
     * @param wc The application configuration.
     */
    public static synchronized void initilise(
            final InterMineAPI imApi,
            final Properties webProps,
            final WebConfig wc) {

        if (isInitialised) { // May be initialized multiple times in tests.
            doShutdown();
        }
        im = imApi;
        webProperties = webProps;
        webConfig = wc;
        emailer = EmailerFactory.getEmailer(webProps);
        mailQueue = new ArrayBlockingQueue<MailAction>(10000);
        mailService = Executors.newCachedThreadPool(new DaemonThreadFactory());
        startMailerThreads(emailer);
        ShutdownHook.registerObject(new InterMineContext());

        isInitialised = true;
    }

    private static void checkInit() {
        if (!isInitialised) {
            throw new ContextNotInitialisedException(
                "Attempt to access InterMineContext before it has  been initialised");
        }
    }

    /**
     * @return The InterMine state object.
     */
    public static InterMineAPI getInterMineAPI() {
        checkInit();
        return im;
    }

    /**
     * @return The structured application configuration.
     */
    public static WebConfig getWebConfig() {
        checkInit();
        return webConfig;
    }

    /**
     * @return The configured properties of the application.
     */
    public static Properties getWebProperties() {
        checkInit();
        return webProperties;
    }

    /**
     * Get the value of a stored attribute.
     * @param name The name to look up.
     * @return The value of the attribute (may be null).
     */
    public static Object getAttribute(String name) {
        return ATTRIBUTES.get(name);
    }

    /**
     * Set an attribute of the context.
     * @param name The name of teh attribute to store.
     * @param value The value of the attribute to store.
     */
    public static void setAttribute(String name, Object value) {
        ATTRIBUTES.put(name, value);
    }

    /**
     * Queue up a message to an emailer.
     * @param action The thing that wants to send an email.
     * @return Whether the action was successfully queued up.
     */
    public static boolean queueMessage(MailAction action) {
        return mailQueue.offer(action);
    }

    /** Start a number of consumer threads that poll for messages to send from the mail queue **/
    private static void startMailerThreads(Emailer emailer) {
        for (int i = 0; i < WORKERS; i++) {
            Runnable r = new MailDaemon(mailQueue, emailer);
            mailService.submit(r);
        }
    }

    /**
     * Send the signal that shutdown is happening - try and release resources.
     */
    public void shutdown() {
        checkInit();
        doShutdown();
    }

    /**
     * Send the signal that shutdown is happening - try and release resources.
     */
    public static void doShutdown() {
        if (mailQueue != null && mailService != null) {
            int leftToSend = mailQueue.size();
            // Allocate more actors to send the remaining messages.
            for (int i = 0; i < leftToSend; i++) {
                Runnable r = new MailDaemon(mailQueue, emailer);
                mailService.submit(r);
            }
            // Tell the pool to close.
            mailService.shutdownNow();
        }
        im = null;
        webProperties = null;
        webConfig = null;
        emailer = null;
        mailQueue = null;
        mailService = null;
        isInitialised = false;
    }


    private static char[] getKeyStorePassword() {
        return getWebProperties().getProperty("security.keystore.password").toCharArray();
    }

    /**
     * @return A key-store containing the keys we trust.
     * @throws KeyStoreException If this platform doesn't support JKS.
     * @throws NoSuchAlgorithmException If keys are stored with algos we don't know how to use.
     * @throws CertificateException If the certificate is wonky.
     * @throws IOException If we can't even access the file-system.
     */
    public static KeyStore getKeyStore()
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        checkInit();
        if (keyStore == null) {
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
