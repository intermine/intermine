package org.intermine.bio.webservice;

import org.intermine.webservice.server.WebService;

public class GenomicRegionSearchServlet extends BioExportServlet
{
    private static final long serialVersionUID = 1L;

    @Override
    protected WebService getService() {
        return new GenomicRegionSearchService(api);
    }
}
