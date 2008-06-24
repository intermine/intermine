package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

import org.intermine.model.InterMineObject;
import org.intermine.pathquery.PathQuery;

/**
 * Builds the query needed to generate the results for the user based on what they clicked on
 * in the widget.
 * @author Julie Sullivan
 */
public interface WidgetURLQuery
{
    /**
     * @param objects list of objects that were used in the widget
     * @return the query generated based on which records the user clicked on in the widget
     */
    public PathQuery generatePathQuery(Collection<InterMineObject> objects);

}
