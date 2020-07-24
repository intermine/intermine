package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2020 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.results.ResultElement;
import org.intermine.webservice.server.core.ResultProcessor;

import java.util.Iterator;
import java.util.List;

public class RDFProcessor extends ResultProcessor {

    @Override
    public void write(Iterator<List<ResultElement>> resultIt, Output output) {
        while (resultIt.hasNext())  {
            List<ResultElement> row = resultIt.next();
            //output.addResultItem(row);

        }
    }
}
