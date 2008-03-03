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

import org.intermine.web.logic.query.PathQuery;

/**
 * Builds the query needed to generate the results for the user based on what they clicked on
 * in the widget.
 * @author Julie Sullivan
 */
public interface WidgetURLQuery
{
    /**
     * @return the query generated based on which records the user clicked on in the widget
     */
    public PathQuery generatePathQuery();

}
