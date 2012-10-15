package org.intermine.web.context;

import java.util.Properties;

import org.intermine.api.InterMineAPI;
import org.intermine.util.Emailer;
import org.intermine.web.logic.config.WebConfig;

public class InterMineContext {

    private static InterMineAPI im;
    private static Properties webProperties;
    private static WebConfig webConfig;
    private static boolean isInitialised = false;
	private static Emailer emailer;


    public static void initilise(final InterMineAPI imApi, Properties webProps,
            WebConfig wc) {
        isInitialised = true;
        im = imApi;
        webProperties = webProps;
        webConfig = wc;
        emailer = new Emailer(webProps);
    }
    
    public static Emailer getEmailer() {
    	return emailer;
    }

    private static void checkInit() {
        if (!isInitialised) {
        throw new ContextNotInitialisedException("Attempt to access InterMineContext before it has"
                + " been initialised");
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

}
