package org.flymine.metadata.presentation;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Iterator;

import org.flymine.util.TypeUtil;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;

/**
 * Add convenience methods to access Model for use with front end.
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */

public class DisplayModel
{

    protected Model model;

    /**
     * Construct a display model with model.
     *
     * @param model the model
     */
    public DisplayModel(Model model) {
        this.model = model;
    }

    /**
     * Return the metadata model.
     *
     * @return the model
     */
    public Model getModel() {
        return this.model;
    }

   /**
     * Get a Collection of fully qualified class names in this model (i.e. including
     * package name).
     * @return Collection of fully qualified class names
     */
    public Collection getClassNames() {
        return new TreeSet(model.getClassNames());
    }

   /**
     * Get a Collection of fully qualified class names in this model (i.e. including
     * package name).
     * @return Collection of fully qualified class names
     */
    public Collection getUnqualifiedClassNames() {
        Collection collection = new ArrayList();
        for (Iterator i = getClassNames().iterator(); i.hasNext();) {
            collection.add(TypeUtil.unqualifiedName((String) i.next()));
        }
        return collection;
    }
}
