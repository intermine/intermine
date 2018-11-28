package org.intermine.webservice.server.fair;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.InterMineAPI;
import org.intermine.web.fair.SemanticMarkupUtil;
import org.intermine.webservice.server.core.JSONService;

/**
 * Serve dataset markup to be added to the report page
 * @author Daniela Butano
 *
 */
public class DataSetMarkupService extends JSONService
{
    /**
     * Constructor
     * @param im The InterMine state object.
     **/
    public DataSetMarkupService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String dataSetName = getRequiredParameter("name");
        addResultItem(SemanticMarkupUtil.getDataSetMarkup(request, dataSetName), false);
    }

    @Override
    public String getResultsKey() {
        return "properties";
    }
}
