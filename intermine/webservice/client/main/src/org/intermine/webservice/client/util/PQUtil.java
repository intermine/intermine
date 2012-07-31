package org.intermine.webservice.client.util;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;

/**
 * Static methods to make certain path-query based operations easier.
 * @author Alex Kalderimis
 *
 */
public final class PQUtil
{

    private PQUtil() {
        // hidden constructor.
    }

    /**
     * Get a list of all the path-strings for the attributes of a given class.
     * @param m The model to use for field look-ups.
     * @param path The path to add a star to. In the most trivial case, this is
     * just the name of the class, but it can have any number of fields descending
     * from it.
     * @return A Collection of path-strings.
     */
    public static Collection<String> getStar(Model m, String path) {
        Path p;
        try {
            p = new Path(m, path);
        } catch (PathException e) {
            throw new IllegalArgumentException("Illegal path while selecting *", e);
        }
        ClassDescriptor cld = p.getLastClassDescriptor();
        List<String> star = new ArrayList<String>();
        for (AttributeDescriptor ad: cld.getAllAttributeDescriptors()) {
            star.add(path + "." + ad.getName());
        }
        return star;
    }
}
