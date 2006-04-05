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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
     * Gets the cache directory.
     * @param servletContext the servlet context
     * @return cache directory
     */
    public static File getCacheDirectory(ServletContext servletContext) {
        Properties p = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String dir = p.getProperty("webapp.cachedir");
        if (StringUtils.isEmpty(dir)) {
            throw new RuntimeException("Please define webapp.cachedir in your build properties");
        }
        File cacheDir = new File(dir);
        if (!cacheDir.exists()) {
            throw new RuntimeException("No such directory: " + cacheDir.getPath());
        }
        String version = p.getProperty("project.releaseVersion");
        cacheDir = new File(cacheDir, version);
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdir()) {
                throw new RuntimeException("Failed to create directory: " + cacheDir.getPath());
            }
        }
        return cacheDir;
    }
    
    /**
     * Given an image on disk, write the image to the client. Assumes content type from
     * the file extensions.
     * @param imgFile image file
     * @param response the http response object
     * @throws IOException if something goes wrong
     */
    public static void sendImageFile(File imgFile, HttpServletResponse response)
        throws IOException {
        String type = StringUtils.substringAfterLast(imgFile.getName(), ".");
        response.setContentType("image/" + type);
        IOUtils.copy(new FileReader(imgFile), response.getOutputStream());
    }

    /**
     * Convert an SQL LIKE/NOT LIKE expression to a * wildcard expression.
     *
     * @param exp  the wildcard expression
     * @return     the SQL LIKE parameter
     */
    public static String wildcardSqlToUser(String exp) {
        StringBuffer sb = new StringBuffer();

        // To quote a '%' in PostgreSQL we need to pass \\% because it strips one level of
        // backslashes when parsing a string and another when parsing a LIKE expression.
        // Java needs backslashes to be backslashed in strings, hence all the blashslashes below
        // see. http://www.postgresql.org/docs/7.3/static/functions-matching.html

        for (int i = 0; i < exp.length(); i++) {
            String substring = exp.substring(i);
            if (substring.startsWith("%")) {
                sb.append("*");
            } else {
                if (substring.startsWith("_")) {
                    sb.append("?");
                } else {
                    if (substring.startsWith("\\\\%")) {
                        sb.append("%");
                        i += 2;
                    } else {
                        if (substring.startsWith("\\\\_")) {
                            sb.append("_");
                            i += 2;
                        } else {
                            if (substring.startsWith("*")) {
                                sb.append("\\*");
                            } else {
                                if (substring.startsWith("?")) {
                                    sb.append("\\?");
                                } else {
                                    if (substring.startsWith("\\\\\\\\")) {
                                        i += 3;
                                        sb.append("\\\\");
                                    } else {
                                        sb.append(substring.charAt(0));
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

        for (int i = 0; i < exp.length(); i++) {
            String substring = exp.substring(i);
            if (substring.startsWith("*")) {
                sb.append("%");
            } else {
                if (substring.startsWith("?")) {
                    sb.append("_");
                } else {
                    if (substring.startsWith("\\*")) {
                        sb.append("*");
                        i++;
                    } else {
                        if (substring.startsWith("\\?")) {
                            sb.append("?");
                            i++;
                        } else {
                            if (substring.startsWith("%")) {
                                sb.append("\\\\%");
                            } else {
                                if (substring.startsWith("_")) {
                                    sb.append("\\\\_");
                                } else {
                                    if (substring.startsWith("\\")) {
                                        sb.append("\\\\\\\\");
                                        i++;
                                    } else {
                                        sb.append(substring.charAt(0));
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
     * @param oldResults the original Results objects
     * @param newBatchSize the new batch size
     * @return a new Results object with a new batch size
     * @throws ObjectStoreException if there is a problem while creating the new Results object
     */
    public static Results changeResultBatchSize(Results oldResults, int newBatchSize)
        throws ObjectStoreException {
        Results newResults = oldResults.getObjectStore().execute(oldResults.getQuery());
        newResults.setBatchSize(newBatchSize);
        return newResults;
    }
}
