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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.StringUtil;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.results.WebState;
import org.intermine.web.logic.session.SessionMethods;

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
     *
     * @param session
     *            the current session
     * @param propertyName
     *            the property to find
     * @param defaultValue
     *            the value to return if the property isn't present
     * @return the int value of the property
     */
    public static int getIntSessionProperty(final HttpSession session,
            final String propertyName, final int defaultValue) {
        final Properties webProperties = SessionMethods
                .getWebProperties(session.getServletContext());
        final String n = webProperties.getProperty(propertyName);

        int intVal = defaultValue;

        try {
            intVal = Integer.parseInt(n);
        } catch (final NumberFormatException e) {
            LOG.warn("Failed to parse " + propertyName + " property: " + n);
        }

        return intVal;
    }

    /**
     * takes a map and puts it in random order also shortens the list to be
     * map.size() = max
     *
     * @param map
     *            The map to be randomised - the Map will be unchanged after the
     *            call
     * @param max
     *            the number of items to be in the final list
     * @param <V>
     *            the value type
     * @return the newly randomised, shortened map
     */
    public static <V> Map<String, V> shuffle(final Map<String, V> map,
            final int max) {
        List<String> keys = new ArrayList<String>(map.keySet());

        Collections.shuffle(keys);

        if (keys.size() > max) {
            keys = keys.subList(0, max);
        }

        final Map<String, V> returnMap = new HashMap<String, V>();

        for (final String key : keys) {
            returnMap.put(key, map.get(key));
        }
        return returnMap;
    }

    /**
     * Return the contents of the page given by prefixURLString + '/' + path as
     * a String. Any relative links in the page will be modified to go via
     * showStatic.do
     *
     * @param prefixURLString
     *            the prefix (including "http://...") of the web site to read
     *            from. eg. http://www.flymine.org/doc/help
     * @param path
     *            the page to retrieve eg. manualFlyMineHome.shtml
     * @return the contents of the page
     * @throws IOException
     *             if there is a problem while reading
     */
    public static String getStaticPage(final String prefixURLString,
            final String path) throws IOException {
        final StringBuffer buf = new StringBuffer();

        final URL url = new URL(prefixURLString + '/' + path);
        final URLConnection connection = url.openConnection();
        final InputStream is = connection.getInputStream();
        final Reader reader = new InputStreamReader(is);
        final BufferedReader br = new BufferedReader(reader);
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
     * Look at the current webapp page and subtab and return the help page and
     * tab.
     *
     * @param request
     *            the request object
     * @return the help page and tab
     */
    public static String[] getHelpPage(final HttpServletRequest request) {
        final HttpSession session = request.getSession();
        final ServletContext servletContext = session.getServletContext();
        final Properties webProps = SessionMethods
                .getWebProperties(servletContext);
        final WebState webState = SessionMethods.getWebState(request
                .getSession());
        final String pageName = (String) request.getAttribute("pageName");
        final String subTab = webState.getSubtab("subtab" + pageName);

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
    
    public final static class HeadResource {
        private final String type;
        private final String url;
        private final String key;

        private HeadResource(String key, String type, String url) {
            this.key = key;
            this.type = type;
            this.url = url;
        }

        public String getKey() {
            return key;
        }
        
        public String getType() {
            return type;
        }

        public String getUrl() {
            return url;
        }
        
        public boolean getIsRelative() {
            return url.startsWith("/");
        }
        
        @Override
        public String toString() {
            return String.format("HeadResouce [type = %s, url = %s]", type, url);
        }
    }
    
    /**
     * Returns the resources for a particular section of the head element.
     * 
     * @param section The section this resource belongs in.
     * 
     * @return A list of page resources, which are the urls for these resources. 
     */
    public static List<HeadResource> getHeadResources(String section) {
        Properties webProperties = InterMineContext.getWebProperties();
        List<HeadResource> ret = new ArrayList<HeadResource>();
        for (String type: new String[]{ "css", "js" }) {
            String key = String.format("head.%s.%s", type, section);
            Properties matches = PropertiesUtil.getPropertiesStartingWith(key, webProperties);
            Set<Object> keys = new TreeSet<Object>(matches.keySet());
            for (Object o: keys) {
                String propName = String.valueOf(o);
                String value = matches.getProperty(propName);
                if (!(value.startsWith("/") || value.startsWith("http"))) {
                    value = String.format("/%s/%s", type, value);
                }
                HeadResource resource = new HeadResource(propName, type, value);
                ret.add(resource);
            }
        }
        return ret;
    }

    /**
     * Formats column name. Replaces " &gt; " with "&amp;nbsp;&amp;gt; ".
     *
     * @param original
     *            original column name
     * @return modified string
     */
    public static String formatColumnName(final String original) {
        // replaces all dots and colons but not dots with following space - they
        // are probably
        // part of name, e.g. 'D. melanogaster'
        return original.replaceAll("&", "&amp;")
                .replaceAll(" > ", "&nbsp;&gt; ").replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;");
    }

    /**
     * Formats a column name, using the webconfig to produce configured labels.
     * EG: MRNA.scoreType --&gt; mRNA &gt; Score Type
     *
     * @param original
     *            The column name (a path string) to format
     * @param request
     *            The request to use to get the configuration off.
     * @return A formatted column name
     */
    public static String formatPath(final String original,
            final HttpServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        final InterMineAPI im = SessionMethods.getInterMineAPI(request);
        final Model model = im.getModel();
        final WebConfig webConfig = SessionMethods.getWebConfig(request);
        return formatPath(original, model, webConfig);
    }

    /**
     * Format a query's view into a list of displayable strings, taking both
     * the query's path descriptions and the application's web configuration into
     * account.
     * @param pq The query to format
     * @param request The request to use to look up configuration from
     * @return A list of displayable strings
     */
    public static List<String> formatPathQueryView(final PathQuery pq,
            final HttpServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        final WebConfig webConfig = SessionMethods.getWebConfig(request);
        return formatPathQueryView(pq, webConfig);
    }

    /**
     * Format a query's view into a list of displayable strings, taking both
     * the query's path descriptions and the application's web configuration into
     * account.
     * @param pq The query to format
     * @param wc The configuration to use to find labels in
     * @return A list of displayable strings
     */
    public static List<String> formatPathQueryView(final PathQuery pq, final WebConfig wc) {
        final List<String> formattedViews = new ArrayList<String>();
        for (final String view : pq.getView()) {
            formattedViews.add(formatPathDescription(view, pq, wc));
        }
        return formattedViews;
    }

    /**
     * Formats a column name, using the webconfig to produce configured labels.
     * EG: MRNA.scoreType --&gt; mRNA &gt; Score Type
     *
     * @param original The column name (a path string) to format
     * @param model The model to use to parse the string
     * @param webConfig The configuration to find labels in
     * @return A formatted column name
     */
    public static String formatPath(final String original, final Model model,
            final WebConfig webConfig) {
        Path viewPath;
        try {
            viewPath = new Path(model, original);
        } catch (final PathException e) {
            return original;
        }
        return formatPath(viewPath, webConfig);
    }

    /**
     * Formats a column name, using the webconfig to produce configured labels.
     * EG: MRNA.scoreType --&gt; mRNA &gt; Score Type
     *
     * @param pathString
     *            A string representing a path to format
     * @param api
     *            the webapp configuration to aquire a model from
     * @param webConfig
     *            The configuration to find labels in
     * @return A formatted column name
     */
    public static String formatPath(final String pathString,
            final InterMineAPI api, final WebConfig webConfig) {
        Path viewPath;
        try {
            viewPath = new Path(api.getModel(), pathString);
        } catch (Throwable t) {
            // In all error cases, return the original string.
            return pathString;
        }
        return formatPath(viewPath, webConfig);
    }

    /**
     * Formats a column name, using the given query to construct a path according to the current
     * state of its subclasses.
     * @param path The path to format.
     * @param pq The query to use for path construction.
     * @param config The configuration to find labels in.
     * @return A nicely formatted string.
     */
    public static String formatPath(final String path, final PathQuery pq, final WebConfig config) {
        Path viewPath;
        try {
            viewPath = pq.makePath(path);
        } catch (Throwable t) {
            // In all error cases, return the original string.
            return path;
        }
        return formatPath(viewPath, config);
    }

    /**
     * Formats a column name, using the webconfig to produce configured labels.
     * EG: MRNA.scoreType --&gt; mRNA &gt; Score Type
     *
     * @param viewColumn
     *            A path representing a column name
     * @param webConfig
     *            The configuration to find labels in
     * @return A formatted column name
     */
    public static String formatPath(final Path viewColumn, final WebConfig webConfig) {
        final ClassDescriptor cd = viewColumn.getStartClassDescriptor();
        if (viewColumn.isRootPath()) {
            return formatClass(cd, webConfig);
        } else {
            return formatClass(cd, webConfig) + " > " + formatFieldChain(viewColumn, webConfig);
        }
    }

    /**
     * Formats a class name, using the web-config to produce configured labels.
     * @param cd The class to display.
     * @param config The web-configuration.
     * @return A nicely labelled string.
     */
    public static String formatClass(ClassDescriptor cd, WebConfig config) {
        Type type = config.getTypes().get(cd.getName());
        if (type == null) {
            return Type.getFormattedClassName(cd.getUnqualifiedName());
        } else {
            return type.getDisplayName();
        }
    }

    /**
     * Format a path into a displayable field name.
     *
     * eg: Employee.fullTime &rarr; Full Time
     *
     * @param s A path represented as a string
     * @param api The InterMine settings bundle
     * @param webConfig The Web Configuration
     * @return A displayable string
     */
    public static String formatField(final String s, final InterMineAPI api,
            final WebConfig webConfig) {
        if (StringUtils.isEmpty(s)) {
            return "";
        }
        Path viewPath;
        try {
            viewPath = new Path(api.getModel(), s);
        } catch (final PathException e) {
            return s;
        }
        return formatField(viewPath, webConfig);
    }

    /**
     * Format a path into a displayable field name.
     *
     * eg: Employee.fullTime &rarr; Full Time
     *
     * @param p A path
     * @param webConfig The Web Configuration
     * @return A displayable string
     */
    public static String formatField(final Path p, final WebConfig webConfig) {
        if (p == null) {
            return "";
        }

        final FieldDescriptor fd = p.getEndFieldDescriptor();
        if (fd == null) {
            return "";
        }
        final ClassDescriptor cld = fd.isAttribute()
                ? p.getLastClassDescriptor() : p.getSecondLastClassDescriptor();

        final FieldConfig fc = FieldConfigHelper.getFieldConfig(webConfig, cld,
                fd);
        if (fc != null) {
            return fc.getDisplayName();
        } else {
            return FieldConfig.getFormattedName(fd.getName());
        }
    }

    /**
     * Format a sequence of fields in a chain.
     * @param p The path representing the fields to format.
     * @param config The web-configuration.
     * @return A formatted string, without the root class.
     */
    public static String formatFieldChain(final Path p, final WebConfig config) {
        if (p == null) {
            return "";
        }
        final ClassDescriptor cd = p.getStartClassDescriptor();
        if (p.endIsAttribute()) {
            final Type type = config.getTypes().get(cd.getName());
            if (type != null) {
                final String pathString = p.getNoConstraintsString();
                final FieldConfig fcg = type.getFieldConfig(
                        pathString.substring(pathString.indexOf(".") + 1));
                if (fcg != null) {
                    return fcg.getDisplayName();
                }
            }
        }

        List<String> elems = p.getElements();
        String firstField = elems.get(0);
        if (firstField == null) {
            return ""; // shouldn't actually happen - but we shouldn't throw exceptions here.
        }
        FieldDescriptor fd = cd.getFieldDescriptorByName(firstField);
        final FieldConfig fc = FieldConfigHelper.getFieldConfig(config, cd, fd);
        String thisPart = "";
        if (fc != null) {
            thisPart = fc.getDisplayName();
        } else {
            thisPart = FieldConfig.getFormattedName(fd.getName());
        }
        if (elems.size() > 1) {
            String root = p.decomposePath().get(1).getLastClassDescriptor().getUnqualifiedName();
            String[] parts = p.toString().split("\\."); // use toString to get subclass info.
            int start = Math.min(2, parts.length - 1);
            String fields = StringUtils.join(Arrays.copyOfRange(parts, start, parts.length), ".");
            String nextPathString = root + "." + fields;
            Path newPath;
            try {
                newPath = new Path(p.getModel(), nextPathString);
            } catch (PathException e) {
                newPath = null;
            }
            return thisPart + " > " + formatFieldChain(newPath, config);
        } else {
            return thisPart;
        }
    }

    /**
     * Format a path represented as a string to the formatted fields, without
     * the class name.
     *
     * So <code>Employee.department.manager.age<code> becomes
     * <code>Department > Manager > Years Alive</code>
     *
     * @param s The path string
     * @param api The InterMine API to use for model lookup.
     * @param webConfig The class name configuration.
     * @return A nicely formatted string.
     */
    public static String formatFieldChain(final String s,
            final InterMineAPI api, final WebConfig webConfig) {
        final String fullPath = formatPath(s, api.getModel(), webConfig);
        if (StringUtils.isEmpty(fullPath)) {
            return fullPath;
        } else {
            final int idx = fullPath.indexOf(">");
            if (idx != -1) {
                return fullPath.substring(idx + 1);
            }
        }
        return fullPath;
    }

    private static String replaceDescribedPart(final String s,
            final Map<String, String> descriptions) {
        final String retval = descriptions.get(s);
        if (retval == null) {
            final int lastDot = s.lastIndexOf('.');
            if (lastDot == -1) {
                return s;
            } else {
                return replaceDescribedPart(s.substring(0, lastDot),
                        descriptions) + " > " + s.substring(lastDot + 1);
            }
        } else {
            return retval;
        }
    }

    /**
     * Return a string suitable for displaying a PathQuery's path, taking any
     * path descriptions it has configured into account.
     *
     * @param s The path to display
     * @param pq The PathQuery it relates to
     * @param config The Web-Configuration to use to lookup labels
     * @return A string suitable for external display.
     */
    public static String formatPathDescription(final String s,
            final PathQuery pq, final WebConfig config) {
        Path p;
        try {
            p = pq.makePath(s);
        } catch (final PathException e) {
            return formatPath(s, pq.getModel(), config); // Format it nicely
            // anyway
        }

        return formatPathDescription(p, pq, config);
    }

    /**
     * Return a string suitable for displaying a PathQuery's path, taking any
     * path descriptions it has configured into account.
     *
     * @param p The path to display
     * @param pq The PathQuery it relates to
     * @param config The Web-Configuration to use to lookup labels
     * @return A string suitable for external display.
     */
    public static String formatPathDescription(final Path p, final PathQuery pq,
            final WebConfig config) {
        final Map<String, String> descriptions = pq.getDescriptions();
        final String withLabels = formatPath(p, config);
        final List<String> labeledParts = Arrays.asList(StringUtils
                .splitByWholeSeparator(withLabels, " > "));

        if (descriptions.isEmpty()) {
            return StringUtils.join(labeledParts, " > ");
        }
        final String withReplaceMents = replaceDescribedPart(
                p.getNoConstraintsString(), descriptions);
        final List<String> originalParts = Arrays.asList(
                StringUtils.split(p.getNoConstraintsString(),
                '.'));
        final int originalPartsSize = originalParts.size();
        final List<String> replacedParts = Arrays.asList(StringUtils
                .splitByWholeSeparator(withReplaceMents, " > "));
        final int replacedSize = replacedParts.size();

        // Else there are some described path segments...
        int partsToKeepFromOriginal = 0;
        int partsToTakeFromReplaced = replacedSize;
        for (int i = 0; i < originalPartsSize; i++) {
            final String fromOriginal = originalParts.get(originalPartsSize
                    - (i + 1));
            final int replaceMentsIndex = replacedSize - (i + 1);
            final String fromReplacement = replaceMentsIndex > 0 ? replacedParts
                    .get(replaceMentsIndex) : null;
            if (fromOriginal != null && fromOriginal.equals(fromReplacement)) {
                partsToKeepFromOriginal++;
                partsToTakeFromReplaced--;
            }
        }
        final List<String> returners = new ArrayList<String>();
        if (partsToTakeFromReplaced > 0) {
            returners.addAll(replacedParts.subList(0, partsToTakeFromReplaced));
        }
        if (partsToKeepFromOriginal > 0) {
            final int start
                = Math.max(0, labeledParts.size() - partsToKeepFromOriginal);
            final int end
                = start + partsToKeepFromOriginal - (originalPartsSize - labeledParts.size());
            returners.addAll(labeledParts.subList(start, end));
        }

        return StringUtils.join(returners, " > ");
    }
}
