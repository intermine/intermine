package org.intermine.webservice.server.fair;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.InterMineAPI;
import org.intermine.web.fair.SemanticMarkupFormatter;
import org.intermine.webservice.server.core.JSONService;

/**
 * Serve bioentity markup to be added to the report page
 * @author Daniela Butano
 *
 */
public class BioEntityMarkupService extends JSONService
{
    /**
     * Constructor
     * @param im The InterMine state object.
     **/
    public BioEntityMarkupService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        int id = Integer.parseInt(getRequiredParameter("id"));
        addResultItem(SemanticMarkupFormatter.formatBioEntity(request, id), false);
    }

    @Override
    public String getResultsKey() {
        return "semantic-markups";
    }
}
