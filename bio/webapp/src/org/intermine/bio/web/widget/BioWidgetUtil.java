package org.intermine.bio.web.widget;

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
import org.intermine.bio.util.BioUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.widget.WidgetHelper;

/**
 * Utility methods for the flymine package.
 * @author Julie Sullivan
 */
public class BioWidgetUtil implements WidgetHelper
{

    /**
     * Constructor (required for widgets)
     */
    public BioWidgetUtil() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<String> getExtraAttributes(ObjectStore os, InterMineBag bag) {
        return BioUtil.getOrganisms(os, bag, false);
    }

}
