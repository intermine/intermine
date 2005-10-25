package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.Results;

import org.intermine.objectstore.ObjectStoreException;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

/**
 * Utility methods for the web package.
 *
 * @author Kim Rutherford
 */

public abstract class WebUtil
{
    protected static final Logger LOG = Logger.getLogger(WebUtil.class);

    /**
     * Lookup an Integer property from the SessionContext and return it.
     * @param session the current session
     * @param propertyName the property to find
     * @param defaultValue the value to return if the property isn't present
     * @return the int value of the property
     */
    public static int getIntSessionProperty(HttpSession session, String propertyName,
                                            int defaultValue) {
        Map webProperties =
            (Map) session.getServletContext().getAttribute(Constants.WEB_PROPERTIES);
        String maxBagSizeString = (String) webProperties.get(propertyName);

        int intVal = defaultValue;

        try {
            intVal = Integer.parseInt(maxBagSizeString);
        } catch (NumberFormatException e) {
            LOG.warn("Failed to parse " + propertyName + " property: " + maxBagSizeString);
        }

        return intVal;
    }

    /**
     * Convert an SQL LIKE/NOT LIKE expression to a * wildcard expression.
     *
     * @param exp  the wildcard expression
     * @return     the SQL LIKE parameter
     */
    public static String wildcardSqlToUser(String exp) {
        StringBuffer sb = new StringBuffer();

        Pattern pattern = Pattern.compile("(%|\\\\%|_|\\\\_|\\|\\*|\\?|.)");
        Matcher matcher = pattern.matcher(exp);
        
        while (matcher.find()) {
            String group = matcher.group();

            if (group.equals("%")) {
                sb.append("*");
            } else {
                if (group.equals("_")) {
                    sb.append("?");
                } else {
                    if (group.equals("\\%")) {
                        sb.append("%");
                    } else {
                        if (group.equals("\\_")) {
                            sb.append("_");
                        } else {
                            if (group.equals("*")) {
                                sb.append("\\*");
                            } else {
                                if (group.equals("?")) {
                                    sb.append("\\?");
                                } else {
                                    if (group.equals("\\")) {
                                        sb.append("\\\\");
                                    } else {
                                        sb.append(group);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Turn a user supplied wildcard expression with * into an SQL LIKE/NOT LIKE
     * expression with %'s.
     *
     * @param exp  the SQL LIKE parameter
     * @return     the equivalent wildcard expression
     */
    public static String wildcardUserToSql(String exp) {
        StringBuffer sb = new StringBuffer();

        Pattern pattern = Pattern.compile("(\\*|\\\\\\*|\\?|\\\\\\?|\\|%|_|.)");
        Matcher matcher = pattern.matcher(exp);
        
        while (matcher.find()) {
            String group = matcher.group();

            if (group.equals("*")) {
                sb.append("%");
            } else {
                if (group.equals("?")) {
                    sb.append("_");
                } else {
                    if (group.equals("\\*")) {
                        sb.append("*");
                    } else {
                        if (group.equals("\\?")) {
                            sb.append("?");
                        } else {
                            if (group.equals("%")) {
                                sb.append("\\%");
                            } else {
                                if (group.equals("_")) {
                                    sb.append("\\_");
                                } else {
                                    if (group.equals("\\")) {
                                        sb.append("\\\\");
                                    } else {
                                        sb.append(group);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return sb.toString();
    }

    /**
     * Make a copy of a Results object, but with a different batch size.
     * @throws ObjectStoreException 
     */
    public static Results changeResultBatchSize(Results oldResults, int newBatchSize)
        throws ObjectStoreException {
        Results newResults = oldResults.getObjectStore().execute(oldResults.getQuery());
        newResults.setBatchSize(newBatchSize);
        return newResults;
    }
}
