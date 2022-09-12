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

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.PermanentURIHelper;
import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.core.JSONService;

/**
 * Generate a permanent URL given a type and internal InterMine ID
 * Permanent URLs are used in the Share button and to set the attribute 'url' in Schema.org
 *
 * The url returned will be empty in the following cases:
 * type is not defined in the model,
 * type is defined in the model but we are not able to generate a permanent URI
 * id is wrong
 *
 * @author Daniela Butano
 */
public class PermanentURLService extends JSONService
{

    /**
     * The constructor
     * @param im the intermine api
     */
    public PermanentURLService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String id = getRequiredParameter("id");
        String type = getOptionalParameter("type");
        String url = (new PermanentURIHelper(request)).getPermanentURL(Integer.parseInt(id));
        if (url == null) {
            addOutputInfo("url", StringUtils.EMPTY);
        } else {
            if (type != null && type.equals(WebServiceRequestParser.FORMAT_PARAMETER_RDF)) {
                url += ".rdf";
            }
            addOutputInfo("url", url);
        }
    }
}
