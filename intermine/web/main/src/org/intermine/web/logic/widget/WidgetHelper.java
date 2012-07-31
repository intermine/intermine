package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;

/**
 * An Interface for widget Utils
 * @author "Xavier Watkins"
 *
 */
public interface WidgetHelper
{
    /**
     * Get the extra attributes needed for the DataSetLoader
     * @param os the objectstore
     * @param bag the bag
     * @return a collection of strings to pass to the datasetloader
     */
    Collection<String> getExtraAttributes(ObjectStore os, InterMineBag bag);
}
