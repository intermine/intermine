package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

/**
 * The type of objects that can process enrichment results for serialisation
 * to the outside world.
 * @author Alex Kalderimis
 *
 */
public interface EnrichmentResultProcessor
{
    /**
     * Format a row of results.
     * @param row The enrichment result row.
     * @return A suitable stringy output.
     */
    List<String> formatRow(List<Object> row);
}
