package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.List;

import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.core.JSONService;

/**
 * Serve up the list of schemata that we have.
 * @author Alexis Kalderimis
 *
 */
public class SchemaListService extends JSONService
{

    /**
     * Constructor
     * @param im InterMine settings
     */
    public SchemaListService(InterMineAPI im) {
        super(im);
    }

    /*
     * @see org.intermine.webservice.server.WebService#execute(
     * javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void execute() throws Exception {
        List<String> schemata =
            Arrays.asList(webProperties.getProperty("schema.filenames", "").split(","));

        addResultItem(schemata, false);
    }

    @Override
    protected String getResultsKey() {
        return "schemata";
    }

    @Override
    protected String getDefaultFileName() {
        return "schemata.json";
    }

}
