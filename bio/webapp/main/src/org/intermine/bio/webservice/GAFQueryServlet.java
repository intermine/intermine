package org.intermine.bio.webservice;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.webservice.server.WebService;

/**
 * A servlet to hand off to the GAF query service.
 *
 * @author Alex Kalderimis
 *
 */
public class GAFQueryServlet extends BioExportServlet
{
    private static final long serialVersionUID = 1L;

    @Override
    protected WebService getService() {
        return new GAFQueryService(api);
    }

}
