package org.intermine.web.logic.export;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import org.intermine.api.results.ResultElement;


/**
 * @author Jakub Kulaviak
 **/
public class ResultElementConverter
{

    /**
     * Converts data from ResultElement to Objects. It takes
     * field value from each ResultElement.
     * @param result result row to be converted
     * @return converted result row
     */
    public List<Object> convert(List<ResultElement> result) {
        List<Object> ret = new ArrayList<Object>();
        for (ResultElement el : result) {
            if (el != null) {
                ret.add(el.getField());
            } else {
                ret.add(null);
            }
        }
        return ret;
    }
}
