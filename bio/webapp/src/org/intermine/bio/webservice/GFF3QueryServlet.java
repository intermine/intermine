package org.intermine.bio.webservice;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.webservice.server.WebService;

/**
 * A servlet to hand off to the GFF3-query-service.
 * @author Alex Kalderimis.
 *
 */
public class GFF3QueryServlet extends BioExportServlet
{

    private static final long serialVersionUID = 4561011192947331380L;

    @Override
    protected WebService getService() {
        return new GFFQueryService(api);
    }
}
