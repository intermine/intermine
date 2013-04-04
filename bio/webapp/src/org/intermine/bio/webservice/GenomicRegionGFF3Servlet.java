package org.intermine.bio.webservice;

import org.intermine.webservice.server.WebService;

public class GenomicRegionGFF3Servlet extends BioExportServlet
{
    private static final long serialVersionUID = -4817002066081168037L;

    protected WebService getService() {
        return new GenomicRegionGFF3Service(api);
    }
}
