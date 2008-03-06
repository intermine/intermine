package org.intermine.web.logic.export;

import java.util.ArrayList;
import java.util.List;

import org.intermine.web.logic.results.ResultElement;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * @author Jakub Kulaviak
 **/
public class ExportTestUtil
{

    static List<ResultElement> getRow(int i, String string, String string2,
            String string3) {
        List<ResultElement> ret = new ArrayList<ResultElement>();
        ret.add(new ResultElement(i));
        ret.add(new ResultElement(string));
        ret.add(new ResultElement(string2));
        ret.add(new ResultElement(string3));
        return ret;
    }

}
