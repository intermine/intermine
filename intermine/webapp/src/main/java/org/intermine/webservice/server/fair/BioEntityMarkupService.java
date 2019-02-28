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

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.web.fair.SemanticMarkupUtil;
import org.intermine.webservice.server.core.JSONService;

import java.util.HashMap;

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
        String entityType = getRequiredParameter("type");
        int id = Integer.parseInt(getRequiredParameter("id"));
        addResultItem(SemanticMarkupUtil.getBioEntityMarkup(request, entityType, id), false);
    }

    @Override
    public String getResultsKey() {
        return "properties";
    }
}
