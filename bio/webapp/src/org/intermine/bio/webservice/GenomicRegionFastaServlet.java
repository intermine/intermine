package org.intermine.bio.webservice;

import org.intermine.webservice.server.WebService;

public class GenomicRegionFastaServlet extends BioExportServlet {

    private static final long serialVersionUID = -5575628803247840902L;

    @Override
    protected WebService getService() {
        return new GenomicRegionFastaService(api);
    }
}
