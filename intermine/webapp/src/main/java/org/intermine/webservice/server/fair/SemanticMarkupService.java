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
 * Serve semantic markup so that a client can add it to the home page
 * @author Daniela Butano
 *
 */
public class SemanticMarkupService extends JSONService
{
    /**
     * Constructor
     * @param im The InterMine state object.
     **/
    public SemanticMarkupService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        addResultItem(SemanticMarkupUtil.getDataCatalogueMarkup(request), false);
    }

    @Override
    public String getResultsKey() {
        return "properties";
    }
}
