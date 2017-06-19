package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;

/**
 * The type of objects that can open resources by names.
 * @author Alex Kalderimis
 *
 */
public interface ResourceOpener
{

    /**
     * Open a resource by name, and return an InputStream to it.
     * @param resourceName The name of the resource.
     * @return The open input stream.
     */
    InputStream openResource(String resourceName);
}
