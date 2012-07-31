package org.intermine.common.swing;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Class to help with multiple <code>ResouceBundle</code>s. This class allows
 * multiple ResouceBundles to be registered, and when a message is
 * requested it searches them in order until the message is found or it is found
 * that none of the ResouceBundles contain the key.
 * 
 * @see ResourceBundle
 */
public final class Messages
{
    /**
     * The registered resouce bundles.
     */
    private static List<ResourceBundle> bundles = new ArrayList<ResourceBundle>(2); 
    
    private Messages() {
    }
    
    /**
     * Register the given resource bundle.
     * 
     * @param bundle The ResourceBundle to add.
     */
    public static void addResourceBundle(ResourceBundle bundle) {
        if (bundle != null) {
            bundles.add(bundle);
        }
    }
    
    /**
     * Get the message given by the key. The message is formatted by
     * {@link MessageFormat} if any further parameters are given.
     * 
     * @param key The message key.
     * @param params Parameters to format arguments in the message.
     * 
     * @return The formatted message.
     * 
     * @throws IllegalStateException if no bundles have been registered.
     * @throws MissingResourceException if no bundles contain a message
     * with the given key.
     */
    public static String getMessage(String key, Object... params) {
        if (bundles.isEmpty()) {
            throw new IllegalStateException("Resource bundles have not been set.");
        }
        
        String message = null;
        if (bundles.size() == 1) {
            message = bundles.get(0).getString(key);
            // Simply throw the original MissingResourceException.
        } else {
            MissingResourceException error = null;
            for (ResourceBundle bundle : bundles) {
                try {
                    message = bundle.getString(key);
                    break;
                } catch (MissingResourceException e) {
                    error = e;
                }
            }
            
            if (message == null) {
                String className = error == null ? null : error.getClassName();
                throw new MissingResourceException("Cannot locate message '" + key
                        + "' in any resource bundle.", className, key);
            }
        }
        
        assert message != null : "No MissingResourceException thrown, but message is null";
        if (params.length > 0) {
            message = MessageFormat.format(message, params);
        }
        return message;
    }
    public static boolean hasMessage(String key, Object... params) {
        if (bundles.isEmpty()) {
            throw new IllegalStateException("Resource bundles have not been set.");
        }
        boolean hasKey = false;
        MissingResourceException error = null;
        if (bundles.size() == 1) {
            try {
                hasKey = (bundles.get(0).getString(key) != null);
            } catch (MissingResourceException e) {
                error = e;
            }
        } else {
            for (ResourceBundle bundle : bundles) {
                try {
                    hasKey = (bundle.getString(key) != null);
                    break;
                } catch (MissingResourceException e) {
                    error = e;
                }
            }
        }
        return hasKey;
    }
}
