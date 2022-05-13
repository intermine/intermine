package org.intermine.webservice.server.fair;

/*
 * Copyright (C) 2002-2022 FlyMine
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
 * Serve markup to be added to the home page
 * @author Daniela Butano
 *
 */
public class HomePageMarkupService extends JSONService
{
    /**
     * Constructor
     * @param im The InterMine state object.
     **/
    public HomePageMarkupService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        if (SemanticMarkupFormatter.isEnabled()) {
            addResultItem(SemanticMarkupFormatter.formatInstance(request,
                    getPermission().getProfile()), false);
        }
    }

    @Override
    public String getResultsKey() {
        return "semantic-markups";
    }
}
