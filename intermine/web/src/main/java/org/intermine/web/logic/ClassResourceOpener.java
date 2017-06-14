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
 * Objects that open resources relative to a classpath.
 * @author Alex Kalderimis
 *
 */
public class ClassResourceOpener implements ResourceOpener
{

    private final Class<?> clazz;

    /**
     * Constructor.
     * @param clazz The class relative to which to lookup resources.
     */
    public ClassResourceOpener(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public InputStream openResource(String resourceName) {
        return clazz.getResourceAsStream(resourceName);
    }


}
