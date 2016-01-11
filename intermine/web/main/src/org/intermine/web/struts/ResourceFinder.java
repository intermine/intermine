package org.intermine.web.struts;

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
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.servlet.ServletContext;

import org.intermine.web.logic.ResourceOpener;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A class for finding resources in the WEB-INF directory at run-time based on patterns.
 * The purpose of this is to allow resources, particularly configuration files to be
 * found and used by giving them a name with a certain pattern, or by placing them in a certain
 * directory.
 *
 * @author Alex Kalderimis
 */
public class ResourceFinder implements ResourceOpener
{
    private final ServletContext context;
    private static final String WEB_INF = "/WEB-INF";

    /**
     * Constructor. Makes a ResourceFinder with reference to the current servlet context,
     * meaning that it knows were to look to find the WEB-INF directory.
     * @param context The servlet context.
     */
    public ResourceFinder(ServletContext context) {
        this.context = context;
        if (this.context == null) {
            throw new NullPointerException("context must not be null");
        }
    }

    @Override
    public InputStream openResource(String resourceName) {
        return context.getResourceAsStream(WEB_INF + "/" + resourceName);
    }

    /**
     * Find all resources matching a pattern in the WEB-INF directory.
     *
     * @param pattern The pattern to match.
     * @return A collection of resource names.
     */
    public Collection<String> findResourcesMatching(Pattern pattern) {
        final ArrayList<String> retval = new ArrayList<String>();
        String realWebInfPath = context.getRealPath(WEB_INF);
        File webInf = new File(realWebInfPath);
        retval.addAll(getResourcesFromDirectory(webInf, pattern));
        return retval;
    }

    private static Collection<String> getResourcesFromDirectory(
        final File directory,
        final Pattern pattern) {
        final ArrayList<String> retval = new ArrayList<String>();
        final File[] fileList = directory.listFiles();
        for (final File file : fileList) {
            if (file.isDirectory()) {
                retval.addAll(getResourcesFromDirectory(file, pattern));
            } else {
                try {
                    final String fileName = file.getCanonicalPath();
                    Matcher m = pattern.matcher(fileName);
                    final boolean accept = m.find();
                    if (accept) {
                        retval.add(m.group());
                    }
                } catch (final IOException e) {
                    throw new Error(e);
                }
            }
        }
        return retval;
    }
}

