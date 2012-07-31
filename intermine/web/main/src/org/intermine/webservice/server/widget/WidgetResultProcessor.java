package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

/**
 * Interfaces for classes that are able to process the results that widgets return.
 * @author ajk59
 *
 */
public interface WidgetResultProcessor
{
    /**
     * Format a list of objects into a string representation.
     * @param row The widget's results.
     * @return A list of strings.
     */
    List<String> formatRow(List<Object> row);
}
