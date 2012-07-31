package org.intermine.webservice.server.core;

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

import org.intermine.objectstore.query.ResultsRow;


/**
 * Interface of parser that parses row of results
 * to list of strings.
 * @author Jakub Kulaviak
 **/
public interface ResultRowParser
{
    /**
     * Parse result row to list of strings.
     * @param resultsRow result row
     * @return list of strings
     */
    List<String> parse(ResultsRow<?> resultsRow);
}
