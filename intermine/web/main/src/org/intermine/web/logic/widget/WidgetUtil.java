package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;

/**
 * An Interface for widget Utils
 * @author "Xavier Watkins"
 *
 */
public interface WidgetUtil
{
    /**
     * Get the extra attributes needed for the DataSetLoader
     * @param os the objectstore
     * @param bag the bag
     * @return a collection of strings to pass to the datasetloader
     */
    public Collection<String> getExtraAttributes(ObjectStore os, InterMineBag bag);
}
