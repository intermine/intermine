package org.intermine.web.logic.export;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.intermine.api.results.ResultElement;
import org.intermine.pathquery.Path;

/**
 * @author Jakub Kulaviak
 **/
public class ResultElementConverter
{
    protected static final Logger LOG = Logger.getLogger(ResultElementConverter.class);

    /**
     * Converts data from ResultElement to Objects. It takes
     * field value from each ResultElement.
     * @param result result row to be converted
     * @param unionPathCollection union path from old and new views in pathquery
     * @param newPathCollection the paths to export
     * @return converted result row
     */
    public List<Object> convert(List<ResultElement> result,
            Collection<Path> unionPathCollection,
            Collection<Path> newPathCollection) {
        List<Object> ret = new ArrayList<Object>();

        if (newPathCollection != null && unionPathCollection != null
                && unionPathCollection.containsAll(newPathCollection)) {
            for (Path p : newPathCollection) {
                ResultElement el = result.get(((List<Path>) unionPathCollection).indexOf(p));
                if (el != null) {
                    ret.add(el.getField());
                } else {
                    ret.add(null);
                }
            }
        } else {
            for (ResultElement el : result) {
                if (el != null) {
                    ret.add(el.getField());
                } else {
                    ret.add(null);
                }
            }
        }

        return ret;
    }
}
