package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;
import org.intermine.util.StringUtil;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathException;
import org.intermine.web.logic.results.WebState;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;

/**
 * Utility methods for the web package.
 *
 * @author Kim Rutherford
 * @author Julie Sullivan
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
        Properties webProperties = SessionMethods.getWebProperties(session.getServletContext());
        String n = webProperties.getProperty(propertyName);

        int intVal = defaultValue;

        try {
            intVal = Integer.parseInt(n);
        } catch (NumberFormatException e) {
            LOG.warn("Failed to parse " + propertyName + " property: " + n);
        }

        return intVal;
    }

    /**
     * takes a map and puts it in random order
     * also shortens the list to be map.size() = max
     * @param map The map to be randomised - the Map will be unchanged after the call
     * @param max the number of items to be in the final list
     * @param <V> the value type
     * @return the newly randomised, shortened map
     */
    public static <V> Map<String, V> shuffle(Map<String, V> map, int max) {
        List<String> keys = new ArrayList<String>(map.keySet());

        Collections.shuffle(keys);

        if (keys.size() > max) {
            keys = keys.subList(0, max);
        }

        Map<String, V> returnMap = new HashMap<String, V>();

        for (String key: keys) {
            returnMap.put(key, map.get(key));
        }
        return returnMap;
    }


    /**
     * Return the contents of the page given by prefixURLString + '/' + path as a String.  Any
     * relative links in the page will be modified to go via showStatic.do
     * @param prefixURLString the prefix (including "http://...") of the web site to read from.
     *    eg. http://www.flymine.org/doc/help
     * @param path the page to retrieve eg. manualFlyMineHome.shtml
     * @return the contents of the page
     * @throws IOException if there is a problem while reading
     */
    public static String getStaticPage(String prefixURLString, String path)
        throws IOException {
        StringBuffer buf = new StringBuffer();

        URL url = new URL(prefixURLString + '/' + path);
        URLConnection connection = url.openConnection();
        InputStream is = connection.getInputStream();
        Reader reader = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(reader);
        String line;
        while ((line = br.readLine()) != null) {
            // replace relative urls ie. href="manualExportfasta.shtml"
            line = line.replaceAll("href=\"([^\"]+)\"",
                                   "href=\"showStatic.do?path=$1\"");
            buf.append(line + "\n");
        }
        return buf.toString();
    }


    /**
     * Look at the current webapp page and subtab and return the help page and tab.
     * @param request the request object
     * @return the help page and tab
     */
    public static String[] getHelpPage(HttpServletRequest request) {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Properties webProps = SessionMethods.getWebProperties(servletContext);
        WebState webState = SessionMethods.getWebState(request.getSession());
        String pageName = (String) request.getAttribute("pageName");
        String subTab = webState.getSubtab("subtab" + pageName);

        String prop;
        if (subTab == null) {
            prop = webProps.getProperty("help.page." + pageName);
        } else {
            prop = webProps.getProperty("help.page." + pageName + "." + subTab);
        }

        if (prop == null) {
            return new String[0];
        }
        return StringUtil.split(prop, ":");
    }

    /**
     * Formats column name. Replaces " &gt; " with "&amp;nbsp;&amp;gt; ".
     * @param original original column name
     * @return modified string
     */
    public static String formatColumnName(String original) {
        // replaces all dots and colons but not dots with following space - they are probably
        // part of name, e.g. 'D. melanogaster'
        return original.replaceAll("&", "&amp;").replaceAll(" > ", "&nbsp;&gt; ")
            .replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    /**
     * Formats a column name, using the webconfig to produce configured labels.
     * EG: MRNA.scoreType --&gt; mRNA &gt; Score Type
     * @param original The column name (a path string) to format
     * @param request The request to use to get the configuration off.
     * @return A formatted column name
     */
    public static String formatPath(String original, HttpServletRequest request) {
        if (request == null) { 
            throw new IllegalArgumentException("request cannot be null");
        }
        final InterMineAPI im = SessionMethods.getInterMineAPI(request);
        final Model model = im.getModel();
        final WebConfig webConfig = SessionMethods.getWebConfig(request);
        return formatPath(original, model, webConfig);
    }

    public static List<String> formatPathQueryView(PathQuery pq, HttpServletRequest request) {
        if (request == null) { 
            throw new IllegalArgumentException("request cannot be null");
        }
        final WebConfig webConfig = SessionMethods.getWebConfig(request);
        List<String> formattedViews = new ArrayList<String>();
        for (String view : pq.getView()) {
            Path p;
            try {
                p = pq.makePath(view);
            } catch (PathException e) {
                throw new RuntimeException(e);
            }
            formattedViews.add(formatPath(p, webConfig));
        }
        return formattedViews;
    }

    /**
     * Formats a column name, using the webconfig to produce configured labels.
     * EG: MRNA.scoreType --&gt; mRNA &gt; Score Type
     * @param original The column name (a path string) to format
     * @param model The model to use to parse the string
     * @param webConfig The configuration to find labels in
     * @return A formatted column name
     */
    public static String formatPath(String original, Model model, WebConfig webConfig) {
        Path viewPath;
        try {
            viewPath = new Path(model, original);
        } catch (PathException e) {
            return original;
        }
        return formatPath(viewPath, webConfig);
    }

    /**
     * Formats a column name, using the webconfig to produce configured labels.
     * EG: MRNA.scoreType --&gt; mRNA &gt; Score Type
     * @param pathString A string representing a path to format
     * @param api the webapp configuration to aquire a model from
     * @param webConfig The configuration to find labels in
     * @return A formatted column name
     */
    public static String formatPath(String pathString, InterMineAPI api, WebConfig webConfig) {
        Path viewPath;
        try {
            viewPath = new Path(api.getModel(), pathString);
        } catch (PathException e) {
            return pathString;
        }
        return formatPath(viewPath, webConfig);
    }

    /**
     * Formats a column name, using the webconfig to produce configured labels.
     * EG: MRNA.scoreType --&gt; mRNA &gt; Score Type
     * @param viewColumn A path representing a column name
     * @param webConfig The configuration to find labels in
     * @return A formatted column name
     */
    public static String formatPath(Path viewColumn, WebConfig webConfig) {
        List<Path> parts = viewColumn.decomposePath();
        List<String> aliasedParts = new ArrayList<String>();
        for (Path p: parts) {
            if (p.isRootPath()) {
                ClassDescriptor cld = p.getStartClassDescriptor();
                Type type = webConfig.getTypes().get(cld.getName());
                if (type != null) {
                    aliasedParts.add(type.getDisplayName());
                } else {
                    aliasedParts.add(Type.getFormattedClassName(cld.getUnqualifiedName()));
                }
            } else {
                aliasedParts.add(formatField(p, webConfig));
            }
        }
        return StringUtils.join(aliasedParts, " > ");
    }
    
    public static String formatField(String s, InterMineAPI api, WebConfig webConfig) {
        if (StringUtils.isEmpty(s)) {
            return "";
        }
        Path viewPath;
        try {
            viewPath = new Path(api.getModel(), s);
        } catch (PathException e) {
            return s;
        }
        return formatField(viewPath, webConfig);
    }

    public static String formatField(Path p, WebConfig webConfig) {
        if (p == null) {
            return "";
        }
        FieldDescriptor fd = p.getEndFieldDescriptor();
        if (fd == null) {
            return "";
        }
        ClassDescriptor cld = (fd.isAttribute())
                              ? p.getLastClassDescriptor()
                              : p.getSecondLastClassDescriptor();

        FieldConfig fc = FieldConfigHelper.getFieldConfig(webConfig, cld, fd);
        if (fc != null) {
            return fc.getDisplayName();
        } else {
            return FieldConfig.getFormattedName(fd.getName());
        }
    }

    public static String formatFieldChain(String s, InterMineAPI api, WebConfig webConfig) {
        String fullPath = formatPath(s, api.getModel(), webConfig);
        if (StringUtils.isEmpty(fullPath)) {
            return fullPath;
        } else {
            int idx = fullPath.indexOf(">");
            if (idx != -1) {
                return fullPath.substring(idx + 1);
            }
        }
        return fullPath;
    }

}
