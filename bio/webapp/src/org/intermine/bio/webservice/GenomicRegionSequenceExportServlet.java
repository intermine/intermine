package org.intermine.bio.webservice;

import org.intermine.webservice.server.WebService;

/**
 * A servlet to hand off to the GenomicRegionSequence query service.
 *
 * @author Fengyuan Hu
 *
 */
public class GenomicRegionSequenceExportServlet extends BioExportServlet
{

    private static final long serialVersionUID = -2778449163131613496L;

    @Override
    protected WebService getService() {
        return new GenomicRegionSequenceExportService(api);
    }
}
